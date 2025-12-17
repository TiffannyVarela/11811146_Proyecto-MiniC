// Generated from c:/Users/tiffa/OneDrive/Documentos/Compiladores II/11811146_Proyecto-MiniC/grammar/MiniC.g4 by ANTLR 4.13.1

package org.minic;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link MiniCParser}.
 */
public interface MiniCListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link MiniCParser#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(MiniCParser.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(MiniCParser.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#declaration}.
	 * @param ctx the parse tree
	 */
	void enterDeclaration(MiniCParser.DeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#declaration}.
	 * @param ctx the parse tree
	 */
	void exitDeclaration(MiniCParser.DeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#initDeclaratorList}.
	 * @param ctx the parse tree
	 */
	void enterInitDeclaratorList(MiniCParser.InitDeclaratorListContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#initDeclaratorList}.
	 * @param ctx the parse tree
	 */
	void exitInitDeclaratorList(MiniCParser.InitDeclaratorListContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#initDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterInitDeclarator(MiniCParser.InitDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#initDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitInitDeclarator(MiniCParser.InitDeclaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#typeSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterTypeSpecifier(MiniCParser.TypeSpecifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#typeSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitTypeSpecifier(MiniCParser.TypeSpecifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#functionDeclaration}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDeclaration(MiniCParser.FunctionDeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#functionDeclaration}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDeclaration(MiniCParser.FunctionDeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#functionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDefinition(MiniCParser.FunctionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#functionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDefinition(MiniCParser.FunctionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#parameterList}.
	 * @param ctx the parse tree
	 */
	void enterParameterList(MiniCParser.ParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#parameterList}.
	 * @param ctx the parse tree
	 */
	void exitParameterList(MiniCParser.ParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#parameter}.
	 * @param ctx the parse tree
	 */
	void enterParameter(MiniCParser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#parameter}.
	 * @param ctx the parse tree
	 */
	void exitParameter(MiniCParser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#compoundStatement}.
	 * @param ctx the parse tree
	 */
	void enterCompoundStatement(MiniCParser.CompoundStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#compoundStatement}.
	 * @param ctx the parse tree
	 */
	void exitCompoundStatement(MiniCParser.CompoundStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(MiniCParser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(MiniCParser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfStatement(MiniCParser.IfStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#ifStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfStatement(MiniCParser.IfStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void enterWhileStatement(MiniCParser.WhileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#whileStatement}.
	 * @param ctx the parse tree
	 */
	void exitWhileStatement(MiniCParser.WhileStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#forStatement}.
	 * @param ctx the parse tree
	 */
	void enterForStatement(MiniCParser.ForStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#forStatement}.
	 * @param ctx the parse tree
	 */
	void exitForStatement(MiniCParser.ForStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#forInit}.
	 * @param ctx the parse tree
	 */
	void enterForInit(MiniCParser.ForInitContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#forInit}.
	 * @param ctx the parse tree
	 */
	void exitForInit(MiniCParser.ForInitContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void enterForUpdate(MiniCParser.ForUpdateContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#forUpdate}.
	 * @param ctx the parse tree
	 */
	void exitForUpdate(MiniCParser.ForUpdateContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#doWhileStatement}.
	 * @param ctx the parse tree
	 */
	void enterDoWhileStatement(MiniCParser.DoWhileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#doWhileStatement}.
	 * @param ctx the parse tree
	 */
	void exitDoWhileStatement(MiniCParser.DoWhileStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#assignmentStatement}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentStatement(MiniCParser.AssignmentStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#assignmentStatement}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentStatement(MiniCParser.AssignmentStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void enterReturnStatement(MiniCParser.ReturnStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#returnStatement}.
	 * @param ctx the parse tree
	 */
	void exitReturnStatement(MiniCParser.ReturnStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void enterExpressionStatement(MiniCParser.ExpressionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void exitExpressionStatement(MiniCParser.ExpressionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(MiniCParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(MiniCParser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalOrExpression(MiniCParser.LogicalOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#logicalOrExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalOrExpression(MiniCParser.LogicalOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalAndExpression(MiniCParser.LogicalAndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#logicalAndExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalAndExpression(MiniCParser.LogicalAndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void enterEqualityExpression(MiniCParser.EqualityExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void exitEqualityExpression(MiniCParser.EqualityExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterRelationalExpression(MiniCParser.RelationalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitRelationalExpression(MiniCParser.RelationalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveExpression(MiniCParser.AdditiveExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveExpression(MiniCParser.AdditiveExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicativeExpression(MiniCParser.MultiplicativeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicativeExpression(MiniCParser.MultiplicativeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpression(MiniCParser.UnaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpression(MiniCParser.UnaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void enterPostfixExpression(MiniCParser.PostfixExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#postfixExpression}.
	 * @param ctx the parse tree
	 */
	void exitPostfixExpression(MiniCParser.PostfixExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryExpression(MiniCParser.PrimaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryExpression(MiniCParser.PrimaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#callExpression}.
	 * @param ctx the parse tree
	 */
	void enterCallExpression(MiniCParser.CallExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#callExpression}.
	 * @param ctx the parse tree
	 */
	void exitCallExpression(MiniCParser.CallExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void enterArgumentList(MiniCParser.ArgumentListContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#argumentList}.
	 * @param ctx the parse tree
	 */
	void exitArgumentList(MiniCParser.ArgumentListContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniCParser#lvalue}.
	 * @param ctx the parse tree
	 */
	void enterLvalue(MiniCParser.LvalueContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniCParser#lvalue}.
	 * @param ctx the parse tree
	 */
	void exitLvalue(MiniCParser.LvalueContext ctx);
}