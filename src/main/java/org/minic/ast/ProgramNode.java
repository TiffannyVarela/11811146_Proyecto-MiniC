package org.minic.ast;

import java.util.ArrayList;
import java.util.List;

public class ProgramNode extends AstNode {

    private List<AstNode> declarationsNodes;
    
    public ProgramNode() {
        this.declarationsNodes = new ArrayList<>();
    }

    public List<AstNode> getDeclarationsNodes() {
        return declarationsNodes;
    }

    public void addDeclarationNode(DeclarationNode declarationNode) {
        declarationsNodes.add(declarationNode);
    }

    public List<AstNode> getChildren(){
        return new ArrayList<>(declarationsNodes);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
