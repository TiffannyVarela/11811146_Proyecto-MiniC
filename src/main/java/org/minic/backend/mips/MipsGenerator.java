package org.minic.backend.mips;

import org.minic.ast.*;
import java.util.*;

/*
 MipsGenerator

 Backend del compilador MiniC encargado de traducir el AST a código ensamblador MIPS.
 
 Implementa:
   - Manejo de secciones .data y .text
   - Convenciones de llamada (stack frame, $fp, $sp)
   - Gestión de variables globales y locales
   - Generación de código para expresiones, control de flujo y llamadas a funciones
 
  El generador utiliza registros temporales ($t0-$t9), etiquetas dinámicas y una tabla de símbolos propia.
 */

public class MipsGenerator {
    // Codigo final generado
    private StringBuilder code = new StringBuilder();

    // Contadores para registros temporales y etiquetas
    private int tempCount = 0;
    private int labelCount = 0;
    // Pila de registros temporales disponibles
    private Stack<String> availableTemps = new Stack<>();
    // Tabla de símbolos para variables globales y locales
    private Map<String, SymbolInfo> globalSymbols = new HashMap<>();
    // Pila de tablas de símbolos para ámbitos locales
    private Deque<Map<String, SymbolInfo>> localScopes = new ArrayDeque<>();
    // Literales de cadena y su etiqueta en .data
    private Map<String, String> stringLiterals = new HashMap<>();
    private int stringLiteralCount = 0;
    // Contexto de función actual y tamaño del frame
    private String currentFunction = null;
    private int currentFrameSize = 0;
    // Secciones del programa MIPS
    private List<String> dataSection = new ArrayList<>();
    private List<String> textSection = new ArrayList<>();

    /*
     * SymbolInfo

      Representa la información necesaria para generar código de una variable en MIPS:
      - Tipo (simple o arreglo)
      - Tamaño
      - Offset relativo a $fp o dirección global
      - Alcance (global o local)
     */
    private class SymbolInfo {
        boolean isArray;
        int arraySize;
        int secondDimension;
        int offset;
        boolean isGlobal;

        SymbolInfo(boolean isArray, int arraySize, int secondDimension, int offset, boolean isGlobal) {
            this.isArray = isArray;
            this.arraySize = arraySize;
            this.secondDimension = secondDimension;
            this.offset = offset;
            this.isGlobal = isGlobal;
        }
        // Calcula el tamaño total en bytes que ocupa el simbolo
        int getTotalSize() {
            if (!isArray)
                return 4;
            if (secondDimension > 0) {
                return arraySize * secondDimension * 4;
            }
            return arraySize * 4;
        }
    }

    // Genera el código MIPS a partir del AST proporcionado
    public String generate(AstNode ast) {
        // Inicializa secciones de datos y texto
        initializeSections();
        if (ast instanceof ProgramNode) {
            // Recolecta símbolos globales
            collectGlobalSymbols((ProgramNode) ast);
        }
        if (ast instanceof ProgramNode) {
            // Genera codigo para funciones y declaraciones
            generateProgram((ProgramNode) ast);
        }
        // Ensambla el código final
        buildFinalCode();
        return code.toString();
    }

    // Construye el código final combinando las secciones de datos y texto
    private void initializeSections() {
        dataSection.add(".data");
        dataSection.add("newline: .asciiz \"\\n\"");
        dataSection.add("true_str: .asciiz \"true\"");
        dataSection.add("false_str: .asciiz \"false\"");
        dataSection.add("result_msg: .asciiz \"Result: \"");

        textSection.add(".text");
        textSection.add(
                ".globl main, __start, print_int, print_char, print_bool, print_str, println, read_int, read_char, read_str, exit");
        textSection.add("");

        addRuntimeFunctions();
    }

