package org.minic.ast;

public class CharNode extends ExpressionNode {
    private String value;

    public CharNode(String value) {
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
