package org.minic.semantic;

import java.util.*;

public class SymbolTable {
    private final Map<String, Symbol> symbols = new HashMap<>();
    private final SymbolTable parent;

    public SymbolTable(SymbolTable parent) {
        this.parent = parent;
    }

    public boolean addSymbol(Symbol symbol){
        if (symbols.containsKey(symbol.getName())) {
            return false; // Símbolo ya existe en el ámbito actual
        }
        symbols.put(symbol.getName(), symbol);
        return true;
    }

    public Symbol lookup(String name) {
        Symbol symbol = symbols.get(name);
        if (symbol != null) {
            return symbol;
        } else if (parent != null) {
            return parent.lookup(name);
        } else {
            return null; // No encontrado
        }
    }

    public Symbol lookupCurrentScope(String name) {
        return symbols.get(name);
    }
}
