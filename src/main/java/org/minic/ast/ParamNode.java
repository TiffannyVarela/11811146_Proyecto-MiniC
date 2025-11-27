package org.minic.ast;
 import java.util.*;

public class ParamNode extends AstNode {
    private String name;
    private String type;

    public ParamNode(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void addDeclarationNode(AstNode varDeclNode) {
        // No implementado para ParamNode
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}