    /*
     Agrega funciones runtime básicas:
      - print_int, print_char, print_bool
      - read_int, read_char, read_str
      - exit
     
      También define el punto de entrada __start
     */
    private void addRuntimeFunctions() {
        textSection.add("# =========================================");
        textSection.add("# RUNTIME FUNCTIONS");
        textSection.add("# =========================================");
        textSection.add("");

        textSection.add("__start:");
        textSection.add("  # === SYSTEM INITIALIZATION ===");
        textSection.add("  addiu $sp, $sp, -64    # Reservar stack para sistema");
        textSection.add("  sw $ra, 60($sp)        # Guardar return address");
        textSection.add("  sw $fp, 56($sp)        # Guardar frame pointer");
        textSection.add("  move $fp, $sp          # Establecer frame pointer");
        textSection.add("  ");
        textSection.add("  # === CALL MAIN ===");
        textSection.add("  jal main               # Ejecutar programa principal");
        textSection.add("  ");
        textSection.add("  # === DISPLAY RESULT ===");
        textSection.add("  move $t0, $v0          # Guardar resultado");
        textSection.add("  la $a0, result_msg     # Cargar mensaje 'Result: '");
        textSection.add("  li $v0, 4              # syscall: print string");
        textSection.add("  syscall");
        textSection.add("  move $a0, $t0          # Cargar resultado numérico");
        textSection.add("  li $v0, 1              # syscall: print integer");
        textSection.add("  syscall");
        textSection.add("  la $a0, newline        # Nueva línea");
        textSection.add("  li $v0, 4");
        textSection.add("  syscall");
        textSection.add("  ");
        textSection.add("  # === EXIT PROGRAM ===");
        textSection.add("  li $v0, 10             # syscall: exit");
        textSection.add("  syscall");
        textSection.add("");

        textSection.add("print_int:");
        textSection.add("  move $a0, $a0          # Argumento ya está en $a0");
        textSection.add("  li $v0, 1              # syscall: print integer");
        textSection.add("  syscall");
        textSection.add("  jr $ra                 # Retornar");
        textSection.add("");

        textSection.add("print_char:");
        textSection.add("  move $a0, $a0");
        textSection.add("  li $v0, 11             # syscall: print character");
        textSection.add("  syscall");
        textSection.add("  jr $ra");
        textSection.add("");

        textSection.add("print_bool:");
        textSection.add("  beqz $a0, print_false  # Si es 0, imprimir false");
        textSection.add("  la $a0, true_str       # Cargar 'true'");
        textSection.add("  li $v0, 4");
        textSection.add("  syscall");
        textSection.add("  jr $ra");
        textSection.add("print_false:");
        textSection.add("  la $a0, false_str      # Cargar 'false'");
        textSection.add("  li $v0, 4");
        textSection.add("  syscall");
        textSection.add("  jr $ra");
        textSection.add("");

        textSection.add("print_str:");
        textSection.add("  move $a0, $a0");
        textSection.add("  li $v0, 4");
        textSection.add("  syscall");
        textSection.add("  jr $ra");
        textSection.add("");

        textSection.add("println:");
        textSection.add("  la $a0, newline");
        textSection.add("  li $v0, 4");
        textSection.add("  syscall");
        textSection.add("  jr $ra");
        textSection.add("");

        textSection.add("read_int:");
        textSection.add("  li $v0, 5              # syscall: read integer");
        textSection.add("  syscall");
        textSection.add("  jr $ra");
        textSection.add("");

        textSection.add("read_char:");
        textSection.add("  li $v0, 12             # syscall: read character");
        textSection.add("  syscall");
        textSection.add("  jr $ra");
        textSection.add("");

        textSection.add("read_str:");
        textSection.add("  addiu $sp, $sp, -8     # Guardar registros");
        textSection.add("  sw $a0, 0($sp)");
        textSection.add("  sw $a1, 4($sp)");
        textSection.add("  move $a0, $a0          # Buffer");
        textSection.add("  move $a1, $a1          # Máxima longitud");
        textSection.add("  li $v0, 8              # syscall: read string");
        textSection.add("  syscall");
        textSection.add("  lw $a1, 4($sp)         # Restaurar registros");
        textSection.add("  lw $a0, 0($sp)");
        textSection.add("  addiu $sp, $sp, 8");
        textSection.add("  jr $ra");
        textSection.add("");

        textSection.add("exit:");
        textSection.add("  move $a0, $a0          # Código de salida");
        textSection.add("  li $v0, 17             # syscall: exit2");
        textSection.add("  syscall");
        textSection.add("  # No retorna");
        textSection.add("");
    }

