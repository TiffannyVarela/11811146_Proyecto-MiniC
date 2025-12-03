package org.minic.backend.mips;

import org.minic.ast.*;
import java.util.*;

public class MipsGenerator {
    private StringBuilder code = new StringBuilder();
    private int tempCount = 0;
    private int labelCount = 0;

    // Gestión de registros temporales
    private Stack<String> availableTemps = new Stack<>();

    // Tabla de símbolos para variables globales
    private Map<String, Integer> globalVars = new HashMap<>();
    private Map<String, Integer> localVars = new HashMap<>();
    private int currentLocal = 0;

    // Manejo de Strings literales
    private Map<String, String> stringLiterals = new HashMap<>();
    private int stringLiteralsCount = 0;

    // Secciones de código
    private List<String> data = new ArrayList<>();
    private List<String> text = new ArrayList<>();

    public String generate(AstNode ast) {
        initializeSections();

        // Procesar AST
        if (ast instanceof ProgramNode) {
            generateProgram((ProgramNode) ast);
        }

        buildFinalCode();
        return code.toString();
    }

    private void initializeSections() {
        data.add(".data");
        data.add("newline: .asciiz \"\\n\"");
        data.add("true_str: .asciiz \"true\"");
        data.add("false_str: .asciiz \"false\"");
        
        text.add(".text");
        text.add(".globl main");
    }

    private void buildFinalCode() {
        code.append(String.join("\n", data));
        code.append("\n\n");
        code.append(String.join("\n", text));
    }

    private void generateProgram(ProgramNode program) {
        for (AstNode node : program.getChildren()) {
            if (node instanceof VarDeclNode) {
                generateGlobalVar((VarDeclNode) node);
            }
        }
        for (AstNode node : program.getChildren()) {
            if (node instanceof FunctionNode) {
                generateFunction((FunctionNode) node);
            }
        }
    }

    private void generateGlobalVar(VarDeclNode varDeclNode) {
        String name = varDeclNode.getName();
        String label = "_" + name;

        if (varDeclNode.isArray()) {
            int size = calculateArraySize(varDeclNode);
            data.add(label + ": .space " + (size * 4));
            globalVars.put(name, size * 4);
        } else {
            data.add(label + ": .word 0");
            globalVars.put(name, 4);
        }
    }

    private void generateFunction(FunctionNode functionNode) {
        int localesSize = calculateLocalVarsSize(functionNode);
        String funcName = functionNode.getName();
        text.add(funcName + ":");
        text.add("  # === PRÓLOGO ===");
        text.add("  addiu $sp, $sp, -" + (8 + 32));
        text.add("  sw $ra, " + (8 + 32 - 4) + "($sp)");
        text.add("  sw $fp, " + (8 + 32 - 8) + "($sp)"); 
        text.add("  move $fp, $sp");
        
        text.add("  # Guardar registros callee-saved usados");
        text.add("  sw $s0, " + (8 + 32 - 12) + "($sp)");
        text.add("  sw $s1, " + (8 + 32 - 16) + "($sp)");
        text.add("  sw $s2, " + (8 + 32 - 20) + "($sp)");
        text.add("  sw $s3, " + (8 + 32 - 24) + "($sp)");
        text.add("  sw $s4, " + (8 + 32 - 28) + "($sp)");
        text.add("  sw $s5, " + (8 + 32 - 32) + "($sp)");
        text.add("  sw $s6, " + (8 + 32 - 36) + "($sp)");
        text.add("  sw $s7, " + (8 + 32 - 40) + "($sp)");

        if (localesSize > 0) {
            int alignedSize = ((localesSize + 7) / 8) * 8;
            text.add("  addiu $sp, $sp, -" + alignedSize);
        }

        generateStatement(functionNode.getBody());

        if (localesSize > 0) {
            int alignedSize = ((localesSize + 7) / 8) * 8;
            text.add("  addiu $sp, $sp, " + alignedSize);
        }
        
        text.add("  # Restaurar registros callee-saved");
        text.add("  lw $s7, " + (8 + 32 - 40) + "($sp)");
        text.add("  lw $s6, " + (8 + 32 - 36) + "($sp)");
        text.add("  lw $s5, " + (8 + 32 - 32) + "($sp)");
        text.add("  lw $s4, " + (8 + 32 - 28) + "($sp)");
        text.add("  lw $s3, " + (8 + 32 - 24) + "($sp)");
        text.add("  lw $s2, " + (8 + 32 - 20) + "($sp)");
        text.add("  lw $s1, " + (8 + 32 - 16) + "($sp)");
        text.add("  lw $s0, " + (8 + 32 - 12) + "($sp)");
        
        text.add("  lw $fp, " + (8 + 32 - 8) + "($sp)");
        text.add("  lw $ra, " + (8 + 32 - 4) + "($sp)");
        text.add("  addiu $sp, $sp, " + (8 + 32));
        text.add("  jr $ra");
    }

