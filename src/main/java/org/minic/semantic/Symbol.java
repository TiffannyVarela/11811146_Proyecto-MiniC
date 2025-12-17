package org.minic.semantic;

import java.util.List;

public class Symbol {
    private final String name;
    private final String type;
    private List<String> paramTypes;
    private final boolean isFunction;

    public Symbol(String name, String type, boolean isFunction) {
        this.name = name;
        this.type = type;
        this.isFunction = isFunction;
    }

    public Symbol(String name, String type, List<String> paramTypes, boolean isFunction) {
        this.name = name;
        this.type = type;
        this.paramTypes = paramTypes;
        this.isFunction = isFunction;
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

    public List<String> getParamTypes() {
        return paramTypes;
    }

    public void setParamTypes(List<String> paramTypes) {
        this.paramTypes = paramTypes;
    }
    
}
