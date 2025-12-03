package org.minic.ast;

public class UnaryOpNode extends ExpressionNode {
    private String operator;
    private ExpressionNode operand;

    public UnaryOpNode(String operator, ExpressionNode operand) {
        this.operator = operator;
        this.operand = operand;
    }

    public String getOperator() {
        return operator;
    }

    public ExpressionNode getOperand() {
        return operand;
    }

    public UnaryOpNode cloneNode() {
        return new UnaryOpNode(this.operator, this.operand.cloneNode());
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}
