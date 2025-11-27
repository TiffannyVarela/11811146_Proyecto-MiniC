package org.minic.ast;

public class ForNode extends StatementNode{
    private StatementNode init;
    private ExpressionNode condition;
    private ExpressionNode increment;
    private StatementNode body;

    public ForNode(StatementNode init, ExpressionNode condition, ExpressionNode increment, StatementNode body){
        this.init=init;
        this.condition=condition;
        this.increment=increment;
        this.body=body;
    }

    public StatementNode getInit(){
        return init;
    }

    public ExpressionNode getCondition(){
        return condition;
    }

    public ExpressionNode getIncrement(){
        return increment;
    }

    public StatementNode getBody(){
        return body;
    }

    public void setInit(StatementNode init){
        this.init = init;
    }

    public void setCondition(ExpressionNode condition){
        this.condition = condition;
    }

    public void setIncrement(ExpressionNode increment){
        this.increment = increment;
    }

    public void setBody(StatementNode body){
        this.body = body;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor){
        return visitor.visit(this);
    }
}

