// Generated from c:/Users/tiffa/OneDrive/Documentos/Compiladores II/11811146_Proyecto-MiniC/grammar/MiniC.g4 by ANTLR 4.13.1

package org.minic;

import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link MiniC}.
 */
public interface MiniCListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link MiniC#program}.
	 * @param ctx the parse tree
	 */
	void enterProgram(MiniC.ProgramContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#program}.
	 * @param ctx the parse tree
	 */
	void exitProgram(MiniC.ProgramContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#declaration}.
	 * @param ctx the parse tree
	 */
	void enterDeclaration(MiniC.DeclarationContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#declaration}.
	 * @param ctx the parse tree
	 */
	void exitDeclaration(MiniC.DeclarationContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#initDeclaratorList}.
	 * @param ctx the parse tree
	 */
	void enterInitDeclaratorList(MiniC.InitDeclaratorListContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#initDeclaratorList}.
	 * @param ctx the parse tree
	 */
	void exitInitDeclaratorList(MiniC.InitDeclaratorListContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#initDeclarator}.
	 * @param ctx the parse tree
	 */
	void enterInitDeclarator(MiniC.InitDeclaratorContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#initDeclarator}.
	 * @param ctx the parse tree
	 */
	void exitInitDeclarator(MiniC.InitDeclaratorContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#typeSpecifier}.
	 * @param ctx the parse tree
	 */
	void enterTypeSpecifier(MiniC.TypeSpecifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#typeSpecifier}.
	 * @param ctx the parse tree
	 */
	void exitTypeSpecifier(MiniC.TypeSpecifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#functionDefinition}.
	 * @param ctx the parse tree
	 */
	void enterFunctionDefinition(MiniC.FunctionDefinitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#functionDefinition}.
	 * @param ctx the parse tree
	 */
	void exitFunctionDefinition(MiniC.FunctionDefinitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#parameterList}.
	 * @param ctx the parse tree
	 */
	void enterParameterList(MiniC.ParameterListContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#parameterList}.
	 * @param ctx the parse tree
	 */
	void exitParameterList(MiniC.ParameterListContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#parameter}.
	 * @param ctx the parse tree
	 */
	void enterParameter(MiniC.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#parameter}.
	 * @param ctx the parse tree
	 */
	void exitParameter(MiniC.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#compoundStatement}.
	 * @param ctx the parse tree
	 */
	void enterCompoundStatement(MiniC.CompoundStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#compoundStatement}.
	 * @param ctx the parse tree
	 */
	void exitCompoundStatement(MiniC.CompoundStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(MiniC.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(MiniC.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#ifStatement}.
	 * @param ctx the parse tree
	 */
	void enterIfStatement(MiniC.IfStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#ifStatement}.
	 * @param ctx the parse tree
	 */
	void exitIfStatement(MiniC.IfStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#whileStatement}.
	 * @param ctx the parse tree
	 */
	void enterWhileStatement(MiniC.WhileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#whileStatement}.
	 * @param ctx the parse tree
	 */
	void exitWhileStatement(MiniC.WhileStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#forStatement}.
	 * @param ctx the parse tree
	 */
	void enterForStatement(MiniC.ForStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#forStatement}.
	 * @param ctx the parse tree
	 */
	void exitForStatement(MiniC.ForStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#doWhileStatement}.
	 * @param ctx the parse tree
	 */
	void enterDoWhileStatement(MiniC.DoWhileStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#doWhileStatement}.
	 * @param ctx the parse tree
	 */
	void exitDoWhileStatement(MiniC.DoWhileStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#assignmentStatement}.
	 * @param ctx the parse tree
	 */
	void enterAssignmentStatement(MiniC.AssignmentStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#assignmentStatement}.
	 * @param ctx the parse tree
	 */
	void exitAssignmentStatement(MiniC.AssignmentStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#returnStatement}.
	 * @param ctx the parse tree
	 */
	void enterReturnStatement(MiniC.ReturnStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#returnStatement}.
	 * @param ctx the parse tree
	 */
	void exitReturnStatement(MiniC.ReturnStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void enterExpressionStatement(MiniC.ExpressionStatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#expressionStatement}.
	 * @param ctx the parse tree
	 */
	void exitExpressionStatement(MiniC.ExpressionStatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(MiniC.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(MiniC.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#logicalOrExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalOrExpression(MiniC.LogicalOrExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#logicalOrExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalOrExpression(MiniC.LogicalOrExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#logicalAndExpression}.
	 * @param ctx the parse tree
	 */
	void enterLogicalAndExpression(MiniC.LogicalAndExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#logicalAndExpression}.
	 * @param ctx the parse tree
	 */
	void exitLogicalAndExpression(MiniC.LogicalAndExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void enterEqualityExpression(MiniC.EqualityExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#equalityExpression}.
	 * @param ctx the parse tree
	 */
	void exitEqualityExpression(MiniC.EqualityExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void enterRelationalExpression(MiniC.RelationalExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#relationalExpression}.
	 * @param ctx the parse tree
	 */
	void exitRelationalExpression(MiniC.RelationalExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void enterAdditiveExpression(MiniC.AdditiveExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#additiveExpression}.
	 * @param ctx the parse tree
	 */
	void exitAdditiveExpression(MiniC.AdditiveExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicativeExpression(MiniC.MultiplicativeExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#multiplicativeExpression}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicativeExpression(MiniC.MultiplicativeExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterUnaryExpression(MiniC.UnaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#unaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitUnaryExpression(MiniC.UnaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void enterPrimaryExpression(MiniC.PrimaryExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#primaryExpression}.
	 * @param ctx the parse tree
	 */
	void exitPrimaryExpression(MiniC.PrimaryExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#callExpression}.
	 * @param ctx the parse tree
	 */
	void enterCallExpression(MiniC.CallExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#callExpression}.
	 * @param ctx the parse tree
	 */
	void exitCallExpression(MiniC.CallExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link MiniC#lvalue}.
	 * @param ctx the parse tree
	 */
	void enterLvalue(MiniC.LvalueContext ctx);
	/**
	 * Exit a parse tree produced by {@link MiniC#lvalue}.
	 * @param ctx the parse tree
	 */
	void exitLvalue(MiniC.LvalueContext ctx);
}