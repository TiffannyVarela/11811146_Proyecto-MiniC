package org.minic.ast;

public class AssignmentNode extends StatementNode {
    private ExpressionNode target;  // Ahora puede ser VariableNode, ArrayAccessNode, etc.
    private ExpressionNode value;

    // Constructor antiguo (mantener para compatibilidad)
    public AssignmentNode(String variableName, ExpressionNode value) {
        this.target = new VariableNode(variableName);
        this.value = value;
    }

    // Nuevo constructor principal
    public AssignmentNode(ExpressionNode target, ExpressionNode value) {
        if (target == null) {
            throw new IllegalArgumentException("Target cannot be null");
        }
        
        // Aceptar cualquier ExpressionNode válido como target
        if (!isValidTarget(target)) {
            throw new IllegalArgumentException(
                "Target must be a variable, identifier, array access, or pointer dereference. Got: " + 
                target.getClass().getSimpleName()
            );
        }
        
        this.target = target;
        this.value = value;
    }

    private boolean isValidTarget(ExpressionNode target) {
        return target instanceof VariableNode ||
               target instanceof IdentifierNode ||
               target instanceof ArrayAccessNode ||
               target instanceof UnaryOpNode && ((UnaryOpNode) target).getOperator().equals("*") ||
               target instanceof BinaryOpNode && ((BinaryOpNode) target).getOperator().equals("[");
    }

    // Método para obtener el nombre de la variable (solo si target es VariableNode)
    public String getVariableName() {
        if (target instanceof VariableNode) {
            return ((VariableNode) target).getName();
        } else if (target instanceof IdentifierNode) {
            return ((IdentifierNode) target).getName();
        }
        return null; // No es una variable simple
    }

    public ExpressionNode getValue() {
        return value;
    }

    // Método para obtener el target (puede ser cualquier ExpressionNode)
    public ExpressionNode getTarget() {
        return target;
    }

    public void setValue(ExpressionNode value) {
        this.value = value;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return "AssignmentNode{" +
               "target=" + (target != null ? target.getClass().getSimpleName() : "null") +
               ", value=" + (value != null ? value.getClass().getSimpleName() : "null") +
               '}';
    }
}