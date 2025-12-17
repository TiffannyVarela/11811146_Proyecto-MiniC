package org.minic.ast;

import java.util.List;

public class StructNode extends DeclarationNode {
    private final List<VarDeclNode> fields;
    
    public StructNode(String name, List<VarDeclNode> fields) {
        super(name);
        this.fields = fields;
    }
    
    public List<VarDeclNode> getFields() { 
        return fields; 
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        return "StructNode(" + getName() + ", fields: " + fields.size() + ")";
    }
}