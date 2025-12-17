package org.minic.semantic;

import org.minic.ast.*;
import java.util.*;

public class SemanticChecker implements AstVisitor<Void> {
    private SymbolTable currentScope;
    private String currentFunction;
    private String currentFunctionReturnType;
    private List<VarDeclNode> globalDeclarations;
    private ProgramNode currentProgram;
    private Map<String, VarDeclNode> localDeclarations;
    private boolean foundReturn;

    public SemanticChecker() {
        this.currentScope = new SymbolTable(null);
        this.currentFunction = null;
        this.currentFunctionReturnType = null;
        this.globalDeclarations = new ArrayList<>();
        this.localDeclarations = new HashMap<>();
        this.foundReturn = false;
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
            for (AstNode child : currentProgram.getDeclarationsNodes()) {
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
        for (AstNode child : node.getDeclarationsNodes()) {
            child.accept(this);
        }
        return null;
    }

    @Override
public Void visit(FunctionNode node) {

    currentFunction = node.getName();
    currentFunctionReturnType = node.getReturnType();
    foundReturn = false;  // 🔑 Reiniciar flag al entrar a la función

    // 🔑 Scope de la función
    SymbolTable oldScope = currentScope;
    currentScope = new SymbolTable(oldScope);

    // 🔑 Registrar parámetros en el scope
    if (node.getParameters() != null) {
        for (VarDeclNode param : node.getParameters()) {
            Symbol paramSymbol = new Symbol(
                    param.getName(),
                    param.getType(),
                    false);
            if (!currentScope.addSymbol(paramSymbol)) {
                addError("Parámetro duplicado: '" + param.getName() + "'");
            }
        }
    }

    // 🔑 Visitar cuerpo de la función
    if (node.getBody() != null) {
        node.getBody().accept(this);
    }

    // 🔍 Verificación final de retorno usando foundReturn
    if (!Type.VOID.equals(currentFunctionReturnType) && !foundReturn) {
        addError("Función '" + currentFunction +
                "' debe retornar un valor de tipo " + currentFunctionReturnType);
    }

    // Restaurar scope
    currentScope = oldScope;
    currentFunction = null;
    currentFunctionReturnType = null;
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
        System.out.println("SEMANTIC CHECKER: Variable: " + node.getVarDeclNode().getType() + " "
                + node.getVarDeclNode().getName());
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
    foundReturn = true;
    if (node.getReturnValue() != null) {
        String exprType = getExpressionType(node.getReturnValue());

        node.getReturnValue().accept(this);

        if (currentFunctionReturnType != null &&
                !Type.isCompatible(currentFunctionReturnType, exprType)) {
            addError("Tipo de retorno incompatible en función '" + currentFunction +
                    "'. Esperado: " + currentFunctionReturnType +
                    ", Obtenido: " + exprType);
        }
    } else if (!Type.VOID.equals(currentFunctionReturnType)) {
        addError("Función '" + currentFunction +
                "' debe retornar un valor de tipo " + currentFunctionReturnType);
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
        // Este método podría no necesitar implementación si todas las subclases están
        // cubiertas
        return null;
    }

    // ADDED: Missing implementation for ArrayAccessNode
    @Override
    public Void visit(ArrayAccessNode node) {
        // Verificar el arreglo
        ExpressionNode arrayExpr = node.getArray();
        String arrayName = null;

        if (arrayExpr instanceof VariableNode) {
            arrayName = ((VariableNode) arrayExpr).getName();
            Symbol symbol = currentScope.lookup(arrayName);

            if (symbol == null) {
                addError("Arreglo no declarado: '" + arrayName + "'");
                return null;
            }

            if (!isArrayType(symbol.getType())) {
                addError("'" + arrayName + "' no es un arreglo");
                return null;
            }
        }

        // Verificar los índices
        List<ExpressionNode> indices = node.getIndices();
        for (int i = 0; i < indices.size(); i++) {
            ExpressionNode index = indices.get(i);
            String indexType = getExpressionType(index);
            if (!Type.INT.equals(indexType)) {
                addError("Índice " + (i + 1) + " del arreglo debe ser de tipo int, no " + indexType);
            }

            // Verificar límites si es constante
            if (index instanceof NumberNode) {
                int indexValue = ((NumberNode) index).getValue();
                if (indexValue < 0) {
                    addError("Índice " + (i + 1) + " del arreglo no puede ser negativo: " + indexValue);
                }

                // Podrías agregar más verificación de límites aquí si tienes información de
                // tamaño
            }
        }

        return null;
    }

    // ADDED: Missing implementation for MemberAccessNode
    @Override
    public Void visit(MemberAccessNode node) {
        // Obtener el objeto/estructura
        ExpressionNode object = node.getStruct();
        String objectType = getExpressionType(object);
        String memberName = node.getMemberName();

        System.out.println("SEMANTIC CHECKER: Acceso a miembro '" + memberName + "' en objeto de tipo: " + objectType);

        // Verificar que el objeto no sea null
        if (objectType.equals(Type.VOID)) {
            addError("No se puede acceder a miembros de una expresión inválida");
            return null;
        }
        addError("Acceso a miembro '" + memberName + "' no soportado en el tipo '" + objectType + "'");

        return null;
    }

    // ADDED: Missing implementation for CastNode
    @Override
    public Void visit(CastNode node) {
        // Obtener el tipo de la expresión y el tipo destino
        ExpressionNode expression = node.getExpression();
        String targetType = node.getTargetType();
        String sourceType = getExpressionType(expression);

        System.out.println("SEMANTIC CHECKER: Verificando cast de " + sourceType + " a " + targetType);

        // Verificar que los tipos no sean inválidos
        if (sourceType.equals(Type.VOID) || targetType.equals(Type.VOID)) {
            addError("No se puede realizar cast desde o hacia el tipo void");
            return null;
        }

        // Verificar compatibilidad de tipos para el cast
        if (!isValidCast(sourceType, targetType)) {
            addError("Cast inválido de '" + sourceType + "' a '" + targetType + "'");
        }

        return null;
    }

    // Método auxiliar para verificar si un cast es válido
    private boolean isValidCast(String sourceType, String targetType) {
        // Casts básicos permitidos:
        // 1. Entre tipos numéricos (int, char, bool como 0/1)
        // 2. De puntero a puntero (si son compatibles)
        // 3. De arreglo a puntero (en algunos contextos)

        if (Type.isNumeric(sourceType) && Type.isNumeric(targetType)) {
            return true; // Cast entre tipos numéricos permitido
        }

        // Cast de cualquier tipo numérico a bool
        if (Type.isNumeric(sourceType) && targetType.equals(Type.BOOLEAN)) {
            return true;
        }

        // Cast de bool a cualquier tipo numérico
        if (sourceType.equals(Type.BOOLEAN) && Type.isNumeric(targetType)) {
            return true;
        }

        // Cast de char a int y viceversa
        if ((sourceType.equals(Type.CHAR) && targetType.equals(Type.INT)) ||
                (sourceType.equals(Type.INT) && targetType.equals(Type.CHAR))) {
            return true;
        }

        // Cast de puntero a puntero (solo si son compatibles)
        if (Type.isPointerType(sourceType) && Type.isPointerType(targetType)) {
            // Permitir cast entre punteros del mismo tipo base
            // Nota: en C se permiten casts entre punteros, pero con advertencias
            return true;
        }

        // Cast de arreglo a puntero (en algunos contextos)
        if (isArrayType(sourceType) && Type.isPointerType(targetType)) {
            return true;
        }

        // Cast de puntero a arreglo (con precaución)
        if (Type.isPointerType(sourceType) && isArrayType(targetType)) {
            return true;
        }

        return false;
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
        } else if (expr instanceof ArrayAccessNode) {
            return getArrayAccessType((ArrayAccessNode) expr);
        } else if (expr instanceof MemberAccessNode) {
            return getMemberAccessType((MemberAccessNode) expr);
        } else if (expr instanceof CastNode) {
            return getCastType((CastNode) expr);
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

        addError("Operador desconocido: '" + operator + "'");
        return Type.VOID;
    }

    private String getArrayAccessType(ArrayAccessNode arrayAccess) {
        ExpressionNode arrayExpr = arrayAccess.getArray();

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

            // Determinar el tipo resultante basado en el número de dimensiones
            String arrayType = symbol.getType();
            List<Integer> dims = getArrayDimensions(arrayType);
            int accessDims = arrayAccess.getIndices().size();

            if (accessDims > dims.size()) {
                addError("Demasiados índices para arreglo '" + arrayName +
                        "'. Esperados: " + dims.size() + ", Usados: " + accessDims);
                return Type.VOID;
            }

            if (accessDims == dims.size()) {
                // Acceso completo: retorna el tipo base
                return getArrayBaseType(arrayType);
            } else {
                // Acceso parcial: retorna un arreglo de dimensión reducida
                // Ejemplo: int[5][10] con un índice retorna int[10]
                String baseType = getArrayBaseType(arrayType);
                StringBuilder resultType = new StringBuilder(baseType);

                // Agregar dimensiones restantes
                for (int i = accessDims; i < dims.size(); i++) {
                    resultType.append("[").append(dims.get(i)).append("]");
                }

                return resultType.toString();
            }
        }

        return Type.VOID;
    }

    private String getMemberAccessType(MemberAccessNode memberAccess) {
        // ExpressionNode object = memberAccess.getStruct();
        // String objectType = getExpressionType(object);

        addError("Acceso a miembro no implementado completamente");
        return Type.VOID;
    }

    private String getCastType(CastNode cast) {
        return cast.getTargetType();
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

        // Runtime functions
        if (isRuntimeFunction(funcName)) {
            return handleRuntimeFunctionCall(funcName, call.getArguments());
        }

        List<String> expectedParams = symbol.getParamTypes();
        List<ExpressionNode> args = call.getArguments();

        // 1️⃣ Cantidad de argumentos
        if (expectedParams.size() != args.size()) {
            addError("Número incorrecto de argumentos en llamada a '" + funcName +
                    "'. Esperados: " + expectedParams.size() +
                    ", Recibidos: " + args.size());
            return symbol.getType();
        }

        // 2️⃣ Tipos de argumentos
        for (int i = 0; i < args.size(); i++) {
            String argType = getExpressionType(args.get(i));
            String paramType = expectedParams.get(i);

            if (!Type.isCompatible(paramType, argType)) {
                addError("Tipo incompatible en argumento " + (i + 1) +
                        " de la función '" + funcName +
                        "'. Esperado: " + paramType +
                        ", Obtenido: " + argType);
            }
        }

        return symbol.getType();
    }

    private VarDeclNode findArrayDecl(String arrayName) {
        Symbol symbol = currentScope.lookup(arrayName);
        if (symbol != null && !symbol.isFunction()) {
            VarDeclNode localDecl = localDeclarations.get(arrayName);
            if (localDecl != null)
                return localDecl;
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

        List<String> paramTypes = new ArrayList<>();
        if (function.getParameters() != null) {
            for (VarDeclNode param : function.getParameters()) {
                paramTypes.add(param.getType());
            }
        }
        Symbol symbol = new Symbol(name, returnType, paramTypes, true);

        if (!currentScope.addSymbol(symbol)) {
            addError("No se pudo agregar función '" + name + "'");
        }
    }

    private boolean isRuntimeFunction(String funcName) {
        return funcName.equals("print_int") || funcName.equals("print_string") ||
                funcName.equals("print_char") || funcName.equals("print_bool") ||
                funcName.equals("println") || funcName.equals("read_int") ||
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
                    addError("Función '" + funcName + "' requiere exactamente 1 argumento, se proporcionaron "
                            + args.size());
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
        if (currentProgram == null)
            return;

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
        if (currentProgram == null)
            return;

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
        if (currentProgram == null)
            return;

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
        } else if (expr instanceof CastNode) {
            CastNode cast = (CastNode) expr;
            return isConstantExpression(cast.getExpression());
        }
        return false;
    }

    private void checkUnusedGlobalVariables() {
        if (currentProgram == null || globalDeclarations.isEmpty())
            return;

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
        if (node == null)
            return;

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
        } else if (node instanceof ArrayAccessNode) {
            ArrayAccessNode arrayAccess = (ArrayAccessNode) node;
            collectUsedGlobalVariables(arrayAccess.getArray(), usedGlobals);
            for (ExpressionNode index : arrayAccess.getIndices()) {
                collectUsedGlobalVariables(index, usedGlobals);
            }
        } else if (node instanceof MemberAccessNode) {
            MemberAccessNode memberAccess = (MemberAccessNode) node;
            collectUsedGlobalVariables(memberAccess.getStruct(), usedGlobals);
        } else if (node instanceof CastNode) {
            CastNode cast = (CastNode) node;
            collectUsedGlobalVariables(cast.getExpression(), usedGlobals);
        } else {
            System.out.println(
                    "Tipo de nodo no cubierto en collectUsedGlobalVariables: " + node.getClass().getSimpleName());
        }
    }

