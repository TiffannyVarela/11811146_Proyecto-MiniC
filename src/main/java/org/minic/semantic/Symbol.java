package org.minic.semantic;

public class Symbol {
    private final String name;
    private final String type;
    private final boolean isFunction;

    public Symbol(String name, String type, boolean isFunction) {
        this.name = name;
        this.type = type;
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
    
}
