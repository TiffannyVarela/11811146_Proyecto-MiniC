package org.minic.ast;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.minic.MiniCBaseListener;
import org.minic.MiniC;
import org.minic.MiniCLexer;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class AstBuilder extends MiniCBaseListener {
    private AstNode root;
    private Stack<AstNode> nodeStack = new Stack<>();
    private Stack<List<StatementNode>> statementListStack = new Stack<>();

    public AstNode build(org.antlr.v4.runtime.tree.ParseTree tree) {
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, tree);
        return root;
    }

    @Override
    public void enterProgram(MiniC.ProgramContext ctx) {
        ProgramNode programNode = new ProgramNode();
        //root = programNode;
        nodeStack.push(programNode);
    }

    @Override
    public void exitProgram(MiniC.ProgramContext ctx) {
        ProgramNode programNode = (ProgramNode) nodeStack.pop();
        root = programNode;
    }

    @Override
    public void enterDeclaration(MiniC.DeclarationContext ctx) {
        String varType = ctx.typeSpecifier().getText();

        for (MiniC.InitDeclaratorContext initDeclCtx : ctx.initDeclaratorList().initDeclarator()) {
            String varName = initDeclCtx.Identifier().getText();
            boolean isArray = initDeclCtx.LBRACK() != null && !initDeclCtx.LBRACK().isEmpty();
            int arraySize = 0;

            if (isArray && initDeclCtx.IntegerConstant() != null && !initDeclCtx.IntegerConstant().isEmpty()) {
                arraySize = Integer.parseInt(initDeclCtx.IntegerConstant(0).getText());
            }

            VarDeclNode varDeclNode = new VarDeclNode(varType, varName, isArray, arraySize);
            ProgramNode programNode = (ProgramNode) nodeStack.peek();
            programNode.addDeclarationNode(varDeclNode);

        }
    }

    @Override
    public void enterFunctionDefinition(MiniC.FunctionDefinitionContext ctx) {
        String returnType = ctx.typeSpecifier().getText();
        String functionName = ctx.Identifier().getText();

        //Procesar parámetros
        List<VarDeclNode> parameters = new ArrayList<>();
        if (ctx.parameterList() != null) {
            for( MiniC.ParameterContext paramCtx : ctx.parameterList().parameter()) {
                String paramType = paramCtx.typeSpecifier().getText();
                String paramName = paramCtx.initDeclarator().Identifier().getText();
                parameters.add(new VarDeclNode(paramType, paramName));
            }
        }

        FunctionNode functionNode = new FunctionNode(returnType, functionName, parameters, null);
        nodeStack.push(functionNode);
        statementListStack.push(new ArrayList<>());
    }

    @Override
    public void exitFunctionDefinition(MiniC.FunctionDefinitionContext ctx) {
        List<StatementNode> bodyStatements = statementListStack.pop();
        BlockNode body = new BlockNode();
        for (StatementNode stmt : bodyStatements) {
            body.addStatement(stmt);
        }

        FunctionNode functionNode = (FunctionNode) nodeStack.pop();
        functionNode.setBody(body);
        if (!nodeStack.isEmpty() && nodeStack.peek() instanceof ProgramNode) {
            ProgramNode programNode = (ProgramNode) nodeStack.peek();
            programNode.addDeclarationNode(functionNode);
        }
    }

    @Override
    public void enterCompoundStatement(MiniC.CompoundStatementContext ctx) {
        statementListStack.push(new ArrayList<>());
    }

    @Override
    public void exitCompoundStatement(MiniC.CompoundStatementContext ctx) {
        List<StatementNode> statements = statementListStack.pop();
        BlockNode blockNode = new BlockNode();
        for (StatementNode stmt : statements) {
            blockNode.addStatement(stmt);
        }

        if (!statementListStack.isEmpty()) {
            statementListStack.peek().add(blockNode);
        }
    }

    @Override
    public void enterIfStatement(MiniC.IfStatementContext ctx) {
        //Processar condición
        ExpressionNode condition = buildExpression(ctx.expression());

        IfNode ifNode = new IfNode(condition, null, null);
        statementListStack.peek().add(ifNode);
        nodeStack.push(ifNode);
        statementListStack.push(new ArrayList<>());
    }

    @Override
    public void exitIfStatement(MiniC.IfStatementContext ctx) {
        List<StatementNode> thenStatements = statementListStack.pop();
        IfNode ifNode = (IfNode) nodeStack.pop();

        //Crear bloque para la rama "then"
        StatementNode thenBlock = null;
        if(!thenStatements.isEmpty()){
            thenBlock = thenStatements.size() == 1 ? thenStatements.get(0) : new BlockNode(thenStatements);
        }

        //Crear bloque para la rama "else" si existe
        StatementNode elseBlock = null;
        if(ctx.ELSE() != null && statementListStack.isEmpty()){
            List<StatementNode> elseStatements = statementListStack.pop();
            if(!elseStatements.isEmpty()){
                elseBlock = elseStatements.size() == 1 ? elseStatements.get(0) : new BlockNode(elseStatements);
            }
        }

        IfNode updatedIfNode = new IfNode(ifNode.getCondition(), (BlockNode) thenBlock, (BlockNode) elseBlock);

        //Actualizar el nodo If en la pila de nodos
        List<StatementNode> currentStatements = statementListStack.peek();
        currentStatements.set(currentStatements.size()-1, updatedIfNode);
    }

    @Override
    public void enterAssignmentStatement(MiniC.AssignmentStatementContext ctx) {
        String varName = ctx.lvalue().Identifier().getText();
        ExpressionNode value = buildExpression(ctx.expression());
        //AssignmentNode assignmentNode = new AssignmentNode(varName, value);
        System.out.println("Assigment: "+varName+"="+value);
    }

    @Override
    public void enterReturnStatement(MiniC.ReturnStatementContext ctx) {
        ExpressionNode returnValue = ctx.expression() != null ? buildExpression(ctx.expression()) : null;
        //ReturnNode returnNode = new ReturnNode(returnValue);
        System.out.println("Return: "+returnValue);
    }

    private ExpressionNode buildExpression(MiniC.ExpressionContext ctx) {
        if (ctx == null) {
            return null;
        }

        if (ctx.getChildCount()==1) {
            return buildPrimaryExpression(ctx.logicalOrExpression().logicalAndExpression(0).equalityExpression(0).relationalExpression(0).additiveExpression(0).multiplicativeExpression(0).unaryExpression(0).primaryExpression());
        }
        return null;
    }

    private ExpressionNode buildPrimaryExpression(MiniC.PrimaryExpressionContext ctx) {
        //Integer
        if (ctx.IntegerConstant() != null) {
            return new NumberNode(ctx.IntegerConstant().getText());
        }

        //Char
        if (ctx.CharConstant() != null) {
            //return new CharNode(ctx.CharConstant().getText());
        }

        //String
        if (ctx.StringLiteral() != null) {
            //return new StringNode(ctx.StringLiteral().getText());
        }

        //Boolean
        if (ctx.TRUE() != null) {
            //return new BooleanNode(true);
        }
        if (ctx.FALSE() != null) {
            //return new BooleanNode(false);
        }

        //Identifier
        if (ctx.lvalue() != null) {
            String varName = ctx.lvalue().Identifier().getText();
            return new VariableNode(varName);
        }

        //CallExpression
        if (ctx.callExpression() != null) {
            String functionName = ctx.callExpression().Identifier().getText();
            List<ExpressionNode> arguments = new ArrayList<>();
            if (ctx.callExpression().expression() != null){
                for (MiniC.ExpressionContext argCtx : ctx.callExpression().expression()) {
                    arguments.add(buildExpression(argCtx));
                }
            }
        return new FunctionCallNode(functionName, arguments);
        }

        //Parenthesized Expression
        if (ctx.LPAREN() != null && ctx.expression() != null){
            return buildExpression(ctx.expression());
        }
        return null;
    }

    @Override
    public void enterEveryRule(org.antlr.v4.runtime.ParserRuleContext ctx) {
        //System.out.println("Entering: " + ctx.getClass().getSimpleName());
    }
}
