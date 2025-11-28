package org.minic.semantic;

public class Type {
    
    public static final String INT = "int";
    public static final String CHAR = "char";
    public static final String STRING = "string";
    public static final String BOOLEAN = "bool";
    public static final String VOID = "void";

    public static boolean isCompatible(String type1, String type2) {
        if (type1.equals(type2)) {
            return true;
        }
 
        if ((type1.equals(INT) && type2.equals(CHAR)) || 
            (type1.equals(CHAR) && type2.equals(INT))) {
            return true;
        }
 
        if (isPointerType(type1) && isPointerType(type2)) {
            return getBaseType(type1).equals(getBaseType(type2));
        }
        
        return false;
    }

    public static boolean isNumeric(String type) {
        return type.equals(INT) || type.equals(CHAR);
    }
    
    public static boolean isPointerType(String type) {
        return type != null && type.endsWith("*");
    }
    
    public static String getBaseType(String pointerType) {
        if (isPointerType(pointerType)) {
            return pointerType.substring(0, pointerType.length() - 1);
        }
        return pointerType;
    }
    
    public static String getPointerType(String baseType) {
        return baseType + "*";
    }
    
    public static boolean isArrayType(String type) {
        return type != null && type.contains("[");
    }
}