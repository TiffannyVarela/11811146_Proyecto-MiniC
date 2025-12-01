package org.minic.optimizer;

import org.minic.ast.*;

public class ConstantFolder {
    
    public static AstNode optimize(AstNode ast) {
        if (ast instanceof ProgramNode) {
            return optimizeProgram((ProgramNode) ast);
        }
        return ast;
    }
    
    private static ProgramNode optimizeProgram(ProgramNode program) {
        ProgramNode optimized = new ProgramNode();
        
        for (AstNode decl : program.getDeclarationsNodes()) {
            if (decl instanceof FunctionNode) {
                // CORREGIDO: Cast a DeclarationNode
                optimized.addDeclarationNode((DeclarationNode) optimizeFunction((FunctionNode) decl));
            } else if (decl instanceof DeclarationNode) {
                // CORREGIDO: Solo agregar DeclarationNode
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
        if (block == null) return null;
        
        BlockNode optimized = new BlockNode();
        for (StatementNode stmt : block.getStatements()) {
            // CORREGIDO: Usar addStatement público
            optimized.getStatements().add(optimizeStatement(stmt));
        }
        return optimized;
    }
    
    private static StatementNode optimizeStatement(StatementNode stmt) {
        if (stmt instanceof ExpressionStatementNode) {
            ExpressionStatementNode exprStmt = (ExpressionStatementNode) stmt;
            ExpressionNode optimizedExpr = optimizeExpression(exprStmt.getExpressionNode());
            return new ExpressionStatementNode(optimizedExpr);
        }
        // Para otros tipos de statements, agregar optimizaciones aquí
        return stmt;
    }
    
    private static ExpressionNode optimizeExpression(ExpressionNode expr) {
        if (expr instanceof BinaryOpNode) {
            return optimizeBinaryOp((BinaryOpNode) expr);
        }
        if (expr instanceof UnaryOpNode) {
            return optimizeUnaryOp((UnaryOpNode) expr);
        }
        return expr;
    }
    
    private static ExpressionNode optimizeBinaryOp(BinaryOpNode binOp) {
        ExpressionNode left = optimizeExpression(binOp.getLeft());
        ExpressionNode right = optimizeExpression(binOp.getRight());
        
        // Constant folding: 3 + 5 → 8
        if (left instanceof NumberNode && right instanceof NumberNode) {
            int leftVal = ((NumberNode) left).getValue();
            int rightVal = ((NumberNode) right).getValue();
            String operator = binOp.getOperator();
            
            switch (operator) {
                case "+": return new NumberNode(leftVal + rightVal);
                case "-": return new NumberNode(leftVal - rightVal);
                case "*": return new NumberNode(leftVal * rightVal);
                case "/": 
                    if (rightVal != 0) return new NumberNode(leftVal / rightVal);
                    break;
                case "%": 
                    if (rightVal != 0) return new NumberNode(leftVal % rightVal);
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
}