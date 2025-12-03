package org.minic.ast;

public class IdentifierNode extends ExpressionNode {
    private String name;

    public IdentifierNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public ExpressionNode cloneNode() {
        return new IdentifierNode(this.name);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}

