package org.minic.ast;

public class DoWhileNode extends StatementNode{

    private StatementNode body;
    private ExpressionNode condition;

    public DoWhileNode(StatementNode body, ExpressionNode condition) {
        this.body = body;
        this.condition = condition;
    }

    public StatementNode getBody() {
        return body;
    }

    public void setBody(StatementNode body) {
        this.body = body;
    }

    public ExpressionNode getCondition() {
        return condition;
    }

    public void setCondition(ExpressionNode condition) {
        this.condition = condition;
    }
    

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
