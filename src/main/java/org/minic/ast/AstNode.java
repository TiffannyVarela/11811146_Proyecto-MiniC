package org.minic.ast;
public abstract class AstNode {
    private int line;
    private int column;

    public AstNode() {
    }

    public AstNode(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public abstract <T> T accept(AstVisitor<T> visitor);

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public void setColumn(int column) {
        this.column = column;
    }
}