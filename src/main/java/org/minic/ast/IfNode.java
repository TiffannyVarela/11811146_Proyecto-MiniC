package org.minic.ast;

import java.util.ArrayList;
import java.util.List;

public class IfNode extends StatementNode {
    private ExpressionNode condition;
    private BlockNode thenBlock;
    private BlockNode elseBlock;

    public IfNode(ExpressionNode condition, BlockNode thenBlock, BlockNode elseBlock) {
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
    }

    public IfNode(ExpressionNode condition, StatementNode thenBlock, StatementNode elseBlock) {
        this.condition = condition;
        this.thenBlock = convertToBlock(thenBlock);
        this.elseBlock = elseBlock != null ? convertToBlock(elseBlock) : null;
    }
    
    private BlockNode convertToBlock(StatementNode stmt) {
        if (stmt instanceof BlockNode) {
            return (BlockNode) stmt;
        } else {
            List<StatementNode> statements = new ArrayList<>();
            statements.add(stmt);
            return new BlockNode(statements);
        }
    }

    public ExpressionNode getCondition() {
        return condition;
    }

    public BlockNode getThenBlock() {
        return thenBlock;
    }

    public BlockNode getElseBlock() {
        return elseBlock;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        return "IfNode{" +
                "condition=" + condition +
                ", thenBlock=" + thenBlock +
                ", elseBlock=" + elseBlock +
                '}';
    }
}
