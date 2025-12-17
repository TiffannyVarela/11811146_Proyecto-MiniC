package org.minic.ast;

public class FloatNode extends LiteralNode {
    private final float value;
    
    public FloatNode(float value) {
        this.value = value;
    }
    
    public FloatNode(String text) {
        this.value = Float.parseFloat(text);
    }
    
    public float getValue() {
        return value;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public ExpressionNode cloneNode() {
        return new FloatNode(value);
    }
}