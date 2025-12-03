package org.minic.ast;

import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.minic.MiniCBaseListener;
import org.minic.MiniCParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/*Recorre el arbol generado por ANTLR y construir el AST correspondiente */

public class AstBuilder extends MiniCBaseListener {
    //Nodo raiz del AST
    private AstNode root;
    //Pila para manejar los nodos con sub-estructuras (funciones, if, loops, etc)
    private Stack<AstNode> nodeStack = new Stack<>();
    //Pilas que mantienen las listas de statement segun el nivel del bloque
    private Stack<List<StatementNode>> statementListStack = new Stack<>();

    //Construye el AST recorriendo el parse tree
    public AstNode build(org.antlr.v4.runtime.tree.ParseTree tree) {
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, tree);
        return root;
    }

    //Inicia el programa, se crea ProgramNode y se coloca en la pila
    @Override
    public void enterProgram(MiniCParser.ProgramContext ctx) {
        ProgramNode programNode = new ProgramNode();
        nodeStack.push(programNode);
    }

    //Cuando el programa fiinaliza se extrae el nodo y se marca como root
    @Override
    public void exitProgram(MiniCParser.ProgramContext ctx) {
        ProgramNode programNode = (ProgramNode) nodeStack.pop();
        root = programNode;
    }

    //Maneja declaraciones globales y locales (variables simples, arreglos, inicializaciones)
    @Override
    public void enterDeclaration(MiniCParser.DeclarationContext ctx) {
        System.out.println("=== AST BUILDER: Procesando declaración ===");
        
        //Es una declaración de función (prototipo)
        if (ctx.functionDeclaration() != null) {
            handleFunctionDeclaration(ctx.functionDeclaration());
            return;
        }
        
        //Es una declaración de variable normal
        handleVariableDeclaration(ctx);
    }

    private void handleFunctionDeclaration(MiniCParser.FunctionDeclarationContext funcDeclCtx) {
        String returnType = funcDeclCtx.typeSpecifier().getText();
        String funcName = funcDeclCtx.Identifier().getText();
        
        System.out.println("AST BUILDER: Prototipo de función: " + returnType + " " + funcName + "()");
        
        // Crear lista de parámetros (si los hay)
        List<VarDeclNode> parameters = null;
        if (funcDeclCtx.parameterList() != null) {
            parameters = new ArrayList<>();
            for (MiniCParser.ParameterContext paramCtx : funcDeclCtx.parameterList().parameter()) {
                String paramType = paramCtx.typeSpecifier().getText();
                String paramName = paramCtx.Identifier().getText();
                parameters.add(new VarDeclNode(paramType, paramName, false, 0, null));
            }
        }
        
        // Crear nodo de función (sin cuerpo)
        FunctionNode funcNode = new FunctionNode(returnType, funcName, parameters, null);
        
        // Agregar como declaración global al programa
        AstNode currentNode = nodeStack.peek();
        if (currentNode instanceof ProgramNode) {
            ProgramNode programNode = (ProgramNode) currentNode;
            programNode.addDeclarationNode(funcNode);
            System.out.println("AST BUILDER: Agregado prototipo de función GLOBAL: " + returnType + " " + funcName);
        }
    }

    private void handleVariableDeclaration(MiniCParser.DeclarationContext ctx) {
        // Verificar que existe typeSpecifier
        MiniCParser.TypeSpecifierContext typeCtx = ctx.typeSpecifier();
        if (typeCtx == null) {
            System.err.println("AST BUILDER: Error: typeSpecifier es null para declaración de variable");
            return;
        }
        
        String varType = typeCtx.getText();
        System.out.println("AST BUILDER: Tipo: " + varType);
        
        // Procesar declaraciones múltiples en la misma línea
        for (MiniCParser.InitDeclaratorContext initDeclCtx : ctx.initDeclaratorList().initDeclarator()) {
            String varName = initDeclCtx.Identifier().getText();
            boolean isArray = initDeclCtx.LBRACK() != null && !initDeclCtx.LBRACK().isEmpty();
            int arraySize = 0;
            ExpressionNode initialNode = null;
            
            // Tamaño del arreglo
            if (isArray && initDeclCtx.IntegerConstant() != null && !initDeclCtx.IntegerConstant().isEmpty()) {
                arraySize = Integer.parseInt(initDeclCtx.IntegerConstant(0).getText());
            }
            
            // Inicialización si existe
            if (initDeclCtx.ASSIGN() != null && initDeclCtx.expression() != null) {
                initialNode = buildExpression(initDeclCtx.expression());
                System.out.println("AST BUILDER: Tiene inicialización: " + varName + " = " + initialNode);
            }

            // Crear el nodo de declaración
            VarDeclNode varDeclNode = new VarDeclNode(varType, varName, isArray, arraySize, initialNode);
            System.out.println("AST BUILDER: Creando declaración: " + varType + " " + varName);
            AstNode currentNode = nodeStack.peek();
            
            // Declaraciones globales
            if (currentNode instanceof ProgramNode) {
                ProgramNode programNode = (ProgramNode) currentNode;
                programNode.addDeclarationNode(varDeclNode);
                System.out.println("AST BUILDER: Agregada declaración GLOBAL: " + varName);
            // Declaraciones dentro de bloques o funciones    
            } else if (currentNode instanceof FunctionNode) {
                if (!statementListStack.isEmpty()) {
                    VarDeclStatementNode declStmt = new VarDeclStatementNode(varDeclNode);
                    statementListStack.peek().add(declStmt);
                    System.out.println("AST BUILDER: Agregada declaración LOCAL: " + varType + " " + varName);
                }
            }
        }
        System.out.println("=== FIN declaración ===");
    }
    
    //Construye un FunctionNode juntos con su lista de parametros
    @Override
    public void enterFunctionDefinition(MiniCParser.FunctionDefinitionContext ctx) {
        String returnType = ctx.typeSpecifier().getText();
        String functionName = ctx.Identifier().getText();
        List<VarDeclNode> parameters = new ArrayList<>();
        //Si existen parametros se procesan
        if (ctx.parameterList() != null) {
            for (MiniCParser.ParameterContext paramCtx : ctx.parameterList().parameter()) {
                String paramType = paramCtx.typeSpecifier().getText();
                String paramName = paramCtx.Identifier().getText();
                parameters.add(new VarDeclNode(paramType, paramName));
            }
        }
        FunctionNode functionNode = new FunctionNode(returnType, functionName, parameters, null);
        //Agregar la funcion como declaracion global
        if (!nodeStack.isEmpty() && nodeStack.peek() instanceof ProgramNode) {
            ProgramNode programNode = (ProgramNode) nodeStack.peek();
            programNode.addDeclarationNode(functionNode);
        }
        nodeStack.push(functionNode);
        statementListStack.push(new ArrayList<>());
    }

    //Al salir de la funcion se arma el BlockNode del cuerpo
    @Override
    public void exitFunctionDefinition(MiniCParser.FunctionDefinitionContext ctx) {
        List<StatementNode> bodyStatements = statementListStack.pop();
        BlockNode body = new BlockNode();
        
        System.out.println("AST BUILDER: Cuerpo de función tiene " + bodyStatements.size() + " statements:");
        for (StatementNode stmt : bodyStatements) {
            System.out.println(" - Statement: " + stmt.getClass().getSimpleName());
            if (stmt instanceof VarDeclStatementNode) {
                VarDeclStatementNode declStmt = (VarDeclStatementNode) stmt;
                System.out.println("   - VarDecl: " + declStmt.getVarDeclNode().getType() + " " + 
                                declStmt.getVarDeclNode().getName() + " = " + 
                                (declStmt.getVarDeclNode().hasInitialNode() ? "Sí" : "No"));
            }
        }
        
        for (StatementNode stmt : bodyStatements) {
            body.addStatement(stmt);
            System.out.println("AST BUILDER: Añadiendo statement al BlockNode: " + stmt.getClass().getSimpleName());
        }
        
        FunctionNode functionNode = (FunctionNode) nodeStack.pop();
        functionNode.setBody(body);
        System.out.println("AST BUILDER: BlockNode creado con " + body.getStatements().size() + " statements");
    }

    //Nueva lista de statements para un bloque {}
    @Override
    public void enterCompoundStatement(MiniCParser.CompoundStatementContext ctx) {
        statementListStack.push(new ArrayList<>());
    }

    //Combierte la lista de statements en un BlockNode y, si hay un bloque anterior existente, lo agrega
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

    //--------------------------Manejo de If-------------------------------
    @Override
    public void enterIfStatement(MiniCParser.IfStatementContext ctx) {
        ExpressionNode condition = buildExpression(ctx.expression());
        IfNode ifNode = new IfNode(condition, null, null);
        statementListStack.peek().add(ifNode);
        nodeStack.push(ifNode);
        //Lista de statements en el bloque THEN
        statementListStack.push(new ArrayList<>());
    }

    @Override
    public void exitIfStatement(MiniCParser.IfStatementContext ctx) {
        List<StatementNode> thenStatements = statementListStack.pop();
        IfNode ifNode = (IfNode) nodeStack.pop();
        StatementNode thenBlock = null;
        if(!thenStatements.isEmpty()){
            thenBlock = thenStatements.size() == 1 ? thenStatements.get(0) : new BlockNode(thenStatements);
        }
        StatementNode elseBlock = null;
        //Si existe un ELSE, se obtiene el ultimo bloque a nivel actual
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

        //Reemplaza el IfNode incompleto con uno completo
        IfNode updatedIfNode = new IfNode(ifNode.getCondition(), (BlockNode) thenBlock, (BlockNode) elseBlock);
        List<StatementNode> currentStatements = statementListStack.peek();
        currentStatements.set(currentStatements.size()-1, updatedIfNode);
    }

    //--------------------------Manejo de While-------------------------------
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

    //--------------------------Manejo de Do While-------------------------------
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

    //--------------------------Manejo de For-------------------------------
    @Override
    public void enterForStatement(MiniCParser.ForStatementContext ctx){
        StatementNode iniNode = null;
        if (ctx.forInit().expressionStatement() !=null && ctx.forInit().expressionStatement().expression() != null) {
            iniNode = new ExpressionStatementNode(buildExpression(ctx.forInit().expressionStatement().expression()));
        } else if (ctx.forInit().declaration() != null) {
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
    public void enterEveryRule(org.antlr.v4.runtime.ParserRuleContext ctx) {
        //System.out.println("Entering: " + ctx.getClass().getSimpleName());
    }
}
