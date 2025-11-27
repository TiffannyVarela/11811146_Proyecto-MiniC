package org.minic.ast;

import java.util.ArrayList;
import java.util.List;

public class FunctionNode extends DeclarationNode {
    private String returnType;
    private List<VarDeclNode> parameters;
    private BlockNode body;

    public FunctionNode(String returnType, String name, List<VarDeclNode> parameters, BlockNode body) {
        super(name);
        this.returnType = returnType;
        this.parameters = parameters != null ? parameters : new ArrayList<>();
        this.body = body;
    }

    public String getReturnType() {
        return returnType;
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

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}
