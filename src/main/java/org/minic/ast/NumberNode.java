package org.minic.ast;

public class NumberNode extends ExpressionNode {
    private String value;

    public NumberNode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}
