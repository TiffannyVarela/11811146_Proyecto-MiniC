package org.minic.ast;

public class AssignmentNode extends StatementNode{
    private String variableName;
    private ExpressionNode value;

    public AssignmentNode(String variableName, ExpressionNode value){
        this.variableName = variableName;
        this.value = value;
    }

    public AssignmentNode(ExpressionNode target, ExpressionNode value) {
        if (target instanceof VariableNode) {
            this.variableName = ((VariableNode) target).getName();
        } else if (target instanceof IdentifierNode) {
            this.variableName = ((IdentifierNode) target).getName();
        } else {
            throw new IllegalArgumentException("Target must be a variable or identifier");
        }
        this.value = value;
    }

    public String getVariableName() {
        return variableName;
    }

    public ExpressionNode getValue() {
        return value;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public void setValue(ExpressionNode value) {
        this.value = value;
    }

    public ExpressionNode getTarget(){
        return new IdentifierNode(variableName);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}
