package org.minic.ast;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.minic.MiniCBaseListener;
import org.minic.MiniCParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/*
 AstBuilder: Recorre el Parse Tree generado por ANTLR y construye el Árbol de Sintaxis Abstracta (AST) del lenguaje MiniC.
 Utiliza el patrón Listener y múltiples pilas para manejar el contexto de nodos, sentencias y expresiones.
 */

public class AstBuilder extends MiniCBaseListener {

    //Nodo raiz
    private AstNode root;
    //Pila de nodos AST activos
    private Stack<AstNode> nodeStack = new Stack<>();
    //Pila de listas de sentencias para manejar bloques y funciones
    private Stack<List<StatementNode>> statementListStack = new Stack<>();
    //Tipo actual para declaraciones de variables
    private String currentType = null;
    //Pila de nodos de expresiones para construir expresiones complejas
    private Stack<ExpressionNode> expressionStack = new Stack<>();

    //Método principal para construir el AST a partir del Parse Tree
    public AstNode build(org.antlr.v4.runtime.tree.ParseTree tree) {
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, tree);
        return root;
    }

    //Inicializa el nodo raíz del programa y su lista de declaraciones globales
    @Override
    public void enterProgram(MiniCParser.ProgramContext ctx) {
        ProgramNode programNode = new ProgramNode();
        nodeStack.push(programNode);
        statementListStack.push(new ArrayList<>());
    }

    //Finaliza la construcción del nodo raíz del programa
    @Override
    public void exitProgram(MiniCParser.ProgramContext ctx) {
        ProgramNode programNode = (ProgramNode) nodeStack.pop();
        List<StatementNode> globalStatements = statementListStack.pop();
        if (!globalStatements.isEmpty()) {
        }
        root = programNode;
    }

    //Maneja declaraciones de funciones y variables
    @Override
    public void enterDeclaration(MiniCParser.DeclarationContext ctx) {
        // Diferencia entre declaración de función y variable
        if (ctx.functionDeclaration() != null) {
            handleFunctionDeclaration(ctx.functionDeclaration());
            return;
        }
        handleVariableDeclaration(ctx);
    }

    //Maneja la declaración de funciones, creando nodos de función y agregándolos al programa
    private void handleFunctionDeclaration(MiniCParser.FunctionDeclarationContext funcDeclCtx) {
        String returnType = funcDeclCtx.typeSpecifier().getText();
        String funcName = funcDeclCtx.Identifier().getText();
        // Lista de parámetros de la función
        List<VarDeclNode> parameters = null;
        if (funcDeclCtx.parameterList() != null) {
            parameters = new ArrayList<>();
            for (MiniCParser.ParameterContext paramCtx : funcDeclCtx.parameterList().parameter()) {
                String paramType = paramCtx.typeSpecifier().getText();
                String paramName = paramCtx.Identifier().getText();
                parameters.add(new VarDeclNode(paramType, paramName, false, 0, 0, null));
            }
        }
        // Crear el nodo de función sin cuerpo
        FunctionNode funcNode = new FunctionNode(returnType, funcName, parameters, null);
        AstNode currentNode = nodeStack.peek();
        if (currentNode instanceof ProgramNode) {
            ProgramNode programNode = (ProgramNode) currentNode;
            programNode.addDeclarationNode(funcNode);
        }
    }

    //Maneja la declaración de variables, creando nodos de declaración de variables y agregándolos al contexto actual
    private void handleVariableDeclaration(MiniCParser.DeclarationContext ctx) {
        currentType = ctx.typeSpecifier().getText();
        for (MiniCParser.InitDeclaratorContext initDeclCtx : ctx.initDeclaratorList().initDeclarator()) {
            String varName = initDeclCtx.Identifier().getText();
            boolean isArray = false;
            int arraySize = 0;
            int secondDimension = 0;
            // Manejo de arreglos
            if (initDeclCtx.LBRACK() != null && initDeclCtx.LBRACK().size() > 0) {
                isArray = true;
                @SuppressWarnings("unused")
                int dimensionCount = initDeclCtx.LBRACK().size();
                List<org.antlr.v4.runtime.tree.TerminalNode> constants = initDeclCtx.IntegerConstant();
                if (constants != null && constants.size() > 0) {
                    if (constants.size() >= 1) {
                        arraySize = Integer.parseInt(constants.get(0).getText());
                    }
                    if (constants.size() >= 2) {
                        secondDimension = Integer.parseInt(constants.get(1).getText());
                    }
                }
            }
            // Nodo de expresión inicial si hay una asignación
            ExpressionNode initialNode = null;
            if (initDeclCtx.ASSIGN() != null && initDeclCtx.expression() != null) {
                if (!expressionStack.isEmpty()) {
                    initialNode = expressionStack.pop();
                }
            }
            //Ajuste e tipo si es arreglo
            String actualType = currentType;
            if (isArray) {
                if (secondDimension > 0) {
                    actualType = currentType + "[][]";
                } else {
                    actualType = currentType + "[]";
                }
            }
            VarDeclNode varDeclNode = new VarDeclNode(actualType, varName, isArray, arraySize, secondDimension,
                    initialNode);
            VarDeclStatementNode declStmt = new VarDeclStatementNode(varDeclNode);
            AstNode currentNode = nodeStack.peek();
            //Variables globales o locales
            if (currentNode instanceof ProgramNode) {
                ProgramNode programNode = (ProgramNode) currentNode;
                programNode.addDeclarationNode(varDeclNode);
            } else {
                statementListStack.peek().add(declStmt);
            }
        }
        currentType = null;
    }

    //Maneja la entrada a la definición de funciones, creando nodos de función y preparando el contexto para el cuerpo
    @Override
    public void enterFunctionDefinition(MiniCParser.FunctionDefinitionContext ctx) {
        String returnType = ctx.typeSpecifier().getText();
        String functionName = ctx.Identifier().getText();
        List<VarDeclNode> parameters = new ArrayList<>();
        if (ctx.parameterList() != null) {
            for (MiniCParser.ParameterContext paramCtx : ctx.parameterList().parameter()) {
                String paramType = paramCtx.typeSpecifier().getText();
                String paramName = paramCtx.Identifier().getText();
                parameters.add(new VarDeclNode(paramType, paramName, false, 0, 0, null));
            }
        }
        FunctionNode functionNode = new FunctionNode(returnType, functionName, parameters, null);
        if (!nodeStack.isEmpty() && nodeStack.peek() instanceof ProgramNode) {
            ProgramNode programNode = (ProgramNode) nodeStack.peek();
            programNode.addDeclarationNode(functionNode);
        }
        nodeStack.push(functionNode);
        statementListStack.push(new ArrayList<>());
    }

    //Finaliza la definición de funciones, asignando el cuerpo construido al nodo de función
    @Override
    public void exitFunctionDefinition(MiniCParser.FunctionDefinitionContext ctx) {
        List<StatementNode> bodyStatements = statementListStack.pop();
        BlockNode body = new BlockNode(bodyStatements);

        FunctionNode functionNode = (FunctionNode) nodeStack.pop();
        functionNode.setBody(body);
    }

    // Maneja la entrada y salida a bloques compuestos, iniciando una nueva lista de sentencias
    @Override
    public void enterCompoundStatement(MiniCParser.CompoundStatementContext ctx) {
        statementListStack.push(new ArrayList<>());
    }

    @Override
    public void exitCompoundStatement(MiniCParser.CompoundStatementContext ctx) {
        List<StatementNode> statements = statementListStack.pop();
        BlockNode blockNode = new BlockNode(statements);
        if (!statementListStack.isEmpty()) {
            statementListStack.peek().add(blockNode);
        }
    }

    // Maneja la entrada y salida a sentencias if, creando nodos IfNode y asignando bloques then y else
    @Override
    public void enterIfStatement(MiniCParser.IfStatementContext ctx) {
        IfNode ifNode = new IfNode(null, null, null);
        statementListStack.peek().add(ifNode);
        nodeStack.push(ifNode);
        statementListStack.push(new ArrayList<>());
    }

    @Override
    public void exitIfStatement(MiniCParser.IfStatementContext ctx) {
        List<StatementNode> thenStatements = statementListStack.pop();
        nodeStack.pop();
        StatementNode thenBlock = thenStatements.size() == 1 ? thenStatements.get(0) : new BlockNode(thenStatements);
        StatementNode elseBlock = null;
        if (ctx.ELSE() != null && !statementListStack.isEmpty()) {
            List<StatementNode> current = statementListStack.peek();
            if (!current.isEmpty()) {
                StatementNode last = current.get(current.size() - 1);
                if (last instanceof BlockNode) {
                    elseBlock = last;
                    current.remove(current.size() - 1);
                }
            }
        }
        ExpressionNode condition = !expressionStack.isEmpty() ? expressionStack.pop() : null;
        IfNode updatedIfNode = new IfNode(condition, (BlockNode) thenBlock, (BlockNode) elseBlock);
        List<StatementNode> currentStatements = statementListStack.peek();
        currentStatements.set(currentStatements.size() - 1, updatedIfNode);
    }

    // Maneja la entrada y salida a sentencias while, creando nodos WhileNode y asignando condición y cuerpo
    @Override
    public void enterWhileStatement(MiniCParser.WhileStatementContext ctx) {
        WhileNode whileNode = new WhileNode(null, null);
        statementListStack.peek().add(whileNode);
        nodeStack.push(whileNode);
        statementListStack.push(new ArrayList<>());
    }

    @Override
    public void exitWhileStatement(MiniCParser.WhileStatementContext ctx) {
        List<StatementNode> bodyStatements = statementListStack.pop();
        nodeStack.pop();
        StatementNode body = bodyStatements.size() == 1 ? bodyStatements.get(0) : new BlockNode(bodyStatements);
        ExpressionNode condition = !expressionStack.isEmpty() ? expressionStack.pop() : null;
        WhileNode updateWhileNode = new WhileNode(condition, body);
        List<StatementNode> currentStatements = statementListStack.peek();
        currentStatements.set(currentStatements.size() - 1, updateWhileNode);
    }

    // Maneja la entrada y salida a sentencias do-while, creando nodos DoWhileNode y asignando cuerpo y condición
    @Override
    public void enterDoWhileStatement(MiniCParser.DoWhileStatementContext ctx) {
        DoWhileNode doWhileNode = new DoWhileNode(null, null);
        statementListStack.peek().add(doWhileNode);
        nodeStack.push(doWhileNode);
        statementListStack.push(new ArrayList<>());
    }

    @Override
    public void exitDoWhileStatement(MiniCParser.DoWhileStatementContext ctx) {
        List<StatementNode> bodyStatements = statementListStack.pop();
        nodeStack.pop();
        StatementNode body = bodyStatements.size() == 1 ? bodyStatements.get(0) : new BlockNode(bodyStatements);
        ExpressionNode condition = !expressionStack.isEmpty() ? expressionStack.pop() : null;
        DoWhileNode update = new DoWhileNode(body, condition);
        List<StatementNode> current = statementListStack.peek();
        current.set(current.size() - 1, update);
    }

    // Maneja la entrada y salida a sentencias for, creando nodos ForNode y asignando inicialización, condición, actualización y cuerpo
    @Override
    public void enterForStatement(MiniCParser.ForStatementContext ctx) {
        ForNode forNode = new ForNode(null, null, null, null);
        statementListStack.peek().add(forNode);
        nodeStack.push(forNode);
        statementListStack.push(new ArrayList<>());
    }

    @Override
    public void exitForStatement(MiniCParser.ForStatementContext ctx) {
        List<StatementNode> bodyStatementNodes = statementListStack.pop();
        nodeStack.pop();
        StatementNode body = bodyStatementNodes.size() == 1 ? bodyStatementNodes.get(0)
                : new BlockNode(bodyStatementNodes);
        ExpressionNode condition = !expressionStack.isEmpty() ? expressionStack.pop() : null;
        ForNode update = new ForNode(null, condition, null, body);
        List<StatementNode> current = statementListStack.peek();
        current.set(current.size() - 1, update);
    }

    // Maneja la salida de sentencias de asignación, creando nodos AssignmentNode
    @Override
    public void exitAssignmentStatement(MiniCParser.AssignmentStatementContext ctx) {
        ExpressionNode value = !expressionStack.isEmpty() ? expressionStack.pop() : null;

        ExpressionNode target = !expressionStack.isEmpty() ? expressionStack.pop() : null;

        if (target == null) {
            System.err.println("ERROR: No se encontró target para asignación: " + ctx.lvalue().getText());
            target = new VariableNode("ERROR_" + ctx.lvalue().getText());
        }

        AssignmentNode assignmentNode = new AssignmentNode(target, value);

        AstNode currentNode = nodeStack.peek();
        if (currentNode instanceof FunctionNode || currentNode instanceof BlockNode) {
            statementListStack.peek().add(assignmentNode);
        } else if (currentNode instanceof ProgramNode) {
            statementListStack.peek().add(assignmentNode);
        }
    }

    // Maneja la entrada de sentencias de expresión, creando nodos ExpressionStatementNode
    @Override
    public void enterExpressionStatement(MiniCParser.ExpressionStatementContext ctx) {
        if (ctx.expression() != null) {
            ExpressionNode expr = !expressionStack.isEmpty() ? expressionStack.pop() : null;
            ExpressionStatementNode expressionStatementNode = new ExpressionStatementNode(expr);
            AstNode currentNode = nodeStack.peek();
            if (currentNode instanceof FunctionNode || currentNode instanceof BlockNode) {
                statementListStack.peek().add(expressionStatementNode);
            } else if (currentNode instanceof ProgramNode) {
                statementListStack.peek().add(expressionStatementNode);
            }
        }
    }

    // Maneja la salida de sentencias de retorno, creando nodos ReturnNode
    @Override
    public void exitReturnStatement(MiniCParser.ReturnStatementContext ctx) {
        ExpressionNode returnValue = !expressionStack.isEmpty() ? expressionStack.pop() : null;
        ReturnNode returnNode = new ReturnNode(returnValue);

        AstNode currentNode = nodeStack.peek();
        if (currentNode instanceof FunctionNode || currentNode instanceof BlockNode) {
            statementListStack.peek().add(returnNode);
        } else if (currentNode instanceof ProgramNode) {
            statementListStack.peek().add(returnNode);
        }
    }

    // Maneja la entrada a expresiones primarias, creando nodos correspondientes para constantes y literales
    @Override
    public void enterPrimaryExpression(MiniCParser.PrimaryExpressionContext ctx) {
        if (ctx.IntegerConstant() != null) {
            int value = Integer.parseInt(ctx.IntegerConstant().getText());
            expressionStack.push(new NumberNode(value));
        } else if (ctx.CharConstant() != null) {
            String text = ctx.CharConstant().getText();
            char value = text.length() > 2 ? text.charAt(1) : '\0';
            expressionStack.push(new CharNode(value));
        } else if (ctx.StringLiteral() != null) {
            String text = ctx.StringLiteral().getText();
            String value = text.length() > 2 ? text.substring(1, text.length() - 1) : "";
            expressionStack.push(new StringNode(value));
        } else if (ctx.TRUE() != null) {
            expressionStack.push(new BooleanNode(true));
        } else if (ctx.FALSE() != null) {
            expressionStack.push(new BooleanNode(false));
        } else if (ctx.lvalue() != null) {
        } else if (ctx.LPAREN() != null && ctx.expression() != null) {
            ;
        }
    }

    // Maneja la entrada a expresiones de llamada a funciones, creando nodos FunctionCallNode
    @Override
    public void enterCallExpression(MiniCParser.CallExpressionContext ctx) {
        String funcName = ctx.Identifier().getText();
        List<ExpressionNode> args = new ArrayList<>();
        if (ctx.argumentList() != null) {
            for (int i = 0; i < ctx.argumentList().expression().size(); i++) {
                if (!expressionStack.isEmpty()) {
                    args.add(0, expressionStack.pop());
                }
            }
        }
        expressionStack.push(new FunctionCallNode(funcName, args));
    }

    // Maneja la salida de lvalues, creando nodos VariableNode, ArrayAccessNode o UnaryOpNode según corresponda
    @Override
    public void exitLvalue(MiniCParser.LvalueContext ctx) {

        if (ctx.Identifier() != null && ctx.LBRACK() == null && ctx.STAR() == null) {
            String varName = ctx.Identifier().getText();
            expressionStack.push(new VariableNode(varName));
            return;
        }

        if (ctx.LBRACK() != null && ctx.expression() != null) {

            ExpressionNode index = expressionStack.pop();

            ExpressionNode base;
            if (!expressionStack.isEmpty()) {
                base = expressionStack.pop();
            } else {
                base = new VariableNode("error");
            }

            if (base instanceof ArrayAccessNode) {
                ArrayAccessNode existing = (ArrayAccessNode) base;
                List<ExpressionNode> newIndices = new ArrayList<>(existing.getIndices());
                newIndices.add(index);
                ArrayAccessNode newNode = new ArrayAccessNode(existing.getArray(), newIndices);
                expressionStack.push(newNode);
            } else {
                List<ExpressionNode> indices = new ArrayList<>();
                indices.add(index);
                ArrayAccessNode newNode = new ArrayAccessNode(base, indices);
                expressionStack.push(newNode);
            }
            return;
        }

        if (ctx.STAR() != null && ctx.expression() != null) {
            if (!expressionStack.isEmpty()) {
                ExpressionNode expr = expressionStack.pop();
                expressionStack.push(new UnaryOpNode("*", expr));
            }
        }
    }

    // Maneja la salida de expresiones multiplicativas, creando nodos BinaryOpNode
    @Override
    public void exitMultiplicativeExpression(MiniCParser.MultiplicativeExpressionContext ctx) {

        if (ctx.unaryExpression().size() <= 1) {
            return;
        }

        List<ExpressionNode> operands = new ArrayList<>();
        List<String> operators = new ArrayList<>();

        for (int i = 0; i < ctx.unaryExpression().size(); i++) {
            operands.add(0, expressionStack.pop());
        }

        for (int i = 1; i < ctx.getChildCount(); i += 2) {
            operators.add(ctx.getChild(i).getText());
        }

        ExpressionNode result = operands.get(0);
        for (int i = 0; i < operators.size(); i++) {
            result = new BinaryOpNode(operators.get(i), result, operands.get(i + 1));
        }

        expressionStack.push(result);
    }

    // Maneja la salida de expresiones aditivas, creando nodos BinaryOpNode
    @Override
    public void exitAdditiveExpression(MiniCParser.AdditiveExpressionContext ctx) {
        if (ctx.multiplicativeExpression().size() > 1) {
            List<ExpressionNode> operands = new ArrayList<>();
            List<String> operators = new ArrayList<>();
            for (int i = 0; i < ctx.multiplicativeExpression().size(); i++) {
                if (!expressionStack.isEmpty()) {
                    operands.add(0, expressionStack.pop());
                }
            }
            for (int i = 0; i < ctx.getChildCount(); i++) {
                org.antlr.v4.runtime.tree.TerminalNode node = ctx.getChild(org.antlr.v4.runtime.tree.TerminalNode.class,
                        i);
                if (node != null) {
                    String symbol = node.getText();
                    if (symbol.equals("+") || symbol.equals("-")) {
                        operators.add(symbol);
                    }
                }
            }
            ExpressionNode result = operands.get(0);
            for (int i = 0; i < operators.size(); i++) {
                result = new BinaryOpNode(operators.get(i), result, operands.get(i + 1));
            }

            expressionStack.push(result);
        }
    }

    // Maneja la salida de expresiones relacionales, creando nodos BinaryOpNode
    @Override
    public void exitRelationalExpression(MiniCParser.RelationalExpressionContext ctx) {

        if (ctx.additiveExpression().size() > 1) {
            List<ExpressionNode> operands = new ArrayList<>();
            List<String> operators = new ArrayList<>();

            for (int i = 0; i < ctx.additiveExpression().size(); i++) {
                if (!expressionStack.isEmpty()) {
                    operands.add(0, expressionStack.pop());
                }
            }

            for (int i = 0; i < ctx.getChildCount(); i++) {
                org.antlr.v4.runtime.tree.TerminalNode node = ctx.getChild(org.antlr.v4.runtime.tree.TerminalNode.class,
                        i);
                if (node != null) {
                    String symbol = node.getText();
                    if (symbol.equals("<") || symbol.equals(">") || symbol.equals("<=") || symbol.equals(">=")) {
                        operators.add(symbol);
                    }
                }
            }

            ExpressionNode result = operands.get(0);
            for (int i = 0; i < operators.size(); i++) {
                result = new BinaryOpNode(operators.get(i), result, operands.get(i + 1));
            }

            expressionStack.push(result);
        }
    }

    // Maneja la salida de expresiones de igualdad, creando nodos BinaryOpNode
    @Override
    public void exitEqualityExpression(MiniCParser.EqualityExpressionContext ctx) {

        if (ctx.relationalExpression().size() > 1) {
            List<ExpressionNode> operands = new ArrayList<>();
            List<String> operators = new ArrayList<>();

            for (int i = 0; i < ctx.relationalExpression().size(); i++) {
                if (!expressionStack.isEmpty()) {
                    operands.add(0, expressionStack.pop());
                }
            }

            for (int i = 0; i < ctx.getChildCount(); i++) {
                org.antlr.v4.runtime.tree.TerminalNode node = ctx.getChild(org.antlr.v4.runtime.tree.TerminalNode.class,
                        i);
                if (node != null) {
                    String symbol = node.getText();
                    if (symbol.equals("==") || symbol.equals("!=")) {
                        operators.add(symbol);
                    }
                }
            }

            ExpressionNode result = operands.get(0);
            for (int i = 0; i < operators.size(); i++) {
                result = new BinaryOpNode(operators.get(i), result, operands.get(i + 1));
            }

            expressionStack.push(result);
        }
    }

    // Maneja la salida de expresiones lógicas AND, creando nodos BinaryOpNode
    @Override
    public void exitLogicalAndExpression(MiniCParser.LogicalAndExpressionContext ctx) {

        if (ctx.equalityExpression().size() > 1) {
            List<ExpressionNode> operands = new ArrayList<>();
            List<String> operators = new ArrayList<>();

            for (int i = 0; i < ctx.equalityExpression().size(); i++) {
                if (!expressionStack.isEmpty()) {
                    operands.add(0, expressionStack.pop());
                }
            }

            for (int i = 0; i < ctx.getChildCount(); i++) {
                org.antlr.v4.runtime.tree.TerminalNode node = ctx.getChild(org.antlr.v4.runtime.tree.TerminalNode.class,
                        i);
                if (node != null) {
                    String symbol = node.getText();
                    if (symbol.equals("&&")) {
                        operators.add(symbol);
                    }
                }
            }

            ExpressionNode result = operands.get(0);
            for (int i = 0; i < operators.size(); i++) {
                result = new BinaryOpNode(operators.get(i), result, operands.get(i + 1));
            }

            expressionStack.push(result);
        }
    }

    // Maneja la salida de expresiones lógicas OR, creando nodos BinaryOpNode
    @Override
    public void exitLogicalOrExpression(MiniCParser.LogicalOrExpressionContext ctx) {

        if (ctx.logicalAndExpression().size() > 1) {
            List<ExpressionNode> operands = new ArrayList<>();
            List<String> operators = new ArrayList<>();

            for (int i = 0; i < ctx.logicalAndExpression().size(); i++) {
                if (!expressionStack.isEmpty()) {
                    operands.add(0, expressionStack.pop());
                }
            }

            for (int i = 0; i < ctx.getChildCount(); i++) {
                org.antlr.v4.runtime.tree.TerminalNode node = ctx.getChild(org.antlr.v4.runtime.tree.TerminalNode.class,
                        i);
                if (node != null) {
                    String symbol = node.getText();
                    if (symbol.equals("||")) {
                        operators.add(symbol);
                    }
                }
            }

            ExpressionNode result = operands.get(0);
            for (int i = 0; i < operators.size(); i++) {
                result = new BinaryOpNode(operators.get(i), result, operands.get(i + 1));
            }

            expressionStack.push(result);
        }
    }

    // Maneja la salida de expresiones de unary, creando nodos UnaryOpNode
    @Override
    public void exitUnaryExpression(MiniCParser.UnaryExpressionContext ctx) {

        if (ctx.NOT() != null || ctx.MINUS() != null || ctx.AMP() != null || ctx.STAR() != null) {
            if (!expressionStack.isEmpty()) {
                ExpressionNode operand = expressionStack.pop();
                String operator = ctx.NOT() != null ? "!" : ctx.MINUS() != null ? "-" : ctx.AMP() != null ? "&" : "*";
                expressionStack.push(new UnaryOpNode(operator, operand));
            }
        }
    }
}