    private void generateStatement(StatementNode statementNode) {
        if (statementNode == null) {
            return;
        }
        
        if (statementNode instanceof ExpressionStatementNode) {
            ExpressionStatementNode exprStmt = (ExpressionStatementNode) statementNode;
            if (exprStmt.getExpressionNode() != null) {
                String temp = generateExpression(exprStmt.getExpressionNode());
                freeTemp(temp);
            }
        } else if (statementNode instanceof ReturnNode) {
            generateReturnStatement((ReturnNode) statementNode);
        } else if (statementNode instanceof BlockNode) {
            generateBlockStatement((BlockNode) statementNode);
        } else if (statementNode instanceof IfNode) {
            generateIfStatement((IfNode) statementNode);
        } else if (statementNode instanceof WhileNode) {
            generateWhileStatement((WhileNode) statementNode);
        } else if (statementNode instanceof DoWhileNode) {
            generateDoWhileStatement((DoWhileNode) statementNode);
        } else if (statementNode instanceof ForNode) {
            generateForStatement((ForNode) statementNode);
        } else if (statementNode instanceof AssignmentNode) {
            generateAssignment((AssignmentNode) statementNode);
        }
    }

    private void generateReturnStatement(ReturnNode returnNode) {
        if (returnNode.getReturnValue() != null) {
            String temp = generateExpression(returnNode.getReturnValue());
            text.add("  move $v0, " + temp);
            freeTemp(temp);
        }
    }
    
    private void generateBlockStatement(BlockNode block) {
        for (StatementNode stmt : block.getStatements()) {
            generateStatement(stmt);
        }
    }

    private void generateIfStatement(IfNode ifStmt) {
        String condition = generateExpression(ifStmt.getCondition());
        String elseLabel = newLabel();
        String endLabel = newLabel();
        
        text.add("  beqz " + condition + ", " + elseLabel);
        freeTemp(condition);
        
        generateStatement(ifStmt.getThenBlock());
        text.add("  j " + endLabel);
        
        text.add(elseLabel + ":");
        if (ifStmt.getElseBlock() != null) {
            generateStatement(ifStmt.getElseBlock());
        }
        
        text.add(endLabel + ":");
    }
    
    private void generateWhileStatement(WhileNode whileStmt) {
        String startLabel = newLabel();
        String endLabel = newLabel();
        
        text.add(startLabel + ":");
        String condition = generateExpression(whileStmt.getCondition());
        text.add("  beqz " + condition + ", " + endLabel);
        freeTemp(condition);
        
        generateStatement(whileStmt.getBody());
        text.add("  j " + startLabel);
        text.add(endLabel + ":");
    }

    private void generateDoWhileStatement(DoWhileNode doWhileNode) {
        String start = newLabel();
        String condition = newLabel();

        text.add(start + ":");
        generateStatement(doWhileNode.getBody());

        text.add(condition + ":");
        String condString = generateExpression(doWhileNode.getCondition());
        text.add("  bnez " + condString + ", " + start);
        freeTemp(condString);
    }

