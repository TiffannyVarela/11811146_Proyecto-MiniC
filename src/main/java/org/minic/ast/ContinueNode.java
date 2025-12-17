package org.minic.ast;

public class ContinueNode extends StatementNode {
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}