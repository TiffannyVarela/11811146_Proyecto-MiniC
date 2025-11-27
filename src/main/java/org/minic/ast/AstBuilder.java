package org.minic.ast;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.minic.MiniCBaseListener;
import org.minic.MiniC;

import java.util.ArrayList;
import java.util.List;

public class AstBuilder extends MiniCBaseListener {
    private AstNode ast;
    private List<DeclarationNode> declarations = new ArrayList<>();
    private List<StatementNode> currentstatements = new ArrayList<>();
    private List<VarDeclNode> currentVarDecls = new ArrayList<>();

    public AstNode build(org.antlr.v4.runtime.tree.ParseTree tree) {
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(this, tree);
        return ast;
    }

    @Override
    public void enterProgram(MiniC.ProgramContext ctx) {
        declarations = new ArrayList<>();
    }

    @Override
    public void exitProgram(MiniC.ProgramContext ctx) {
        ast = new ProgramNode(declarations);
    }

    @Override
    public void enterFunctionDecl(MiniC.FunctionDeclContext ctx) {
        currentVarDecls = new ArrayList<>();
    }

    @Override
    public void exitFunctionDecl(MiniC.FunctionDeclContext ctx) {
        String returnType = ctx.type().getText();
        String functionName = ctx.ID().getText();
        //Construir el cuerpo de la función
        BlockNode body = buildBlock(ctx.block());

        //Crear el nodo de función
        FunctionNode functionNode = new FunctionNode(returnType, functionName, new ArrayList<>(currentVarDecls), body);
        declarations.add(functionNode);
        currentVarDecls = new ArrayList<>();
    }
        @Override
        public void enterParam(MiniC.ParamContext ctx) {
            String type = ctx.type().getText();
            String name = ctx.ID().getText();
            VarDeclNode paramNode = new VarDeclNode(type, name, null);
            currentVarDecls.add(paramNode);
        }

        @Override
        public void enterVarDecl(MiniC.VarDeclContext ctx) {
            String type = ctx.type().getText();
            String name = ctx.ID().getText();
            ExpressionNode initialValue = null;
            if (ctx.expression() != null) {
                // Aquí deberías convertir ctx.expression() en un ExpressionNode
                // Por simplicidad, asumimos que ya tienes un método para hacerlo
                initialValue = buildExpression(ctx.expression());
            }
            VarDeclNode varDeclNode = new VarDeclNode(type, name, initialValue);
            currentVarDecls.add(varDeclNode);
    }
    
    private BlockNode buildBlock(MiniC.BlockContext ctx) {
        if (ctx == null) {
            return null;
        }
        List<StatementNode> statements = new ArrayList<>();
        for (MiniC.StatementContext stmtCtx : ctx.statement()) {
            // Aquí deberías convertir cada stmtCtx en un StatementNode
            // Por simplicidad, asumimos que ya tienes un método para hacerlo
            //StatementNode stmtNode = buildStatement(stmtCtx);
            //statements.add(stmtNode);
        }
        return new BlockNode(statements);
    }

    private ExpressionNode buildExpression(MiniC.ExpressionContext ctx) {
        // Implementa la lógica para convertir ctx en un ExpressionNode
        return null; // Placeholder
    }

    private StatementNode buildStatement(MiniC.StatementContext ctx) {
        // Implementa la lógica para convertir ctx en un StatementNode
        return null; // Placeholder
    }
}
