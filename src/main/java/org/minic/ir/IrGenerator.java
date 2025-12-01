package org.minic.ir;

import org.minic.ast.*;
import java.util.*;

public class IrGenerator {
    private List<String> irCode;
    private int tempCount;
    private int labelCount;
    
    // Tabla de símbolos para temporales
    private Map<String, String> tempMap;
    
    public IrGenerator() {
        this.irCode = new ArrayList<>();
        this.tempCount = 0;
        this.labelCount = 0;
        this.tempMap = new HashMap<>();
    }

    public List<String> generate(AstNode ast) {
        irCode.clear();
        tempCount = 0;
        labelCount = 0;
        
        // Cabecera del código IR
        irCode.add("; ============ CÓDIGO IR GENERADO ============");
        irCode.add("; Generado por MiniC Compiler");
        irCode.add("");
        
        if (ast instanceof ProgramNode) {
            generateProgram((ProgramNode) ast);
        }
        
        return irCode;
    }

    private void generateProgram(ProgramNode program) {
        irCode.add("; === DECLARACIONES GLOBALES ===");
        
        // Procesar declaraciones globales
        for (AstNode node : program.getChildren()) {
            if (node instanceof VarDeclNode) {
                generateGlobalVar((VarDeclNode) node);
            }
        }
        
        irCode.add("");
        irCode.add("; === FUNCIONES ===");
        
        // Procesar funciones
        for (AstNode node : program.getChildren()) {
            if (node instanceof FunctionNode) {
                generateFunction((FunctionNode) node);
            }
        }
    }

    private void generateGlobalVar(VarDeclNode varDecl) {
        String name = varDecl.getName();
        String type = varDecl.getType();
        
        if (varDecl.isArray()) {
            int size = varDecl.getArraySize();
            irCode.add("GLOBAL " + name + ": array[" + type + ", " + size + "]");
        } else {
            irCode.add("GLOBAL " + name + ": " + type);
        }
    }

    private void generateFunction(FunctionNode function) {
        String funcName = function.getName();
        String returnType = function.getReturnType();
        
        irCode.add("");
        irCode.add("FUNCTION " + funcName + ": " + returnType);
        
        // Parámetros
        if (function.getParameters() != null && !function.getParameters().isEmpty()) {
            StringBuilder params = new StringBuilder("PARAMS: ");
            for (VarDeclNode param : function.getParameters()) {
                params.append(param.getName()).append(":").append(param.getType()).append(" ");
            }
            irCode.add(params.toString());
        }
        
        // Cuerpo de la función
        if (function.getBody() != null) {
            generateBlock(function.getBody());
        }
        
        irCode.add("ENDFUNC " + funcName);
    }

    private void generateBlock(BlockNode block) {
        for (StatementNode stmt : block.getStatements()) {
            generateStatement(stmt);
        }
    }

    private void generateStatement(StatementNode stmt) {
        if (stmt instanceof ExpressionStatementNode) {
            generateExpressionStatement((ExpressionStatementNode) stmt);
        } else if (stmt instanceof ReturnNode) {
            generateReturnStatement((ReturnNode) stmt);
        } else if (stmt instanceof BlockNode) {
            generateBlock((BlockNode) stmt);
        } else if (stmt instanceof IfNode) {
            generateIfStatement((IfNode) stmt);
        } else if (stmt instanceof WhileNode) {
            generateWhileStatement((WhileNode) stmt);
        } else if (stmt instanceof DoWhileNode) {
            generateDoWhileStatement((DoWhileNode) stmt);
        } else if (stmt instanceof ForNode) {
            generateForStatement((ForNode) stmt);
        } else if (stmt instanceof AssignmentNode) {
            generateAssignment((AssignmentNode) stmt);
        }
    }

    private void generateExpressionStatement(ExpressionStatementNode exprStmt) {
        if (exprStmt.getExpressionNode() != null) {
            String temp = generateExpression(exprStmt.getExpressionNode());
            // No hacemos nada con el resultado de expresiones sueltas
            freeTemp(temp);
        }
    }

    private void generateReturnStatement(ReturnNode returnNode) {
        if (returnNode.getReturnValue() != null) {
            String temp = generateExpression(returnNode.getReturnValue());
            irCode.add("RETURN " + temp);
            freeTemp(temp);
        } else {
            irCode.add("RETURN");
        }
    }

    private void generateIfStatement(IfNode ifStmt) {
        String condition = generateExpression(ifStmt.getCondition());
        String elseLabel = newLabel();
        String endLabel = newLabel();
        
        irCode.add("IF " + condition + " == 0 GOTO " + elseLabel);
        freeTemp(condition);
        
        generateStatement(ifStmt.getThenBlock());
        irCode.add("GOTO " + endLabel);
        
        irCode.add(elseLabel + ":");
        if (ifStmt.getElseBlock() != null) {
            generateStatement(ifStmt.getElseBlock());
        }
        
        irCode.add(endLabel + ":");
    }

    private void generateWhileStatement(WhileNode whileStmt) {
        String startLabel = newLabel();
        String endLabel = newLabel();
        
        irCode.add(startLabel + ":");
        String condition = generateExpression(whileStmt.getCondition());
        irCode.add("IF " + condition + " == 0 GOTO " + endLabel);
        freeTemp(condition);
        
        generateStatement(whileStmt.getBody());
        irCode.add("GOTO " + startLabel);
        irCode.add(endLabel + ":");
    }

    private void generateDoWhileStatement(DoWhileNode doWhile) {
        String startLabel = newLabel();
        String conditionLabel = newLabel();
        
        irCode.add(startLabel + ":");
        generateStatement(doWhile.getBody());
        
        irCode.add(conditionLabel + ":");
        String condition = generateExpression(doWhile.getCondition());
        irCode.add("IF " + condition + " != 0 GOTO " + startLabel);
        freeTemp(condition);
    }

