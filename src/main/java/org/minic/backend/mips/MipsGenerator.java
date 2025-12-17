package org.minic.backend.mips;

import org.minic.ast.*;
import java.util.*;

public class MipsGenerator {
    private StringBuilder code = new StringBuilder();
    private int tempCount = 0;
    private int labelCount = 0;

    // Gestión de registros temporales
    private Stack<String> availableTemps = new Stack<>();

    // Tablas de símbolos
    private Map<String, SymbolInfo> globalSymbols = new HashMap<>();
    private Deque<Map<String, SymbolInfo>> localScopes = new ArrayDeque<>();
    private Map<String, String> stringLiterals = new HashMap<>();
    private int stringLiteralCount = 0;

    // Información de funciones actuales
    private String currentFunction = null;
    private int currentFrameSize = 0;

    // Secciones
    private List<String> dataSection = new ArrayList<>();
    private List<String> textSection = new ArrayList<>();

    // Clase para información de símbolos (optimizada)
    private class SymbolInfo {
        boolean isArray;
        int arraySize;
        int secondDimension; // Para arrays 2D
        int offset; // Offset en el frame (locales) o dirección (globales)
        boolean isGlobal;

        SymbolInfo(boolean isArray, int arraySize, int secondDimension, int offset, boolean isGlobal) {
            this.isArray = isArray;
            this.arraySize = arraySize;
            this.secondDimension = secondDimension;
            this.offset = offset;
            this.isGlobal = isGlobal;
        }

        int getTotalSize() {
            if (!isArray)
                return 4;
            if (secondDimension > 0) {
                return arraySize * secondDimension * 4; // int[10][5] = 10*5*4 bytes
            }
            return arraySize * 4;
        }
    }

    public String generate(AstNode ast) {
        initializeSections();

        // Primera pasada: recolectar símbolos globales
        if (ast instanceof ProgramNode) {
            collectGlobalSymbols((ProgramNode) ast);
        }

        // Segunda pasada: generar código
        if (ast instanceof ProgramNode) {
            generateProgram((ProgramNode) ast);
        }

        buildFinalCode();
        return code.toString();
    }

    private void initializeSections() {
        dataSection.add(".data");
        dataSection.add("newline: .asciiz \"\\n\"");
        dataSection.add("true_str: .asciiz \"true\"");
        dataSection.add("false_str: .asciiz \"false\"");

        textSection.add(".text");
        textSection.add(".globl main");
        textSection.add("");

        // Registrar funciones runtime
        textSection.add("# =========================================");
        textSection.add("# RUNTIME FUNCTIONS (provided by runtime.s)");
        textSection.add("# =========================================");
    }

    private void collectGlobalSymbols(ProgramNode program) {
        int globalOffset = 0;

        for (AstNode node : program.getDeclarationsNodes()) {
            if (node instanceof VarDeclNode) {
                VarDeclNode varDecl = (VarDeclNode) node;
                String name = varDecl.getName();

                // Determinar dimensiones del array
                int arraySize = varDecl.getArraySize();
                int secondDim = 0;

                // Para arrays 2D, necesitamos detectar ambas dimensiones
                if (varDecl.isArray() && varDecl.getType().contains("[][]")) {
                    // Asumimos formato "int[10][5]" o similar
                    // En una implementación real, necesitarías parsear esto
                    arraySize = 10; // Valor por defecto
                    secondDim = 5; // Valor por defecto
                }

                SymbolInfo info = new SymbolInfo(
                        varDecl.isArray(),
                        arraySize,
                        secondDim,
                        globalOffset,
                        true);

                globalSymbols.put(name, info);
                globalOffset += info.getTotalSize();

                // Agregar a sección .data
                String label = "_" + name;
                if (varDecl.isArray()) {
                    if (secondDim > 0) {
                        // Array 2D
                        dataSection.add(label + ": .space " + (arraySize * secondDim * 4));
                    } else {
                        // Array 1D
                        dataSection.add(label + ": .space " + (arraySize * 4));
                    }
                } else {
                    dataSection.add(label + ": .word 0");
                }
            }
            // Las funciones no necesitan procesamiento en collectGlobalSymbols
        }
    }

    private void generateProgram(ProgramNode program) {
        // Generar funciones
        for (AstNode node : program.getDeclarationsNodes()) {
            if (node instanceof FunctionNode) {
                generateFunction((FunctionNode) node);
            }
        }
    }

