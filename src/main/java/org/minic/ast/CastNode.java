package org.minic.ast;

public class CastNode extends ExpressionNode {
    private final String targetType;
    private final ExpressionNode expression;
    
    public CastNode(String targetType, ExpressionNode expression) {
        this.targetType = targetType;
        this.expression = expression;
    }
    
    public String getTargetType() { return targetType; }
    public ExpressionNode getExpression() { return expression; }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public ExpressionNode cloneNode() {
        return new CastNode(targetType, expression.cloneNode());
    }
}