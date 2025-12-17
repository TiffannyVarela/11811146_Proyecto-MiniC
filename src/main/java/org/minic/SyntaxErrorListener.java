package org.minic;

import org.antlr.v4.runtime.*;

public class SyntaxErrorListener extends BaseErrorListener {
    
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                          int line, int charPositionInLine, String msg, RecognitionException e) {
        
        int adjustedColumn = charPositionInLine + 1;
        String cleanMsg = cleanAntlrMessage(msg);
        ErrorManager.addError(line, adjustedColumn, cleanMsg);
        showRealErrorContext(recognizer, line, charPositionInLine);
    }
    
    private String cleanAntlrMessage(String antlrMsg) {
        String msg = antlrMsg.trim();
        
        if (msg.startsWith("missing")) {
            if (msg.contains("';'")) return "falta punto y coma (;)";
            if (msg.contains("')'")) return "falta paréntesis de cierre )";
            if (msg.contains("']'")) return "falta corchete de cierre ]";
            if (msg.contains("'}'")) return "falta llave de cierre }";
            return "falta algo en la sintaxis";
        }
        
        if (msg.startsWith("extraneous input")) {
            return "entrada extraña o inesperada";
        }
        
        if (msg.startsWith("mismatched input")) {
            return "entrada no coincide con lo esperado";
        }
        
        if (msg.contains("no viable alternative")) {
            return "sintaxis inválida";
        }
        
        return "error de sintaxis";
    }
    
    private void showRealErrorContext(Recognizer<?, ?> recognizer, int line, int charPositionInLine) {
        try {
            if (!(recognizer instanceof Parser)) return;
            
            Parser parser = (Parser) recognizer;
            TokenStream tokens = parser.getInputStream();
            if (tokens == null) return;
            
            CharStream input = tokens.getTokenSource().getInputStream();
            if (input == null) return;
            
            String fullText = input.toString();
            String[] lines = fullText.split("\r?\n", -1);
            
            if (line > 0 && line <= lines.length) {
                String errorLine = lines[line - 1];
                
                int displayColumn = 1;
                for (int i = 0; i < charPositionInLine && i < errorLine.length(); i++) {
                    if (errorLine.charAt(i) == '\t') {
                        displayColumn += 4;
                    } else {
                        displayColumn++;
                    }
                }
                
                System.err.printf("\n[Línea %d, Columna %d]\n", line, displayColumn);
                System.err.println("  " + errorLine.replace("\t", "    "));
                
                StringBuilder pointer = new StringBuilder("  ");
                for (int i = 0; i < charPositionInLine && i < errorLine.length(); i++) {
                    if (errorLine.charAt(i) == '\t') {
                        pointer.append("    ");
                    } else {
                        pointer.append(" ");
                    }
                }
                pointer.append("^ aquí");
                System.err.println(pointer.toString());
            }
            
        } catch (Exception ex) {
            // Ignorar errores en visualización
        }
    }
}