    private void generateFunction(FunctionNode function) {
        currentFunction = function.getName();
        localScopes.clear();
        localScopes.push(new HashMap<>());

        textSection.add("");
        textSection.add("# =========================================");
        textSection.add("# FUNCTION: " + currentFunction);
        textSection.add("# =========================================");
        textSection.add(currentFunction + ":");

        // ============ PRÓLOGO (ABI O32) ============
        textSection.add("  # === PROLOGUE ==="); // Aqui

        // Guardar $ra y $fp
        textSection.add("  addiu $sp, $sp, -8");
        textSection.add("  sw $ra, 4($sp)");
        textSection.add("  sw $fp, 0($sp)");
        textSection.add("  move $fp, $sp");

        // Calcular tamaño del frame
        int localVarSize = calculateLocalVarsSize(function);
        int savedRegSize = 0;
        int paramSize = function.getParameters() != null ? function.getParameters().size() * 4 : 0;
        currentFrameSize = 8 + paramSize + savedRegSize + localVarSize;
        currentFrameSize = ((currentFrameSize + 7) / 8) * 8;

        // Reservar espacio en stack
        textSection.add("  addiu $sp, $sp, -" + currentFrameSize);

        // Procesar parámetros (ABI O32: primeros 4 en $a0-$a3, resto en stack)
        if (function.getParameters() != null) {
            processParameters(function.getParameters());
        }

        // Generar cuerpo
        if (function.getBody() != null) {
            generateBlock(function.getBody());
        }

        // ============ EPÍLOGO ============
        textSection.add("");
        textSection.add("  # === EPILOGUE ==="); // Aqui

        // Restaurar $fp y $ra
        textSection.add("  move $sp, $fp");
        textSection.add("  lw $fp, 0($sp)");
        textSection.add("  lw $ra, 4($sp)");
        textSection.add("  addiu $sp, $sp, 8");

        // Retornar
        textSection.add("  jr $ra");

        currentFunction = null;
    }

    private int calculateLocalVarsSize(FunctionNode function) {
        int size = 0;

        // Recolectar variables locales del cuerpo
        if (function.getBody() != null) {
            size = calculateBlockLocalSize(function.getBody());
        }

        return size;
    }

    private int calculateBlockLocalSize(BlockNode block) {
        int size = 0;

        for (StatementNode stmt : block.getStatements()) {
            if (stmt instanceof VarDeclStatementNode) {
                VarDeclNode varDecl = ((VarDeclStatementNode) stmt).getVarDeclNode();

                int varSize = varDecl.isArray()
                        ? varDecl.getArraySize() * 4
                        : 4;

                varSize = ((varSize + 3) / 4) * 4;

                int offset = -(size + varSize);

                SymbolInfo info = new SymbolInfo(
                        varDecl.isArray(),
                        varDecl.getArraySize(),
                        0,
                        offset,
                        false);

                localScopes.peek().put(varDecl.getName(), info);
                size += varSize;
            }
        }
        return size;
    }

    private void processParameters(List<VarDeclNode> parameters) {
        textSection.add("  # === Process parameters ===");

        int offset = 8;

        for (int i = 0; i < parameters.size(); i++) {
            VarDeclNode param = parameters.get(i);

            localScopes.peek().put(param.getName(),
                    new SymbolInfo(false, 0, 0, offset, false));

            if (i < 4) {
                textSection.add("  sw $a" + i + ", " + offset + "($fp)");
            }

            offset += 4;
        }
    }

    private void generateBlock(BlockNode block) {
        boolean isRootBlock = localScopes.size() == 1;

        if (!isRootBlock) {
            localScopes.push(new HashMap<>());
        }

        for (StatementNode stmt : block.getStatements()) {
            generateStatement(stmt);
        }

        if (!isRootBlock) {
            localScopes.pop();
        }
    }

