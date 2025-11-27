package org.minic.ast;

public class LiteralNode extends ExpressionNode {
    private Object value;
    private String type;

    public LiteralNode(Object value, String type) {
        this.value = value;
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public String getType() {
        return type;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}
