package org.minic.ast;

public class LiteralNode extends ExpressionNode {

    @Override
    public ExpressionNode cloneNode() {
        return new LiteralNode();
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}

