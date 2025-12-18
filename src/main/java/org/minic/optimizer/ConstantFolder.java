package org.minic.optimizer;

import java.util.HashMap;
import java.util.Map;

import org.minic.ast.*;

public class ConstantFolder {
    
    private static final Map<String, Integer> constants = new HashMap<>();

    public static AstNode optimize(AstNode ast) {
    constants.clear();

    if (ast instanceof ProgramNode program) {
        AstNode result = optimizeProgram(program);
        return result;
    }

    return ast;
}

    private static ProgramNode optimizeProgram(ProgramNode program) {
        ProgramNode optimized = new ProgramNode();

        for (AstNode decl : program.getDeclarationsNodes()) {
            if (decl instanceof FunctionNode fn) {
                optimized.addDeclarationNode(optimizeFunction(fn));
            } else if (decl instanceof DeclarationNode dn) {
                optimized.addDeclarationNode(dn);
            }
        }

        return optimized;
    }

    private static FunctionNode optimizeFunction(FunctionNode function) {
        constants.clear();

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
            optimized.getStatements().add(optimizeStatement(stmt));
        }

        return optimized;
    }

    private static StatementNode optimizeStatement(StatementNode stmt) {

        if (stmt instanceof ExpressionStatementNode exprStmt) {
            ExpressionNode optimizedExpr =
                    optimizeExpression(exprStmt.getExpressionNode());
            return new ExpressionStatementNode(optimizedExpr);
        }

        if (stmt instanceof VarDeclStatementNode varStmt) {
            return optimizeVarDeclStatement(varStmt);
        }

        if (stmt instanceof ReturnNode ret) {
            return optimizeReturn(ret);
        }

        if (stmt instanceof BlockNode block) {
            return optimizeBlock(block);
        }

        return stmt;
    }

    private static VarDeclStatementNode optimizeVarDeclStatement(
            VarDeclStatementNode stmt) {

        VarDeclNode var = stmt.getVarDeclNode();

        if (var.hasInitialNode()) {
            ExpressionNode optimizedInit =
                    optimizeExpression(var.getInitialNode());

            if (optimizedInit instanceof NumberNode num) {
                constants.put(var.getName(), num.getValue());
            } else {
                constants.remove(var.getName());
            }

            VarDeclNode optimizedVar = new VarDeclNode(
                    var.getType(),
                    var.getName(),
                    var.isArray(),
                    var.getArraySize(),
                    optimizedInit
            );

            return new VarDeclStatementNode(optimizedVar);
        }

        constants.remove(var.getName());
        return stmt;
    }

    private static ReturnNode optimizeReturn(ReturnNode ret) {
        if (ret.getReturnValue() != null) {
            ExpressionNode optimized =
                    optimizeExpression(ret.getReturnValue());
            return new ReturnNode(optimized);
        }
        return ret;
    }

    private static ExpressionNode optimizeExpression(ExpressionNode expr) {

        if (expr instanceof VariableNode var) {
            if (constants.containsKey(var.getName())) {
                return new NumberNode(constants.get(var.getName()));
            }
            return expr;
        }

        if (expr instanceof BinaryOpNode bin) {
            return optimizeBinary(bin);
        }

        if (expr instanceof UnaryOpNode un) {
            return optimizeUnary(un);
        }

        return expr;
    }
    private static ExpressionNode optimizeBinary(BinaryOpNode bin) {

        ExpressionNode left = optimizeExpression(bin.getLeft());
        ExpressionNode right = optimizeExpression(bin.getRight());

        if (left instanceof NumberNode l && right instanceof NumberNode r) {
            int a = l.getValue();
            int b = r.getValue();

            return switch (bin.getOperator()) {
                case "+"  -> new NumberNode(a + b);
                case "-"  -> new NumberNode(a - b);
                case "*"  -> new NumberNode(a * b);
                case "/"  -> (b != 0) ? new NumberNode(a / b) : bin;
                case "%"  -> (b != 0) ? new NumberNode(a % b) : bin;

                case "==" -> new BooleanNode(a == b);
                case "!=" -> new BooleanNode(a != b);
                case "<"  -> new BooleanNode(a < b);
                case "<=" -> new BooleanNode(a <= b);
                case ">"  -> new BooleanNode(a > b);
                case ">=" -> new BooleanNode(a >= b);

                case "&&" -> new BooleanNode(a != 0 && b != 0);
                case "||" -> new BooleanNode(a != 0 || b != 0);

                default -> bin;
            };
        }

        if (left != bin.getLeft() || right != bin.getRight()) {
            return new BinaryOpNode(bin.getOperator(), left, right);
        }

        return bin;
    }
    private static ExpressionNode optimizeUnary(UnaryOpNode un) {

        ExpressionNode operand = optimizeExpression(un.getOperand());

        if (operand instanceof NumberNode num) {
            int v = num.getValue();

            return switch (un.getOperator()) {
                case "-" -> new NumberNode(-v);
                case "!" -> new BooleanNode(v == 0);
                default -> un;
            };
        }

        if (operand != un.getOperand()) {
            return new UnaryOpNode(un.getOperator(), operand);
        }

        return un;
    }
}
