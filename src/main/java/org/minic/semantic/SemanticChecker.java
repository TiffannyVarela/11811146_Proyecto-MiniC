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

    private void registerRuntimeFunctions() {
        currentScope.addSymbol(new Symbol("print_int", Type.VOID, true));
        currentScope.addSymbol(new Symbol("print_string", Type.VOID, true));
        currentScope.addSymbol(new Symbol("print_char", Type.VOID, true));
        currentScope.addSymbol(new Symbol("print_bool", Type.VOID, true));
        currentScope.addSymbol(new Symbol("println", Type.VOID, true));
        currentScope.addSymbol(new Symbol("read_int", Type.INT, true));
        currentScope.addSymbol(new Symbol("read_string", Type.STRING, true));
        currentScope.addSymbol(new Symbol("read_char", Type.CHAR, true));
    }

    public void check(AstNode ast) {
        errors.clear();
        registerRuntimeFunctions();

        System.out.println("=== INICIANDO ANÁLISIS SEMÁNTICO ===");
        System.out.println("Tipo del nodo raíz: " + ast.getClass().getSimpleName());

        if (ast instanceof ProgramNode) {
            ProgramNode program = (ProgramNode) ast;
            System.out.println("ProgramNode tiene " + program.getChildren().size() + " hijos");
            for (AstNode node : program.getChildren()) {
                System.out.println(" - Hijo: " + node.getClass().getSimpleName());
            }
            visitProgram(program);
        } else {
            addError("El nodo raíz debe ser un ProgramNode");
        }
        
        if (!errors.isEmpty()) {
            System.err.println("Errores semánticos encontrados:");
            for (String error : errors) {
                System.err.println("  - " + error);
            }
            throw new RuntimeException("Compilación fallida debido a errores semánticos");
        }
        System.out.println("=== FIN ANÁLISIS SEMÁNTICO ===");
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    private void visitProgram(ProgramNode program) {
        for (AstNode node : program.getChildren()) {
            if (node instanceof VarDeclNode) {
                visitGlobalVarDecl((VarDeclNode) node);
            } else if (node instanceof FunctionNode) {
                FunctionNode func = (FunctionNode) node;
                if (func.getBody() != null) {
                    visitFunctionDecl(func);
                }
            }
        }
        
        for (AstNode node : program.getChildren()) {
            if (node instanceof FunctionNode) {
                FunctionNode func = (FunctionNode) node;
                if (func.getBody() != null) {
                    visitFunction(func);
                }
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
        
        System.out.println("SEMANTIC CHECKER: Visitando bloque con " + block.getStatements().size() + " statements");
        
        try {
            for (StatementNode stmt : block.getStatements()) {
                System.out.println("SEMANTIC CHECKER: Procesando statement: " + stmt.getClass().getSimpleName());
                visitStatement(stmt);
            }
        } finally {
            currentScope = oldScope;
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

    public Void visit(VarDeclNode node) {
        String name = node.getName();
        String type = node.getType();
        
        System.out.println("SEMANTIC CHECKER: Registrando variable en tabla de símbolos: " + type + " " + name);
        if (currentScope.lookupCurrentScope(name) != null) {
            addError("Variable '" + name + "' ya está declarada en este ámbito");
            return null;
        }
        
        Symbol symbol = new Symbol(name, type, false);
        if (currentScope.addSymbol(symbol)) {
            System.out.println("SEMANTIC CHECKER: ✓ Variable '" + name + "' registrada exitosamente");
        } else {
            System.out.println("SEMANTIC CHECKER: ✗ No se pudo registrar variable '" + name + "'");
            addError("No se pudo agregar variable '" + name + "'");
        }

        if (node.hasInitialNode()) {
            System.out.println("SEMANTIC CHECKER: Verificando inicialización para: " + name);
            String initType = visitExpression(node.getInitialNode());
            if (!Type.isCompatible(type, initType)) {
                addError("Inicialización incompatible para variable '" + name + 
                        "'. Esperado: " + type + ", Obtenido: " + initType);
            }
        }
        
        return null;
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
        
        if (target instanceof VariableNode) {
            String varName = ((VariableNode) target).getName();
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

    private String visitVariable(VariableNode variable) {
        String name = variable.getName();
        Symbol symbol = currentScope.lookup(name);
        
        System.out.println("SEMANTIC CHECKER: Buscando variable: " + name);
        
        if (symbol == null) {
            addError("Variable no declarada: '" + name + "'");
            return Type.VOID;
        }
        
        if (symbol.isFunction()) {
            addError("'" + name + "' es una función, no se puede usar como variable");
            return Type.VOID;
        }
        
        System.out.println("SEMANTIC CHECKER: ✓ Variable encontrada: " + name + " de tipo " + symbol.getType());
        return symbol.getType();
    }

    private String visitExpression(ExpressionNode expr) {
        if (expr instanceof NumberNode) {
            return Type.INT;
        } else if (expr instanceof VariableNode) {
            return visitVariable((VariableNode) expr);
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
        
        addError("Expresión de tipo desconocido: " + (expr != null ? expr.getClass().getSimpleName() : "null"));
        return Type.VOID;
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
        return funcName.equals("print_int") || funcName.equals("print_string") || 
            funcName.equals("print_char") || funcName.equals("print_bool") ||
            funcName.equals("println") || funcName.equals("read_int")|| 
            funcName.equals("read_string") || 
            funcName.equals("read_char");
    }

    private String handleRuntimeFunctionCall(String funcName, List<ExpressionNode> args) {
        switch (funcName) {
            case "print_int":
            case "print_string":
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

    public Void visit(VarDeclStatementNode node) {
        System.out.println("SEMANTIC CHECKER: === VISITANDO VarDeclStatementNode ===");
        System.out.println("SEMANTIC CHECKER: Variable: " + node.getVarDeclNode().getType() + " " + node.getVarDeclNode().getName());
        visit(node.getVarDeclNode());
        System.out.println("SEMANTIC CHECKER: === FIN VarDeclStatementNode ===");
        return null;
    }

    private void visitStatement(StatementNode stmt) {
        System.out.println("SEMANTIC CHECKER: visitStatement - Tipo: " + stmt.getClass().getSimpleName());
        
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
        } else if (stmt instanceof VarDeclStatementNode) {
            System.out.println("SEMANTIC CHECKER: Encontrado VarDeclStatementNode - llamando a visit");
            visit((VarDeclStatementNode) stmt);
        } else {
            addError("Tipo de statement no soportado: " + stmt.getClass().getSimpleName());
        }
    }
}



