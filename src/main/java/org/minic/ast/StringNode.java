package org.minic.ast;

public class StringNode extends LiteralNode {
    private String value;

    public StringNode(String text) {
        this.value = extractStringValue(text);
    }

    private static String extractStringValue(String text) {
        if (text.length() >= 2) {
            return text.substring(1, text.length() - 1);
        } else {
            return text;
        }
    }

    public String getValue() {
        return value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}