    // Recolecta símbolos globales del programa
    private void collectGlobalSymbols(ProgramNode program) {
        int globalOffset = 0;

        for (AstNode node : program.getDeclarationsNodes()) {
            if (node instanceof VarDeclNode) {
                VarDeclNode varDecl = (VarDeclNode) node;
                String name = varDecl.getName();

                int arraySize = varDecl.getArraySize();
                int secondDim = 0;
                if (varDecl.isArray() && varDecl.getType().contains("[][]")) {
                    arraySize = 10;
                    secondDim = 5;
                }

                SymbolInfo info = new SymbolInfo(
                        varDecl.isArray(),
                        arraySize,
                        secondDim,
                        globalOffset,
                        true);

                globalSymbols.put(name, info);
                globalOffset += info.getTotalSize();

                String label = "_" + name;
                if (varDecl.isArray()) {
                    if (secondDim > 0) {
                        dataSection.add(label + ": .space " + (arraySize * secondDim * 4));
                    } else {
                        dataSection.add(label + ": .space " + (arraySize * 4));
                    }
                } else {
                    dataSection.add(label + ": .word 0");
                }
            }
        }
    }

    // Genera código para todas las funciones del programa
    private void generateProgram(ProgramNode program) {
        for (AstNode node : program.getDeclarationsNodes()) {
            if (node instanceof FunctionNode) {
                generateFunction((FunctionNode) node);
            }
        }
    }

    /*
      Agrega funciones runtime básicas:
      - print_int, print_char, print_bool
      - read_int, read_char, read_str
      - exit
     
      También define el punto de entrada __start
     */
    private void generateFunction(FunctionNode function) {
        currentFunction = function.getName();
        // Nueva tabla de símbolos para el ámbito local
        localScopes.clear();
        localScopes.push(new HashMap<>());

        textSection.add("");
        textSection.add("# =========================================");
        textSection.add("# FUNCTION: " + currentFunction);
        textSection.add("# =========================================");
        textSection.add(currentFunction + ":");

        textSection.add("  # === PROLOGUE ===");
        // Configuración del stack frame
        if (function.getName().equals("main")) {
            textSection.add("  move $fp, $sp          # $fp apunta al tope actual del stack");

            int localVarSize = calculateLocalVarsSize(function);
            if (localVarSize < 32) {
                localVarSize = 32;
            }
            localVarSize = ((localVarSize + 7) / 8) * 8;
            textSection.add("  addiu $sp, $sp, -" + localVarSize + "  # Reservar espacio para variables locales");
            currentFrameSize = localVarSize;

            textSection.add("  # Inicializar todas las variables locales a 0");
            for (int offset = 0; offset < localVarSize; offset += 4) {
                textSection.add("  sw $zero, " + offset + "($sp)");
            }

            initMainOffsets(function);

            if (function.getBody() != null) {
                preprocessMainDeclarations(function.getBody());
            }
        } else {
            textSection.add("  addiu $sp, $sp, -8");
            textSection.add("  sw $ra, 4($sp)");
            textSection.add("  sw $fp, 0($sp)");
            textSection.add("  move $fp, $sp");

            int localVarSize = calculateLocalVarsSize(function);
            int paramSize = function.getParameters() != null ? function.getParameters().size() * 4 : 0;
            currentFrameSize = 8 + paramSize + localVarSize;
            currentFrameSize = ((currentFrameSize + 7) / 8) * 8;

            textSection.add("  addiu $sp, $sp, -" + (currentFrameSize - 8));

            initLocalVarOffsets(function);

            if (function.getParameters() != null) {
                processParameters(function.getParameters());
            }
        }

        if (function.getBody() != null) {
            generateBlock(function.getBody());
        }

        textSection.add("");
        textSection.add("  # === EPILOGUE ===");
        // Restauración del stack frame y retorno
        if (function.getName().equals("main")) {
            if (currentFrameSize > 0) {
                textSection.add("  addiu $sp, $sp, " + currentFrameSize + "  # Liberar variables locales");
            }
            textSection.add("  jr $ra  # Retorna a __start");
        } else {
            textSection.add("  move $sp, $fp");
            textSection.add("  lw $fp, 0($sp)");
            textSection.add("  lw $ra, 4($sp)");
            textSection.add("  addiu $sp, $sp, 8");
            textSection.add("  jr $ra");
        }

        currentFunction = null;
    }

