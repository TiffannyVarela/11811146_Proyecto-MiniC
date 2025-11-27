package org.minic.ast;

import java.util.List;
import java.util.ArrayList;

public class BlockNode extends StatementNode {
    private List<StatementNode> statements;

    public BlockNode() {
        this.statements = new ArrayList<>();
    }

    public BlockNode(List<StatementNode> statements) {
        this.statements = statements;
    }

    public List<StatementNode> getStatements() {
        return statements;
    }

    void addStatement(StatementNode statement) {
        statements.add(statement);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}
