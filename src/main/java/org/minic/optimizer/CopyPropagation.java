package org.minic.optimizer;

import org.minic.ast.*;
import java.util.*;

public class CopyPropagation {
    
    private static class CopyInfo {
        ExpressionNode value;
        int useCount;  // Cuántas veces se ha usado para propagación
        boolean canPropagate;  // Si todavía se puede propagar
        
        CopyInfo(ExpressionNode value) {
            this.value = value;
            this.useCount = 0;
            this.canPropagate = true;
        }
        
        // Dejar de propagar después de cierto número de usos
        // o si el valor es complejo
        boolean shouldPropagate() {
            return canPropagate && useCount < 3 && isSimpleValue(value);
        }
        
        private boolean isSimpleValue(ExpressionNode expr) {
            return expr instanceof NumberNode || 
                   expr instanceof BooleanNode ||
                   expr instanceof CharNode ||
                   expr instanceof StringNode ||
                   (expr instanceof VariableNode && !isModified((VariableNode) expr));
        }
        
        private boolean isModified(VariableNode var) {
            // En una implementación completa, deberías llevar
            // seguimiento de qué variables son modificadas
            return false; // Por ahora asumimos que no
        }
    }
    
    public static AstNode optimize(AstNode ast) {
        if (ast instanceof ProgramNode) {
            return optimizeProgram((ProgramNode) ast);
        }
        return ast;
    }
    
    private static ProgramNode optimizeProgram(ProgramNode program) {
        List<AstNode> optimizedDecls = new ArrayList<>();
        
        for (AstNode decl : program.getChildren()) {
            if (decl instanceof FunctionNode) {
                optimizedDecls.add(optimizeFunction((FunctionNode) decl));
            } else {
                optimizedDecls.add(decl);
            }
        }
        
        return new ProgramNode(optimizedDecls);
    }
    
    private static FunctionNode optimizeFunction(FunctionNode function) {
        Map<String, CopyInfo> copies = new HashMap<>();
        Set<String> modifiedVars = new HashSet<>();
        BlockNode optimizedBody = propagateInBlock(function.getBody(), copies, modifiedVars);
        
        return new FunctionNode(
            function.getReturnType(),
            function.getName(),
            function.getParameters(),
            optimizedBody
        );
    }
    
    private static BlockNode propagateInBlock(BlockNode block, Map<String, CopyInfo> copies, Set<String> modifiedVars) {
        List<StatementNode> optimizedStatements = new ArrayList<>();
        
        for (StatementNode stmt : block.getStatements()) {
            StatementNode optimized = propagateInStatement(stmt, copies, modifiedVars);
            if (optimized != null) {
                optimizedStatements.add(optimized);
                // Actualizar variables modificadas
                trackModifiedVars(optimized, modifiedVars);
            }
        }
        
        return new BlockNode(optimizedStatements);
    }
    
    private static void trackModifiedVars(StatementNode stmt, Set<String> modifiedVars) {
        if (stmt instanceof AssignmentNode) {
            AssignmentNode assign = (AssignmentNode) stmt;
            if (assign.getTarget() instanceof VariableNode) {
                String varName = ((VariableNode) assign.getTarget()).getName();
                modifiedVars.add(varName);
                // Si una variable es modificada, ya no se puede propagar su valor
            }
        } else if (stmt instanceof VarDeclStatementNode) {
            // Las declaraciones no modifican variables existentes
        }
        // Otros statements pueden modificar variables indirectamente
    }
    
    private static StatementNode propagateInStatement(StatementNode stmt, Map<String, CopyInfo> copies, Set<String> modifiedVars) {
        if (stmt instanceof AssignmentNode) {
            return propagateInAssignment((AssignmentNode) stmt, copies, modifiedVars);
        } else if (stmt instanceof VarDeclStatementNode) {
            return propagateInVarDecl((VarDeclStatementNode) stmt, copies, modifiedVars);
        } else if (stmt instanceof IfNode) {
            return propagateInIf((IfNode) stmt, copies, modifiedVars);
        } else if (stmt instanceof WhileNode) {
            return propagateInWhile((WhileNode) stmt, copies, modifiedVars);
        } else if (stmt instanceof ForNode) {
            return propagateInFor((ForNode) stmt, copies, modifiedVars);
        } else if (stmt instanceof ReturnNode) {
            return propagateInReturn((ReturnNode) stmt, copies, modifiedVars);
        } else if (stmt instanceof ExpressionStatementNode) {
            return propagateInExpressionStmt((ExpressionStatementNode) stmt, copies, modifiedVars);
        }
        
        return stmt;
    }
    
