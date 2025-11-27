package org.minic.ast;

public class NumberNode extends LiteralNode {
    private int value;

    public NumberNode(int value) {
        this.value = value;
    }

    public NumberNode(String text) {
        this.value = Integer.parseInt(text);
    }

    public int getValue() {
        return value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}