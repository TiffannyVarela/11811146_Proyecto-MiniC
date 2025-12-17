package org.minic.ast;

import java.util.ArrayList;
import java.util.List;

public class ArrayDimensionsNode extends AstNode {
    private List<Integer> dimensions;
    
    public ArrayDimensionsNode(List<Integer> dimensions) {
        this.dimensions = dimensions != null ? new ArrayList<>(dimensions) : new ArrayList<>();
    }
    
    public List<Integer> getDimensions() {
        return new ArrayList<>(dimensions); // Devuelve copia
    }
    
    public int getDimensionCount() {
        return dimensions.size();
    }
    
    public int getDimension(int index) {
        if (index >= 0 && index < dimensions.size()) {
            return dimensions.get(index);
        }
        return -1; // Error
    }
    
    public boolean hasVariableDimensions() {
        for (int dim : dimensions) {
            if (dim == -1) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return null;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ArrayDimensions[");
        for (int i = 0; i < dimensions.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(dimensions.get(i) == -1 ? "[]" : "[" + dimensions.get(i) + "]");
        }
        sb.append("]");
        return sb.toString();
    }
}