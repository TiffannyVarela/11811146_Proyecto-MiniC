package org.minic.semantic;

import org.minic.ast.*;
import java.util.*;

public class SemanticChecker implements AstVisitor<Void> {
    private SymbolTable currentScope;
    private String currentFunction;
    private String currentFunctionReturnType;
    private boolean hasReturnStatement;
    private List<VarDeclNode> globalDeclarations;
    private ProgramNode currentProgram; 
    private Map<String, VarDeclNode> localDeclarations;

    public SemanticChecker() {
        this.currentScope = new SymbolTable(null);
        this.currentFunction = null;
        this.currentFunctionReturnType = null;
        this.hasReturnStatement = false;
        this.globalDeclarations = new ArrayList<>(); 
        this.localDeclarations = new HashMap<>();
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
        org.minic.ErrorManager.cleanErrors();
        registerRuntimeFunctions();

        System.out.println("=== INICIANDO ANÁLISIS SEMÁNTICO ===");
        System.out.println("Tipo del nodo raíz: " + ast.getClass().getSimpleName());

        if (ast instanceof ProgramNode) {
            this.currentProgram = (ProgramNode) ast;
            for(AstNode child : currentProgram.getDeclarationsNodes()){
                if (child instanceof FunctionNode) {
                    visitFunctionDecl((FunctionNode) child);
                }
            }
            collectGlobalDeclarations((ProgramNode) ast);
            ast.accept(this);

            checkFunctionDuplicates();
            checkGlobalInitializers();
            checkUnusedGlobalVariables();
            checkMainFunction();
        } else {
            addError("El nodo raíz debe ser un ProgramNode");
        }
        
        if (org.minic.ErrorManager.hasErrors()) {
            System.err.println("Errores semánticos encontrados: ");
            org.minic.ErrorManager.printErrors();
            org.minic.ErrorManager.throwIfErrors();
        }
        System.out.println("=== FIN ANÁLISIS SEMÁNTICO ===");
    }

    @Override
    public Void visit(ProgramNode node) {
        for(AstNode child : node.getDeclarationsNodes()){
            child.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(FunctionNode node) {
        String oldFunction = currentFunction;
        String oldReturnType = currentFunctionReturnType;
        boolean oldHasReturn = hasReturnStatement;
        
        currentFunction = node.getName();
        currentFunctionReturnType = node.getReturnType();
        hasReturnStatement = false;
        
        SymbolTable functionScope = new SymbolTable(currentScope);
        SymbolTable oldScope = currentScope;
        currentScope = functionScope;
        
        try {
            // Registrar parámetros - AHORA USANDO VarDeclNode
            if (node.getParameters() != null) {
                for (VarDeclNode param : node.getParameters()) {
                    // Llamar directamente a visit(VarDeclNode) en lugar de param.accept(this)
                    visit(param);
                }
            }
            
            // Visitar el cuerpo
            if (node.getBody() != null) {
                node.getBody().accept(this);
            }
            
            // Verificar return
            if (!Type.VOID.equals(currentFunctionReturnType) && !hasReturnStatement) {
                addError("Función '" + currentFunction + "' debe retornar un valor");
            }
            
        } finally {
            currentScope = oldScope;
            currentFunction = oldFunction;
            currentFunctionReturnType = oldReturnType;
            hasReturnStatement = oldHasReturn;
        }
        return null;
    }
    @Override
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
            localDeclarations.put(name, node);
        } else {
            System.out.println("SEMANTIC CHECKER: ✗ No se pudo registrar variable '" + name + "'");
            addError("No se pudo agregar variable '" + name + "'");
        }

        if (node.hasInitialNode()) {
            System.out.println("SEMANTIC CHECKER: Verificando inicialización para: " + name);
            String initType = getExpressionType(node.getInitialNode());
            if (!Type.isCompatible(type, initType)) {
                addError("Inicialización incompatible para variable '" + name + 
                        "'. Esperado: " + type + ", Obtenido: " + initType);
            }
        }
        
        return null;
    }

