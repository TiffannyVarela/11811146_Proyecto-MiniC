package org.minic.optimizer;

import org.minic.ast.*;

public class ConstantFolder {
    
    public static AstNode optimize(AstNode ast) {
        System.out.println("=== INICIANDO OPTIMIZACIÓN DE CONSTANTES ===");
        if (ast instanceof ProgramNode) {
            return optimizeProgram((ProgramNode) ast);
        }
        System.out.println("=== FIN OPTIMIZACIÓN ===");
        return ast;
    }
    
    private static ProgramNode optimizeProgram(ProgramNode program) {
        System.out.println("Optimizando programa con " + program.getDeclarationsNodes().size() + " declaraciones");
        ProgramNode optimized = new ProgramNode();
        for (AstNode decl : program.getDeclarationsNodes()) {
            System.out.println("  Declaración: " + decl.getClass().getSimpleName());
            if (decl instanceof FunctionNode) {
                System.out.println("  Es FunctionNode: " + ((FunctionNode) decl).getName());
                optimized.addDeclarationNode((DeclarationNode) optimizeFunction((FunctionNode) decl));
            } else if (decl instanceof DeclarationNode) {
                optimized.addDeclarationNode((DeclarationNode) decl);
            }
        }
        
        return optimized;
    }
    
    private static FunctionNode optimizeFunction(FunctionNode function) {
        BlockNode optimizedBody = optimizeBlock(function.getBody());
        return new FunctionNode(
            function.getReturnType(),
            function.getName(),
            function.getParameters(),
            optimizedBody
        );
    }
    
    private static BlockNode optimizeBlock(BlockNode block) {
        if (block == null) {
            System.out.println("optimizeBlock: block es null!");
            return null;
        }
        
        System.out.println("optimizeBlock INICIO: procesando bloque con " + block.getStatements().size() + " statements");
        
        BlockNode optimized = new BlockNode();
        for (int i = 0; i < block.getStatements().size(); i++) {
            StatementNode stmt = block.getStatements().get(i);
            System.out.println("  Procesando statement " + i + ": " + stmt.getClass().getSimpleName());
            
            // Agrega logging ANTES de llamar a optimizeStatement
            System.out.println("  Llamando a optimizeStatement...");
            StatementNode optimizedStmt = optimizeStatement(stmt);
            System.out.println("  optimizeStatement retornó: " + optimizedStmt.getClass().getSimpleName());
            
            optimized.getStatements().add(optimizedStmt);
        }
        
        System.out.println("optimizeBlock FIN: bloque optimizado tiene " + optimized.getStatements().size() + " statements");
        return optimized;
    }
    
    private static StatementNode optimizeStatement(StatementNode stmt) {
        System.out.println("optimizeStatement INICIO: " + stmt.getClass().getSimpleName());
        
        if (stmt instanceof ExpressionStatementNode) {
            ExpressionStatementNode exprStmt = (ExpressionStatementNode) stmt;
            System.out.println("  Es ExpressionStatementNode");
            ExpressionNode optimizedExpr = optimizeExpression(exprStmt.getExpressionNode());
            return new ExpressionStatementNode(optimizedExpr);
        } else if (stmt instanceof VarDeclStatementNode) {
            System.out.println("  Es VarDeclStatementNode");
            return optimizeVarDeclStatement((VarDeclStatementNode) stmt);
        } else if (stmt instanceof ReturnNode) {
            System.out.println("  Es ReturnNode");
            return optimizeReturnStatement((ReturnNode) stmt);
        } else if (stmt instanceof BlockNode) {
            System.out.println("  Es BlockNode - llamando a optimizeBlock directamente");
            // En lugar de llamar a optimizeBlock (que crea un ciclo), 
            // procesa los statements directamente
            BlockNode block = (BlockNode) stmt;
            BlockNode optimizedBlock = new BlockNode();
            for (StatementNode innerStmt : block.getStatements()) {
                optimizedBlock.getStatements().add(optimizeStatement(innerStmt));
            }
            return optimizedBlock;
        }
        
        System.out.println("optimizeStatement: tipo no manejado: " + stmt.getClass().getSimpleName());
        return stmt;
    }

    private static VarDeclStatementNode optimizeVarDeclStatement(VarDeclStatementNode varDeclStmt) {
        System.out.println("Optimizando VarDeclStatementNode: " + varDeclStmt.getVarDeclNode().getName());
        
        VarDeclNode varDecl = varDeclStmt.getVarDeclNode();
        
        if (varDecl.hasInitialNode()) {
            System.out.println("  Tiene inicialización, optimizando...");
            ExpressionNode optimizedInit = optimizeExpression(varDecl.getInitialNode());
            System.out.println("  Inicialización original: " + varDecl.getInitialNode().getClass().getSimpleName());
            System.out.println("  Inicialización optimizada: " + optimizedInit.getClass().getSimpleName());
            
            VarDeclNode optimizedVarDecl = new VarDeclNode(
                varDecl.getType(),
                varDecl.getName(),
                varDecl.isArray(),
                varDecl.getArraySize(),
                optimizedInit
            );
            return new VarDeclStatementNode(optimizedVarDecl);
        } else {
            System.out.println("  No tiene inicialización");
        }
        
        return varDeclStmt;
    }
    
