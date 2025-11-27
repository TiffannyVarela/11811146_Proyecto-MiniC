package org.minic.semantic;

public class Type {
    
    public static final String INT = "int";
    public static final String CHAR = "char";
    public static final String STRING = "string";
    public static final String BOOLEAN = "boolean";
    public static final String VOID = "void";

    public static boolean isCompatible(String type1, String type2) {
        if (type1.equals(type2)) {
            return true;
        }
        // Agregar reglas de compatibilidad adicionales si es necesario
        return false;
    }

    public static boolean isNumeric(String type) {
        return type.equals(INT);
    }

}
