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
            System.exit(1);
            return;
        }

        try {
            // Leer el archivo de lista de pruebas
            List<String> testFiles = Files.readAllLines(Paths.get(args[0]));

            System.out.println("Compilando pruebas definidas en: " + args[0]);

            for (String testFile : testFiles) {
                if (testFile.trim().isEmpty() || testFile.startsWith("#")) {
                    continue;
                }
                System.out.println("--------------------------------------------------");
                System.out.println("Compilando: " + testFile);
                System.out.println("--------------------------------------------------");

                try {
                    compileTestFile(testFile.trim());
                    System.out.println("Compilación exitosa para: " + testFile);
                } catch (Exception e) {
                    System.err.println("Error durante la compilación de " + testFile + ": "); 
                    System.err.println("Razon: " + e.getMessage());
                    if (e.getCause() != null) {
                        System.err.println("Causa: " + e.getCause().getMessage());
                    }
                }
            }

            System.out.println("Proceso de compilación finalizado.");
        } catch (IOException e) {
            System.err.println("Error al leer el archivo de lista de pruebas: " + e.getMessage());
            System.exit(1);
        }
    }

    public static void compileTestFile(String testFile) throws Exception {
        // Leer el contenido del archivo de prueba
        CharStream input = CharStreams.fromFileName(testFile);

        //Crear lexer
        MiniCLexer lexer = new MiniCLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        //Crear parser
        MiniC parser = new MiniC(tokens);

        //Manejo de errores
        parser.removeErrorListeners();
        BaseErrorListener errorListener = new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new RuntimeException("Error de sintaxis en línea " + line + ", posición " + charPositionInLine + ": " + msg, e);
            }
        };
        parser.addErrorListener(new DiagnosticErrorListener());

        //Parseo y construcción del árbol
        MiniC.ProgramContext tree = parser.program();
        AstBuilder astBuilder = new AstBuilder();
        AstNode ast = astBuilder.build(tree);

        // Aquí puedes agregar más pasos, como la generación de código o análisis semántico
        Compiler.compile(ast, testFile);
    }
}