    // Inicializa offsets de variables locales en main
    private void initMainOffsets(FunctionNode function) {
        if (function.getBody() != null) {
            int currentOffset = 0;

            for (StatementNode stmt : function.getBody().getStatements()) {
                if (stmt instanceof VarDeclStatementNode) {
                    VarDeclNode varDecl = ((VarDeclStatementNode) stmt).getVarDeclNode();

                    int varSize = varDecl.isArray() ? varDecl.getArraySize() * 4 : 4;
                    varSize = ((varSize + 3) / 4) * 4;

                    SymbolInfo info = new SymbolInfo(
                            varDecl.isArray(),
                            varDecl.getArraySize(),
                            0,
                            currentOffset,
                            false);

                    localScopes.peek().put(varDecl.getName(), info);
                    currentOffset += varSize;
                } else if (stmt instanceof BlockNode) {
                    currentOffset = processBlockForOffsets((BlockNode) stmt, currentOffset);
                }
            }
        }
    }

    // Procesa bloques anidados para asignar offsets a variables locales
    private int processBlockForOffsets(BlockNode block, int startOffset) {
        int currentOffset = startOffset;

        for (StatementNode stmt : block.getStatements()) {
            if (stmt instanceof VarDeclStatementNode) {
                VarDeclNode varDecl = ((VarDeclStatementNode) stmt).getVarDeclNode();

                int varSize = varDecl.isArray() ? varDecl.getArraySize() * 4 : 4;
                varSize = ((varSize + 3) / 4) * 4;

                SymbolInfo info = new SymbolInfo(
                        varDecl.isArray(),
                        varDecl.getArraySize(),
                        0,
                        currentOffset,
                        false);

                localScopes.peek().put(varDecl.getName(), info);
                currentOffset += varSize;
            } else if (stmt instanceof BlockNode) {
                localScopes.push(new HashMap<>());
                currentOffset = processBlockForOffsets((BlockNode) stmt, currentOffset);
                localScopes.pop();
            }
        }

        return currentOffset;
    }

    // Preprocesa declaraciones en main para inicializar variables con valores
    private void preprocessMainDeclarations(BlockNode block) {
        for (StatementNode stmt : block.getStatements()) {
            if (stmt instanceof VarDeclStatementNode) {
                VarDeclNode varDecl = ((VarDeclStatementNode) stmt).getVarDeclNode();

                if (varDecl.hasInitialNode()) {
                    String initValue = generateExpression(varDecl.getInitialNode());
                    storeVariableMain(varDecl.getName(), initValue);
                    freeTemp(initValue);
                }
            } else if (stmt instanceof BlockNode) {
                localScopes.push(new HashMap<>());
                preprocessMainDeclarations((BlockNode) stmt);
                localScopes.pop();
            }
        }
    }

