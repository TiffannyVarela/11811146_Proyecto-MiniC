package org.minic.semantic;

import org.minic.ast.*;
import java.util.*;
import org.minic.ErrorManager;

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
        ErrorManager.cleanErrors();
        registerRuntimeFunctions();

        if (ast instanceof ProgramNode) {
            this.currentProgram = (ProgramNode) ast;
            for (AstNode child : currentProgram.getDeclarationsNodes()) {
                if (child instanceof FunctionNode) {
                    visitFunctionDecl((FunctionNode) child);
                }
            }

            collectGlobalDeclarations(currentProgram);
            ast.accept(this);

            checkFunctionDuplicates();
            checkGlobalInitializers();
            checkUnusedGlobalVariables();
            checkMainFunction();
        } else {
            addError("El nodo raíz debe ser un ProgramNode");
        }
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
        foundReturn = false;

        SymbolTable oldScope = currentScope;
        currentScope = new SymbolTable(oldScope);

        if (node.getParameters() != null) {
            for (VarDeclNode param : node.getParameters()) {
                Symbol paramSymbol = new Symbol(param.getName(), param.getType(), false);
                if (!currentScope.addSymbol(paramSymbol)) {
                    addError("Parámetro duplicado: '" + param.getName() + "'");
                }
            }
        }

        if (node.getBody() != null) {
            node.getBody().accept(this);
        }

        if (!Type.VOID.equals(currentFunctionReturnType) && !foundReturn) {
            addError("Función '" + currentFunction +
                    "' debe retornar un valor de tipo " + currentFunctionReturnType);
        }

        currentScope = oldScope;
        currentFunction = null;
        currentFunctionReturnType = null;

        return null;
    }

    @Override
