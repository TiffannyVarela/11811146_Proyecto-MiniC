package org.minic.ast;

public class CharNode extends LiteralNode {
    private char value;

    public CharNode(String text) {
        this.value = extractCharValue(text);
    }

    private static char extractCharValue(String text) {
        if (text.length() >= 3) {
            return text.charAt(1);
        } else {
            return text.charAt(0);
        }
    }

    public char getValue() {
        return value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}