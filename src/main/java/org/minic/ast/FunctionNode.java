package org.minic.ast;

import java.util.List;

public class FunctionNode extends DeclarationNode {
    private String returnType;
    private String name;
    private List<VarDeclNode> parameters;
    private BlockNode body;

    public FunctionNode(String returnType, String name, List<VarDeclNode> parameters, BlockNode body) {
        super(name);
        this.returnType = returnType;
        this.name = name;
        //this.parameters = parameters != null ? parameters : new ArrayList<>();
        this.parameters = parameters;
        this.body = body;
    }

    public String getReturnType() {
        return returnType;
    }

    public String getName() {
        return name;
    }

    public List<VarDeclNode> getParameters() {
        return parameters;
    }

    public void addParameter(VarDeclNode parameter) {
        parameters.add(parameter);
    }

    public BlockNode getBody() {
        return body;
    }

    public void setBody(BlockNode body) {
        this.body = body;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}
