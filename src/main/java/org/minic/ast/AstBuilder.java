package org.minic.ast;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.minic.MiniCBaseListener;
import org.minic.MiniCParser;

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
    public void enterProgram(MiniCParser.ProgramContext ctx) {
        ProgramNode programNode = new ProgramNode();
        //root = programNode;
        nodeStack.push(programNode);
    }

    @Override
    public void exitProgram(MiniCParser.ProgramContext ctx) {
        ProgramNode programNode = (ProgramNode) nodeStack.pop();
        root = programNode;
    }

    @Override
    public void enterDeclaration(MiniCParser.DeclarationContext ctx) {
        String varType = ctx.typeSpecifier().getText();

        for (MiniCParser.InitDeclaratorContext initDeclCtx : ctx.initDeclaratorList().initDeclarator()) {
            String varName = initDeclCtx.Identifier().getText();
            boolean isArray = initDeclCtx.LBRACK() != null && !initDeclCtx.LBRACK().isEmpty();
            int arraySize = 0;
            ExpressionNode initialNode = null;

            if (isArray && initDeclCtx.IntegerConstant() != null && !initDeclCtx.IntegerConstant().isEmpty()) {
                arraySize = Integer.parseInt(initDeclCtx.IntegerConstant(0).getText());
            }

            // Verificar inicialización solo si los métodos existen
            try {
                if (initDeclCtx.ASSIGN() != null && initDeclCtx.expression() != null) {
                    initialNode = buildExpression(initDeclCtx.expression());
                }
            } catch (Exception e) {
                // Si los métodos no existen, ignorar inicialización
                System.out.println("Advertencia: Inicialización no soportada en esta versión del parser");
            }

            VarDeclNode varDeclNode;
            if (initialNode != null) {
                varDeclNode = new VarDeclNode(varType, varName, isArray, arraySize, initialNode);
            } else {
                varDeclNode = new VarDeclNode(varType, varName, isArray, arraySize);
            }

            // CORRECCIÓN: Verificar qué tipo de nodo está en el tope de la pila
            AstNode currentNode = nodeStack.peek();
            
            if (currentNode instanceof ProgramNode) {
                // Declaración global - agregar al ProgramNode
                ProgramNode programNode = (ProgramNode) currentNode;
                programNode.addDeclarationNode(varDeclNode);
            } else if (currentNode instanceof FunctionNode) {
                // Declaración local dentro de función - agregar al bloque actual
                // No agregar al FunctionNode directamente, se maneja en el statement list
                if (!statementListStack.isEmpty()) {
                    // Crear una declaración como statement
                    // Necesitarías crear un nuevo tipo de nodo o usar AssignmentNode
                    if (initialNode != null) {
                        AssignmentNode initAssignment = new AssignmentNode(varName, initialNode);
                        statementListStack.peek().add(initAssignment);
                        System.out.println("Inicialización local: " + varName + " = " + initialNode);
                    }
                }
            }
        }
    }

    @Override
    public void enterFunctionDefinition(MiniCParser.FunctionDefinitionContext ctx) {
        String returnType = ctx.typeSpecifier().getText();
        String functionName = ctx.Identifier().getText();

        // Procesar parámetros
        List<VarDeclNode> parameters = new ArrayList<>();
        if (ctx.parameterList() != null) {
            for (MiniCParser.ParameterContext paramCtx : ctx.parameterList().parameter()) {
                String paramType = paramCtx.typeSpecifier().getText();
                String paramName = paramCtx.Identifier().getText();
                parameters.add(new VarDeclNode(paramType, paramName));
            }
        }

        FunctionNode functionNode = new FunctionNode(returnType, functionName, parameters, null);
        
        // AGREGAR AL PROGRAM NODE PRIMERO
        if (!nodeStack.isEmpty() && nodeStack.peek() instanceof ProgramNode) {
            ProgramNode programNode = (ProgramNode) nodeStack.peek();
            programNode.addDeclarationNode(functionNode);
        }
        
        // LUEGO PUSHEAR A LA PILA
        nodeStack.push(functionNode);
        statementListStack.push(new ArrayList<>());
    }

    @Override
    public void exitFunctionDefinition(MiniCParser.FunctionDefinitionContext ctx) {
        List<StatementNode> bodyStatements = statementListStack.pop();
        BlockNode body = new BlockNode();
        for (StatementNode stmt : bodyStatements) {
            body.addStatement(stmt);
        }

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
        BlockNode blockNode = new BlockNode();
        for (StatementNode stmt : statements) {
            blockNode.addStatement(stmt);
        }

        if (!statementListStack.isEmpty()) {
            statementListStack.peek().add(blockNode);
        }
    }

    @Override
    public void enterIfStatement(MiniCParser.IfStatementContext ctx) {
        //Processar condición
        ExpressionNode condition = buildExpression(ctx.expression());

        IfNode ifNode = new IfNode(condition, null, null);
        statementListStack.peek().add(ifNode);
        nodeStack.push(ifNode);
        statementListStack.push(new ArrayList<>());
    }

    @Override
    public void exitIfStatement(MiniCParser.IfStatementContext ctx) {
        List<StatementNode> thenStatements = statementListStack.pop();
        IfNode ifNode = (IfNode) nodeStack.pop();

        //Crear bloque para la rama "then"
        StatementNode thenBlock = null;
        if(!thenStatements.isEmpty()){
            thenBlock = thenStatements.size() == 1 ? thenStatements.get(0) : new BlockNode(thenStatements);
        }

        //Crear bloque para la rama "else" si existe
        StatementNode elseBlock = null;
        if(ctx.ELSE() != null){
            if (!statementListStack.isEmpty()) {
                List<StatementNode> current = statementListStack.peek();
                if (!current.isEmpty()) {
                    StatementNode last = current.get(current.size()-1);
                    if (last instanceof BlockNode) {
                        elseBlock = last;
                        current.remove(current.size()-1);
                    }
                }
            }
        }

        IfNode updatedIfNode = new IfNode(ifNode.getCondition(), (BlockNode) thenBlock, (BlockNode) elseBlock);

        //Actualizar el nodo If en la pila de nodos
        List<StatementNode> currentStatements = statementListStack.peek();
        currentStatements.set(currentStatements.size()-1, updatedIfNode);
    }

    @Override
    public void enterDoWhileStatement(MiniCParser.DoWhileStatementContext ctx){
        DoWhileNode doWhileNode = new DoWhileNode(null, null);
        statementListStack.peek().add(doWhileNode);
        nodeStack.push(doWhileNode);
        statementListStack.push(new ArrayList<>());
    }

    @Override
    public void exitDoWhileStatement(MiniCParser.DoWhileStatementContext ctx){
        List<StatementNode> bodyStatements = statementListStack.pop();
        nodeStack.pop();

        StatementNode body = bodyStatements.size() == 1 ? bodyStatements.get(0) : new BlockNode(bodyStatements);
        ExpressionNode condition = buildExpression(ctx.expression());

        DoWhileNode update = new DoWhileNode(body, condition);

        List<StatementNode> current = statementListStack.peek();
        current.set(current.size()-1, update);
    }

    @Override
    public void enterForStatement(MiniCParser.ForStatementContext ctx){
        StatementNode iniNode = null;
        if (ctx.forInit().expressionStatement() !=null && ctx.forInit().expressionStatement().expression() != null) {
            iniNode = new ExpressionStatementNode(buildExpression(ctx.forInit().expressionStatement().expression()));
        } else if (ctx.forInit().declaration() != null) {
            //Manejo de forInit de ser necesario
        }

        ExpressionNode condition = ctx.expression() != null ? buildExpression(ctx.expression()) : null;
        ExpressionNode increment = ctx.forUpdate() != null ? buildExpression(ctx.forUpdate().expression()) : null;

        ForNode forNode = new ForNode(iniNode, condition, increment, null);
        statementListStack.peek().add(forNode);
        nodeStack.push(forNode);
        statementListStack.push(new ArrayList<>());
    }

    @Override
    public void exitForStatement(MiniCParser.ForStatementContext ctx){
        List<StatementNode> bodyStatementNodes = statementListStack.pop();
        ForNode forNode = (ForNode) nodeStack.pop();

        StatementNode body = bodyStatementNodes.size() == 1 ? bodyStatementNodes.get(0) : new BlockNode(bodyStatementNodes);

        ForNode update = new ForNode(forNode.getInit(), forNode.getCondition(), forNode.getIncrement(), body);

        List<StatementNode> current = statementListStack.peek();
        current.set(current.size()-1, update);
    }

    @Override
    public void enterAssignmentStatement(MiniCParser.AssignmentStatementContext ctx) {
        String varName = ctx.lvalue().Identifier().getText();
        ExpressionNode value = buildExpression(ctx.expression());
        AssignmentNode assignmentNode = new AssignmentNode(varName, value);
        if (!statementListStack.isEmpty()) {
            statementListStack.peek().add(assignmentNode);
        }
        System.out.println("Assigment: "+varName+"="+value);
    }

    @Override
    public void enterExpressionStatement(MiniCParser.ExpressionStatementContext ctx){
        if (ctx.expression() != null) {
            ExpressionNode expressionNode = buildExpression(ctx.expression());
            ExpressionStatementNode expressionStatementNode = new ExpressionStatementNode(expressionNode);
            if (!statementListStack.isEmpty()) {
                statementListStack.peek().add(expressionStatementNode);
            }
        }
    }

    @Override
    public void enterReturnStatement(MiniCParser.ReturnStatementContext ctx) {
        ExpressionNode returnValue = ctx.expression() != null ? buildExpression(ctx.expression()) : null;
        ReturnNode returnNode = new ReturnNode(returnValue);
        if (!statementListStack.isEmpty()) {
            statementListStack.peek().add(returnNode);
        }
        System.out.println("Return: "+returnValue);
    }

    private ExpressionNode buildExpression(MiniCParser.ExpressionContext ctx) {
        if (ctx == null) {
            return null;
        }

        /*if (ctx.getChildCount()==1) {
            return buildPrimaryExpression(ctx.logicalOrExpression().logicalAndExpression(0).equalityExpression(0).relationalExpression(0).additiveExpression(0).multiplicativeExpression(0).unaryExpression(0).primaryExpression());
        }
        return null;*/

        return buildLogicalOrExpression(ctx.logicalOrExpression());
    }

    private ExpressionNode buildPrimaryExpression(MiniCParser.PrimaryExpressionContext ctx) {
        //Integer
        if (ctx.IntegerConstant() != null) {
            return new NumberNode(ctx.IntegerConstant().getText());
        }

        //Char
        if (ctx.CharConstant() != null) {
            return new CharNode(ctx.CharConstant().getText());
        }

        //String
        if (ctx.StringLiteral() != null) {
            return new StringNode(ctx.StringLiteral().getText());
        }

        //Boolean
        if (ctx.TRUE() != null) {
            return new BooleanNode(true);
        }
        if (ctx.FALSE() != null) {
            return new BooleanNode(false);
        }

        //Identifier
        if (ctx.lvalue() != null) {
            String varName = ctx.lvalue().Identifier().getText();
            return new VariableNode(varName);
        }

        //Parenthesized Expression
        if (ctx.LPAREN() != null && ctx.expression() != null){
            return buildExpression(ctx.expression());
        }
        return null;
    }

    private ExpressionNode buildCallExpression(MiniCParser.CallExpressionContext ctx){
        String functionName = ctx.Identifier().getText();
            List<ExpressionNode> arguments = new ArrayList<>();
            if (ctx.argumentList() != null){
                for (MiniCParser.ExpressionContext argCtx : ctx.argumentList().expression()) {
                    arguments.add(buildExpression(argCtx));
                }
            }
        return new FunctionCallNode(functionName, arguments);
    }

    private ExpressionNode buildLogicalOrExpression(MiniCParser.LogicalOrExpressionContext ctx) {
        if (ctx.logicalAndExpression().size() == 1) {
            return buildLogicalAndExpression(ctx.logicalAndExpression(0));
        }

        // Aquí podrías manejar la construcción de nodos para expresiones lógicas "OR"
        ExpressionNode left = buildLogicalAndExpression(ctx.logicalAndExpression(0));
        for (int i = 1; i < ctx.logicalAndExpression().size(); i++) {
            ExpressionNode right = buildLogicalAndExpression(ctx.logicalAndExpression(i));
            left = new BinaryOpNode("||", left, right);
        }
        return left;
    }

    private ExpressionNode buildLogicalAndExpression(MiniCParser.LogicalAndExpressionContext ctx) {
        if (ctx.equalityExpression().size() == 1) {
            return buildEqualityExpression(ctx.equalityExpression(0));
        }
        ExpressionNode left = buildEqualityExpression(ctx.equalityExpression(0));
        for (int i = 1; i < ctx.equalityExpression().size(); i++) {
            ExpressionNode right = buildEqualityExpression(ctx.equalityExpression(i));
            left = new BinaryOpNode("&&", left, right);
        }
        return left;
    }

    private ExpressionNode buildEqualityExpression(MiniCParser.EqualityExpressionContext ctx) {
        if (ctx.relationalExpression().size() == 1) {
            return buildRelationalExpression(ctx.relationalExpression(0));
        }

        ExpressionNode left = buildRelationalExpression(ctx.relationalExpression(0));
        for (int i = 1; i < ctx.relationalExpression().size(); i++) {
            ExpressionNode right = buildRelationalExpression(ctx.relationalExpression(i));
            String operator = ctx.getChild(2 * i - 1).getText(); // Obtener el operador (== o !=)
            left = new BinaryOpNode(operator, left, right);
        }
        return left;
    }

    private ExpressionNode buildRelationalExpression(MiniCParser.RelationalExpressionContext ctx) {
        if (ctx.additiveExpression().size() == 1) {
            return buildAdditiveExpression(ctx.additiveExpression(0));
        }

        //Manejas expreciones relacionales (<,>,<=,>=)
        ExpressionNode left = buildAdditiveExpression(ctx.additiveExpression(0));
        for (int i=1; i< ctx.additiveExpression().size(); i++){
            String operator = ctx.getChild(2*i-1).getText();
            ExpressionNode right = buildAdditiveExpression(ctx.additiveExpression(i));
            left = new BinaryOpNode(operator, left, right);
        }
        return left;
    }

    private ExpressionNode buildAdditiveExpression(MiniCParser.AdditiveExpressionContext ctx){
        if (ctx.multiplicativeExpression().size() == 1) {
            return buildMultiplicativeExpression(ctx.multiplicativeExpression(0));
        }

        //Manejar +,*
        ExpressionNode left = buildMultiplicativeExpression(ctx.multiplicativeExpression(0));
        for(int i=1; i<ctx.multiplicativeExpression().size(); i++){
            String operator = ctx.getChild(2*i-1).getText();
            ExpressionNode right = buildMultiplicativeExpression(ctx.multiplicativeExpression(i));
            left=new BinaryOpNode(operator, left, right);
        }
        return left;
    }

    private ExpressionNode buildMultiplicativeExpression(MiniCParser.MultiplicativeExpressionContext ctx){
        if (ctx.unaryExpression().size() == 1) {
            return buildUnaryExpression(ctx.unaryExpression(0));
        }

        //Manejar *,/,%
        ExpressionNode left = buildUnaryExpression(ctx.unaryExpression(0));
        for(int i=1; i<ctx.unaryExpression().size(); i++){
            String operator = ctx.getChild(2*i-1).getText();
            ExpressionNode right = buildUnaryExpression(ctx.unaryExpression(i));
            left = new BinaryOpNode(operator, left, right);
        }
        return left;
    }

    private ExpressionNode buildUnaryExpression(MiniCParser.UnaryExpressionContext ctx){
        //Expresion primaria, sin operador unario
        if (ctx.postfixExpression() != null) {
            return buildPostFixExpression(ctx.postfixExpression());
        }

        //Manejar !,-,&,*
        String operator = ctx.getChild(0).getText();
        ExpressionNode operand = buildUnaryExpression(ctx.unaryExpression());
        return new UnaryOpNode(operator, operand);
    }

    private ExpressionNode buildPostFixExpression(MiniCParser.PostfixExpressionContext ctx){
        if (ctx.primaryExpression() != null) {
            return buildPrimaryExpression(ctx.primaryExpression());
        }
        if (ctx.callExpression() != null) {
            return buildCallExpression(ctx.callExpression());
        }
        return null;
    }

    @Override
    public void enterWhileStatement(MiniCParser.WhileStatementContext ctx){
        ExpressionNode condition = buildExpression(ctx.expression());
        WhileNode whileNode = new WhileNode(condition, null);
        statementListStack.peek().add(whileNode);
        nodeStack.push(whileNode);
        statementListStack.push(new ArrayList<>());
    }

    @Override
    public void exitWhileStatement(MiniCParser.WhileStatementContext ctx){
        List<StatementNode> bodyStatements = statementListStack.pop();
        WhileNode whileNode = (WhileNode) nodeStack.pop();

        StatementNode body = bodyStatements.size() == 1 ? bodyStatements.get(0) : new BlockNode(bodyStatements);

        WhileNode updateWhileNode = new WhileNode(whileNode.getCondition(), body);

        List<StatementNode> currentStatements = statementListStack.peek();
        currentStatements.set(currentStatements.size()-1, updateWhileNode);
    }

    @Override
    public void enterEveryRule(org.antlr.v4.runtime.ParserRuleContext ctx) {
        //System.out.println("Entering: " + ctx.getClass().getSimpleName());
    }
}