    @Override
    public Void visit(VarDeclStatementNode node) {
        System.out.println("SEMANTIC CHECKER: === VISITANDO VarDeclStatementNode ===");
        System.out.println("SEMANTIC CHECKER: Variable: " + node.getVarDeclNode().getType() + " " + node.getVarDeclNode().getName());
        node.getVarDeclNode().accept(this);
        System.out.println("SEMANTIC CHECKER: === FIN VarDeclStatementNode ===");
        return null;
    }

    @Override
    public Void visit(BlockNode node) {
        SymbolTable blockScope = new SymbolTable(currentScope);
        SymbolTable oldScope = currentScope;
        Map<String, VarDeclNode> oldLocals = new HashMap<>(localDeclarations);
        localDeclarations.clear();
        
        currentScope = blockScope;
        
        System.out.println("SEMANTIC CHECKER: Visitando bloque con " + node.getStatements().size() + " statements");
        
        try {
            for (StatementNode stmt : node.getStatements()) {
                stmt.accept(this);
            }
        } finally {
            currentScope = oldScope;
            localDeclarations = oldLocals;
        }
        return null;
    }

    @Override
    public Void visit(IfNode node) {
        String conditionType = getExpressionType(node.getCondition());
        if (!Type.BOOLEAN.equals(conditionType)) {
            addError("La condición del if debe ser de tipo boolean, no " + conditionType);
        }
        
        node.getThenBlock().accept(this);
        if (node.getElseBlock() != null) {
            node.getElseBlock().accept(this);
        }
        return null;
    }

    @Override
    public Void visit(WhileNode node) {
        String conditionType = getExpressionType(node.getCondition());
        if (!Type.BOOLEAN.equals(conditionType)) {
            addError("La condición del while debe ser de tipo boolean, no " + conditionType);
        }
        
        node.getBody().accept(this);
        return null;
    }

    @Override
    public Void visit(DoWhileNode node) {
        node.getBody().accept(this);
        
        String conditionType = getExpressionType(node.getCondition());
        if (!Type.BOOLEAN.equals(conditionType)) {
            addError("La condición del do-while debe ser de tipo boolean, no " + conditionType);
        }
        return null;
    }

    @Override
    public Void visit(ForNode node) {
        SymbolTable forScope = new SymbolTable(currentScope);
        SymbolTable oldScope = currentScope;
        currentScope = forScope;
        
        try {
            if (node.getInit() != null) {
                node.getInit().accept(this);
            }
            
            if (node.getCondition() != null) {
                String conditionType = getExpressionType(node.getCondition());
                if (!Type.BOOLEAN.equals(conditionType)) {
                    addError("La condición del for debe ser de tipo boolean, no " + conditionType);
                }
            }
            
            if (node.getIncrement() != null) {
                getExpressionType(node.getIncrement()); // Solo para verificación
            }
            
            node.getBody().accept(this);
            
        } finally {
            currentScope = oldScope;
        }
        return null;
    }

    @Override
    public Void visit(ReturnNode node) {
        hasReturnStatement = true;
        
        if (node.getReturnValue() != null) {
            String exprType = getExpressionType(node.getReturnValue());
            
            if (currentFunctionReturnType != null && !Type.isCompatible(currentFunctionReturnType, exprType)) {
                addError("Tipo de retorno incompatible en función '" + currentFunction + 
                        "'. Esperado: " + currentFunctionReturnType + ", Obtenido: " + exprType);
            }
        } else if (!Type.VOID.equals(currentFunctionReturnType)) {
            addError("Función '" + currentFunction + "' debe retornar un valor de tipo " + currentFunctionReturnType);
        }
        return null;
    }

    @Override
    public Void visit(ExpressionStatementNode node) {
        if (node.getExpressionNode() != null) {
            getExpressionType(node.getExpressionNode());
        }
        return null;
    }