public Void visit(VarDeclNode node) {
    String name = node.getName();
    String type = node.getType();
    if (node.isArray()) {
        type += "[]";
    }

    if (currentScope.lookupCurrentScope(name) != null) {
        addError("Variable '" + name + "' ya está declarada en este ámbito");
        return null;
    }

    Symbol symbol = new Symbol(name, type, false);
    currentScope.addSymbol(symbol);
    localDeclarations.put(name, node);

    if (node.hasInitialNode()) {
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
        node.getVarDeclNode().accept(this);
        return null;
    }

    @Override
    public Void visit(BlockNode node) {
        SymbolTable oldScope = currentScope;
        Map<String, VarDeclNode> oldLocals = new HashMap<>(localDeclarations);
        currentScope = new SymbolTable(currentScope);
        localDeclarations.clear();

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
        SymbolTable oldScope = currentScope;
        currentScope = new SymbolTable(currentScope);

        try {
            if (node.getInit() != null) node.getInit().accept(this);
            if (node.getCondition() != null) {
                String condType = getExpressionType(node.getCondition());
                if (!Type.BOOLEAN.equals(condType)) {
                    addError("La condición del for debe ser boolean, no " + condType);
                }
            }
            if (node.getIncrement() != null) getExpressionType(node.getIncrement());
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
            if (!Type.isCompatible(currentFunctionReturnType, exprType)) {
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
        if (symbol == null) addError("Variable no declarada: '" + name + "'");
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

        for (ExpressionNode arg : node.getArguments()) {
            getExpressionType(arg);
        }
        return null;
    }

    @Override
    public Void visit(NumberNode node) { return null; }
    @Override
    public Void visit(CharNode node) { return null; }
    @Override
    public Void visit(StringNode node) { return null; }
    @Override
    public Void visit(BooleanNode node) { return null; }
    @Override
    public Void visit(DeclarationNode node) { return null; }
    @Override
    public Void visit(IdentifierNode node) { return null; }
    @Override
    public Void visit(LiteralNode node) { return null; }

    @Override
    public Void visit(ParamNode node) {
        if (!currentScope.addSymbol(new Symbol(node.getName(), node.getType(), false))) {
            addError("Parámetro duplicado o inválido: " + node.getName());
        }
        return null;
    }

    @Override
    public Void visit(StatementNode node) { return null; }
//Funcion
@Override
public Void visit(ArrayAccessNode node) {
    ExpressionNode arrayExpr = node.getArray();
    
    String arrayName;
    int line = 0;
    int column = 0;
    
    if (arrayExpr instanceof VariableNode) {
        arrayName = ((VariableNode) arrayExpr).getName();
        line = ((VariableNode) arrayExpr).getLine();
        column = ((VariableNode) arrayExpr).getColumn();
    } else if (arrayExpr instanceof ArrayAccessNode) {
        ExpressionNode baseExpr = arrayExpr;
        while (baseExpr instanceof ArrayAccessNode) {
            baseExpr = ((ArrayAccessNode) baseExpr).getArray();
        }
        
        if (baseExpr instanceof VariableNode) {
            arrayName = ((VariableNode) baseExpr).getName();
            line = ((VariableNode) baseExpr).getLine();
            column = ((VariableNode) baseExpr).getColumn();
        } else {
            addError(node.getLine(), node.getColumn(), "Acceso inválido a arreglo - no se encontró variable base");
            return null;
        }
    } else {
        addError(node.getLine(), node.getColumn(), "Acceso inválido a arreglo - expresión base no válida");
        return null;
    }
    
    Symbol symbol = currentScope.lookup(arrayName);
    
    if (symbol == null) {
        addError(line, column, "Variable no declarada: '" + arrayName + "'");
        return null;
    }
    
    String symbolType = symbol.getType();
    boolean isArray = symbolType.endsWith("[]");
    
    if (!isArray) {
        addError(line, column, "'" + arrayName + "' no es un arreglo");
    }
    
    int declaredDimensions = 0;
    String tempType = symbolType;
    while (tempType.endsWith("[]")) {
        declaredDimensions++;
        tempType = tempType.substring(0, tempType.length() - 2);
    }
    
    int accessDimensions = node.getIndices().size();
    
    if (accessDimensions > declaredDimensions) {
        addError(line, column, 
            "Acceso a demasiadas dimensiones en '" + arrayName + 
            "'. Declarado: " + declaredDimensions + 
            ", Accediendo: " + accessDimensions);
    }
    
    for (ExpressionNode index : node.getIndices()) {
        String idxType = getExpressionType(index);
        if (!Type.INT.equals(idxType)) {
            addError(
                index.getLine(),
                index.getColumn(),
                "Índice de arreglo debe ser int, no " + idxType
            );
        }
    }

    return null;
}


    @Override
    public Void visit(MemberAccessNode node) {
        addError("Acceso a miembro no implementado completamente");
        return null;
    }

    @Override
    public Void visit(CastNode node) {
        getExpressionType(node.getExpression());
        return null;
    }

    @Override
    public Void visit(AssignmentNode node) {
        if (node.getTarget() != null) {
            node.getTarget().accept(this);
        }
        if (node.getValue() != null) {
            node.getValue().accept(this);
        }

        return null;
    }

    @Override
    public Void visit(ArrayDimensionsNode node) { return null; }

    private String getExpressionType(ExpressionNode expr) {
        if (expr instanceof NumberNode) return Type.INT;
        if (expr instanceof BooleanNode) return Type.BOOLEAN;
        if (expr instanceof CharNode) return Type.CHAR;
        if (expr instanceof StringNode) return Type.STRING;
        if (expr instanceof VariableNode) return getVariableType((VariableNode) expr);
        if (expr instanceof BinaryOpNode) return getBinaryOpType((BinaryOpNode) expr);
        if (expr instanceof UnaryOpNode) return getUnaryOpType((UnaryOpNode) expr);
        if (expr instanceof FunctionCallNode) return getFunctionCallType((FunctionCallNode) expr);
        if (expr instanceof ArrayAccessNode) return getArrayAccessType((ArrayAccessNode) expr);
        if (expr instanceof CastNode) return getCastType((CastNode) expr);
        if (expr instanceof MemberAccessNode) return Type.VOID;

        addError("Expresión de tipo desconocido: " + (expr != null ? expr.getClass().getSimpleName() : "null"));
        return Type.VOID;
    }

    private String getVariableType(VariableNode variable) {
        Symbol s = currentScope.lookup(variable.getName());
        if (s == null) return Type.VOID;
        return s.getType();
    }

    private String getBinaryOpType(BinaryOpNode binOp) { return Type.INT; }
    private String getUnaryOpType(UnaryOpNode unaryOp) { return Type.INT; }
    private String getFunctionCallType(FunctionCallNode call) { return Type.VOID; }
//Funcion
    private String getArrayAccessType(ArrayAccessNode node) {
    ExpressionNode arrayExpr = node.getArray();
    String baseType = getExpressionType(arrayExpr);
    
    if (baseType == null || baseType.equals(Type.VOID)) {
        return Type.VOID;
    }
    
    int accessDimensions = node.getIndices().size();
    String currentType = baseType;
    
    for (int i = 0; i < accessDimensions; i++) {
        if (currentType.endsWith("[]")) {
            currentType = currentType.substring(0, currentType.length() - 2);
        } else {
            return Type.VOID;
        }
    }
    
    return currentType;
}

    private String getCastType(CastNode cast) { return cast.getTargetType(); }

    private void addError(String message) { ErrorManager.addError(message); }

    private void addError(int line, int column, String message) {
    ErrorManager.addError(line, column, message);
}

    private void collectGlobalDeclarations(ProgramNode program) {
        globalDeclarations.clear();
        for (AstNode node : program.getDeclarationsNodes()) {
            if (node instanceof VarDeclNode) globalDeclarations.add((VarDeclNode) node);
        }
    }

    private void visitFunctionDecl(FunctionNode function) {
        String name = function.getName();
        List<String> paramTypes = new ArrayList<>();
        if (function.getParameters() != null) {
            for (VarDeclNode param : function.getParameters()) {
                paramTypes.add(param.getType());
            }
        }
        Symbol symbol = new Symbol(name, function.getReturnType(), paramTypes, true);
        currentScope.addSymbol(symbol);
    }

    private void checkFunctionDuplicates() {
        if (currentProgram == null) return;
        Set<String> names = new HashSet<>();
        for (AstNode node : currentProgram.getDeclarationsNodes()) {
            if (node instanceof FunctionNode) {
                String n = ((FunctionNode) node).getName();
                if (!names.add(n)) addError("Función duplicada: " + n);
            }
        }
    }

    private void checkGlobalInitializers() {
        if (currentProgram == null) return;
        for (VarDeclNode var : globalDeclarations) {
            if (var.hasInitialNode() && !isConstantExpression(var.getInitialNode())) {
                addError("Inicializador global debe ser constante: " + var.getName());
            }
        }
    }

    private boolean isConstantExpression(ExpressionNode expr) {
        return expr instanceof NumberNode || expr instanceof BooleanNode ||
               expr instanceof CharNode || expr instanceof StringNode;
    }

    private void checkUnusedGlobalVariables() {
    }

    private void checkMainFunction() {
        if (currentProgram == null) return;
        boolean found = false;
        for (AstNode node : currentProgram.getDeclarationsNodes()) {
            if (node instanceof FunctionNode fn && fn.getName().equals("main")) {
                found = true;
                if (!Type.INT.equals(fn.getReturnType())) addError("main debe retornar int");
                if (fn.getParameters() != null && !fn.getParameters().isEmpty())
                    addError("main no debe tener parámetros");
                break;
            }
        }
        if (!found) addError("No se encontró la función main");
    }
}
