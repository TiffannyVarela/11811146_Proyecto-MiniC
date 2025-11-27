lexer grammar MiniCLexer;

@header {
package org.minic;
}

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
CharConstant: '\'' . '\'';
StringLiteral: '"' (~["\r\n])* '"';

// Espacios y comentarios
Whitespace: [ \t\r\n]+ -> skip;
LineComment: '//' ~[\r\n]* -> skip;
BlockComment: '/*' .*? '*/' -> skip;
