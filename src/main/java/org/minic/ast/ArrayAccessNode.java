package org.minic.ast;

import java.util.ArrayList;
import java.util.List;

public class ArrayAccessNode extends ExpressionNode {
    private ExpressionNode array;
    private List<ExpressionNode> indices;
    
    public ArrayAccessNode(ExpressionNode array, List<ExpressionNode> indices) {
        this.array = array;
        this.indices = indices != null ? new ArrayList<>(indices) : new ArrayList<>();
    }
    
    public ExpressionNode getArray() {
        return array;
    }
    
    public List<ExpressionNode> getIndices() {
        return indices;
    }
    
    public int getNumDimensions() {
        return indices.size();
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public ExpressionNode cloneNode() {
        ExpressionNode clonedArray = array != null ? array.cloneNode() : null;
        
        List<ExpressionNode> clonedIndices = new ArrayList<>();
        for (ExpressionNode index : indices) {
            clonedIndices.add(index.cloneNode());
        }
        
        return new ArrayAccessNode(clonedArray, clonedIndices);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(array != null ? array.toString() : "null");
        for (ExpressionNode index : indices) {
            sb.append("[").append(index != null ? index.toString() : "null").append("]");
        }
        return sb.toString();
    }
}