    private boolean isArrayType(String type) {
        if (type == null)
            return false;

        // Buscar corchetes en el tipo
        int bracketCount = 0;
        for (char c : type.toCharArray()) {
            if (c == '[')
                bracketCount++;
        }
        return bracketCount > 0;
    }

    /**
     * Obtiene el tipo base de un tipo de arreglo
     * Ejemplo: "int[10][5]" -> "int"
     */
    private String getArrayBaseType(String arrayType) {
        if (!isArrayType(arrayType)) {
            return arrayType;
        }

        // Encontrar el primer corchete
        int bracketIndex = arrayType.indexOf('[');
        if (bracketIndex > 0) {
            return arrayType.substring(0, bracketIndex).trim();
        }
        return arrayType;
    }

    /**
     * Obtiene las dimensiones de un arreglo
     * Ejemplo: "int[10][5]" -> [10, 5]
     */
    private List<Integer> getArrayDimensions(String arrayType) {
        List<Integer> dimensions = new ArrayList<>();

        if (!isArrayType(arrayType)) {
            return dimensions;
        }

        // Extraer dimensiones usando regex
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\[(\\d+)\\]");
        java.util.regex.Matcher matcher = pattern.matcher(arrayType);

        while (matcher.find()) {
            try {
                dimensions.add(Integer.parseInt(matcher.group(1)));
            } catch (NumberFormatException e) {
                // Ignorar dimensiones no constantes
            }
        }

        return dimensions;
    }