    private void generateStatement(StatementNode stmt) {
        if (stmt == null)
            return;

        if (stmt instanceof ExpressionStatementNode) {
            generateExpressionStatement((ExpressionStatementNode) stmt);
        } else if (stmt instanceof ReturnNode) {
            generateReturn((ReturnNode) stmt);
        } else if (stmt instanceof IfNode) {
            generateIf((IfNode) stmt);
        } else if (stmt instanceof WhileNode) {
            generateWhile((WhileNode) stmt);
        } else if (stmt instanceof ForNode) {
            generateFor((ForNode) stmt);
        } else if (stmt instanceof DoWhileNode) {
            generateDoWhile((DoWhileNode) stmt);
        } else if (stmt instanceof AssignmentNode) {
            generateAssignment((AssignmentNode) stmt);
        } else if (stmt instanceof VarDeclStatementNode) {
            generateVarDecl((VarDeclStatementNode) stmt);
        } else if (stmt instanceof BlockNode) {
            generateBlock((BlockNode) stmt);
        }
    }

    private void generateExpressionStatement(ExpressionStatementNode exprStmt) {
        if (exprStmt.getExpressionNode() != null) {
            String temp = generateExpression(exprStmt.getExpressionNode());
            freeTemp(temp);
        }
    }

    private void generateReturn(ReturnNode ret) {
        if (ret.getReturnValue() != null) {
            String value = generateExpression(ret.getReturnValue());
            textSection.add("  move $v0, " + value);
            freeTemp(value);
        }
        // El epílogo se encargará del retorno
    }

    private void generateIf(IfNode ifStmt) {
        String condition = generateExpression(ifStmt.getCondition());
        String elseLabel = newLabel();
        String endLabel = newLabel();

        textSection.add("  beqz " + condition + ", " + elseLabel);
        freeTemp(condition);

        generateStatement(ifStmt.getThenBlock());
        textSection.add("  j " + endLabel);

        textSection.add(elseLabel + ":");
        if (ifStmt.getElseBlock() != null) {
            generateStatement(ifStmt.getElseBlock());
        }

        textSection.add(endLabel + ":");
    }

    private void generateWhile(WhileNode whileStmt) {
        String startLabel = newLabel();
        String endLabel = newLabel();

        textSection.add(startLabel + ":");
        String condition = generateExpression(whileStmt.getCondition());
        textSection.add("  beqz " + condition + ", " + endLabel);
        freeTemp(condition);

        generateStatement(whileStmt.getBody());
        textSection.add("  j " + startLabel);

        textSection.add(endLabel + ":");
    }

    private void generateFor(ForNode forStmt) {
        // Inicialización
        if (forStmt.getInit() != null) {
            generateStatement(forStmt.getInit());
        }

        String startLabel = newLabel();
        String endLabel = newLabel();

        textSection.add(startLabel + ":");

        // Condición
        if (forStmt.getCondition() != null) {
            String condition = generateExpression(forStmt.getCondition());
            textSection.add("  beqz " + condition + ", " + endLabel);
            freeTemp(condition);
        }

        // Cuerpo
        generateStatement(forStmt.getBody());

        // Incremento
        if (forStmt.getIncrement() != null) {
            String increment = generateExpression(forStmt.getIncrement());
            freeTemp(increment);
        }

        textSection.add("  j " + startLabel);
        textSection.add(endLabel + ":");
    }

    private void generateDoWhile(DoWhileNode doWhile) {
        String startLabel = newLabel();
        String conditionLabel = newLabel();

        textSection.add(startLabel + ":");
        generateStatement(doWhile.getBody());

        textSection.add(conditionLabel + ":");
        String condition = generateExpression(doWhile.getCondition());
        textSection.add("  bnez " + condition + ", " + startLabel);
        freeTemp(condition);
    }

    private void generateAssignment(AssignmentNode assign) {
        String value = generateExpression(assign.getValue());
        ExpressionNode target = assign.getTarget();

        if (target instanceof VariableNode) {
            String varName = ((VariableNode) target).getName();
            storeVariable(varName, value);
        } else if (target instanceof ArrayAccessNode) {
            generateArrayStore((ArrayAccessNode) target, value);
        }

        freeTemp(value);
    }

    private void generateVarDecl(VarDeclStatementNode varDeclStmt) {
    VarDeclNode varDecl = varDeclStmt.getVarDeclNode();
    String name = varDecl.getName();

    Map<String, SymbolInfo> currentScope = localScopes.peek();

    if (!currentScope.containsKey(name)) {
        int size = varDecl.isArray()
                ? varDecl.getArraySize() * 4
                : 4;

        size = ((size + 3) / 4) * 4;

        currentFrameSize += size;

        SymbolInfo info = new SymbolInfo(
                varDecl.isArray(),
                varDecl.getArraySize(),
                0,
                -currentFrameSize,
                false
        );

        currentScope.put(name, info);
    }

    if (varDecl.hasInitialNode()) {
        String initValue = generateExpression(varDecl.getInitialNode());
        storeVariable(name, initValue);
        freeTemp(initValue);
    }
}