    @Override
    public Void visit(AssignmentNode node) {
        ExpressionNode target = node.getTarget();
        String valueType = getExpressionType(node.getValue());
        
        if (target instanceof VariableNode) {
            String varName = ((VariableNode) target).getName();
            Symbol symbol = currentScope.lookup(varName);
            
            if (symbol == null) {
                addError("Variable no declarada: '" + varName + "'");
                return null;
            }
            
            if (symbol.isFunction()) {
                addError("No se puede asignar a la función '" + varName + "'");
                return null;
            }
            
            if (!Type.isCompatible(symbol.getType(), valueType)) {
                addError("Asignación incompatible para variable '" + varName + 
                        "'. Esperado: " + symbol.getType() + ", Obtenido: " + valueType);
            }
            
            checkArrayAccess(target, "asignación a " + varName);
            
        } else if (target instanceof BinaryOpNode) {
            BinaryOpNode arrayAccess = (BinaryOpNode) target;
            if (arrayAccess.getOperator().equals("[")) {
                ExpressionNode arrayExpr = arrayAccess.getLeft();
                ExpressionNode indexExpr = arrayAccess.getRight();
                
                if (arrayExpr instanceof VariableNode) {
                    String arrayName = ((VariableNode) arrayExpr).getName();
                    Symbol symbol = currentScope.lookup(arrayName);
                    
                    if (symbol == null) {
                        addError("Arreglo no declarado: '" + arrayName + "'");
                        return null;
                    }
                    
                    if (!isArrayType(symbol.getType())) {
                        addError("'" + arrayName + "' no es un arreglo");
                        return null;
                    }
                    
                    String indexType = getExpressionType(indexExpr);
                    if (!Type.INT.equals(indexType)) {
                        addError("Índice de arreglo debe ser de tipo int, no " + indexType);
                    }
                    
                    if (!Type.isCompatible(symbol.getType(), valueType)) {
                        addError("Asignación incompatible para arreglo '" + arrayName + 
                                "'. Esperado: " + symbol.getType() + ", Obtenido: " + valueType);
                    }
                }
            }
        } else {
            addError("El objetivo de la asignación debe ser un identificador o acceso a arreglo");
        }
        return null;
    }

    @Override
    public Void visit(BinaryOpNode node) {
        getExpressionType(node.getLeft());
        getExpressionType(node.getRight());
        return null;
    }

    @Override
    public Void visit(UnaryOpNode node) {
        getExpressionType(node.getOperand());
        return null;
    }

    @Override
    public Void visit(VariableNode node) {
        String name = node.getName();
        Symbol symbol = currentScope.lookup(name);
        
        System.out.println("SEMANTIC CHECKER: Buscando variable: " + name);
        
        if (symbol == null) {
            addError("Variable no declarada: '" + name + "'");
            return null;
        }
        
        if (symbol.isFunction()) {
            addError("'" + name + "' es una función, no se puede usar como variable");
            return null;
        }
        
        System.out.println("SEMANTIC CHECKER: ✓ Variable encontrada: " + name + " de tipo " + symbol.getType());
        return null;
    }

    @Override
    public Void visit(FunctionCallNode node) {
        String funcName = node.getFunctionName();
        Symbol symbol = currentScope.lookup(funcName);
        
        if (symbol == null) {
            addError("Función no declarada: '" + funcName + "'");
            return null;
        }
        
        if (!symbol.isFunction()) {
            addError("'" + funcName + "' no es una función");
            return null;
        }
        
        for (ExpressionNode arg : node.getArguments()) {
            getExpressionType(arg);
        }
        
        return null;
    }

    @Override
    public Void visit(NumberNode node) {
        return null;
    }

    @Override
    public Void visit(CharNode node) {
        return null;
    }

    @Override
    public Void visit(StringNode node) {
        return null;
    }

    @Override
    public Void visit(BooleanNode node) {
        return null;
    }

