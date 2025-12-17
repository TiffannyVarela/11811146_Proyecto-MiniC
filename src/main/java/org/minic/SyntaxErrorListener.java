package org.minic;

import org.antlr.v4.runtime.*;

public class SyntaxErrorListener extends BaseErrorListener {
    
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                          int line, int charPositionInLine, String msg, RecognitionException e) {
        
        ErrorInfo errorInfo = calculateErrorPosition(recognizer, offendingSymbol, 
                                                    line, charPositionInLine, msg);        
        ErrorManager.addError(errorInfo.line, errorInfo.column, errorInfo.message);
    }
    
    private ErrorInfo calculateErrorPosition(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                           int reportedLine, int reportedCharPos, String msg) {
        
        ErrorInfo info = new ErrorInfo();
        info.reportedLine = reportedLine;
        info.reportedCharPos = reportedCharPos;
        info.originalMessage = msg;
        
        try {
            if (!(recognizer instanceof Parser)) {
                return getFallbackInfo(reportedLine, reportedCharPos, msg);
            }
            
            Parser parser = (Parser) recognizer;
            TokenStream tokens = parser.getInputStream();
            if (tokens == null) {
                return getFallbackInfo(reportedLine, reportedCharPos, msg);
            }
            
            // Obtener el token que causó el error
            Token errorToken = null;
            if (offendingSymbol instanceof Token) {
                errorToken = (Token) offendingSymbol;
            }
            
            info.message = cleanAntlrMessage(msg);
            
            if (msg.contains("missing ';'")) {
                
                int tokenIndex = ((CommonTokenStream)tokens).index();
                
                if (tokenIndex > 0) {
                    for (int i = tokenIndex - 1; i >= 0; i--) {
                        Token token = tokens.get(i);
                        int type = token.getType();
                        
                        // Ignorar espacios, comentarios, newlines
                        if (type != MiniCLexer.Whitespace && 
                            type != MiniCLexer.LineComment && 
                            type != MiniCLexer.BlockComment) {
                            
                            
                            // La columna es: posición del token + longitud del token
                            info.line = token.getLine();
                            info.column = token.getCharPositionInLine() + token.getText().length() + 1;
                            return info;
                        }
                    }
                }
                
                if (errorToken != null) {
                    info.line = errorToken.getLine();
                    info.column = errorToken.getCharPositionInLine() + 1;
                } else {
                    info.line = reportedLine;
                    info.column = reportedCharPos + 1;
                }
                
            } else if (msg.contains("extraneous") || msg.contains("mismatched")) {
                if (errorToken != null) {
                    info.line = errorToken.getLine();
                    info.column = errorToken.getCharPositionInLine() + 1;
                } else {
                    info.line = reportedLine;
                    info.column = reportedCharPos + 1;
                }
            } else {
                info.line = reportedLine;
                info.column = reportedCharPos + 1;
            }
            
        } catch (Exception ex) {
            ex.printStackTrace();
            return getFallbackInfo(reportedLine, reportedCharPos, msg);
        }
        
        return info;
    }
    
    private ErrorInfo getFallbackInfo(int line, int charPos, String msg) {
        ErrorInfo info = new ErrorInfo();
        info.line = line;
        info.column = charPos + 1;
        info.message = cleanAntlrMessage(msg);
        return info;
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
            int start = msg.indexOf("'") + 1;
            int end = msg.indexOf("'", start);
            if (start > 0 && end > start) {
                String extra = msg.substring(start, end);
                return "entrada extraña: '" + extra + "'";
            }
            return "entrada extraña o inesperada";
        }
        
        if (msg.startsWith("mismatched input")) {
            int start = msg.indexOf("'") + 1;
            int end = msg.indexOf("'", start);
            if (start > 0 && end > start) {
                String mismatch = msg.substring(start, end);
                return "entrada no coincide: '" + mismatch + "'";
            }
            return "entrada no coincide con lo esperado";
        }
        
        if (msg.contains("no viable alternative")) {
            return "sintaxis inválida";
        }
        
        return "error de sintaxis";
    }
    
    private static class ErrorInfo {
        int reportedLine;
        int reportedCharPos;
        int line;
        int column;
        String originalMessage;
        String message;
    }
}