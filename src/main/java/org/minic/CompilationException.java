package org.minic;

public class CompilationException extends RuntimeException {
    private int line = -1;
    private int column = -1;
    private String rawMessage;
    
    public CompilationException(String message) {
        super(message);
        this.rawMessage = message;
    }
    
    public CompilationException(String message, Throwable cause) {
        super(message, cause);
        this.rawMessage = message;
    }
    
    public CompilationException(int line, int column, String message) {
        super("Línea " + line + ", Columna " + column + ": " + message);
        this.line = line;
        this.column = column;
        this.rawMessage = message;
    }
    
    public int getLine() {
        return line;
    }
    
    public int getColumn() {
        return column;
    }
    
    // ESTE MÉTODO ES CRÍTICO
    public String getRawMessage() {
        return rawMessage;
    }
}