    private String generateExpression(ExpressionNode expr) {
        if (expr instanceof NumberNode) {
            return generateNumber((NumberNode) expr);
        } else if (expr instanceof VariableNode) {
            return loadVariable(((VariableNode) expr).getName());
        } else if (expr instanceof BinaryOpNode) {
            return generateBinaryOp((BinaryOpNode) expr);
        } else if (expr instanceof UnaryOpNode) {
            return generateUnaryOp((UnaryOpNode) expr);
        } else if (expr instanceof FunctionCallNode) {
            return generateFunctionCall((FunctionCallNode) expr);
        } else if (expr instanceof BooleanNode) {
            return generateBoolean((BooleanNode) expr);
        } else if (expr instanceof StringNode) {
            return generateStringLiteral((StringNode) expr);
        } else if (expr instanceof CharNode) {
            return generateChar((CharNode) expr);
        } else if (expr instanceof ArrayAccessNode) {
            return generateArrayLoad((ArrayAccessNode) expr);
        }

        // Fallback
        String temp = newTemp();
        textSection.add("  li " + temp + ", 0");
        return temp;
    }

    private String generateNumber(NumberNode num) {
        String temp = newTemp();
        textSection.add("  li " + temp + ", " + num.getValue());
        return temp;
    }

    private String loadVariable(String varName) {
        SymbolInfo info = findSymbol(varName);
        if (info == null) {
            throw new RuntimeException("Variable no encontrada: " + varName);
        }

        String temp = newTemp();

        if (info.isGlobal) {
            textSection.add("  la $t9, _" + varName);
            textSection.add("  lw " + temp + ", 0($t9)");
        } else {
            textSection.add("  lw " + temp + ", " + info.offset + "($fp)");
        }

        return temp;
    }

    private void storeVariable(String varName, String valueReg) {
        SymbolInfo info = findSymbol(varName);
        if (info == null) {
            throw new RuntimeException("Variable no encontrada: " + varName);
        }

        if (info.isGlobal) {
            textSection.add("  la $t9, _" + varName);
            textSection.add("  sw " + valueReg + ", 0($t9)");
        } else {
            textSection.add("  sw " + valueReg + ", " + info.offset + "($fp)");
        }
    }

    private SymbolInfo findSymbol(String name) {
        for (Map<String, SymbolInfo> scope : localScopes) {
            if (scope.containsKey(name)) {
                return scope.get(name);
            }
        }

        if (globalSymbols.containsKey(name)) {
            return globalSymbols.get(name);
        }

        return null;
    }

    private String generateBinaryOp(BinaryOpNode binOp) {
        String left = generateExpression(binOp.getLeft());
        String right = generateExpression(binOp.getRight());
        String result = newTemp();

        switch (binOp.getOperator()) {
            case "+":
                textSection.add("  addu " + result + ", " + left + ", " + right);
                break;
            case "-":
                textSection.add("  subu " + result + ", " + left + ", " + right);
                break;
            case "*":
                textSection.add("  mul " + result + ", " + left + ", " + right);
                break;
            case "/":
                textSection.add("  div " + result + ", " + left + ", " + right);
                break;
            case "%":
                textSection.add("  div " + left + ", " + right);
                textSection.add("  mfhi " + result);
                break;
            case "==":
                textSection.add("  seq " + result + ", " + left + ", " + right);
                break;
            case "!=":
                textSection.add("  sne " + result + ", " + left + ", " + right);
                break;
            case "<":
                textSection.add("  slt " + result + ", " + left + ", " + right);
                break;
            case ">":
                textSection.add("  sgt " + result + ", " + left + ", " + right);
                break;
            case "<=":
                textSection.add("  sle " + result + ", " + left + ", " + right);
                break;
            case ">=":
                textSection.add("  sge " + result + ", " + left + ", " + right);
                break;
            case "&&":
                textSection.add("  and " + result + ", " + left + ", " + right);
                break;
            case "||":
                textSection.add("  or " + result + ", " + left + ", " + right);
                break;
            default:
                textSection.add("  addu " + result + ", " + left + ", " + right);
        }

        freeTemp(left);
        freeTemp(right);
        return result;
    }