    /**
     * Obtiene el tipo de elemento de un arreglo
     * Ejemplo: "int[10][5]" -> "int[5]" para la primera dimensión
     */
    private String getArrayElementType(String arrayType, int dimension) {
        if (!isArrayType(arrayType)) {
            return arrayType;
        }

        // Para arreglos 2D, el elemento es arreglo 1D
        List<Integer> dims = getArrayDimensions(arrayType);
        if (dims.size() > 1 && dimension == 0) {
            // Primer acceso: a[i] retorna un arreglo 1D
            String baseType = getArrayBaseType(arrayType);
            return baseType + "[" + dims.get(1) + "]";
        } else if (dims.size() == 1 || (dims.size() > 1 && dimension == 1)) {
            // Segundo acceso: a[i][j] o único acceso: retorna tipo base
            return getArrayBaseType(arrayType);
        }

        return Type.VOID;
    }

    // ============ MÉTODOS PARA MEJORAR EL CHECKEO DE ARREGLOS 2D ============

    private void checkMultiDimensionalArrayAccess(ExpressionNode expr, String context) {
        if (expr instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) expr;
            if (binOp.getOperator().equals("[")) {
                // Verificar acceso anidado para 2D
                ExpressionNode left = binOp.getLeft();
                ExpressionNode right = binOp.getRight();

                // Si el izquierdo es otro acceso a arreglo, es 2D
                if (left instanceof BinaryOpNode) {
                    BinaryOpNode innerAccess = (BinaryOpNode) left;
                    if (innerAccess.getOperator().equals("[")) {
                        checkArrayAccess2D(innerAccess, right, context);
                    }
                }
            }
        } else if (expr instanceof ArrayAccessNode) {
            // Usar el nuevo nodo ArrayAccessNode
            ArrayAccessNode arrayAccess = (ArrayAccessNode) expr;
            if (arrayAccess.getIndices().size() > 1) {
                checkArrayAccess2D(arrayAccess, context);
            }
        }
    }

    private void checkArrayAccess2D(BinaryOpNode innerAccess, ExpressionNode secondIndex, String context) {
        // Verificar primer acceso
        ExpressionNode arrayExpr = innerAccess.getLeft();
        ExpressionNode firstIndex = innerAccess.getRight();

        if (arrayExpr instanceof VariableNode) {
            String arrayName = ((VariableNode) arrayExpr).getName();
            Symbol symbol = currentScope.lookup(arrayName);

            if (symbol != null) {
                // Verificar que sea arreglo 2D
                List<Integer> dims = getArrayDimensions(symbol.getType());
                if (dims.size() != 2) {
                    addError("Se intentó acceso 2D a '" + arrayName +
                            "' que tiene " + dims.size() + " dimensiones");
                }

                // Verificar índices
                checkArrayIndex(firstIndex, "primer índice de " + arrayName);
                checkArrayIndex(secondIndex, "segundo índice de " + arrayName);

                // Verificar límites si son constantes
                if (firstIndex instanceof NumberNode && secondIndex instanceof NumberNode) {
                    int i = ((NumberNode) firstIndex).getValue();
                    int j = ((NumberNode) secondIndex).getValue();

                    VarDeclNode arrayDecl = findArrayDecl(arrayName);
                    if (arrayDecl != null && arrayDecl.hasSecondDimension()) {
                        if (i < 0 || i >= arrayDecl.getArraySize() ||
                                j < 0 || j >= arrayDecl.getSecondDimension()) {
                            addError("Índices fuera de límites en " + context +
                                    ": [" + i + "][" + j + "] para arreglo " +
                                    arrayDecl.getArraySize() + "x" + arrayDecl.getSecondDimension());
                        }
                    }
                }
            }
        }
    }

    private void checkArrayAccess2D(ArrayAccessNode arrayAccess, String context) {
        List<ExpressionNode> indices = arrayAccess.getIndices();
        if (indices.size() != 2) {
            addError("Acceso a arreglo con " + indices.size() +
                    " índices (se esperaban 2)");
            return;
        }

        ExpressionNode arrayExpr = arrayAccess.getArray();
        if (arrayExpr instanceof VariableNode) {
            String arrayName = ((VariableNode) arrayExpr).getName();
            Symbol symbol = currentScope.lookup(arrayName);

            if (symbol != null) {
                // Verificar dimensiones
                List<Integer> dims = getArrayDimensions(symbol.getType());
                if (dims.size() != 2) {
                    addError("Se intentó acceso 2D a '" + arrayName +
                            "' que tiene " + dims.size() + " dimensiones");
                }

                // Verificar índices
                for (int i = 0; i < indices.size(); i++) {
                    checkArrayIndex(indices.get(i), "índice " + (i + 1) + " de " + arrayName);
                }
            }
        }
    }

    private void checkArrayIndex(ExpressionNode index, String context) {
        String indexType = getExpressionType(index);
        if (!Type.INT.equals(indexType)) {
            addError("Índice de arreglo en " + context +
                    " debe ser de tipo int, no " + indexType);
        }
    }

    // ============ MÉTODO PARA ACTUALIZAR EL CHECK DE ARREGLOS ============

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

        } else if (target instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) target;
            if (binOp.getOperator().equals("[")) {
                // ACCESO A ARREGLO 1D o 2D
                ExpressionNode arrayExpr = binOp.getLeft();
                ExpressionNode indexExpr = binOp.getRight();

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

                    // Verificar índice
                    String indexType = getExpressionType(indexExpr);
                    if (!Type.INT.equals(indexType)) {
                        addError("Índice de arreglo debe ser de tipo int, no " + indexType);
                    }

                    // Determinar tipo esperado del elemento
                    String expectedType;
                    List<Integer> dims = getArrayDimensions(symbol.getType());
                    if (dims.size() == 2) {
                        // Si es arreglo 2D y estamos asignando a a[i], necesitamos otro índice
                        // Esto es un acceso parcial, debería ser a[i][j] = valor
                        expectedType = getArrayElementType(symbol.getType(), 0);
                    } else {
                        // Arreglo 1D
                        expectedType = getArrayBaseType(symbol.getType());
                    }

                    // Verificar compatibilidad
                    if (!Type.isCompatible(expectedType, valueType)) {
                        addError("Asignación incompatible para arreglo '" + arrayName +
                                "'. Esperado: " + expectedType + ", Obtenido: " + valueType);
                    }

                } else if (arrayExpr instanceof BinaryOpNode) {
                    // ACCESO 2D: a[i][j] = valor
                    BinaryOpNode innerAccess = (BinaryOpNode) arrayExpr;
                    if (innerAccess.getOperator().equals("[")) {
                        checkArrayAccess2D(innerAccess, indexExpr, "asignación");

                        // Verificar tipo del valor
                        if (innerAccess.getLeft() instanceof VariableNode) {
                            String arrayName = ((VariableNode) innerAccess.getLeft()).getName();
                            Symbol symbol = currentScope.lookup(arrayName);
                            if (symbol != null) {
                                String expectedType = getArrayBaseType(symbol.getType());
                                if (!Type.isCompatible(expectedType, valueType)) {
                                    addError("Asignación incompatible para arreglo 2D '" +
                                            arrayName + "'. Esperado: " + expectedType +
                                            ", Obtenido: " + valueType);
                                }
                            }
                        }
                    }
                }
            }
        } else if (target instanceof ArrayAccessNode) {
            // Usar nuevo nodo ArrayAccessNode
            ArrayAccessNode arrayAccess = (ArrayAccessNode) target;
            checkMultiDimensionalArrayAccess(target, "asignación");

            // Verificar tipo
            ExpressionNode arrayExpr = arrayAccess.getArray();
            if (arrayExpr instanceof VariableNode) {
                String arrayName = ((VariableNode) arrayExpr).getName();
                Symbol symbol = currentScope.lookup(arrayName);
                if (symbol != null) {
                    int dims = getArrayDimensions(symbol.getType()).size();
                    String expectedType = dims == 2 ? getArrayBaseType(symbol.getType())
                            : getArrayElementType(symbol.getType(), 0);

                    if (!Type.isCompatible(expectedType, valueType)) {
                        addError("Asignación incompatible para arreglo '" + arrayName +
                                "'. Esperado: " + expectedType + ", Obtenido: " + valueType);
                    }
                }
            }
        } else if (target instanceof MemberAccessNode) {
            // Asignación a miembro de estructura
            // MemberAccessNode memberAccess = (MemberAccessNode) target;
            addError("Asignación a miembro de estructura no implementada completamente");
        } else {
            addError("El objetivo de la asignación debe ser un identificador o acceso a arreglo");
        }
        return null;
    }

    @Override
    public Void visit(ArrayDimensionsNode node) {
        // Este nodo no necesita verificación semántica especial
        return null;
    }

    private boolean blockHasGuaranteedReturn(BlockNode block) {
        if (block == null || block.getStatements().isEmpty()) {
            return false;
        }

        StatementNode last = block.getStatements()
                .get(block.getStatements().size() - 1);

        // 🔑 CLAVE: si el último statement es otro bloque, entramos
        if (last instanceof BlockNode) {
            return blockHasGuaranteedReturn((BlockNode) last);
        }

        return statementGuaranteesReturn(last);
    }

    private boolean statementGuaranteesReturn(StatementNode stmt) {
        if (stmt instanceof ReturnNode) {
            return true;
        }

        if (stmt instanceof IfNode) {
            IfNode ifNode = (IfNode) stmt;
            if (ifNode.getElseBlock() == null) {
                return false;
            }
            return blockHasGuaranteedReturn(ifNode.getThenBlock())
                    && blockHasGuaranteedReturn(ifNode.getElseBlock());
        }

        if (stmt instanceof BlockNode) {
            return blockHasGuaranteedReturn((BlockNode) stmt);
        }

        return false;
    }

}