package org.minic.ast;

public class VarDeclNode extends DeclarationNode {
    private String type;
    private String name;
    private boolean isArray;
    private int arraySize;
    
    public VarDeclNode(String type, String name) {
        super(name);        
        this.type = type;
        this.name = name;
        this.isArray = false;
    }

    public VarDeclNode(String type, String name, boolean isArray, int arraySize) {
        super(name);        
        this.type = type;
        this.name = name;
        this.isArray = isArray;
        this.arraySize = arraySize;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public boolean isArray() {
        return isArray;
    }

    public int getArraySize() {
        return arraySize;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}
