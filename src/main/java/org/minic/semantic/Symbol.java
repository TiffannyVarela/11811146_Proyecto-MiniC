package org.minic.semantic;

import java.util.List;

public class Symbol {

    private final String name;
    private final String type;
    private final boolean isFunction;
    private final boolean isArray;
    private List<String> paramTypes;

    public Symbol(String name, String type, boolean isFunction) {
        this.name = name;
        this.type = type;
        this.isFunction = isFunction;
        this.isArray = type != null && type.contains("[");
    }
    
    public Symbol(String name, String type, List<String> paramTypes, boolean isFunction) {
        this.name = name;
        this.type = type;
        this.paramTypes = paramTypes;
        this.isFunction = isFunction;
        this.isArray = false;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isFunction() {
        return isFunction;
    }

    public boolean isArray() {
        return isArray;
    }

    public List<String> getParamTypes() {
        return paramTypes;
    }
}