    private static ExpressionNode optimizeExpression(ExpressionNode expr) {
        System.out.println("optimizeExpression: " + expr.getClass().getSimpleName());
        
        if (expr instanceof BinaryOpNode) {
            return optimizeBinaryOp((BinaryOpNode) expr);
        }
        if (expr instanceof UnaryOpNode) {
            return optimizeUnaryOp((UnaryOpNode) expr);
        }
        if (expr instanceof NumberNode) {
            System.out.println("  Es NumberNode: " + ((NumberNode) expr).getValue());
        }
        
        return expr;
    }
    
    private static ExpressionNode optimizeBinaryOp(BinaryOpNode binOp) {
        System.out.println("Optimizando binary op: " + binOp.getOperator() + 
                         " left=" + binOp.getLeft().getClass().getSimpleName() + 
                         " right=" + binOp.getRight().getClass().getSimpleName());
        
        ExpressionNode left = optimizeExpression(binOp.getLeft());
        ExpressionNode right = optimizeExpression(binOp.getRight());
        
        if (left instanceof NumberNode && right instanceof NumberNode) {
            int leftVal = ((NumberNode) left).getValue();
            int rightVal = ((NumberNode) right).getValue();
            String operator = binOp.getOperator();
            
            System.out.println("  ¡Puedo optimizar! " + leftVal + " " + operator + " " + rightVal);
            
            switch (operator) {
                case "+": 
                    int result = leftVal + rightVal;
                    System.out.println("  Resultado: " + result);
                    return new NumberNode(result);
                case "-": 
                    result = leftVal - rightVal;
                    System.out.println("  Resultado: " + result);
                    return new NumberNode(result);
                case "*": 
                    result = leftVal * rightVal;
                    System.out.println("  Resultado: " + result);
                    return new NumberNode(result);
                case "/": 
                    if (rightVal != 0) {
                        result = leftVal / rightVal;
                        System.out.println("  Resultado: " + result);
                        return new NumberNode(result);
                    }
                    break;
                case "%": 
                    if (rightVal != 0) {
                        result = leftVal / rightVal;
                        System.out.println("  Resultado: " + result);
                        return new NumberNode(result);
                    }
                    break;
                case "==": return new BooleanNode(leftVal == rightVal);
                case "!=": return new BooleanNode(leftVal != rightVal);
                case "<": return new BooleanNode(leftVal < rightVal);
                case ">": return new BooleanNode(leftVal > rightVal);
                case "<=": return new BooleanNode(leftVal <= rightVal);
                case ">=": return new BooleanNode(leftVal >= rightVal);
                case "&&": return new BooleanNode((leftVal != 0) && (rightVal != 0));
                case "||": return new BooleanNode((leftVal != 0) || (rightVal != 0));
            }
        } else {
            System.out.println("  No puedo optimizar (no son ambos NumberNode)");
        }
        
        return new BinaryOpNode(binOp.getOperator(), left, right);
    }
    
    private static ExpressionNode optimizeUnaryOp(UnaryOpNode unaryOp) {
        ExpressionNode operand = optimizeExpression(unaryOp.getOperand());
        
        if (operand instanceof NumberNode) {
            int value = ((NumberNode) operand).getValue();
            String operator = unaryOp.getOperator();
            
            switch (operator) {
                case "-": return new NumberNode(-value);
                case "!": return new BooleanNode(value == 0);
            }
        }
        
        return new UnaryOpNode(unaryOp.getOperator(), operand);
    }

    private static StatementNode optimizeReturnStatement(ReturnNode returnStmt) {
        System.out.println("optimizeReturnStatement: tiene return value? " + (returnStmt.getReturnValue() != null));
        
        if (returnStmt.getReturnValue() != null) {
            System.out.println("  Optimizando expresión de retorno...");
            ExpressionNode optimizedReturn = optimizeExpression(returnStmt.getReturnValue());
            System.out.println("  Expresión original: " + returnStmt.getReturnValue().getClass().getSimpleName());
            System.out.println("  Expresión optimizada: " + optimizedReturn.getClass().getSimpleName());
            return new ReturnNode(optimizedReturn);
        }
        return returnStmt;
    }
}