    private void generateForStatement(ForNode forNode) {
        // Inicialización
        if (forNode.getInit() != null) {
            generateStatement(forNode.getInit());
        }

        String start = newLabel();
        String end = newLabel();

        text.add(start + ":");

        // Condición
        if (forNode.getCondition() != null) {
            String condicion = generateExpression(forNode.getCondition());
            text.add("  beqz " + condicion + ", " + end);
            freeTemp(condicion);
        }

        // Cuerpo
        generateStatement(forNode.getBody());

        // Incremento
        if (forNode.getIncrement() != null) {
            String increment = generateExpression(forNode.getIncrement());
            freeTemp(increment);
        }

        text.add("  j " + start);
        text.add(end + ":");
    }

    private void generateAssignment(AssignmentNode assign) {
        String value = generateExpression(assign.getValue());
        ExpressionNode target = assign.getTarget();
        
        if (target instanceof IdentifierNode) {
            String varName = ((IdentifierNode) target).getName();
            storeVariable(varName, value);
        }
        
        freeTemp(value);
    }

    private String generateExpression(ExpressionNode expr) {
        if (expr instanceof NumberNode) {
            return generateNumber((NumberNode) expr);
        } else if (expr instanceof IdentifierNode) {
            String name = ((IdentifierNode)expr).getName();
            VarDeclNode declNode = findVarDecl(name);
            if (declNode != null && declNode.isArray()) {
                return generateArrayAccess(expr);
            }
            return generateIdentifier((IdentifierNode) expr);
        } else if (expr instanceof BinaryOpNode) {
            BinaryOpNode binaryOpNode = (BinaryOpNode) expr;
            if (binaryOpNode.getOperator().equals("[")) {
                return generateArrayIndexing(binaryOpNode);
            }
            return generateBinaryOp((BinaryOpNode) expr);
        } else if (expr instanceof UnaryOpNode) {
            return generateUnaryOp((UnaryOpNode) expr);
        } else if (expr instanceof FunctionCallNode) {
            return generateFunctionCall((FunctionCallNode) expr);
        } else if (expr instanceof BooleanNode) {
            return generateBoolean((BooleanNode) expr);
        } else if (expr instanceof StringNode) {
            return generateString((StringNode) expr);
        } else if (expr instanceof CharNode) {
            return generateChar((CharNode) expr);
        }
        
        // Fallback
        String temp = newTemp();
        text.add("  li " + temp + ", 0");
        return temp;
    }

    private String generateNumber(NumberNode number) {
        String temp = newTemp();
        text.add("  li " + temp + ", " + number.getValue());
        return temp;
    }

    private String generateIdentifier(IdentifierNode identifier) {
        String temp = newTemp();
        String varName = identifier.getName();
        loadVariable(varName, temp);
        return temp;
    }
    
    private String generateBinaryOp(BinaryOpNode binOp) {
        String left = generateExpression(binOp.getLeft());
        String right = generateExpression(binOp.getRight());
        String result = newTemp();
        
        switch (binOp.getOperator()) {
            case "+":
                text.add("  add " + result + ", " + left + ", " + right);
                break;
            case "-":
                text.add("  sub " + result + ", " + left + ", " + right);
                break;
            case "*":
                text.add("  mul " + result + ", " + left + ", " + right);
                break;
            case "/":
                text.add("  div " + result + ", " + left + ", " + right);
                break;
            case "%":
                text.add("  div " + left + ", " + right);
                text.add("  mfhi " + result);
                break;
            case "==":
                text.add("  seq " + result + ", " + left + ", " + right);
                break;
            case "!=":
                text.add("  sne " + result + ", " + left + ", " + right);
                break;
            case "<":
                text.add("  slt " + result + ", " + left + ", " + right);
                break;
            case ">":
                text.add("  sgt " + result + ", " + left + ", " + right);
                break;
            case "<=":
                text.add("  sle " + result + ", " + left + ", " + right);
                break;
            case ">=":
                text.add("  sge " + result + ", " + left + ", " + right);
                break;
            case "&&":
                text.add("  and " + result + ", " + left + ", " + right);
                break;
            case "||":
                text.add("  or " + result + ", " + left + ", " + right);
                break;
            default:
                text.add("  add " + result + ", " + left + ", " + right);
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
                text.add("  neg " + result + ", " + operand);
                break;
            case "!":
                text.add("  seq " + result + ", " + operand + ", 0");
                break;
            case "&": // Dirección de
                if (unaryOp.getOperand() instanceof IdentifierNode) {
                    String varName = ((IdentifierNode) unaryOp.getOperand()).getName();
                    if (globalVars.containsKey(varName)) {
                        text.add("  la " + result + ", _" + varName);
                    } else if (localVars.containsKey(varName)) {
                        text.add("  addiu " + result + ", $fp, " + localVars.get(varName));
                    }
                }
                break;
            case "*":
                text.add("  lw " + result + ", 0(" + operand + ")");
                break;
            default:
                text.add("  move " + result + ", " + operand);
        }
        
        freeTemp(operand);
        return result;
    }
    
