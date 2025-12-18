package org.minic;

import java.util.*;

public class ErrorManager {
    private static final List<CompilationException> errors = new ArrayList<>();
    private static boolean verboseMode = true;
    private static String sourceText = "";

    public static void setVerboseMode(boolean verbose) {
        verboseMode = verbose;
    }

    public static void setSourceText(String text) {
        sourceText = text;
        if (!text.isEmpty()) {
            String[] lines = text.split("\r?\n", -1);
            for (int i = 0; i < Math.min(lines.length, 5); i++) {
            }
        }
    }

    public static void addError(CompilationException error) {
        errors.add(error);
    }

    public static void addError(String message) {
        errors.add(new CompilationException(message));
    }

    public static void addError(int line, int column, String message) {
        if (line <= 0)
            line = 1;
        if (column <= 0)
            column = 1;

        errors.add(new CompilationException(line, column, message));
    }

    public static void throwIfErrors() {
        if (!errors.isEmpty()) {
            printErrors();
            throw new CompilationException("Compilacion fallida con " + errors.size() + " error/es");
        }
    }

    public static void printErrors() {
        if (errors.isEmpty()) {
            return;
        }

        if (verboseMode) {
            System.err.println("\n=== ERRORES DE COMPILACION ===");
        }

        for (CompilationException error : errors) {
            if (verboseMode && error.getLine() > 0 && error.getColumn() > 0) {
                showErrorWithContext(error);
            } else {
                System.err.println(error.getMessage());
            }
        }

        if (verboseMode) {
            System.err.println("===================================");
        }
    }

    private static void showErrorWithContext(CompilationException error) {
        int line = error.getLine();
        int column = error.getColumn();

        if (sourceText == null || sourceText.isEmpty()) {
            System.err.println(error.getMessage());
            return;
        }

        // Dividir por líneas
        String[] lines = sourceText.split("\r?\n", -1);

        if (line > 0 && line <= lines.length) {
            String errorLine = lines[line - 1];

            System.err.println("Línea " + line + ", Columna " + column + ": " + error.getRawMessage());
            System.err.println("  " + errorLine.replace("\t", "    "));

            int pointerPos = Math.min(column - 1, errorLine.length());

            // Mostrar puntero
            StringBuilder pointer = new StringBuilder("  ");
            for (int i = 0; i < pointerPos; i++) {
                pointer.append(" ");
            }
            pointer.append("^");
            System.err.println(pointer.toString());

        } else {
            // Línea fuera de rango
            System.err.println(error.getMessage());
        }
    }

    public static boolean hasErrors() {
        return !errors.isEmpty();
    }

    public static void cleanErrors() {
        errors.clear();
        sourceText = "";
    }

    public static List<CompilationException> getErrors() {
        return new ArrayList<>(errors);
    }
}