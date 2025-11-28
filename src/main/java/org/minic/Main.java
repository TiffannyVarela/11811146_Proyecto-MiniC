package org.minic;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.antlr.v4.runtime.*;
import org.minic.ast.AstBuilder;
import org.minic.ast.AstNode;

public class Main {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java org.minic.Main <archivo_lista_pruebas.txt>");
            System.err.println("O: java org.minic.Main <archivo.mc> (para un solo archivo)");
            System.exit(1);
            return;
        }

        try {
            String inputFile = args[0];

            if (inputFile.endsWith(".txt")) {
                compileFromTestList(inputFile);
            } else if (inputFile.endsWith(".mc")) {
                compileSingleFile(inputFile);
            } else {
                System.err.println("Error: El archivo debe ser .mc (MiniC) o .txt (lista de pruebas)");
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("Error fatal: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void compileFromTestList(String testListFile) throws IOException {
        List<String> testFiles = Files.readAllLines(Paths.get(testListFile));

        System.out.println("Compilando pruebas definidas en: " + testListFile);
        System.out.println("Encontrados " + testFiles.size() + " archivos en la lista");

        int successCount = 0;
        int failCount = 0;

        for (String testFile : testFiles) {
            if (testFile.trim().isEmpty() || testFile.startsWith("#")) {
                continue;
            }
            
            System.out.println("\n" + "=" .repeat(60));
            System.out.println("Compilando: " + testFile);
            System.out.println("=" .repeat(60));

            try {
                compileTestFile(testFile.trim());
                System.out.println("Compilación EXITOSA para: " + testFile);
                successCount++;
            } catch (Exception e) {
                System.err.println("Error durante la compilación de " + testFile);
                System.err.println("   Razón: " + e.getMessage());
                if (e.getCause() != null) {
                    System.err.println("   Causa: " + e.getCause().getMessage());
                }
                failCount++;
            }
        }

        System.out.println("\n" + "=" .repeat(60));
        System.out.println("RESUMEN DE COMPILACIÓN:");
        System.out.println("Éxitos: " + successCount);
        System.out.println("Fallos: " + failCount);
        System.out.println("Total: " + (successCount + failCount));
        System.out.println("=" .repeat(60));
    }

    private static void compileSingleFile(String sourceFile) {
        System.out.println("Compilando archivo: " + sourceFile);
        System.out.println("=" .repeat(50));

        try {
            compileTestFile(sourceFile);
            System.out.println("Compilación EXITOSA!");
        } catch (Exception e) {
            System.err.println("Compilación FALLIDA!");
            System.err.println("Error: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Causa: " + e.getCause().getMessage());
            }
            System.exit(1);
        }
    }

    public static void compileTestFile(String testFile) throws Exception {
        if (!Files.exists(Paths.get(testFile))) {
            throw new FileNotFoundException("Archivo no encontrado: " + testFile);
        }

        System.out.println("Leyendo archivo: " + testFile);
        CharStream input = CharStreams.fromFileName(testFile);

        // Crear lexer
        System.out.println("Análisis léxico...");
        MiniCLexer lexer = new MiniCLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Crear parser
        System.out.println("Análisis sintáctico...");
        MiniC parser = new MiniC(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, 
                                  int line, int charPositionInLine, String msg, RecognitionException e) {
                String errorMsg = String.format("Error de sintaxis en línea %d, posición %d: %s", 
                                              line, charPositionInLine, msg);
                throw new RuntimeException(errorMsg, e);
            }
        });

        // Parseo y construcción del árbol
        MiniC.ProgramContext tree = parser.program();
        
        System.out.println("Construyendo AST...");
        AstBuilder astBuilder = new AstBuilder();
        AstNode ast = astBuilder.build(tree);

        // Compilar el AST
        Compiler.compile(ast, testFile);
        
        System.out.println("Proceso de compilación completado");
    }
}