package org.minic.ast;

public interface AstVisitor<T> {

    T visit(ProgramNode node);
    T visit(FunctionNode node);

    T visit(VarDeclNode node);
    T visit(DeclarationNode node);

    T visit (BinaryOpNode node);
    T visit (UnaryOpNode node);
    T visit (VariableNode node);
    T visit (FunctionCallNode node);

    T visit (NumberNode node);
    T visit (CharNode node);
    T visit (StringNode node);
    T visit (BooleanNode node);

    T visit (BlockNode node);
    T visit(IfNode node);
    T visit(WhileNode node);
    T visit(ForNode node);
    T visit(DoWhileNode node);
    T visit(AssignmentNode node);
    T visit(ReturnNode node);
    T visit(ExpressionStatementNode node);


    T visit (IdentifierNode node);
    T visit (LiteralNode node);    
    T visit (ParamNode node);
    T visit (StatementNode node);
    T visit (VarDeclStatementNode node);
    T visit (CastNode node);
    T visit (MemberAccessNode node);
    T visit (ArrayAccessNode node);
    T visit (ArrayDimensionsNode node);
}
