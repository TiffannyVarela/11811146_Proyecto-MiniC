package org.minic.ast;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.minic.MiniCBaseListener;
import org.minic.MiniCParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/* Recorre el árbol generado por ANTLR y construye el AST correspondiente */

public class AstBuilder extends MiniCBaseListener {
    private AstNode root;
    private Stack<AstNode> nodeStack = new Stack<>();
    private Stack<List<StatementNode>> statementListStack = new Stack<>();
    private String currentType = null;
    private Stack<ExpressionNode> expressionStack = new Stack<>();
    
    public AstNode build(org.antlr.v4.runtime.tree.ParseTree tree) {
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, tree);
        return root;
    }

    @Override
    public void enterProgram(MiniCParser.ProgramContext ctx) {
        ProgramNode programNode = new ProgramNode();
        nodeStack.push(programNode);
        statementListStack.push(new ArrayList<>());
    }

    @Override
    public void exitProgram(MiniCParser.ProgramContext ctx) {
        ProgramNode programNode = (ProgramNode) nodeStack.pop();
        List<StatementNode> globalStatements = statementListStack.pop();
        if (!globalStatements.isEmpty()) {
            System.out.println("AST BUILDER: Ignorando statements globales que no son funciones ni declaracion de variables.");
        }
        root = programNode;
    }

    @Override
    public void enterDeclaration(MiniCParser.DeclarationContext ctx) {

        if (ctx.functionDeclaration() != null) {
            handleFunctionDeclaration(ctx.functionDeclaration());
            return;
        }

        handleVariableDeclaration(ctx);
    }

    private void handleFunctionDeclaration(MiniCParser.FunctionDeclarationContext funcDeclCtx) {
        String returnType = funcDeclCtx.typeSpecifier().getText();
        String funcName = funcDeclCtx.Identifier().getText();

        List<VarDeclNode> parameters = null;
        if (funcDeclCtx.parameterList() != null) {
            parameters = new ArrayList<>();
            for (MiniCParser.ParameterContext paramCtx : funcDeclCtx.parameterList().parameter()) {
                String paramType = paramCtx.typeSpecifier().getText();
                String paramName = paramCtx.Identifier().getText();
                parameters.add(new VarDeclNode(paramType, paramName, false, 0, 0, null));
            }
        }

        FunctionNode funcNode = new FunctionNode(returnType, funcName, parameters, null);

        AstNode currentNode = nodeStack.peek();
        if (currentNode instanceof ProgramNode) {
            ProgramNode programNode = (ProgramNode) currentNode;
            programNode.addDeclarationNode(funcNode);
        }
    }

    private void handleVariableDeclaration(MiniCParser.DeclarationContext ctx) {
        currentType = ctx.typeSpecifier().getText();

        for (MiniCParser.InitDeclaratorContext initDeclCtx : ctx.initDeclaratorList().initDeclarator()) {
            String varName = initDeclCtx.Identifier().getText();
            boolean isArray = false;
            int arraySize = 0;
            int secondDimension = 0;

            if (initDeclCtx.arrayDimensions() != null) {
                MiniCParser.ArrayDimensionsContext dims = initDeclCtx.arrayDimensions();
                if (dims.IntegerConstant().size() == 1) {
                    isArray = true;
                    arraySize = Integer.parseInt(dims.IntegerConstant(0).getText());
                } else if (dims.IntegerConstant().size() == 2) {
                    isArray = true;
                    arraySize = Integer.parseInt(dims.IntegerConstant(0).getText());
                    secondDimension = Integer.parseInt(dims.IntegerConstant(1).getText());
                }
            }

            ExpressionNode initialNode = null;
            if (initDeclCtx.ASSIGN() != null && initDeclCtx.expression() != null) {
                if (!expressionStack.isEmpty()) {
                    initialNode = expressionStack.pop();
                }
            }
            VarDeclNode varDeclNode = new VarDeclNode(currentType, varName, isArray, arraySize, secondDimension, initialNode);
            VarDeclStatementNode declStmt = new VarDeclStatementNode(varDeclNode);
            statementListStack.peek().add(declStmt);
        }

        currentType = null;
    }

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

    @Override
    public void exitFunctionDefinition(MiniCParser.FunctionDefinitionContext ctx) {
        List<StatementNode> bodyStatements = statementListStack.pop();
        BlockNode body = new BlockNode(bodyStatements);

        FunctionNode functionNode = (FunctionNode) nodeStack.pop();
        functionNode.setBody(body);
    }

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
        StatementNode body = bodyStatementNodes.size() == 1 ? bodyStatementNodes.get(0) : new BlockNode(bodyStatementNodes);
        ExpressionNode condition = !expressionStack.isEmpty() ? expressionStack.pop() : null;
        ForNode update = new ForNode(null, condition, null, body);
        List<StatementNode> current = statementListStack.peek();
        current.set(current.size() - 1, update);
    }

    @Override
    public void exitAssignmentStatement(MiniCParser.AssignmentStatementContext ctx) {
        String varName = ctx.lvalue().getText();
        ExpressionNode value = !expressionStack.isEmpty() ? expressionStack.pop() : null;
        AssignmentNode assignmentNode = new AssignmentNode(varName, value);

        AstNode currentNode = nodeStack.peek();
        if (currentNode instanceof FunctionNode || currentNode instanceof BlockNode) {
            statementListStack.peek().add(assignmentNode);
        } else if (currentNode instanceof ProgramNode) {
            statementListStack.peek().add(assignmentNode);
        }
    }

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
        } else if (ctx.LPAREN() != null && ctx.expression() != null) {;
        }
    }

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
//Funcion
@Override
public void exitLvalue(MiniCParser.LvalueContext ctx) {
    if (ctx.Identifier() != null && ctx.LBRACK() == null && ctx.STAR() == null) {
        String varName = ctx.Identifier().getText();
        if (!varName.contains("[")) {
            expressionStack.push(new VariableNode(varName));
        }
        return;
    }

    if (ctx.lvalue() != null && ctx.LBRACK() != null && ctx.expression() != null) {
        ExpressionNode index = expressionStack.pop();
        ExpressionNode base = expressionStack.pop();
        
        if (base instanceof ArrayAccessNode) {
            ArrayAccessNode existing = (ArrayAccessNode) base;
            List<ExpressionNode> newIndices = new ArrayList<>(existing.getIndices());
            newIndices.add(index);
            expressionStack.push(new ArrayAccessNode(existing.getArray(), newIndices));
        } else {
            List<ExpressionNode> indices = new ArrayList<>();
            indices.add(index);
            expressionStack.push(new ArrayAccessNode(base, indices));
        }
    }
}

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
                org.antlr.v4.runtime.tree.TerminalNode node = ctx.getChild(org.antlr.v4.runtime.tree.TerminalNode.class, i);
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
                org.antlr.v4.runtime.tree.TerminalNode node = ctx.getChild(org.antlr.v4.runtime.tree.TerminalNode.class, i);
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
                org.antlr.v4.runtime.tree.TerminalNode node = ctx.getChild(org.antlr.v4.runtime.tree.TerminalNode.class, i);
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
                org.antlr.v4.runtime.tree.TerminalNode node = ctx.getChild(org.antlr.v4.runtime.tree.TerminalNode.class, i);
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
                org.antlr.v4.runtime.tree.TerminalNode node = ctx.getChild(org.antlr.v4.runtime.tree.TerminalNode.class, i);
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

    @Override
    public void exitUnaryExpression(MiniCParser.UnaryExpressionContext ctx) {
        
        if (ctx.NOT() != null || ctx.MINUS() != null || ctx.AMP() != null || ctx.STAR() != null) {
            if (!expressionStack.isEmpty()) {
                ExpressionNode operand = expressionStack.pop();
                String operator = ctx.NOT() != null ? "!" : 
                                 ctx.MINUS() != null ? "-" : 
                                 ctx.AMP() != null ? "&" : "*";
                expressionStack.push(new UnaryOpNode(operator, operand));
            }
        }
    }
}