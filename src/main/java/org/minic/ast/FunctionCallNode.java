package org.minic.ast;

import java.util.ArrayList;
import java.util.List;
public class FunctionCallNode extends ExpressionNode {
    private String functionName;
    private List<ExpressionNode> arguments;

    public FunctionCallNode(String functionName, List<ExpressionNode> arguments) {
        this.functionName = functionName;
        this.arguments = arguments;
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<ExpressionNode> getArguments() {
        return arguments;
    }

    @Override
    public ExpressionNode cloneNode() {
        List<ExpressionNode> clonedArgs = new ArrayList<>();
        for (ExpressionNode arg : arguments) {
            clonedArgs.add(arg.cloneNode());
        }
        return new FunctionCallNode(this.functionName, clonedArgs);
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }
    
}