    private String generateFunctionCall(FunctionCallNode call) {
        String funcName = call.getFunctionName();
        if (funcName.equals("read_str")) {
            return generateRuntimeReadStr(call);
        } else if (funcName.equals("print_int")) {
            return generateRuntimePrintInt(call);
        } else if (funcName.equals("print_str")) {
            return generateRuntimePrintStr(call);
        } else if (funcName.equals("print_char")) {
            return generateRuntimePrintChar(call);
        } else if (funcName.equals("print_bool")) {
            return generateRuntimePrintBool(call);
        } else if (funcName.equals("println")) {
            return generateRuntimePrintLn(call);
        } else if (funcName.equals("read_int")) {
            return generateRuntimeReadInt();
        } else if (funcName.equals("read_char")) {
            return generateRuntimeReadChar();
        }
        
        return generateNormalFunctionCall(call);
    }

    private String generateRuntimeReadStr(FunctionCallNode call) {
        if (call.getArguments().size() >= 2) {
            String bufArg = generateExpression(call.getArguments().get(0));
            String maxlenArg = generateExpression(call.getArguments().get(1));
            
            text.add("  move $a0, " + bufArg);
            text.add("  move $a1, " + maxlenArg);
            text.add("  jal read_str");
            
            freeTemp(bufArg);
            freeTemp(maxlenArg);
        }
        return newTemp();
    }

    private String generateRuntimePrintInt(FunctionCallNode call) {
        if (!call.getArguments().isEmpty()) {
            String arg = generateExpression(call.getArguments().get(0));
            text.add("  move $a0, " + arg);
            text.add("  li $v0, 1");
            text.add("  syscall");
            freeTemp(arg);
        }
        return newTemp();
    }
    
    private String generateRuntimePrintStr(FunctionCallNode call) {
        if (!call.getArguments().isEmpty()) {
            String arg = generateExpression(call.getArguments().get(0));
            text.add("  move $a0, " + arg);
            text.add("  li $v0, 4");
            text.add("  syscall");
            freeTemp(arg);
        }
        return newTemp();
    }
    
    private String generateRuntimePrintChar(FunctionCallNode call) {
        if (!call.getArguments().isEmpty()) {
            String arg = generateExpression(call.getArguments().get(0));
            text.add("  move $a0, " + arg);
            text.add("  li $v0, 11");
            text.add("  syscall");
            freeTemp(arg);
        }
        return newTemp();
    }
    
    private String generateRuntimePrintBool(FunctionCallNode call) {
        if (!call.getArguments().isEmpty()) {
            String arg = generateExpression(call.getArguments().get(0));
            String trueLabel = newLabel();
            String endLabel = newLabel();
            
            text.add("  bnez " + arg + ", " + trueLabel);
            // Imprimir false
            text.add("  la $a0, false_str");
            text.add("  li $v0, 4");
            text.add("  syscall");
            text.add("  j " + endLabel);
            // Imprimir true
            text.add(trueLabel + ":");
            text.add("  la $a0, true_str");
            text.add("  li $v0, 4");
            text.add("  syscall");
            text.add(endLabel + ":");
            
            freeTemp(arg);
        }
        return newTemp();
    }
    
    private String generateRuntimePrintLn(FunctionCallNode call) {
        text.add("  la $a0, newline");
        text.add("  li $v0, 4");
        text.add("  syscall");
        return newTemp();
    }
    
