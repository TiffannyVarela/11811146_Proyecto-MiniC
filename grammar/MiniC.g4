grammar MiniC;

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
    | Identifier (LBRACK IntegerConstant RBRACK)* ASSIGN expression
    | Identifier ASSIGN expression
    | STAR Identifier
    | STAR Identifier ASSIGN expression
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
    : typeSpecifier Identifier
    ;

compoundStatement
    : LBRACE (declaration | statement)* RBRACE
    ;

statement
    : compoundStatement
    | ifStatement
    | whileStatement
    | forStatement
    | doWhileStatement
    | assignmentStatement
    | returnStatement
    | expressionStatement
    ;

ifStatement
    : IF LPAREN expression RPAREN statement (ELSE statement)?
    ;

whileStatement
    : WHILE LPAREN expression RPAREN statement
    ;

forStatement
    : FOR LPAREN forInit? SEMI expression? SEMI forUpdate? RPAREN statement
    ;

forInit
    : expressionStatement
    | declaration
    ;

forUpdate
    : expression
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

// Expressions
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
    : (NOT | MINUS | AMP | STAR) unaryExpression
    | postfixExpression
    ;

postfixExpression
    : primaryExpression
    | callExpression
    ;

primaryExpression
    : IntegerConstant
    | CharConstant
    | StringLiteral
    | TRUE
    | FALSE
    | LPAREN expression RPAREN
    | lvalue
    ;

callExpression
    : Identifier LPAREN argumentList? RPAREN
    ;

argumentList
    : expression (COMMA expression)*
    ;

lvalue
    : Identifier
    | STAR expression
    | lvalue LBRACK expression RBRACK
    ;

// Lexer rules

// Palabras reservadas
INT: 'int';
CHAR: 'char';
BOOL: 'bool';
VOID: 'void';
STRING: 'string';

IF: 'if';
ELSE: 'else';
WHILE: 'while';
FOR: 'for';
DO: 'do';
RETURN: 'return';
TRUE: 'true';
FALSE: 'false';

// Símbolos
SEMI: ';';
COMMA: ',';
ASSIGN: '=';

LPAREN: '(';
RPAREN: ')';
LBRACE: '{';
RBRACE: '}';
LBRACK: '[';
RBRACK: ']';

PLUS: '+';
MINUS: '-';
STAR: '*';
DIV: '/';
MOD: '%';

NOT:  '!';
AND:  '&&';
OR:   '||';
EQ:   '==';
NEQ:  '!=';
LT:   '<';
GT:   '>';
LE:   '<=';
GE:   '>=';
AMP: '&';

// Identificadores y literales
Identifier: [a-zA-Z_][a-zA-Z0-9_]*;
IntegerConstant: [0-9]+;
CharConstant: '\'' . '\'';  // Carácter simple
StringLiteral: '"' (~["\\\r\n] | '\\' ["\\nrt])* '"';

// Espacios y comentarios
Whitespace: [ \t\r\n]+ -> skip;
LineComment: '//' ~[\r\n]* -> skip;
BlockComment: '/*' .*? '*/' -> skip;