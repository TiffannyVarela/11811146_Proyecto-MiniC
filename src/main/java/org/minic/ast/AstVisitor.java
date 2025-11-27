package org.minic.ast;


public interface AstVisitor<T> {

    //Declaraciones
    T visit(FunctionNode node);
    T visit(VarDeclNode node);

    //Statements
    T visit (BlockNode node);
    T visit(ReturnNode node);//
    T visit(IfNode node);//
    T visit(WhileNode node);//
    T visit(ExpressionStatementNode node);//

    //Expresiones
    T visit (AssignmentNode node);//
    T visit (BinaryOpNode node);
    T visit (UnaryOpNode node);
    T visit (VariableNode node);//
    T visit (IdentifierNode node);
    T visit (LiteralNode node);
    T visit (FunctionCallNode node);//
    T visit (NumberNode node);//
    T visit (ParamNode node);
    T visit (CharNode node);//
    T visit (StringNode node);//
    T visit (BooleanNode node);//

    //Nodo raiz
    T visit(ProgramNode node);

}