    private String generateRuntimeReadInt() {
        text.add("  li $v0, 5");
        text.add("  syscall");
        String result = newTemp();
        text.add("  move " + result + ", $v0");
        return result;
    }
    
    private String generateRuntimeReadChar() {
        text.add("  li $v0, 12");
        text.add("  syscall");
        String result = newTemp();
        text.add("  move " + result + ", $v0");
        return result;
    }
    
    private String generateNormalFunctionCall(FunctionCallNode call) {
        String funcName = call.getFunctionName();
        
        // Pasar argumentos (primeros 4 en $a0-$a3)
        List<ExpressionNode> args = call.getArguments();
        for (int i = 0; i < Math.min(args.size(), 4); i++) {
            String argTemp = generateExpression(args.get(i));
            text.add("  move $a" + i + ", " + argTemp);
            freeTemp(argTemp);
        }
        
        // Llamar función
        text.add("  jal " + funcName);
        
        // Resultado en $v0, mover a temporal
        String result = newTemp();
        text.add("  move " + result + ", $v0");
        return result;
    }

    private String generateBoolean(BooleanNode booleanNode) {
        String temp = newTemp();
        int value = booleanNode.getValue() ? 1 : 0;
        text.add("  li " + temp + ", " + value);
        return temp;
    }
    
    private String generateString(StringNode stringNode) {
        String value = stringNode.getValue();
        
        if (!stringLiterals.containsKey(value)) {
            String label = "str_" + (stringLiteralsCount++);
            data.add(label + ": .asciiz \"" + escapeString(value) + "\"");
            stringLiterals.put(value, label);
        }
        
        String label = stringLiterals.get(value);
        String temp = newTemp();
        text.add("  la " + temp + ", " + label);
        return temp;
    }
    
    private String generateChar(CharNode charNode) {
        String temp = newTemp();
        char value = charNode.getValue();
        text.add("  li " + temp + ", " + (int) value);
        return temp;
    }

    private String newTemp() {
        if (!availableTemps.isEmpty()) {
            return availableTemps.pop();
        }
        if (tempCount < 10) {
            return "$t" + (tempCount++);
        } else {
            return allocateStackTemp();
        }
    }
    
    private void freeTemp(String temp) {
        if (temp.startsWith("$t") && tempCount > 0) {
            availableTemps.push(temp);
        }
    }
    
    private String allocateStackTemp() {
        currentLocal -= 4;
        return currentLocal + "($fp)";
    }
    
    private String newLabel() {
        return "L" + (labelCount++);
    }
    
    private int calculateArraySize(VarDeclNode varDecl) {
        if (varDecl.isArray()) {
            return varDecl.getArraySize();
        }
        return 1;
    }
    
    private int calculateLocalVarsSize(FunctionNode function) {
        localVars.clear();
        currentLocal = -8;
        
        if (function.getParameters() != null) {
            for (VarDeclNode param : function.getParameters()) {
                allocateLocalVar(param.getName(), param.isArray());
            }
        }

        currentLocal = alignTo8Bytes(currentLocal);

        currentLocal -=32;
        currentLocal = alignTo8Bytes(currentLocal);
        
        return Math.abs(currentLocal);
    }
    
    private void allocateLocalVar(String name, boolean isArray) {
        if (isArray) {
            // Para arrays, reservar espacio para todos los elementos
            VarDeclNode varDecl = findVarDecl(name);
            if (varDecl != null) {
                int arraySize = calculateArraySize(varDecl);
                currentLocal -= arraySize * 4;
            } else {
                currentLocal -= 16; // Tamaño por defecto
            }
        } else {
            // Para variables simples, 4 bytes
            currentLocal -= 4;
        }
        currentLocal = (currentLocal/4)*4;
        localVars.put(name, currentLocal);
    }
    
    private VarDeclNode findVarDecl(String varName) {
        return null;
    }
    
    private void loadVariable(String varName, String destReg) {
        if (globalVars.containsKey(varName)) {
            text.add("  la $t9, _" + varName);
            text.add("  lw " + destReg + ", 0($t9)");
        } else if (localVars.containsKey(varName)) {
            int offset = localVars.get(varName);
            text.add("  lw " + destReg + ", " + offset + "($fp)");
        } else {
            throw new RuntimeException("Variable no encontrada: " + varName);
        }
    }
    