    private String generateUnaryOp(UnaryOpNode unaryOp) {
        String operand = generateExpression(unaryOp.getOperand());
        String result = newTemp();

        switch (unaryOp.getOperator()) {
            case "-":
                textSection.add("  neg " + result + ", " + operand);
                break;
            case "!":
                textSection.add("  seq " + result + ", " + operand + ", 0");
                break;
            case "&": // Dirección de
                if (unaryOp.getOperand() instanceof VariableNode) {
                    String varName = ((VariableNode) unaryOp.getOperand()).getName();
                    SymbolInfo info = findSymbol(varName);

                    if (info != null) {
                        if (info.isGlobal) {
                            textSection.add("  la " + result + ", _" + varName);
                        } else {
                            textSection.add("  addiu " + result + ", $fp, " + info.offset);
                        }
                    }
                }
                break;
            case "*": // Desreferencia
                textSection.add("  lw " + result + ", 0(" + operand + ")");
                break;
            default:
                textSection.add("  move " + result + ", " + operand);
        }

        freeTemp(operand);
        return result;
    }

    private String generateFunctionCall(FunctionCallNode call) { // Aqui
        String funcName = call.getFunctionName();
        List<ExpressionNode> args = call.getArguments();

        // Guardar registros temporales caller-saved que estén en uso
        saveCallerSavedRegs();

        // Pasar argumentos (ABI O32)
        for (int i = 0; i < args.size() && i < 4; i++) {
            String argTemp = generateExpression(args.get(i));
            textSection.add("  move $a" + i + ", " + argTemp);
            freeTemp(argTemp);
        }

        // Argumentos 5+ van al stack (caller responsibility)
        if (args.size() > 4) {
            for (int i = args.size() - 1; i >= 4; i--) {
                String arg = generateExpression(args.get(i));
                textSection.add("  addiu $sp, $sp, -4");
                textSection.add("  sw " + arg + ", 0($sp)");
                freeTemp(arg);
            }
        }

        // Llamar función
        textSection.add("  jal " + funcName);

        // Limpiar argumentos del stack si hubo más de 4
        if (args.size() > 4) {
            int stackArgsSize = (args.size() - 4) * 4;
            textSection.add("  addiu $sp, $sp, " + stackArgsSize);
        }

        // Restaurar registros
        restoreCallerSavedRegs();

        // Resultado en $v0
        String result = newTemp();
        textSection.add("  move " + result + ", $v0");
        return result;
    }

    private void saveCallerSavedRegs() {
        // En una implementación real, necesitarías trackear qué $t registers están en
        // uso
        textSection.add("  # Save caller-saved registers (simplified)");
        // Por ahora no guardamos nada, asumimos que no hay valores importantes
    }

    private void restoreCallerSavedRegs() {
        textSection.add("  # Restore caller-saved registers (simplified)");
    }

    private String generateBoolean(BooleanNode bool) {
        String temp = newTemp();
        int value = bool.getValue() ? 1 : 0;
        textSection.add("  li " + temp + ", " + value);
        return temp;
    }

    private String generateStringLiteral(StringNode str) {
        String value = str.getValue();
        String label;

        if (stringLiterals.containsKey(value)) {
            label = stringLiterals.get(value);
        } else {
            label = "str_" + stringLiteralCount++;
            dataSection.add(label + ": .asciiz \"" + escapeString(value) + "\"");
            stringLiterals.put(value, label);
        }

        String temp = newTemp();
        textSection.add("  la " + temp + ", " + label);
        return temp;
    }