    private void generateForStatement(ForNode forStmt) {
        // Inicialización
        if (forStmt.getInit() != null) {
            generateStatement(forStmt.getInit());
        }

        String startLabel = newLabel();
        String endLabel = newLabel();

        irCode.add(startLabel + ":");

        // Condición
        if (forStmt.getCondition() != null) {
            String condition = generateExpression(forStmt.getCondition());
            irCode.add("IF " + condition + " == 0 GOTO " + endLabel);
            freeTemp(condition);
        }

        // Cuerpo
        generateStatement(forStmt.getBody());

        // Incremento
        if (forStmt.getIncrement() != null) {
            String increment = generateExpression(forStmt.getIncrement());
            freeTemp(increment);
        }

        irCode.add("GOTO " + startLabel);
        irCode.add(endLabel + ":");
    }

    private void generateAssignment(AssignmentNode assign) {
        String value = generateExpression(assign.getValue());
        ExpressionNode target = assign.getTarget();
        
        if (target instanceof IdentifierNode) {
            String varName = ((IdentifierNode) target).getName();
            irCode.add(varName + " = " + value);
        } else if (target instanceof VariableNode) {
            String varName = ((VariableNode) target).getName();
            irCode.add(varName + " = " + value);
        }
        
        freeTemp(value);
    }

    // ========== GENERACIÓN DE EXPRESIONES ==========

    private String generateExpression(ExpressionNode expr) {
        if (expr instanceof NumberNode) {
            return generateNumber((NumberNode) expr);
        } else if (expr instanceof IdentifierNode) {
            return generateIdentifier((IdentifierNode) expr);
        } else if (expr instanceof VariableNode) {
            return generateVariable((VariableNode) expr);
        } else if (expr instanceof BinaryOpNode) {
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
        irCode.add(temp + " = 0");
        return temp;
    }

    private String generateNumber(NumberNode number) {
        String temp = newTemp();
        irCode.add(temp + " = " + number.getValue());
        return temp;
    }

    private String generateIdentifier(IdentifierNode identifier) {
        String temp = newTemp();
        irCode.add(temp + " = " + identifier.getName());
        return temp;
    }

    private String generateVariable(VariableNode variable) {
        String temp = newTemp();
        irCode.add(temp + " = " + variable.getName());
        return temp;
    }

    private String generateBinaryOp(BinaryOpNode binOp) {
        String left = generateExpression(binOp.getLeft());
        String right = generateExpression(binOp.getRight());
        String result = newTemp();
        
        String operator = binOp.getOperator();
        String irOperator = convertOperator(operator);
        
        irCode.add(result + " = " + left + " " + irOperator + " " + right);
        
        freeTemp(left);
        freeTemp(right);
        return result;
    }

    private String generateUnaryOp(UnaryOpNode unaryOp) {
        String operand = generateExpression(unaryOp.getOperand());
        String result = newTemp();
        String operator = unaryOp.getOperator();
        
        switch (operator) {
            case "-":
                irCode.add(result + " = -" + operand);
                break;
            case "!":
                irCode.add(result + " = !" + operand);
                break;
            case "&":
                irCode.add(result + " = &" + operand);
                break;
            case "*":
                irCode.add(result + " = *" + operand);
                break;
            default:
                irCode.add(result + " = " + operator + operand);
        }
        
        freeTemp(operand);
        return result;
    }

    private String generateFunctionCall(FunctionCallNode call) {
        String funcName = call.getFunctionName();
        String result = newTemp();
        
        // Construir lista de argumentos
        StringBuilder argsBuilder = new StringBuilder();
        if (call.getArguments() != null && !call.getArguments().isEmpty()) {
            for (ExpressionNode arg : call.getArguments()) {
                String argTemp = generateExpression(arg);
                argsBuilder.append(argTemp).append(" ");
                freeTemp(argTemp);
            }
        }
        
        irCode.add(result + " = CALL " + funcName + " " + argsBuilder.toString().trim());
        return result;
    }

    private String generateBoolean(BooleanNode booleanNode) {
        String temp = newTemp();
        int value = booleanNode.getValue() ? 1 : 0;
        irCode.add(temp + " = " + value);
        return temp;
    }

    private String generateString(StringNode stringNode) {
        String temp = newTemp();
        String value = stringNode.getValue();
        // Escapar comillas para el IR
        String escapedValue = value.replace("\"", "\\\"");
        irCode.add(temp + " = \"" + escapedValue + "\"");
        return temp;
    }

    private String generateChar(CharNode charNode) {
        String temp = newTemp();
        char value = charNode.getValue();
        irCode.add(temp + " = '" + value + "'");
        return temp;
    }

    // ========== MÉTODOS AUXILIARES ==========

    private String newTemp() {
        String temp = "t" + tempCount++;
        tempMap.put(temp, "int"); // Asumimos tipo int por defecto
        return temp;
    }

    private void freeTemp(String temp) {
        // En esta implementación simple, no liberamos temporales
        // En una implementación más avanzada, podrías reutilizarlos
    }

    private String newLabel() {
        return "L" + labelCount++;
    }

    private String convertOperator(String operator) {
        switch (operator) {
            case "&&": return "AND";
            case "||": return "OR";
            case "==": return "EQ";
            case "!=": return "NE";
            case "<": return "LT";
            case ">": return "GT";
            case "<=": return "LE";
            case ">=": return "GE";
            case "+": return "ADD";
            case "-": return "SUB";
            case "*": return "MUL";
            case "/": return "DIV";
            case "%": return "MOD";
            default: return operator;
        }
    }
}