    // Almacena el valor en la variable especificada en main
    private void storeVariableMain(String varName, String valueReg) {
        SymbolInfo info = findSymbol(varName);
        if (info == null) {
            throw new RuntimeException("Variable no encontrada: " + varName);
        }

        if (info.isGlobal) {
            textSection.add("  la $t9, _" + varName);
            textSection.add("  sw " + valueReg + ", 0($t9)");
        } else if (currentFunction != null && currentFunction.equals("main")) {
            textSection.add("  sw " + valueReg + ", " + info.offset + "($sp)");
        } else {
            textSection.add("  sw " + valueReg + ", " + info.offset + "($fp)");
        }
    }

    // Carga el valor de la variable especificada en un registro temporal
    private String loadVariable(String varName) {
        SymbolInfo info = findSymbol(varName);
        if (info == null) {
            throw new RuntimeException("Variable no encontrada: " + varName);
        }

        String temp = newTemp();

        if (info.isGlobal) {
            textSection.add("  la $t9, _" + varName);
            textSection.add("  lw " + temp + ", 0($t9)");
        } else if (currentFunction != null && currentFunction.equals("main")) {
            textSection.add("  lw " + temp + ", " + info.offset + "($sp)");
        } else {
            textSection.add("  lw " + temp + ", " + info.offset + "($fp)");
        }

        return temp;
    }

    // Inicializa offsets de variables locales en funciones distintas de main
    private void initLocalVarOffsets(FunctionNode function) {
        int offset = -currentFrameSize;

        if (function.getBody() != null) {
            initBlockVarOffsets(function.getBody(), offset);
        }
    }

    // Inicializa offsets de variables locales en bloques anidados
    private void initBlockVarOffsets(BlockNode block, int baseOffset) {
        int currentOffset = baseOffset;

        for (StatementNode stmt : block.getStatements()) {
            if (stmt instanceof VarDeclStatementNode) {
                VarDeclNode varDecl = ((VarDeclStatementNode) stmt).getVarDeclNode();

                int varSize = varDecl.isArray() ? varDecl.getArraySize() * 4 : 4;
                varSize = ((varSize + 3) / 4) * 4;
                currentOffset -= varSize;

                SymbolInfo info = new SymbolInfo(
                        varDecl.isArray(),
                        varDecl.getArraySize(),
                        0,
                        currentOffset,
                        false);

                localScopes.peek().put(varDecl.getName(), info);

                if (varDecl.hasInitialNode()) {
                    String initValue = generateExpression(varDecl.getInitialNode());
                    storeVariable(varDecl.getName(), initValue);
                    freeTemp(initValue);
                }
            } else if (stmt instanceof BlockNode) {
                localScopes.push(new HashMap<>());
                initBlockVarOffsets((BlockNode) stmt, currentOffset);
                localScopes.pop();
            }
        }
    }

    // Almacena el valor en la variable especificada
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

    // Calcula el tamaño total de variables locales en la función
    private int calculateLocalVarsSize(FunctionNode function) {
        int size = 0;
        if (function.getBody() != null) {
            size = calculateBlockLocalSize(function.getBody());
        }

        return size;
    }

    // Calcula el tamaño total de variables locales en un bloque
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

    // Procesa parámetros de función y los almacena en el stack frame
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

    // Genera código para un bloque de declaraciones
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

    // Genera código para una declaración específica
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

    // Genera código para una declaración de expresión
    private void generateExpressionStatement(ExpressionStatementNode exprStmt) {
        if (exprStmt.getExpressionNode() != null) {
            String temp = generateExpression(exprStmt.getExpressionNode());
            freeTemp(temp);
        }
    }

    // Genera código para una declaración de retorno
    private void generateReturn(ReturnNode ret) {
        if (ret.getReturnValue() != null) {
            String value = generateExpression(ret.getReturnValue());
            textSection.add("  move $v0, " + value);
            freeTemp(value);
        }
    }

    // Genera código para una declaración if-else
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

