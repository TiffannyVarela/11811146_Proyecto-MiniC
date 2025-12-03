package org.minic.optimizer;

import org.minic.ast.*;
import java.util.*;

public class DeadCodeEliminator {
    
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
        // Análisis de uso de variables
        Set<String> usedVariables = new HashSet<>();
        collectUsedVariables(function.getBody(), usedVariables);
        
        // Eliminar declaraciones no usadas
        BlockNode optimizedBody = removeUnusedDeclarations(function.getBody(), usedVariables);
        
        // Eliminar código muerto en statements
        List<StatementNode> optimizedStatements = new ArrayList<>();
        for (StatementNode stmt : optimizedBody.getStatements()) {
            StatementNode optimizedStmt = optimizeStatement(stmt, usedVariables);
            if (optimizedStmt != null) {
                optimizedStatements.add(optimizedStmt);
            }
        }
        
        BlockNode newBody = new BlockNode(optimizedStatements);
        return new FunctionNode(
            function.getReturnType(),
            function.getName(),
            function.getParameters(),
            newBody
        );
    }
    
    private static void collectUsedVariables(BlockNode block, Set<String> usedVars) {
        for (StatementNode stmt : block.getStatements()) {
            collectUsedVariables(stmt, usedVars);
        }
    }
    
    private static void collectUsedVariables(StatementNode stmt, Set<String> usedVars) {
        if (stmt instanceof AssignmentNode) {
            AssignmentNode assign = (AssignmentNode) stmt;
            collectUsedVariables(assign.getTarget(), usedVars);
            collectUsedVariables(assign.getValue(), usedVars);
        } else if (stmt instanceof ExpressionStatementNode) {
            ExpressionStatementNode exprStmt = (ExpressionStatementNode) stmt;
            if (exprStmt.getExpressionNode() != null) {
                collectUsedVariables(exprStmt.getExpressionNode(), usedVars);
            }
        } else if (stmt instanceof ReturnNode) {
            ReturnNode returnStmt = (ReturnNode) stmt;
            if (returnStmt.getReturnValue() != null) {
                collectUsedVariables(returnStmt.getReturnValue(), usedVars);
            }
        } else if (stmt instanceof IfNode) {
            IfNode ifStmt = (IfNode) stmt;
            collectUsedVariables(ifStmt.getCondition(), usedVars);
            collectUsedVariables(ifStmt.getThenBlock(), usedVars);
            if (ifStmt.getElseBlock() != null) {
                collectUsedVariables(ifStmt.getElseBlock(), usedVars);
            }
        } else if (stmt instanceof WhileNode) {
            WhileNode whileStmt = (WhileNode) stmt;
            collectUsedVariables(whileStmt.getCondition(), usedVars);
            collectUsedVariables(whileStmt.getBody(), usedVars);
        } else if (stmt instanceof ForNode) {
            ForNode forStmt = (ForNode) stmt;
            if (forStmt.getInit() != null) {
                collectUsedVariables(forStmt.getInit(), usedVars);
            }
            if (forStmt.getCondition() != null) {
                collectUsedVariables(forStmt.getCondition(), usedVars);
            }
            if (forStmt.getIncrement() != null) {
                collectUsedVariables(forStmt.getIncrement(), usedVars);
            }
            collectUsedVariables(forStmt.getBody(), usedVars);
        } else if (stmt instanceof BlockNode) {
            BlockNode block = (BlockNode) stmt;
            for (StatementNode child : block.getStatements()) {
                collectUsedVariables(child, usedVars);
            }
        }
    }

    private static void collectUsedVariables(ExpressionNode expr, Set<String> usedVars) {
        if (expr instanceof VariableNode) {
            usedVars.add(((VariableNode) expr).getName());
        } else if (expr instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) expr;
            collectUsedVariables(binOp.getLeft(), usedVars);
            collectUsedVariables(binOp.getRight(), usedVars);
        } else if (expr instanceof UnaryOpNode) {
            UnaryOpNode unary = (UnaryOpNode) expr;
            collectUsedVariables(unary.getOperand(), usedVars);
        } else if (expr instanceof FunctionCallNode) {
            FunctionCallNode call = (FunctionCallNode) expr;
            for (ExpressionNode arg : call.getArguments()) {
                collectUsedVariables(arg, usedVars);
            }
        }
    }
    
    private static BlockNode removeUnusedDeclarations(BlockNode block, Set<String> usedVars) {
        List<StatementNode> optimized = new ArrayList<>();
        
        for (StatementNode stmt : block.getStatements()) {
            if (stmt instanceof VarDeclStatementNode) {
                VarDeclStatementNode varDeclStmt = (VarDeclStatementNode) stmt;
                String varName = varDeclStmt.getVarDeclNode().getName();
                if (usedVars.contains(varName)) {
                    optimized.add(stmt);
                }
                // Si no se usa, se elimina (no se agrega a optimized)
            } else {
                optimized.add(stmt);
            }
        }
        
        return new BlockNode(optimized);
    }
    
    private static StatementNode optimizeStatement(StatementNode stmt, Set<String> usedVars) {
        // Eliminar asignaciones a variables no usadas
        if (stmt instanceof AssignmentNode) {
            AssignmentNode assign = (AssignmentNode) stmt;
            if (assign.getTarget() instanceof VariableNode) {
                String varName = ((VariableNode) assign.getTarget()).getName();
                if (!usedVars.contains(varName)) {
                    return null; // Eliminar esta asignación
                }
            }
        }
        
        // Eliminar expresiones sin efecto
        if (stmt instanceof ExpressionStatementNode) {
            ExpressionStatementNode exprStmt = (ExpressionStatementNode) stmt;
            if (hasNoSideEffects(exprStmt.getExpressionNode())) {
                return null; // Eliminar expresión sin efecto
            }
        }
        
        return stmt;
    }
    
    private static boolean hasNoSideEffects(ExpressionNode expr) {
        // Verificar si la expresión no tiene efectos secundarios
        if (expr == null) return true;
        if (expr instanceof NumberNode || expr instanceof BooleanNode || 
            expr instanceof CharNode || expr instanceof StringNode) {
            return true; // Literales no tienen efectos
        }
        if (expr instanceof VariableNode) {
            return true; // Acceso a variable no tiene efectos
        }
        // Llamadas a función y operaciones pueden tener efectos
        return false;
    }
}