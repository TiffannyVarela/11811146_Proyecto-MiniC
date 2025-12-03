package org.minic;

public class CompilationException extends RuntimeException {
    private int line = 0;
    private int column = 0;
    

    public CompilationException(String message) {
        super(message);
    }
    
    public CompilationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CompilationException(int line, int column, String message){
        super(String.format("Linea %d, Columna %d: %s", line, column, message));
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }
}
