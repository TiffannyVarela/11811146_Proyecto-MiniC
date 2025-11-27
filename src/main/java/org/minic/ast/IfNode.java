package org.minic.ast;

public class IfNode extends StatementNode {
    private ExpressionNode condition;
    private BlockNode thenBlock;
    private BlockNode elseBlock;

    public IfNode(ExpressionNode condition, BlockNode thenBlock, BlockNode elseBlock) {
        this.condition = condition;
        this.thenBlock = thenBlock;
        this.elseBlock = elseBlock;
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
