package org.minic;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;

import java.io.*;
import java.nio.file.*;

public class TreePrinter {
    
    public static void printToConsole(ParseTree tree, Parser parser) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("  ÁRBOL DE PARSE (PARSE TREE)");
        System.out.println("=".repeat(80));
        
        if (tree == null) {
            System.out.println("Árbol vacío");
            return;
        }
        
        printTree(tree, "", true, parser);
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("  LEYENDA:");
        System.out.println("  ├── Nodo de regla (no terminal)");
        System.out.println("  └── Token (terminal)");
        System.out.println("  [rule_name]     : Regla del parser");
        System.out.println("  'literal'       : Palabra reservada u operador");
        System.out.println("  ID: 'nombre'    : Identificador");
        System.out.println("  INT: valor      : Literal entero");
        System.out.println("  CHAR: 'c'       : Literal carácter");
        System.out.println("  STR: \"texto\"   : Literal cadena");
        System.out.println("  BOOL: true/false: Literal booleano");
        System.out.println("=".repeat(80));
    }
   
    public static void saveToFile(ParseTree tree, Parser parser, String outputPath) {
    try {
        if (tree == null || parser == null || outputPath == null) {
            System.err.println("No se puede guardar árbol: parámetros inválidos");
            return;
        }
        
        Path path = Paths.get(outputPath);
        
        if (ErrorManager.hasErrors()) {
            System.err.println("No se guarda árbol porque hay errores de compilación");
            return;
        }
        
        try {
            Files.createDirectories(path.getParent());
        } catch (Exception e) {
            System.err.println("No se pudo crear directorio: " + e.getMessage());
            return;
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
            writer.println("// ===========================================");
            writer.println("// ÁRBOL DE PARSE - MiniC Compiler");
            writer.println("// Generado: " + java.time.LocalDateTime.now());
            writer.println("// ===========================================");
            writer.println();
            
            saveTreeToWriter(tree, "", true, parser, writer);
            
            writer.println();
            writer.println("// ===========================================");
            writer.println("// FIN DEL ÁRBOL");
            writer.println("// ===========================================");
        }
        
        System.out.println("Árbol de parse guardado en: " + outputPath);
        
    } catch (Exception e) {
        System.err.println("Error guardando árbol: " + e.getMessage());
    }
}

    private static void printTree(ParseTree tree, String indent, boolean isLast, Parser parser) {
        String marker = isLast ? "└── " : "├── ";
        System.out.print(indent + marker);
        
        if (tree instanceof TerminalNode) {
            printTerminalNode((TerminalNode) tree, parser);
        } else {
            printRuleNode(tree, parser);
        }
        
        String newIndent = indent + (isLast ? "    " : "│   ");
        for (int i = 0; i < tree.getChildCount(); i++) {
            printTree(tree.getChild(i), newIndent, i == tree.getChildCount() - 1, parser);
        }
    }
    
    private static void saveTreeToWriter(ParseTree tree, String indent, boolean isLast, 
                                         Parser parser, PrintWriter writer) {
        String marker = isLast ? "└── " : "├── ";
        writer.print(indent + marker);
        
        if (tree instanceof TerminalNode) {
            saveTerminalNode((TerminalNode) tree, parser, writer);
        } else {
            saveRuleNode(tree, parser, writer);
        }
        
        String newIndent = indent + (isLast ? "    " : "│   ");
        for (int i = 0; i < tree.getChildCount(); i++) {
            saveTreeToWriter(tree.getChild(i), newIndent, i == tree.getChildCount() - 1, 
                           parser, writer);
        }
    }
    
    private static void printTerminalNode(TerminalNode node, Parser parser) {
        Token token = node.getSymbol();
        String tokenName = parser.getVocabulary().getDisplayName(token.getType());
        String tokenText = token.getText();
        
        // Filtrar tokens no interesantes
        if (tokenName.equals("WS") || tokenName.equals("LINE_COMMENT") || 
            tokenName.equals("BLOCK_COMMENT")) {
            return;
        }
        switch (tokenName) {
            case "Identifier":
                System.out.println("ID: '" + tokenText + "'");
                break;
            case "IntegerConstant":
                System.out.println("INT: " + tokenText);
                break;
            case "CharConstant":
                String charContent = tokenText.length() > 2 ? 
                    tokenText.substring(1, tokenText.length() - 1) : tokenText;
                System.out.println("CHAR: '" + charContent + "'");
                break;
            case "StringLiteral":
                String strContent = tokenText.length() > 2 ? 
                    tokenText.substring(1, tokenText.length() - 1) : tokenText;
                System.out.println("STR: \"" + strContent + "\"");
                break;
            case "TRUE":
            case "FALSE":
                System.out.println("BOOL: " + tokenText.toLowerCase());
                break;
            default:
                if (tokenText.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    System.out.println("'" + tokenText + "'");
                } else {
                    System.out.println("OP: '" + tokenText + "'");
                }
        }
    }
    
    private static void saveTerminalNode(TerminalNode node, Parser parser, PrintWriter writer) {
        Token token = node.getSymbol();
        String tokenName = parser.getVocabulary().getDisplayName(token.getType());
        String tokenText = token.getText();
        
        if (tokenName.equals("WS") || tokenName.equals("LINE_COMMENT") || 
            tokenName.equals("BLOCK_COMMENT")) {
            return;
        }
        
        switch (tokenName) {
            case "Identifier":
                writer.println("ID: '" + tokenText + "'");
                break;
            case "IntegerConstant":
                writer.println("INT: " + tokenText);
                break;
            case "CharConstant":
                String charContent = tokenText.length() > 2 ? 
                    tokenText.substring(1, tokenText.length() - 1) : tokenText;
                writer.println("CHAR: '" + charContent + "'");
                break;
            case "StringLiteral":
                String strContent = tokenText.length() > 2 ? 
                    tokenText.substring(1, tokenText.length() - 1) : tokenText;
                writer.println("STR: \"" + strContent + "\"");
                break;
            case "TRUE":
            case "FALSE":
                writer.println("BOOL: " + tokenText.toLowerCase());
                break;
            default:
                if (tokenText.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    writer.println("'" + tokenText + "'");
                } else {
                    writer.println("OP: '" + tokenText + "'");
                }
        }
    }
    
    private static void printRuleNode(ParseTree tree, Parser parser) {
        if (tree instanceof RuleContext) {
            RuleContext ctx = (RuleContext) tree;
            String ruleName = parser.getRuleNames()[ctx.getRuleIndex()];
            
            String displayName = formatRuleName(ruleName);
            System.out.println("[" + displayName + "]");
        }
    }
    
    private static void saveRuleNode(ParseTree tree, Parser parser, PrintWriter writer) {
        if (tree instanceof RuleContext) {
            RuleContext ctx = (RuleContext) tree;
            String ruleName = parser.getRuleNames()[ctx.getRuleIndex()];
            String displayName = formatRuleName(ruleName);
            writer.println("[" + displayName + "]");
        }
    }
    
    private static String formatRuleName(String ruleName) {
        StringBuilder formatted = new StringBuilder();
        
        for (int i = 0; i < ruleName.length(); i++) {
            char c = ruleName.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                formatted.append(' ');
                formatted.append(Character.toLowerCase(c));
            } else {
                formatted.append(c);
            }
        }
        
        return formatted.toString();
    }
    
    public static void printStats(ParseTree tree, Parser parser) {
        if (tree == null) return;
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("  ESTADÍSTICAS DEL ÁRBOL DE PARSE");
        System.out.println("=".repeat(80));
        
        java.util.Map<String, Integer> ruleCounts = new java.util.HashMap<>();
        java.util.Map<String, Integer> tokenCounts = new java.util.HashMap<>();
        
        collectStats(tree, parser, ruleCounts, tokenCounts);
        
        System.out.println("\nNODOS DE REGLAS (no terminales):");
        System.out.println("-".repeat(40));
        ruleCounts.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .forEach(entry -> System.out.printf("  %-25s: %3d\n", 
                formatRuleName(entry.getKey()), entry.getValue()));
        
        System.out.println("\nTOKENS (terminales):");
        System.out.println("-".repeat(40));
        tokenCounts.entrySet().stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .forEach(entry -> System.out.printf("  %-25s: %3d\n", 
                entry.getKey(), entry.getValue()));
        
        int totalRules = ruleCounts.values().stream().mapToInt(Integer::intValue).sum();
        int totalTokens = tokenCounts.values().stream().mapToInt(Integer::intValue).sum();
        int totalNodes = totalRules + totalTokens;
        
        System.out.println("\nTOTALES:");
        System.out.println("-".repeat(40));
        System.out.printf("  Nodos de reglas: %d\n", totalRules);
        System.out.printf("  Tokens:          %d\n", totalTokens);
        System.out.printf("  Total de nodos:  %d\n", totalNodes);
        System.out.println("=".repeat(80));
    }

    private static void collectStats(ParseTree tree, Parser parser,
                                     java.util.Map<String, Integer> ruleCounts,
                                     java.util.Map<String, Integer> tokenCounts) {
        if (tree instanceof TerminalNode) {
            TerminalNode node = (TerminalNode) tree;
            Token token = node.getSymbol();
            String tokenName = parser.getVocabulary().getDisplayName(token.getType());
            
            if (!tokenName.equals("WS") && !tokenName.equals("LINE_COMMENT") && 
                !tokenName.equals("BLOCK_COMMENT")) {
                tokenCounts.put(tokenName, tokenCounts.getOrDefault(tokenName, 0) + 1);
            }
        } else if (tree instanceof RuleContext) {
            RuleContext ctx = (RuleContext) tree;
            String ruleName = parser.getRuleNames()[ctx.getRuleIndex()];
            ruleCounts.put(ruleName, ruleCounts.getOrDefault(ruleName, 0) + 1);
            
            for (int i = 0; i < tree.getChildCount(); i++) {
                collectStats(tree.getChild(i), parser, ruleCounts, tokenCounts);
            }
        }
    }
}