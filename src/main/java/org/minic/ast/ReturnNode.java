package org.minic.ast;

public class ReturnNode extends StatementNode{

    private ExpressionNode returnValue;


    public ReturnNode(ExpressionNode returnValue) {
        this.returnValue = returnValue;
    }

    public ExpressionNode getReturnValue() {
        return returnValue;
    }

    public void setReturnValue(ExpressionNode returnValue){
        this.returnValue = returnValue;
    }

    public boolean hasReturnValue(){
        return returnValue != null;
    }


    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}
