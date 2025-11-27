package org.minic.ast;

public class WhileNode extends StatementNode{

    private ExpressionNode condition;
    private StatementNode body;

    public WhileNode(ExpressionNode condition, StatementNode body){
        this.condition=condition;
        this.body=body;
    }

    public ExpressionNode getCondition(){
        return condition;
    }

    public StatementNode getBody(){
        return body;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor){
        return visitor.visit(this);
    }
}