    private String escapeString(String str) {
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private String generateChar(CharNode charNode) {
        String temp = newTemp();
        char value = charNode.getValue();
        textSection.add("  li " + temp + ", " + (int) value);
        return temp;
    }

    // ============ ARRAY ACCESS METHODS ============

    private String generateArrayLoad(ArrayAccessNode arrayAccess) {
        SymbolInfo info = findSymbol(getArrayName(arrayAccess));
        if (info == null || !info.isArray) {
            throw new RuntimeException("Array no encontrado: " + getArrayName(arrayAccess));
        }

        List<ExpressionNode> indices = arrayAccess.getIndices();
        String result = newTemp();

        if (indices.size() == 1) {
            // Array 1D
            String index = generateExpression(indices.get(0));
            calculateArrayOffset1D(getArrayName(arrayAccess), index, result);
        } else if (indices.size() == 2) {
            // Array 2D
            String index1 = generateExpression(indices.get(0));
            String index2 = generateExpression(indices.get(1));
            calculateArrayOffset2D(getArrayName(arrayAccess), index1, index2, result);
        } else {
            throw new RuntimeException("Arrays de más de 2 dimensiones no soportados");
        }

        // Cargar valor
        textSection.add("  lw " + result + ", 0(" + result + ")");
        return result;
    }

    private void generateArrayStore(ArrayAccessNode arrayAccess, String value) {
        SymbolInfo info = findSymbol(getArrayName(arrayAccess));
        if (info == null || !info.isArray) {
            throw new RuntimeException("Array no encontrado: " + getArrayName(arrayAccess));
        }

        List<ExpressionNode> indices = arrayAccess.getIndices();
        String addrReg = newTemp();

        if (indices.size() == 1) {
            // Array 1D
            String index = generateExpression(indices.get(0));
            calculateArrayOffset1D(getArrayName(arrayAccess), index, addrReg);
        } else if (indices.size() == 2) {
            // Array 2D
            String index1 = generateExpression(indices.get(0));
            String index2 = generateExpression(indices.get(1));
            calculateArrayOffset2D(getArrayName(arrayAccess), index1, index2, addrReg);
        } else {
            throw new RuntimeException("Arrays de más de 2 dimensiones no soportados");
        }

        // Almacenar valor
        textSection.add("  sw " + value + ", 0(" + addrReg + ")");
        freeTemp(addrReg);
    }

    private String getArrayName(ArrayAccessNode arrayAccess) {
        // Extraer nombre del array del nodo
        ExpressionNode arrayExpr = arrayAccess.getArray();
        if (arrayExpr instanceof VariableNode) {
            return ((VariableNode) arrayExpr).getName();
        }
        throw new RuntimeException("Array access no soportado");
    }

    private void calculateArrayOffset1D(String arrayName, String indexReg, String resultReg) {
        SymbolInfo info = findSymbol(arrayName);

        textSection.add("  # Calculate 1D array offset for " + arrayName);

        // offset = base + index * 4
        if (info.isGlobal) {
            textSection.add("  la " + resultReg + ", _" + arrayName);
        } else {
            textSection.add("  addiu " + resultReg + ", $fp, " + info.offset);
        }

        textSection.add("  sll $t8, " + indexReg + ", 2"); // index * 4
        textSection.add("  addu " + resultReg + ", " + resultReg + ", $t8");

        freeTemp(indexReg);
    }

    private void calculateArrayOffset2D(String arrayName, String index1Reg, String index2Reg, String resultReg) {
        SymbolInfo info = findSymbol(arrayName);

        textSection.add("  # Calculate 2D array offset for " + arrayName);

        // offset = base + (i * cols + j) * 4
        if (info.isGlobal) {
            textSection.add("  la " + resultReg + ", _" + arrayName);
        } else {
            textSection.add("  addiu " + resultReg + ", $fp, " + info.offset);
        }

        // i * cols (usar segunda dimensión)
        textSection.add("  li $t8, " + info.secondDimension);
        textSection.add("  mul $t9, " + index1Reg + ", $t8");

        // + j
        textSection.add("  addu $t9, $t9, " + index2Reg);

        // * 4
        textSection.add("  sll $t9, $t9, 2");

        // + base
        textSection.add("  addu " + resultReg + ", " + resultReg + ", $t9");

        freeTemp(index1Reg);
        freeTemp(index2Reg);
    }

    private String newTemp() {
        if (!availableTemps.isEmpty()) {
            return availableTemps.pop();
        }
        if (tempCount < 10) {
            String temp = "$t" + tempCount;
            tempCount++;
            return temp;
        }
        throw new RuntimeException("No hay registros temporales disponibles");
    }

    private void freeTemp(String temp) {
        if (temp.startsWith("$t")) {
            availableTemps.push(temp);
        }
    }

    private String newLabel() {
        return "L" + labelCount++;
    }

    private void buildFinalCode() {
        // Construir sección .data
        code.append(String.join("\n", dataSection));
        code.append("\n\n");

        // Construir sección .text
        code.append(String.join("\n", textSection));
    }
}