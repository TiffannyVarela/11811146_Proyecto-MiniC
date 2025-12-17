package org.minic.ast;

public class BreakNode extends StatementNode {
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}