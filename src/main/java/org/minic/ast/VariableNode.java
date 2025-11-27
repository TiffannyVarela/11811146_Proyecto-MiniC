package org.minic.ast;

public class VariableNode extends ExpressionNode {
    private String name;

    public VariableNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "VariableNode{" +
                "name='" + name + '\'' +
                '}';
    }
    
}
