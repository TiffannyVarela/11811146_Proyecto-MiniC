package org.minic.ir;

import org.minic.ast.*;
import java.util.*;

/*
  IrGenerator

  Generador de Código Intermedio (IR) del compilador MiniC.
  Esta clase traduce el AST a una representación intermedia de tres direcciones (Three-Address Code).
 
  El IR generado:
   - Usa temporales (t0, t1, ...)
   - Usa etiquetas (L0, L1, ...)
   - Simplifica expresiones y control de flujo
 
  Sirve como puente entre el AST y el backend (MIPS).
 */

public class IrGenerator {
    // Lista para almacenar las instrucciones IR generadas
    private List<String> irCode;
    // Contadores para temporales y etiquetas
    private int tempCount;
    private int labelCount;
    // Mapa para rastrear tipos de temporales
    private Map<String, String> tempMap;

    public IrGenerator() {
        this.irCode = new ArrayList<>();
        this.tempCount = 0;
        this.labelCount = 0;
        this.tempMap = new HashMap<>();
    }

    // Método principal para generar código IR a partir del AST
    public List<String> generate(AstNode ast) {
        // Reiniciar estado
        irCode.clear();
        tempCount = 0;
        labelCount = 0;

        irCode.add("; ============ CÓDIGO IR GENERADO ============");
        irCode.add("");
        // El IR solo se genera si el AST es un programa válido
        if (ast instanceof ProgramNode) {
            generateProgram((ProgramNode) ast);
        }

        return irCode;
    }

    // Genera código IR para un nodo de programa
    private void generateProgram(ProgramNode program) {
        irCode.add("; === DECLARACIONES GLOBALES ===");
        // Generar código para variables globales
        for (AstNode node : program.getChildren()) {
            if (node instanceof VarDeclNode) {
                generateGlobalVar((VarDeclNode) node);
            }
        }

        irCode.add("");
        irCode.add("; === FUNCIONES ===");
        // Generar código para funciones
        for (AstNode node : program.getChildren()) {
            if (node instanceof FunctionNode) {
                generateFunction((FunctionNode) node);
            }
        }
    }

    // Genera código IR para una variable global
    private void generateGlobalVar(VarDeclNode varDecl) {
        String name = varDecl.getName();
        String type = varDecl.getType();
        // Variables globales pueden ser arrays o simples
        if (varDecl.isArray()) {
            int size = varDecl.getArraySize();
            irCode.add("GLOBAL " + name + ": array[" + type + ", " + size + "]");
        } else {
            irCode.add("GLOBAL " + name + ": " + type);
        }
    }

    // Genera código IR para una función
    private void generateFunction(FunctionNode function) {
        String funcName = function.getName();
        String returnType = function.getReturnType();

        irCode.add("");
        irCode.add("FUNCTION " + funcName + ": " + returnType);
        // Parámetros de la función
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
        // Manejo de retorno para funciones void
        if ("void".equals(function.getReturnType())) {
            irCode.add("RETURN");
        }

        irCode.add("ENDFUNC " + funcName);
    }

    private void generateBlock(BlockNode block) {
        for (int i = 0; i < block.getStatements().size(); i++) {
            StatementNode stmt = block.getStatements().get(i);
            generateStatement(stmt);
        }
    }

