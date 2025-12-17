package org.minic.ast;

public class VarDeclNode extends DeclarationNode {
    private String type;
    private String name;
    private boolean isArray;
    private int arraySize;
    private ExpressionNode initialNode;
    private int secondDimension = 0;
    
    public VarDeclNode(String type, String name) {
        super(name);        
        this.type = type;
        this.name = name;
        this.isArray = false;
        this.initialNode = null;
    }

    public VarDeclNode(String type, String name, boolean isArray, int arraySize) {
        super(name);        
        this.type = type;
        this.name = name;
        this.isArray = isArray;
        this.arraySize = arraySize;
        this.initialNode = null;
    }

    public VarDeclNode(String type, String name, boolean isArray, int arraySize, ExpressionNode initialNode) {
        super(name);        
        this.type = type;
        this.name = name;
        this.isArray = isArray;
        this.arraySize = arraySize;
        this.initialNode = initialNode;
    }

    public VarDeclNode(String type, String name, boolean isArray, int arraySize, int secondDimension, ExpressionNode initialNode) {
        super(name);        
        this.type = type;
        this.name = name;
        this.isArray = isArray;
        this.arraySize = arraySize;
        this.secondDimension = secondDimension;
        this.initialNode = initialNode;
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

    public ExpressionNode getInitialNode(){
        return initialNode;
    }

    public void setInitialNode( ExpressionNode initialNode){
        this.initialNode = initialNode;
    }

    public boolean hasInitialNode(){
        return initialNode != null;
    }

    public void setArray(boolean isArray) {
        this.isArray = isArray;
    }
    
    public void setArraySize(int arraySize) {
        this.arraySize = arraySize;
        this.isArray = true;
    }

    public int getSecondDimension() {
        return secondDimension;
    }

    public void setSecondDimension(int secondDimension) {
        this.secondDimension = secondDimension;
    }

    public boolean hasSecondDimension() {
        return isArray && secondDimension > 0;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}
