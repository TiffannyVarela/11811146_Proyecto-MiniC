package org.minic.ast;

public class BooleanNode extends LiteralNode {
    private boolean value;

    public BooleanNode(boolean value) {
        this.value = value;
    }

    public boolean getValue() {
        return value;
    }

    public BooleanNode cloneNode() {
        return new BooleanNode(this.value);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}