    // Genera código IR para una sentencia
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
        } else if (stmt instanceof VarDeclStatementNode) {
            generateVarDeclStatement((VarDeclStatementNode) stmt);
        } else {
        }
    }

    // Genera código IR para una declaración de variable
    private void generateVarDeclStatement(VarDeclStatementNode varDeclStmt) {
        VarDeclNode varDecl = varDeclStmt.getVarDeclNode();
        String varName = varDecl.getName();
        String type = varDecl.getType();
        // Declarar variable local
        if (varDecl.isArray()) {
            irCode.add("LOCAL " + varName + ": array[" + type + ", " + varDecl.getArraySize() + "]");
        } else {
            irCode.add("LOCAL " + varName + ": " + type);
        }
        // Inicializar si hay un valor inicial
        if (varDecl.hasInitialNode()) {
            String initValue = generateExpression(varDecl.getInitialNode());
            irCode.add(varName + " = " + initValue);
            freeTemp(initValue);
        } else {
        }
    }

    // Genera código IR para una sentencia de expresión
    private void generateExpressionStatement(ExpressionStatementNode exprStmt) {
        if (exprStmt.getExpressionNode() != null) {
            String temp = generateExpression(exprStmt.getExpressionNode());
            freeTemp(temp);
        }
    }

    // Genera código IR para una sentencia de retorno
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
        if (forStmt.getInit() != null) {
            generateStatement(forStmt.getInit());
        }

        String startLabel = newLabel();
        String endLabel = newLabel();

        irCode.add(startLabel + ":");

        if (forStmt.getCondition() != null) {
            String condition = generateExpression(forStmt.getCondition());
            irCode.add("IF " + condition + " == 0 GOTO " + endLabel);
            freeTemp(condition);
        }

        generateStatement(forStmt.getBody());
        if (forStmt.getIncrement() != null) {
            String increment = generateExpression(forStmt.getIncrement());
            freeTemp(increment);
        }

        irCode.add("GOTO " + startLabel);
        irCode.add(endLabel + ":");
    }

    // Genera código IR para una asignación
    private void generateAssignment(AssignmentNode assign) {
        String value = generateExpression(assign.getValue());
        ExpressionNode target = assign.getTarget();

        if (target instanceof IdentifierNode) {
            irCode.add(((IdentifierNode) target).getName() + " = " + value);

        } else if (target instanceof VariableNode) {
            irCode.add(((VariableNode) target).getName() + " = " + value);

        } else if (target instanceof ArrayAccessNode) {
            ArrayAccessNode arrayAccess = (ArrayAccessNode) target;

            ExpressionNode arrayExpr = arrayAccess.getArray();
            String arrayName;

            if (arrayExpr instanceof IdentifierNode) {
                arrayName = ((IdentifierNode) arrayExpr).getName();
            } else if (arrayExpr instanceof VariableNode) {
                arrayName = ((VariableNode) arrayExpr).getName();
            } else {
                arrayName = generateExpression(arrayExpr);
            }

            ExpressionNode indexExpr = arrayAccess.getIndices().get(0);
            String indexTemp = generateExpression(indexExpr);

            irCode.add(arrayName + "[" + indexTemp + "] = " + value);

            freeTemp(indexTemp);
        }

        freeTemp(value);
    }

    // Genera código IR para una expresión
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
        } else if (expr instanceof ArrayAccessNode) {
            return generateArrayAccess((ArrayAccessNode) expr);
        }

        String temp = newTemp();
        irCode.add(temp + " = 0");
        return temp;
    }

    // Genera código IR para un número
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

        StringBuilder args = new StringBuilder();
        if (call.getArguments() != null) {
            for (ExpressionNode arg : call.getArguments()) {
                String temp = generateExpression(arg);
                args.append(temp).append(" ");
                freeTemp(temp);
            }
        }
        String result = newTemp();
        irCode.add(result + " = CALL " + funcName + " " + args.toString().trim());
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

    // Genera un nuevo temporal
    private String newTemp() {
        String temp = "t" + tempCount++;
        tempMap.put(temp, "int");
        return temp;
    }

    private void freeTemp(String temp) {
        tempMap.remove(temp);
    }

    private String newLabel() {
        return "L" + labelCount++;
    }

    // Convierte operadores a su representación en IR
    private String convertOperator(String operator) {
        switch (operator) {
            case "&&":
                return "AND";
            case "||":
                return "OR";
            case "==":
                return "EQ";
            case "!=":
                return "NE";
            case "<":
                return "LT";
            case ">":
                return "GT";
            case "<=":
                return "LE";
            case ">=":
                return "GE";
            case "+":
                return "+";
            case "-":
                return "-";
            case "*":
                return "*";
            case "/":
                return "/";
            case "%":
                return "%";
            default:
                return operator;
        }
    }

    // Genera código IR para el acceso a un array
    private String generateArrayAccess(ArrayAccessNode arrayAccess) {
        ExpressionNode arrayExpr = arrayAccess.getArray();
        String arrayName;

        if (arrayExpr instanceof IdentifierNode) {
            arrayName = ((IdentifierNode) arrayExpr).getName();
        } else if (arrayExpr instanceof VariableNode) {
            arrayName = ((VariableNode) arrayExpr).getName();
        } else {
            String tempArray = generateExpression(arrayExpr);
            arrayName = tempArray;
        }
        ExpressionNode indexExpr = arrayAccess.getIndices().get(0);
        String indexTemp = generateExpression(indexExpr);

        String result = newTemp();
        irCode.add(result + " = " + arrayName + "[" + indexTemp + "]");

        freeTemp(indexTemp);
        return result;
    }
}