    private void storeVariable(String varName, String srcReg) {
        if (globalVars.containsKey(varName)) {
            text.add("  la $t9, _" + varName);
            text.add("  sw " + srcReg + ", 0($t9)");
        } else if (localVars.containsKey(varName)) {
            int offset = localVars.get(varName);
            text.add("  lw " + srcReg + ", " + offset + "($fp)");
        } else {
            throw new RuntimeException("Variable no encontrada: " + varName);
        }
    }
    
    private String escapeString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\t", "\\t");
    }

    private String generateArrayAccess(ExpressionNode expr) {
        if (expr instanceof IdentifierNode) {
            // Acceso simple a variable
            return generateIdentifier((IdentifierNode) expr);
        } else if (expr instanceof BinaryOpNode) {
            BinaryOpNode binOp = (BinaryOpNode) expr;
            if (binOp.getOperator().equals("[")) {
                return generateArrayIndexing(binOp);
            }
        }
        throw new RuntimeException("Expresión de acceso no soportada");
    }

    private String generateArrayIndexing(BinaryOpNode arrayAccess) {
        
        ExpressionNode arrayNameNode = arrayAccess.getLeft();
        ExpressionNode indexNode = arrayAccess.getRight();
        
        if (!(arrayNameNode instanceof IdentifierNode)) {
            throw new RuntimeException("Se esperaba identificador de array");
        }
        
        String arrayName = ((IdentifierNode) arrayNameNode).getName();
        String indexTemp = generateExpression(indexNode);
        String resultTemp = newTemp();
        
        VarDeclNode arrayDecl = findVarDecl(arrayName);
        if (arrayDecl != null && arrayDecl.isArray()) {
            if (arrayDecl.getType().contains("[][]")) {
                text.add("  # Cálculo de offset para arreglo 2D " + arrayName);
                
                //int rows = arrayDecl.getArraySize();
                int cols = 5;
                
                if (indexNode instanceof BinaryOpNode) {
                    BinaryOpNode nestedIndex = (BinaryOpNode) indexNode;
                    if (nestedIndex.getOperator().equals("[")) {
                        String iTemp = generateExpression(nestedIndex.getLeft());
                        String jTemp = generateExpression(nestedIndex.getRight());
                        
                        text.add("  li $t8, " + cols);
                        text.add("  mul $t9, " + iTemp + ", $t8");
                        text.add("  add $t9, $t9, " + jTemp);
                        text.add("  li $t8, 4");
                        text.add("  mul $t9, $t9, $t8");
                        
                        if (globalVars.containsKey(arrayName)) {
                            text.add("  la $t8, _" + arrayName);
                        } else if (localVars.containsKey(arrayName)) {
                            text.add("  addiu $t8, $fp, " + localVars.get(arrayName));
                        }
                        
                        text.add("  add $t8, $t8, $t9");
                        text.add("  lw " + resultTemp + ", 0($t8)");
                        
                        freeTemp(iTemp);
                        freeTemp(jTemp);
                        freeTemp(indexTemp);
                        return resultTemp;
                    }
                }
            }
            
            text.add("  # Cálculo de offset para arreglo 1D " + arrayName);
            text.add("  li $t8, 4");
            text.add("  mul $t9, " + indexTemp + ", $t8");
            
            if (globalVars.containsKey(arrayName)) {
                text.add("  la $t8, _" + arrayName);
            } else if (localVars.containsKey(arrayName)) {
                text.add("  addiu $t8, $fp, " + localVars.get(arrayName));
            }
            
            text.add("  add $t8, $t8, $t9");
            text.add("  lw " + resultTemp + ", 0($t8)");
        } else {
            throw new RuntimeException(arrayName + " no es un arreglo");
        }
        
        freeTemp(indexTemp);
        return resultTemp;
    }

    private int alignTo8Bytes(int size) {
        // Alinear a múltiplo de 8
        return ((size + 7) / 8) * 8;
    }
}