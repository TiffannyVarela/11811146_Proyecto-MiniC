package org.minic.ast;

public abstract class DeclarationNode extends AstNode {
    protected String name;

    public DeclarationNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
