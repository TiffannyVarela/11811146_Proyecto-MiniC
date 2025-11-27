parser grammar MiniC;

options {tokenVocab=MiniCLexer;}

@header {
package org.minic;
}

// Parser rules
program
    : (declaration | functionDefinition)* EOF
    ;

declaration
    : typeSpecifier initDeclaratorList SEMI
    ;

initDeclaratorList
    : initDeclarator (COMMA initDeclarator)*
    ;

initDeclarator
    : Identifier (LBRACK IntegerConstant RBRACK)*
    | STAR initDeclarator
    ;

typeSpecifier
    : INT
    | CHAR
    | BOOL
    | VOID
    | STRING
    ;

functionDefinition
    : typeSpecifier Identifier LPAREN parameterList? RPAREN compoundStatement
    ;

parameterList
    : parameter (COMMA parameter)*
    ;

parameter
    : typeSpecifier initDeclarator
    ;

compoundStatement
    : LBRACE (declaration | statement)* RBRACE
    ;

statement
    :compoundStatement
    |ifStatement
    |whileStatement
    |forStatement
    |doWhileStatement
    |assignmentStatement
    |returnStatement
    |expressionStatement
    ;

ifStatement
    : IF LPAREN expression RPAREN statement (ELSE statement)?
    ;

whileStatement
    : WHILE LPAREN expression RPAREN statement
    ;

forStatement
    : FOR LPAREN expressionStatement expression? SEMI expression? RPAREN statement
    ;

doWhileStatement
    : DO statement WHILE LPAREN expression RPAREN SEMI
    ;

assignmentStatement
    : lvalue ASSIGN expression SEMI
    ;

returnStatement
    : RETURN expression? SEMI
    ;

expressionStatement
    : expression? SEMI
    ;

// Expressions precedence climbing

expression
    : logicalOrExpression
    ;

logicalOrExpression
    : logicalAndExpression (OR logicalAndExpression)*
    ;

logicalAndExpression
    : equalityExpression (AND equalityExpression)*
    ;

equalityExpression
    : relationalExpression ((EQ | NEQ) relationalExpression)*
    ;

relationalExpression
    : additiveExpression ((LT | GT | LE | GE) additiveExpression)*
    ;

additiveExpression
    : multiplicativeExpression ((PLUS | MINUS) multiplicativeExpression)*
    ;

multiplicativeExpression
    : unaryExpression ((STAR | DIV | MOD) unaryExpression)*
    ;

unaryExpression
    : (NOT | MINUS | AMP| STAR) unaryExpression
    | primaryExpression
    ;

primaryExpression
    : IntegerConstant
    | CharConstant
    | StringLiteral
    | TRUE
    | FALSE
    | LPAREN expression RPAREN
    |lvalue
    |callExpression
    ;

callExpression
    :Identifier LPAREN (expression (COMMA expression)*)? RPAREN
    ;

lvalue
    : Identifier (LBRACK expression RBRACK)*
    ;
