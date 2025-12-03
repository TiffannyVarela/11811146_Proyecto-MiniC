package org.minic.ast;

public class CharNode extends ExpressionNode {
    private char value;

    public CharNode(String text) {
        this.value = extractCharValue(text);
    }

    public CharNode(char value) {
        this.value = value;
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

    public CharNode cloneNode() {
        return new CharNode(this.value);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}