    // Genera código para una declaración while
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

    // Genera código para una declaración for
    private void generateFor(ForNode forStmt) {
        if (forStmt.getInit() != null) {
            generateStatement(forStmt.getInit());
        }

        String startLabel = newLabel();
        String endLabel = newLabel();

        textSection.add(startLabel + ":");
        if (forStmt.getCondition() != null) {
            String condition = generateExpression(forStmt.getCondition());
            textSection.add("  beqz " + condition + ", " + endLabel);
            freeTemp(condition);
        }
        generateStatement(forStmt.getBody());

        if (forStmt.getIncrement() != null) {
            String increment = generateExpression(forStmt.getIncrement());
            freeTemp(increment);
        }

        textSection.add("  j " + startLabel);
        textSection.add(endLabel + ":");
    }

    // Genera código para una declaración do-while
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

    // Genera código para una asignación
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

    // Genera código para una declaración de variable
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
                    false);

            currentScope.put(name, info);
        }

        if (varDecl.hasInitialNode()) {
            String initValue = generateExpression(varDecl.getInitialNode());
            storeVariable(name, initValue);
            freeTemp(initValue);
        }
    }

    // Genera código para una expresión y devuelve el registro temporal con el resultado
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

        String temp = newTemp();
        textSection.add("  li " + temp + ", 0");
        return temp;
    }

    // Genera código para un literal booleano
    private String generateNumber(NumberNode num) {
        String temp = newTemp();
        textSection.add("  li " + temp + ", " + num.getValue());
        return temp;
    }

    // Busca el símbolo en los ámbitos locales y globales
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

    // Genera código para una operación binaria
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

    // Genera código para una operación unaria
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
            case "&":
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
            case "*":
                textSection.add("  lw " + result + ", 0(" + operand + ")");
                break;
            default:
                textSection.add("  move " + result + ", " + operand);
        }

        freeTemp(operand);
        return result;
    }

    // Genera código para una llamada a función
    private String generateFunctionCall(FunctionCallNode call) {
        String funcName = call.getFunctionName();
        List<ExpressionNode> args = call.getArguments();
        saveCallerSavedRegs();

        for (int i = 0; i < args.size() && i < 4; i++) {
            String argTemp = generateExpression(args.get(i));
            textSection.add("  move $a" + i + ", " + argTemp);
            freeTemp(argTemp);
        }

        if (args.size() > 4) {
            for (int i = args.size() - 1; i >= 4; i--) {
                String arg = generateExpression(args.get(i));
                textSection.add("  addiu $sp, $sp, -4");
                textSection.add("  sw " + arg + ", 0($sp)");
                freeTemp(arg);
            }
        }

        textSection.add("  jal " + funcName);

        if (args.size() > 4) {
            int stackArgsSize = (args.size() - 4) * 4;
            textSection.add("  addiu $sp, $sp, " + stackArgsSize);
        }

        restoreCallerSavedRegs();

        String result = newTemp();
        textSection.add("  move " + result + ", $v0");
        return result;
    }

    // Guarda los registros caller-saved antes de una llamada a función
    private void saveCallerSavedRegs() {
        textSection.add("  # Save caller-saved registers");
    }

    // Restaura los registros caller-saved después de una llamada a función
    private void restoreCallerSavedRegs() {
        textSection.add("  # Restore caller-saved registers");
    }

    // Genera código para un literal booleano
    private String generateBoolean(BooleanNode bool) {
        String temp = newTemp();
        int value = bool.getValue() ? 1 : 0;
        textSection.add("  li " + temp + ", " + value);
        return temp;
    }

    // Genera código para un literal de cadena
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

    // Escapa caracteres especiales en una cadena
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

    private String generateArrayLoad(ArrayAccessNode arrayAccess) {
        SymbolInfo info = findSymbol(getArrayName(arrayAccess));
        if (info == null || !info.isArray) {
            throw new RuntimeException("Array no encontrado: " + getArrayName(arrayAccess));
        }

        List<ExpressionNode> indices = arrayAccess.getIndices();
        String result = newTemp();

        if (indices.size() == 1) {
            String index = generateExpression(indices.get(0));
            calculateArrayOffset1D(getArrayName(arrayAccess), index, result);
        } else if (indices.size() == 2) {
            String index1 = generateExpression(indices.get(0));
            String index2 = generateExpression(indices.get(1));
            calculateArrayOffset2D(getArrayName(arrayAccess), index1, index2, result);
        } else {
            throw new RuntimeException("Arrays de más de 2 dimensiones no soportados");
        }

        textSection.add("  lw " + result + ", 0(" + result + ")");
        return result;
    }

    // Genera código para almacenar un valor en un array
    private void generateArrayStore(ArrayAccessNode arrayAccess, String value) {
        SymbolInfo info = findSymbol(getArrayName(arrayAccess));
        if (info == null || !info.isArray) {
            throw new RuntimeException("Array no encontrado: " + getArrayName(arrayAccess));
        }

        List<ExpressionNode> indices = arrayAccess.getIndices();
        String addrReg = newTemp();

        if (indices.size() == 1) {
            String index = generateExpression(indices.get(0));
            calculateArrayOffset1D(getArrayName(arrayAccess), index, addrReg);
        } else if (indices.size() == 2) {
            String index1 = generateExpression(indices.get(0));
            String index2 = generateExpression(indices.get(1));
            calculateArrayOffset2D(getArrayName(arrayAccess), index1, index2, addrReg);
        } else {
            throw new RuntimeException("Arrays de más de 2 dimensiones no soportados");
        }

        textSection.add("  sw " + value + ", 0(" + addrReg + ")");
        freeTemp(addrReg);
    }

    private String getArrayName(ArrayAccessNode arrayAccess) {
        ExpressionNode arrayExpr = arrayAccess.getArray();
        if (arrayExpr instanceof VariableNode) {
            return ((VariableNode) arrayExpr).getName();
        }
        throw new RuntimeException("Array access no soportado");
    }

    // Calcula el offset para un array 1D
    private void calculateArrayOffset1D(String arrayName, String indexReg, String resultReg) {
        SymbolInfo info = findSymbol(arrayName);

        textSection.add("  # Calculate 1D array offset for " + arrayName);

        if (info.isGlobal) {
            textSection.add("  la " + resultReg + ", _" + arrayName);
        } else {
            textSection.add("  addiu " + resultReg + ", $fp, " + info.offset);
        }

        textSection.add("  sll $t8, " + indexReg + ", 2");
        textSection.add("  addu " + resultReg + ", " + resultReg + ", $t8");

        freeTemp(indexReg);
    }

    // Calcula el offset para un array 2D
    private void calculateArrayOffset2D(String arrayName, String index1Reg, String index2Reg, String resultReg) {
        SymbolInfo info = findSymbol(arrayName);

        textSection.add("  # Calculate 2D array offset for " + arrayName);

        if (info.isGlobal) {
            textSection.add("  la " + resultReg + ", _" + arrayName);
        } else {
            textSection.add("  addiu " + resultReg + ", $fp, " + info.offset);
        }

        textSection.add("  li $t8, " + info.secondDimension);
        textSection.add("  mul $t9, " + index1Reg + ", $t8");
        textSection.add("  addu $t9, $t9, " + index2Reg);
        textSection.add("  sll $t9, $t9, 2");
        textSection.add("  addu " + resultReg + ", " + resultReg + ", $t9");

        freeTemp(index1Reg);
        freeTemp(index2Reg);
    }

    // Genera un nuevo registro temporal
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

    // Construye el código final combinando las secciones de datos y texto
    private void buildFinalCode() {
        code.append(String.join("\n", dataSection));
        code.append("\n\n");
        code.append(String.join("\n", textSection));
    }
}