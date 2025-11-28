package org.minic.semantic;

import org.minic.ast.*;
import java.util.*;

public class SemanticChecker {
    private SymbolTable currentScope;
    private List<String> errors;
    private String currentFunction;
    private String currentFunctionReturnType;
    private boolean hasReturnStatement;

    public SemanticChecker() {
        this.errors = new ArrayList<>();
        this.currentScope = new SymbolTable(null);
        this.currentFunction = null;
        this.currentFunctionReturnType = null;
        this.hasReturnStatement = false;
    }

    public void check(AstNode ast) {
        errors.clear();
        
        if (ast instanceof ProgramNode) {
            visitProgram((ProgramNode) ast);
        }
        
        if (!errors.isEmpty()) {
            System.err.println("Errores semánticos encontrados:");
            for (String error : errors) {
                System.err.println("  - " + error);
            }
            throw new RuntimeException("Compilación fallida debido a errores semánticos");
        }
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    private void visitProgram(ProgramNode program) {
        for (AstNode node : program.getChildren()) {
            if (node instanceof VarDeclNode) {
                visitGlobalVarDecl((VarDeclNode) node);
            } else if (node instanceof FunctionNode) {
                visitFunctionDecl((FunctionNode) node);
            }
        }
        
        for (AstNode node : program.getChildren()) {
            if (node instanceof FunctionNode) {
                visitFunction((FunctionNode) node);
            }
        }
        
        Symbol mainSymbol = currentScope.lookup("main");
        if (mainSymbol == null || !mainSymbol.isFunction()) {
            addError("No se encontró la función 'main'");
        }
    }

    private void visitGlobalVarDecl(VarDeclNode varDecl) {
        String name = varDecl.getName();
        String type = varDecl.getType();
        
        if (currentScope.lookupCurrentScope(name) != null) {
            addError("Variable global '" + name + "' ya está declarada");
            return;
        }
        
        if (varDecl.isArray()) {
            int arraySize = varDecl.getArraySize();
            if (arraySize <= 0) {
                addError("Tamaño de array global inválido para '" + name + "': " + arraySize);
            }
        }
        
        Symbol symbol = new Symbol(name, type, false);
        if (!currentScope.addSymbol(symbol)) {
            addError("No se pudo agregar variable global '" + name + "'");
        }
    }

    private void visitFunctionDecl(FunctionNode function) {
        String name = function.getName();
        String returnType = function.getReturnType();
        
        if (currentScope.lookupCurrentScope(name) != null) {
            addError("Función '" + name + "' ya está declarada");
            return;
        }
        
        Symbol symbol = new Symbol(name, returnType, true);
        if (!currentScope.addSymbol(symbol)) {
            addError("No se pudo agregar función '" + name + "'");
        }
    }

    private void visitFunction(FunctionNode function) {
        String oldFunction = currentFunction;
        String oldReturnType = currentFunctionReturnType;
        boolean oldHasReturn = hasReturnStatement;
        
        currentFunction = function.getName();
        currentFunctionReturnType = function.getReturnType();
        hasReturnStatement = false;
        
        SymbolTable functionScope = new SymbolTable(currentScope);
        SymbolTable oldScope = currentScope;
        currentScope = functionScope;
        
        try {
            if (function.getParameters() != null) {
                for (VarDeclNode param : function.getParameters()) {
                    visitParameter(param);
                }
            }
            
            visitBlock(function.getBody());
            
            if (!Type.VOID.equals(currentFunctionReturnType) && !hasReturnStatement) {
                addError("Función '" + currentFunction + "' debe retornar un valor");
            }
            
        } finally {
            currentScope = oldScope;
            currentFunction = oldFunction;
            currentFunctionReturnType = oldReturnType;
            hasReturnStatement = oldHasReturn;
        }
    }

    private void visitParameter(VarDeclNode param) {
        String name = param.getName();
        String type = param.getType();
        
        if (currentScope.lookupCurrentScope(name) != null) {
            addError("Parámetro '" + name + "' ya está declarado en la función");
            return;
        }
        
        Symbol symbol = new Symbol(name, type, false);
        if (!currentScope.addSymbol(symbol)) {
            addError("No se pudo agregar parámetro '" + name + "'");
        }
    }

    private void visitBlock(BlockNode block) {
        SymbolTable blockScope = new SymbolTable(currentScope);
        SymbolTable oldScope = currentScope;
        currentScope = blockScope;
        
        try {
            for (StatementNode stmt : block.getStatements()) {
                visitStatement(stmt);
            }
        } finally {
            currentScope = oldScope;
        }
    }

    private void visitStatement(StatementNode stmt) {
        if (stmt instanceof ExpressionStatementNode) {
            visitExpressionStatement((ExpressionStatementNode) stmt);
        } else if (stmt instanceof ReturnNode) {
            visitReturnStatement((ReturnNode) stmt);
        } else if (stmt instanceof BlockNode) {
            visitBlock((BlockNode) stmt);
        } else if (stmt instanceof IfNode) {
            visitIfStatement((IfNode) stmt);
        } else if (stmt instanceof WhileNode) {
            visitWhileStatement((WhileNode) stmt);
        } else if (stmt instanceof DoWhileNode) {
            visitDoWhileStatement((DoWhileNode) stmt);
        } else if (stmt instanceof ForNode) {
            visitForStatement((ForNode) stmt);
        } else if (stmt instanceof AssignmentNode) {
            visitAssignment((AssignmentNode) stmt);
        }
    }

    private void visitExpressionStatement(ExpressionStatementNode exprStmt) {
        if (exprStmt.getExpressionNode() != null) {
            visitExpression(exprStmt.getExpressionNode());
        }
    }

    private void visitReturnStatement(ReturnNode returnNode) {
        hasReturnStatement = true;
        
        if (returnNode.getReturnValue() != null) {
            String exprType = visitExpression(returnNode.getReturnValue());
            
            if (currentFunctionReturnType != null && !Type.isCompatible(currentFunctionReturnType, exprType)) {
                addError("Tipo de retorno incompatible en función '" + currentFunction + 
                        "'. Esperado: " + currentFunctionReturnType + ", Obtenido: " + exprType);
            }
        } else if (!Type.VOID.equals(currentFunctionReturnType)) {
            addError("Función '" + currentFunction + "' debe retornar un valor de tipo " + currentFunctionReturnType);
        }
    }

    private void visitIfStatement(IfNode ifStmt) {
        String conditionType = visitExpression(ifStmt.getCondition());
        if (!Type.BOOLEAN.equals(conditionType)) {
            addError("La condición del if debe ser de tipo boolean, no " + conditionType);
        }
        
        visitStatement(ifStmt.getThenBlock());
        if (ifStmt.getElseBlock() != null) {
            visitStatement(ifStmt.getElseBlock());
        }
    }

    private void visitWhileStatement(WhileNode whileStmt) {
        String conditionType = visitExpression(whileStmt.getCondition());
        if (!Type.BOOLEAN.equals(conditionType)) {
            addError("La condición del while debe ser de tipo boolean, no " + conditionType);
        }
        
        visitStatement(whileStmt.getBody());
    }

    private void visitDoWhileStatement(DoWhileNode doWhile) {
        visitStatement(doWhile.getBody());
        
        String conditionType = visitExpression(doWhile.getCondition());
        if (!Type.BOOLEAN.equals(conditionType)) {
            addError("La condición del do-while debe ser de tipo boolean, no " + conditionType);
        }
    }

    private void visitForStatement(ForNode forStmt) {
        SymbolTable forScope = new SymbolTable(currentScope);
        SymbolTable oldScope = currentScope;
        currentScope = forScope;
        
        try {
            if (forStmt.getInit() != null) {
                visitStatement(forStmt.getInit());
            }
            
            if (forStmt.getCondition() != null) {
                String conditionType = visitExpression(forStmt.getCondition());
                if (!Type.BOOLEAN.equals(conditionType)) {
                    addError("La condición del for debe ser de tipo boolean, no " + conditionType);
                }
            }
            
            if (forStmt.getIncrement() != null) {
                visitExpression(forStmt.getIncrement());
            }
            
            visitStatement(forStmt.getBody());
            
        } finally {
            currentScope = oldScope;
        }
    }

    private void visitAssignment(AssignmentNode assign) {
        ExpressionNode target = assign.getTarget();
        String valueType = visitExpression(assign.getValue());
        
        if (target instanceof IdentifierNode) {
            String varName = ((IdentifierNode) target).getName();
            Symbol symbol = currentScope.lookup(varName);
            
            if (symbol == null) {
                addError("Variable no declarada: '" + varName + "'");
                return;
            }
            
            if (symbol.isFunction()) {
                addError("No se puede asignar a la función '" + varName + "'");
                return;
            }
            
            if (!Type.isCompatible(symbol.getType(), valueType)) {
                addError("Asignación incompatible para variable '" + varName + 
                        "'. Esperado: " + symbol.getType() + ", Obtenido: " + valueType);
            }
        } else {
            addError("El objetivo de la asignación debe ser un identificador");
        }
    }

    private String visitExpression(ExpressionNode expr) {
        if (expr instanceof NumberNode) {
            return Type.INT;
        } else if (expr instanceof IdentifierNode) {
            return visitIdentifier((IdentifierNode) expr);
        } else if (expr instanceof BinaryOpNode) {
            return visitBinaryOp((BinaryOpNode) expr);
        } else if (expr instanceof UnaryOpNode) {
            return visitUnaryOp((UnaryOpNode) expr);
        } else if (expr instanceof FunctionCallNode) {
            return visitFunctionCall((FunctionCallNode) expr);
        } else if (expr instanceof BooleanNode) {
            return Type.BOOLEAN;
        } else if (expr instanceof StringNode) {
            return Type.STRING;
        } else if (expr instanceof CharNode) {
            return Type.CHAR;
        }
        
        addError("Expresión de tipo desconocido");
        return Type.VOID;
    }

    private String visitIdentifier(IdentifierNode identifier) {
        String name = identifier.getName();
        Symbol symbol = currentScope.lookup(name);
        
        if (symbol == null) {
            addError("Identificador no declarado: '" + name + "'");
            return Type.VOID;
        }
        
        if (symbol.isFunction()) {
            addError("'" + name + "' es una función, no se puede usar como variable");
            return Type.VOID;
        }
        
        return symbol.getType();
    }

    private String visitBinaryOp(BinaryOpNode binOp) {
        String leftType = visitExpression(binOp.getLeft());
        String rightType = visitExpression(binOp.getRight());
        String operator = binOp.getOperator();
        
        if (operator.equals("+") || operator.equals("-") || operator.equals("*") || 
            operator.equals("/") || operator.equals("%")) {
            
            if (!Type.isNumeric(leftType) || !Type.isNumeric(rightType)) {
                addError("Operador '" + operator + "' requiere operandos numéricos, no " + 
                        leftType + " y " + rightType);
                return Type.VOID;
            }
            return Type.INT;
        }
        
        if (operator.equals("==") || operator.equals("!=") || operator.equals("<") || 
            operator.equals(">") || operator.equals("<=") || operator.equals(">=")) {
            
            if (!Type.isCompatible(leftType, rightType)) {
                addError("Operador '" + operator + "' requiere operandos compatibles, no " + 
                        leftType + " y " + rightType);
                return Type.BOOLEAN;
            }
            return Type.BOOLEAN;
        }
        if (operator.equals("&&") || operator.equals("||")) {
            if (!Type.BOOLEAN.equals(leftType) || !Type.BOOLEAN.equals(rightType)) {
                addError("Operador '" + operator + "' requiere operandos booleanos, no " + 
                        leftType + " and " + rightType);
                return Type.BOOLEAN;
            }
            return Type.BOOLEAN;
        }
        
        addError("Operador desconocido: '" + operator + "'");
        return Type.VOID;
    }

    private String visitUnaryOp(UnaryOpNode unaryOp) {
        String operandType = visitExpression(unaryOp.getOperand());
        String operator = unaryOp.getOperator();
        
        if (operator.equals("-")) {
            if (!Type.isNumeric(operandType)) {
                addError("Operador unario '-' requiere operando numérico, no " + operandType);
                return Type.VOID;
            }
            return Type.INT;
        } else if (operator.equals("!")) {
            if (!Type.BOOLEAN.equals(operandType)) {
                addError("Operador unario '!' requiere operando booleano, no " + operandType);
                return Type.VOID;
            }
            return Type.BOOLEAN;
        } else if (operator.equals("&")) {
            return Type.getPointerType(operandType);
        } else if (operator.equals("*")) {
            if (Type.isPointerType(operandType)) {
                return Type.getBaseType(operandType);
            } else {
                addError("Operador '*' requiere operando puntero, no " + operandType);
                return Type.VOID;
            }
        }
        
        addError("Operador unario desconocido: '" + operator + "'");
        return Type.VOID;
    }

    private String visitFunctionCall(FunctionCallNode call) {
    String funcName = call.getFunctionName();
    Symbol symbol = currentScope.lookup(funcName);
    
    if (symbol == null) {
        addError("Función no declarada: '" + funcName + "'");
        return Type.VOID;
    }
    
    if (!symbol.isFunction()) {
        addError("'" + funcName + "' no es una función");
        return Type.VOID;
    }
    
    List<ExpressionNode> args = call.getArguments();
    
    if (isRuntimeFunction(funcName)) {
        return handleRuntimeFunctionCall(funcName, args);
    }

    for (int i = 0; i < args.size(); i++) {
        String argType = visitExpression(args.get(i));
        if (Type.VOID.equals(argType)) {
            addError("Argumento " + (i + 1) + " de la función '" + funcName + "' es inválido");
        }
    }
    
    return symbol.getType();
}

private boolean isRuntimeFunction(String funcName) {
    return funcName.equals("print_int") || funcName.equals("print_str") || 
           funcName.equals("print_char") || funcName.equals("print_bool") ||
           funcName.equals("println") || funcName.equals("read_int") || 
           funcName.equals("read_char");
}

private String handleRuntimeFunctionCall(String funcName, List<ExpressionNode> args) {
    switch (funcName) {
        case "print_int":
        case "print_str":
        case "print_char":
        case "print_bool":
            if (args.size() != 1) {
                addError("Función '" + funcName + "' requiere exactamente 1 argumento, se proporcionaron " + args.size());
            } else {
                String argType = visitExpression(args.get(0));
                if (funcName.equals("print_int") && !Type.INT.equals(argType)) {
                    addError("Función 'print_int' requiere argumento de tipo int, no " + argType);
                } else if (funcName.equals("print_str") && !Type.STRING.equals(argType)) {
                    addError("Función 'print_str' requiere argumento de tipo string, no " + argType);
                } else if (funcName.equals("print_char") && !Type.CHAR.equals(argType)) {
                    addError("Función 'print_char' requiere argumento de tipo char, no " + argType);
                } else if (funcName.equals("print_bool") && !Type.BOOLEAN.equals(argType)) {
                    addError("Función 'print_bool' requiere argumento de tipo bool, no " + argType);
                }
            }
            return Type.VOID; 
            
        case "println":
            if (args.size() != 0) {
                addError("Función 'println' no requiere argumentos, se proporcionaron " + args.size());
            }
            return Type.VOID;
            
        case "read_int":
        case "read_char":
            if (args.size() != 0) {
                addError("Función '" + funcName + "' no requiere argumentos, se proporcionaron " + args.size());
            }
            return funcName.equals("read_int") ? Type.INT : Type.CHAR;
            
        default:
            return Type.VOID;
    }
}

    private void addError(String message) {
        errors.add(message);
    }
}