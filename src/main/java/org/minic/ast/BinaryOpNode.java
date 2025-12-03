package org.minic.ast;

public class BinaryOpNode extends ExpressionNode {
    private String operator;
    private ExpressionNode left;
    private ExpressionNode right;

    public BinaryOpNode(String operator, ExpressionNode left, ExpressionNode right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
    }

    public String getOperator() {
        return operator;
    }

    public ExpressionNode getLeft() {
        return left;
    }

    public ExpressionNode getRight() {
        return right;
    }

    public BinaryOpNode cloneNode() {
        return new BinaryOpNode(this.operator, this.left.cloneNode(), this.right.cloneNode());
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}
