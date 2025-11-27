package org.minic.ast;

public class VarDeclNode extends DeclarationNode {
    private String type;
    private ExpressionNode initialValue;

    public VarDeclNode(String type, String name) {
        super(name);
        this.type = type;
    }

    public VarDeclNode(String type, String name, ExpressionNode initialValue) {
        super(name);
        this.type = type;
        this.initialValue = initialValue;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public ExpressionNode getInitialValue() {
        return initialValue;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}