    private static StatementNode propagateInAssignment(AssignmentNode assign, Map<String, CopyInfo> copies, Set<String> modifiedVars) {
        ExpressionNode target = assign.getTarget();
        ExpressionNode value = propagateInExpression(assign.getValue(), copies, modifiedVars);
        
        if (target instanceof VariableNode) {
            String targetName = ((VariableNode) target).getName();
            
            // Si la variable de destino está siendo modificada,
            // invalidar cualquier copia que apunte a ella
            invalidateCopiesTo(targetName, copies);
            
            // Registrar nueva copia si es apropiado
            if (isPropagatableValue(value)) {
                copies.put(targetName, new CopyInfo(value));
            } else {
                copies.remove(targetName); // No propagar valores complejos
            }
            
            // Marcar como modificada
            modifiedVars.add(targetName);
        }
        
        return new AssignmentNode(target, value);
    }
    
    private static void invalidateCopiesTo(String varName, Map<String, CopyInfo> copies) {
        // Invalidar copias que apunten a esta variable
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, CopyInfo> entry : copies.entrySet()) {
            CopyInfo info = entry.getValue();
            if (info.value instanceof VariableNode) {
                String sourceName = ((VariableNode) info.value).getName();
                if (sourceName.equals(varName)) {
                    toRemove.add(entry.getKey());
                }
            }
        }
        for (String key : toRemove) {
            copies.remove(key);
        }
    }
    
    private static boolean isPropagatableValue(ExpressionNode value) {
        return value instanceof NumberNode ||
               value instanceof BooleanNode ||
               value instanceof CharNode ||
               value instanceof StringNode ||
               (value instanceof VariableNode && isSimpleVariable((VariableNode) value));
    }
    
    private static boolean isSimpleVariable(VariableNode var) {
        // Variables simples que no son arreglos ni punteros
        return true; // Simplificado
    }
    
    private static ExpressionNode propagateInExpression(ExpressionNode expr, Map<String, CopyInfo> copies, Set<String> modifiedVars) {
        if (expr instanceof VariableNode) {
            String varName = ((VariableNode) expr).getName();
            CopyInfo info = copies.get(varName);
            
            if (info != null && info.shouldPropagate() && !modifiedVars.contains(varName)) {
                info.useCount++; 
                return info.value.cloneNode();
            }
            return expr;
            
        } else if (expr instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) expr;
            ExpressionNode left = propagateInExpression(binOp.getLeft(), copies, modifiedVars);
            ExpressionNode right = propagateInExpression(binOp.getRight(), copies, modifiedVars);
            
            // Constant folding simple si ambos son constantes
            if (left instanceof NumberNode && right instanceof NumberNode) {
                NumberNode leftNum = (NumberNode) left;
                NumberNode rightNum = (NumberNode) right;
                int result = evaluateBinaryOp(binOp.getOperator(), leftNum.getValue(), rightNum.getValue());
                return new NumberNode(result);
            }
            
            return new BinaryOpNode(binOp.getOperator(), left, right);
            
        } else if (expr instanceof UnaryOpNode) {
            UnaryOpNode unary = (UnaryOpNode) expr;
            ExpressionNode operand = propagateInExpression(unary.getOperand(), copies, modifiedVars);
            
            // Constant folding para operadores unarios
            if (operand instanceof NumberNode) {
                NumberNode num = (NumberNode) operand;
                int result = evaluateUnaryOp(unary.getOperator(), num.getValue());
                return new NumberNode(result);
            }
            
            return new UnaryOpNode(unary.getOperator(), operand);
            
        } else if (expr instanceof FunctionCallNode) {
            // No propagar dentro de llamadas a funciones (pueden tener efectos secundarios)
            return expr;
        }
        
        return expr;
    }
    
    private static int evaluateBinaryOp(String operator, int left, int right) {
        switch (operator) {
            case "+": return left + right;
            case "-": return left - right;
            case "*": return left * right;
            case "/": return right != 0 ? left / right : 0;
            case "%": return right != 0 ? left % right : 0;
            default: return 0;
        }
    }
    
    private static int evaluateUnaryOp(String operator, int operand) {
        switch (operator) {
            case "-": return -operand;
            case "!": return operand == 0 ? 1 : 0;
            default: return operand;
        }
    }
    
    private static StatementNode propagateInVarDecl(VarDeclStatementNode varDeclStmt, Map<String, CopyInfo> copies, Set<String> modifiedVars) {
        VarDeclNode varDecl = varDeclStmt.getVarDeclNode();
        VarDeclNode newVarDecl = new VarDeclNode(varDecl.getType(), varDecl.getName());
        newVarDecl.setArray(varDecl.isArray());
        newVarDecl.setArraySize(varDecl.getArraySize());
        
        if (varDecl.hasInitialNode()) {
            ExpressionNode initValue = propagateInExpression(varDecl.getInitialNode(), copies, modifiedVars);
            newVarDecl.setInitialNode(initValue);
            
            // Registrar como copia si es propagable
            if (isPropagatableValue(initValue)) {
                copies.put(varDecl.getName(), new CopyInfo(initValue));
            }
        }
        
        return new VarDeclStatementNode(newVarDecl);
    }
    
    private static StatementNode propagateInIf(IfNode ifStmt, Map<String, CopyInfo> copies, Set<String> modifiedVars) {
        ExpressionNode condition = propagateInExpression(ifStmt.getCondition(), copies, modifiedVars);
        
        // Crear copias locales para cada rama
        Map<String, CopyInfo> thenCopies = new HashMap<>(copies);
        Map<String, CopyInfo> elseCopies = new HashMap<>(copies);
        Set<String> thenModified = new HashSet<>(modifiedVars);
        Set<String> elseModified = new HashSet<>(modifiedVars);
        
        StatementNode thenBlock = propagateInStatement(ifStmt.getThenBlock(), thenCopies, thenModified);
        StatementNode elseBlock = ifStmt.getElseBlock() != null ? 
            propagateInStatement(ifStmt.getElseBlock(), elseCopies, elseModified) : null;
        
        // Fusionar copias que sean iguales en ambas ramas
        mergeCopies(copies, thenCopies, elseCopies);
        
        return new IfNode(condition, thenBlock, elseBlock);
    }
    
    private static void mergeCopies(Map<String, CopyInfo> result, 
                                   Map<String, CopyInfo> thenCopies, 
                                   Map<String, CopyInfo> elseCopies) {
        result.clear();
        for (Map.Entry<String, CopyInfo> entry : thenCopies.entrySet()) {
            String varName = entry.getKey();
            CopyInfo thenInfo = entry.getValue();
            CopyInfo elseInfo = elseCopies.get(varName);
            
            if (elseInfo != null && thenInfo.value.equals(elseInfo.value)) {
                // Mismo valor en ambas ramas, mantener la copia
                result.put(varName, thenInfo);
            }
        }
    }
    
    private static StatementNode propagateInWhile(WhileNode whileStmt, Map<String, CopyInfo> copies, Set<String> modifiedVars) {
        // Para loops, asumimos que todas las variables pueden ser modificadas
        // Por seguridad, no propagamos copias a través de loops
        Map<String, CopyInfo> loopCopies = new HashMap<>();
        Set<String> loopModified = new HashSet<>(modifiedVars);
        
        ExpressionNode condition = propagateInExpression(whileStmt.getCondition(), loopCopies, loopModified);
        StatementNode body = propagateInStatement(whileStmt.getBody(), loopCopies, loopModified);
        
        // No propagar copias fuera del loop (pueden cambiar en cada iteración)
        return new WhileNode(condition, body);
    }
    
    private static StatementNode propagateInFor(ForNode forStmt, Map<String, CopyInfo> copies, Set<String> modifiedVars) {
        // Similar a while
        Map<String, CopyInfo> loopCopies = new HashMap<>();
        Set<String> loopModified = new HashSet<>(modifiedVars);
        
        StatementNode init = forStmt.getInit() != null ? 
            propagateInStatement(forStmt.getInit(), loopCopies, loopModified) : null;
        ExpressionNode condition = forStmt.getCondition() != null ? 
            propagateInExpression(forStmt.getCondition(), loopCopies, loopModified) : null;
        ExpressionNode increment = forStmt.getIncrement() != null ? 
            propagateInExpression(forStmt.getIncrement(), loopCopies, loopModified) : null;
        StatementNode body = propagateInStatement(forStmt.getBody(), loopCopies, loopModified);
        
        return new ForNode(init, condition, increment, body);
    }
    
    private static StatementNode propagateInReturn(ReturnNode returnStmt, Map<String, CopyInfo> copies, Set<String> modifiedVars) {
        if (returnStmt.getReturnValue() != null) {
            ExpressionNode value = propagateInExpression(returnStmt.getReturnValue(), copies, modifiedVars);
            return new ReturnNode(value);
        }
        return returnStmt;
    }
    
    private static StatementNode propagateInExpressionStmt(ExpressionStatementNode exprStmt, 
                                                          Map<String, CopyInfo> copies, 
                                                          Set<String> modifiedVars) {
        if (exprStmt.getExpressionNode() != null) {
            ExpressionNode expr = propagateInExpression(exprStmt.getExpressionNode(), copies, modifiedVars);
            return new ExpressionStatementNode(expr);
        }
        return exprStmt;
    }

}