package org.minic;

import java.util.*;

public class ErrorManager {
    private static final List<CompilationException> errors = new ArrayList<>();

    public static void addError(CompilationException error){
        errors.add(error);
    }

    public static void addError(String message){
        errors.add(new CompilationException(message));
    }

    public static void addError(int line, int column, String message) {
        errors.add(new CompilationException(line, column, message));
    }

    public static void throwIfErrors(){
        if (!errors.isEmpty()) {
            printErrors();
            throw new CompilationException("Compilacion fallida con " + errors.size() + " error/es");
        }
    }

    public static void printErrors(){
        if (errors.isEmpty()) {
            return;
        }

        System.err.println("\n=== ERRORES DE COMPILACION ===");
        for(CompilationException error : errors){
            System.err.println(error.getMessage());
        }
        System.err.println("===================================");
    }

    public static boolean hasErrors(){
        return !errors.isEmpty();
    }

    public static void cleanErrors() {
        errors.clear();
    }

    public static List<CompilationException> getErrors() {
        return new ArrayList<>(errors);
    }
    
}
