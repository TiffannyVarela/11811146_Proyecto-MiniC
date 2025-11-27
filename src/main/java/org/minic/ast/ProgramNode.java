package org.minic.ast;

import java.util.ArrayList;
import java.util.List;

public class ProgramNode extends AstNode {

    private List<DeclarationNode> declarationsNodes;
    
    public ProgramNode() {
        this.declarationsNodes = new ArrayList<>();
    }

    public ProgramNode(List<DeclarationNode> declarationsNodes) {
        this.declarationsNodes = declarationsNodes;
    }

    public List<DeclarationNode> getDeclarationsNodes() {
        return declarationsNodes;
    }

    public void setDeclarationsNodes(List<DeclarationNode> declarationsNodes) {
        this.declarationsNodes = declarationsNodes;
    }

    public void addDeclarationNode(DeclarationNode declarationNode) {
        declarationsNodes.add(declarationNode);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
