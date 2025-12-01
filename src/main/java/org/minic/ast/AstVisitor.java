package org.minic.ast;


public interface AstVisitor<T> {

    //Nodos de programa
    T visit(ProgramNode node);
    T visit(FunctionNode node);

    //Declaraciones
    T visit(VarDeclNode node);
    T visit(DeclarationNode node);

    //Expresiones
    T visit (BinaryOpNode node);
    T visit (UnaryOpNode node);
    T visit (VariableNode node);
    T visit (FunctionCallNode node);

    //Literales
    T visit (NumberNode node);
    T visit (CharNode node);
    T visit (StringNode node);
    T visit (BooleanNode node);

    //Statements
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

}
