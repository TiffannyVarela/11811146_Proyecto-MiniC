package org.minic.ast;

public class VarDeclStatementNode extends StatementNode{
    private VarDeclNode varDeclNode;

    public VarDeclStatementNode(VarDeclNode varDeclNode){
        this.varDeclNode=varDeclNode;
    }

    public VarDeclNode getVarDeclNode(){
        return varDeclNode;
    }

    public void setVarDeclNode (VarDeclNode varDeclNode){
        this.varDeclNode=varDeclNode;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