    @Override
    public Void visit(DeclarationNode node) {
        // Implementar según sea necesario
        return null;
    }

    @Override
    public Void visit(IdentifierNode node) {
        // Implementar según sea necesario
        return null;
    }

    @Override
    public Void visit(LiteralNode node) {
        // Implementar según sea necesario
        return null;
    }

    @Override
    public Void visit(ParamNode node) {
        String name = node.getName();
        String type = node.getType();
        
        if (currentScope.lookupCurrentScope(name) != null) {
            addError("Parámetro '" + name + "' ya está declarado en la función");
            return null;
        }
        
        Symbol symbol = new Symbol(name, type, false);
        if (!currentScope.addSymbol(symbol)) {
            addError("No se pudo agregar parámetro '" + name + "'");
        }
        return null;
    }

    @Override
    public Void visit(StatementNode node) {
        // Este método podría no necesitar implementación si todas las subclases están cubiertas
        return null;
    }

    // Métodos auxiliares
    private String getExpressionType(ExpressionNode expr) {
        if (expr instanceof NumberNode) {
            return Type.INT;
        } else if (expr instanceof VariableNode) {
            return getVariableType((VariableNode) expr);
        } else if (expr instanceof BinaryOpNode) {
            return getBinaryOpType((BinaryOpNode) expr);
        } else if (expr instanceof UnaryOpNode) {
            return getUnaryOpType((UnaryOpNode) expr);
        } else if (expr instanceof FunctionCallNode) {
            return getFunctionCallType((FunctionCallNode) expr);
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

    private String getVariableType(VariableNode variable) {
        String name = variable.getName();
        Symbol symbol = currentScope.lookup(name);
        
        if (symbol == null) {
            addError("Variable no declarada: '" + name + "'");
            return Type.VOID;
        }
        
        if (symbol.isFunction()) {
            addError("'" + name + "' es una función, no se puede usar como variable");
            return Type.VOID;
        }
        
        return symbol.getType();
    }

    private String getBinaryOpType(BinaryOpNode binOp) {
        String leftType = getExpressionType(binOp.getLeft());
        String rightType = getExpressionType(binOp.getRight());
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
        
        if (operator.equals("[")) {
            // Es un acceso a arreglo
            return getArrayAccessType(binOp);
        }
        
        addError("Operador desconocido: '" + operator + "'");
        return Type.VOID;
    }

    private String getArrayAccessType(BinaryOpNode arrayAccess) {
        ExpressionNode arrayExpr = arrayAccess.getLeft();
        ExpressionNode indexExpr = arrayAccess.getRight();
        
        if (arrayExpr instanceof VariableNode) {
            String arrayName = ((VariableNode) arrayExpr).getName();
            Symbol symbol = currentScope.lookup(arrayName);
            
            if (symbol == null) {
                addError("Arreglo no declarado: '" + arrayName + "'");
                return Type.VOID;
            }
            
            if (!isArrayType(symbol.getType())) {
                addError("'" + arrayName + "' no es un arreglo");
                return Type.VOID;
            }
            
            String indexType = getExpressionType(indexExpr);
            if (!Type.INT.equals(indexType)) {
                addError("Índice de arreglo debe ser de tipo int, no " + indexType);
            }
            
            return Type.getBaseType(symbol.getType());
        }
        
        return Type.VOID;
    }

    private String getUnaryOpType(UnaryOpNode unaryOp) {
        String operandType = getExpressionType(unaryOp.getOperand());
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

    private String getFunctionCallType(FunctionCallNode call) {
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
        
        // Verificar argumentos para funciones runtime
        if (isRuntimeFunction(funcName)) {
            return handleRuntimeFunctionCall(funcName, call.getArguments());
        }
        
        return symbol.getType();
    }

    private void checkArrayAccess(ExpressionNode expr, String context) {
        if (expr instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) expr;
            if (binOp.getOperator().equals("[")) {
                ExpressionNode arrayExpr = binOp.getLeft();
                ExpressionNode indexExpr = binOp.getRight();
                
                if (arrayExpr instanceof VariableNode) {
                    String arrayName = ((VariableNode) arrayExpr).getName();
                    Symbol symbol = currentScope.lookup(arrayName);
                    
                    if (symbol != null && !symbol.isFunction()) {
                        if (isArrayType(symbol.getType())) {
                            String indexType = getExpressionType(indexExpr);
                            if (!Type.INT.equals(indexType)) {
                                addError("Índice de arreglo debe ser de tipo int, no " + indexType);
                            }
                            
                            if (indexExpr instanceof NumberNode) {
                                VarDeclNode arrayDecl = findArrayDecl(arrayName);
                                if (arrayDecl != null) {
                                    checkArrayBounds(arrayDecl, indexExpr, context);
                                }
                            }
                        } else {
                            addError("'" + arrayName + "' no es un arreglo");
                        }
                    }
                }
            }
        }
    }

    private boolean isArrayType(String type) {
        return type != null && type.contains("[");
    }

    private void checkArrayBounds(VarDeclNode array, ExpressionNode index, String context) {
        if (array.isArray() && index instanceof NumberNode) {
            int indexValue = ((NumberNode) index).getValue();
            int arraySize = array.getArraySize();
            if (indexValue < 0 || indexValue >= arraySize) {
                addError("Índice fuera de límites en " + context + 
                        ": " + indexValue + " para arreglo de tamaño " + arraySize);
            }
        }
    }

    private VarDeclNode findArrayDecl(String arrayName) {
        Symbol symbol = currentScope.lookup(arrayName);
        if (symbol != null && !symbol.isFunction()) {
            VarDeclNode localDecl = localDeclarations.get(arrayName);
            if (localDecl != null) return localDecl;
            return findInGlobalDeclarations(arrayName);
        }
        return null;
    }

    private VarDeclNode findInGlobalDeclarations(String arrayName) {
        for (VarDeclNode decl : globalDeclarations) {
            if (decl.getName().equals(arrayName)) {
                return decl;
            }
        }
        return null;
    }

    private void collectGlobalDeclarations(ProgramNode program) {
        this.currentProgram = program;
        globalDeclarations.clear();
        
        for (AstNode node : program.getDeclarationsNodes()) {
            if (node instanceof VarDeclNode) {
                globalDeclarations.add((VarDeclNode) node);
            }
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
                    String argType = getExpressionType(args.get(0));
                    if (funcName.equals("print_int") && !Type.INT.equals(argType)) {
                        addError("Función 'print_int' requiere argumento de tipo int, no " + argType);
                    } else if (funcName.equals("print_string") && !Type.STRING.equals(argType)) {
                        addError("Función 'print_string' requiere argumento de tipo string, no " + argType);
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
        org.minic.ErrorManager.addError(message);
    }

    private void checkMainFunction() {
        if (currentProgram == null) return;
        
        boolean hasMain = false;
        for (AstNode node : currentProgram.getDeclarationsNodes()) {
            if (node instanceof FunctionNode) {
                FunctionNode func = (FunctionNode) node;
                if (func.getName().equals("main")) {
                    hasMain = true;
                    if (!Type.INT.equals(func.getReturnType())) {
                        addError("Función 'main' debe retornar int, no " + func.getReturnType());
                    }
                    if (func.getParameters() != null && !func.getParameters().isEmpty()) {
                        addError("Función 'main' no debe tener parámetros");
                    }
                    break;
                }
            }
        }
        
        if (!hasMain) {
            addError("No se encontró la función 'main'");
        }
    }

    // Métodos para verificación adicional
    private void checkFunctionDuplicates() {
        if (currentProgram == null) return;
        
        Set<String> functionNames = new HashSet<>();
        for (AstNode node : currentProgram.getDeclarationsNodes()) {
            if (node instanceof FunctionNode) {
                FunctionNode func = (FunctionNode) node;
                String name = func.getName();
                
                if (functionNames.contains(name)) {
                    addError("Función '" + name + "' está duplicada");
                } else {
                    functionNames.add(name);
                }
            }
        }
    }

    private void checkGlobalInitializers() {
        if (currentProgram == null) return;
        
        for (AstNode node : currentProgram.getDeclarationsNodes()) {
            if (node instanceof VarDeclNode) {
                VarDeclNode varDecl = (VarDeclNode) node;
                if (varDecl.hasInitialNode()) {
                    checkConstantInitializer(varDecl.getInitialNode(), varDecl.getName());
                }
            }
        }
    }

    private void checkConstantInitializer(ExpressionNode expr, String varName) {
        if (!isConstantExpression(expr)) {
            addError("Inicializador para variable global '" + varName + "' debe ser constante");
        }
    }

    private boolean isConstantExpression(ExpressionNode expr) {
        if (expr instanceof NumberNode || expr instanceof BooleanNode || 
            expr instanceof CharNode || expr instanceof StringNode) {
            return true;
        } else if (expr instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) expr;
            return isConstantExpression(binOp.getLeft()) && 
                   isConstantExpression(binOp.getRight());
        } else if (expr instanceof UnaryOpNode) {
            UnaryOpNode unary = (UnaryOpNode) expr;
            return isConstantExpression(unary.getOperand());
        }
        return false;
    }

    private void checkUnusedGlobalVariables() {
        if (currentProgram == null || globalDeclarations.isEmpty()) return;
        
        Set<String> usedGlobals = new HashSet<>();
        collectUsedGlobalVariables(currentProgram, usedGlobals);
        
        for (VarDeclNode global : globalDeclarations) {
            if (!usedGlobals.contains(global.getName())) {
                System.out.println("Advertencia: Variable global '" + global.getName() + "' declarada pero no usada");
            }
        }
    }

    private void collectUsedGlobalVariables(ProgramNode program, Set<String> usedGlobals) {
        for (AstNode node : program.getDeclarationsNodes()) {
            collectUsedGlobalVariables(node, usedGlobals);
        }
    }

    private void collectUsedGlobalVariables(AstNode node, Set<String> usedGlobals) {
        if (node == null) return;
        
        // Buscar recursivamente usos de variables globales
        if (node instanceof VariableNode) {
            VariableNode varNode = (VariableNode) node;
            for (VarDeclNode global : globalDeclarations) {
                if (global.getName().equals(varNode.getName())) {
                    usedGlobals.add(varNode.getName());
                    break;
                }
            }
        } else if (node instanceof IdentifierNode) {
            IdentifierNode idNode = (IdentifierNode) node;
            for (VarDeclNode global : globalDeclarations) {
                if (global.getName().equals(idNode.getName())) {
                    usedGlobals.add(idNode.getName());
                    break;
                }
            }
        }
        
        // Visitar recursivamente según el tipo de nodo
        if (node instanceof ProgramNode) {
            ProgramNode program = (ProgramNode) node;
            for (AstNode child : program.getDeclarationsNodes()) {
                collectUsedGlobalVariables(child, usedGlobals);
            }
        } else if (node instanceof BlockNode) {
            BlockNode block = (BlockNode) node;
            for (StatementNode stmt : block.getStatements()) {
                collectUsedGlobalVariables(stmt, usedGlobals);
            }
        } else if (node instanceof FunctionNode) {
            FunctionNode func = (FunctionNode) node;
            // Visitar parámetros
            if (func.getParameters() != null) {
                for (VarDeclNode param : func.getParameters()) {
                    collectUsedGlobalVariables(param, usedGlobals);
                }
            }
            // Visitar cuerpo
            if (func.getBody() != null) {
                collectUsedGlobalVariables(func.getBody(), usedGlobals);
            }
        } else if (node instanceof IfNode) {
            IfNode ifNode = (IfNode) node;
            collectUsedGlobalVariables(ifNode.getCondition(), usedGlobals);
            collectUsedGlobalVariables(ifNode.getThenBlock(), usedGlobals);
            if (ifNode.getElseBlock() != null) {
                collectUsedGlobalVariables(ifNode.getElseBlock(), usedGlobals);
            }
        } else if (node instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) node;
            collectUsedGlobalVariables(whileNode.getCondition(), usedGlobals);
            collectUsedGlobalVariables(whileNode.getBody(), usedGlobals);
        } else if (node instanceof DoWhileNode) {
            DoWhileNode doWhile = (DoWhileNode) node;
            collectUsedGlobalVariables(doWhile.getBody(), usedGlobals);
            collectUsedGlobalVariables(doWhile.getCondition(), usedGlobals);
        } else if (node instanceof ForNode) {
            ForNode forNode = (ForNode) node;
            if (forNode.getInit() != null) {
                collectUsedGlobalVariables(forNode.getInit(), usedGlobals);
            }
            if (forNode.getCondition() != null) {
                collectUsedGlobalVariables(forNode.getCondition(), usedGlobals);
            }
            if (forNode.getIncrement() != null) {
                collectUsedGlobalVariables(forNode.getIncrement(), usedGlobals);
            }
            collectUsedGlobalVariables(forNode.getBody(), usedGlobals);
        } else if (node instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) node;
            collectUsedGlobalVariables(binOp.getLeft(), usedGlobals);
            collectUsedGlobalVariables(binOp.getRight(), usedGlobals);
        } else if (node instanceof UnaryOpNode) {
            UnaryOpNode unary = (UnaryOpNode) node;
            collectUsedGlobalVariables(unary.getOperand(), usedGlobals);
        } else if (node instanceof FunctionCallNode) {
            FunctionCallNode call = (FunctionCallNode) node;
            for (ExpressionNode arg : call.getArguments()) {
                collectUsedGlobalVariables(arg, usedGlobals);
            }
        } else if (node instanceof VarDeclNode) {
            VarDeclNode varDecl = (VarDeclNode) node;
            if (varDecl.hasInitialNode()) {
                collectUsedGlobalVariables(varDecl.getInitialNode(), usedGlobals);
            }
        } else if (node instanceof AssignmentNode) {
            AssignmentNode assign = (AssignmentNode) node;
            collectUsedGlobalVariables(assign.getTarget(), usedGlobals);
            collectUsedGlobalVariables(assign.getValue(), usedGlobals);
        } else if (node instanceof ReturnNode) {
            ReturnNode ret = (ReturnNode) node;
            if (ret.getReturnValue() != null) {
                collectUsedGlobalVariables(ret.getReturnValue(), usedGlobals);
            }
        } else if (node instanceof ExpressionStatementNode) {
            ExpressionStatementNode exprStmt = (ExpressionStatementNode) node;
            if (exprStmt.getExpressionNode() != null) {
                collectUsedGlobalVariables(exprStmt.getExpressionNode(), usedGlobals);
            }
        } else if (node instanceof NumberNode || node instanceof BooleanNode || 
                node instanceof CharNode || node instanceof StringNode ||
                node instanceof LiteralNode) {
            // Nodos literales, no contienen referencias a variables
            return;
        } else if (node instanceof VarDeclStatementNode) {
            VarDeclStatementNode varDeclStmt = (VarDeclStatementNode) node;
            collectUsedGlobalVariables(varDeclStmt.getVarDeclNode(), usedGlobals);
        } else if (node instanceof ParamNode) {
            // Los parámetros no contienen referencias a variables globales
            return;
        } else if (node instanceof DeclarationNode || node instanceof StatementNode) {
            // Nodos abstractos
            return;
        } else {
            System.out.println("Tipo de nodo no cubierto en collectUsedGlobalVariables: " + node.getClass().getSimpleName());
        }
    }
}