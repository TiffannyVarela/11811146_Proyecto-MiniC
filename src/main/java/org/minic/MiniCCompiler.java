package org.minic;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.antlr.v4.runtime.*;
import org.minic.ast.AstBuilder;
import org.minic.ast.AstNode;

public class MiniCCompiler {
    
        public static void main(String[] args) {
    if (args.length == 0) {
        showHelp();
        System.exit(1);
    }

    try {
        boolean dumpIr = false;
        boolean optimize = false;
        String outputFile = null;
        String inputFile = null;
        
        // PRIMERO verificar si es --help
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--help")) {
                showHelp();
                return;  // Salir inmediatamente después de mostrar ayuda
            }
        }
        
        // LUEGO procesar los demás argumentos
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--dump-ir":
                    dumpIr = true;
                    break;
                case "-O":
                    optimize = true;
                    break;
                case "-o":
                    if (i + 1 < args.length) {
                        outputFile = args[++i];
                    }
                    break;
                case "--test":
                    if (i + 1 < args.length) {
                        runTestSuite(args[++i]);
                        return;
                    }
                    break;
                default:
                    // Si no es una opción, asumimos que es el archivo de entrada
                    if (!args[i].startsWith("-") && inputFile == null) {
                        inputFile = args[i];
                    }
                    break;
            }
        }
        
        // Compilar archivo individual
        if (inputFile != null) {
            compileTestFile(inputFile, dumpIr, optimize, outputFile);
        } else {
            System.err.println("Error: No se especificó archivo de entrada");
            System.exit(1);
        }
        
    } catch (Exception e) {
        System.err.println("Error durante la compilación: " + e.getMessage());
        e.printStackTrace();
        System.exit(1);
    }
}

    private static void showHelp() {
        System.out.println("Uso: minic <archivo.mc> [opciones]");
        System.out.println("   o: minic --test <lista_pruebas.txt>");
        System.out.println();
        System.out.println("Opciones:");
        System.out.println("  -S              Generar código MIPS (.s)");
        System.out.println("  -o <archivo>    Especificar archivo de salida");
        System.out.println("  --dump-ir       Mostrar código intermedio");
        System.out.println("  --test <lista>  Ejecutar suite de pruebas");
        System.out.println("  -O              Habilitar optimizaciones");
        System.out.println("  --help          Mostrar esta ayuda");
        System.out.println();
        System.out.println("Ejemplos:");
        System.out.println("  minic programa.mc");
        System.out.println("  minic programa.mc --dump-ir -O -o salida.s");
        System.out.println("  minic --test pruebas.txt");
    }

    public static void runTestSuite(String testListFile) throws Exception {
        System.out.println("========================================");
        System.out.println("   EJECUTANDO SUITE DE PRUEBAS");
        System.out.println("========================================");
        
        if (!Files.exists(Paths.get(testListFile))) {
            throw new FileNotFoundException("Archivo de lista de pruebas no encontrado: " + testListFile);
        }

        List<String> testFiles = Files.readAllLines(Paths.get(testListFile));
        int successCount = 0;
        int totalCount = 0;

        for (String testFile : testFiles) {
            testFile = testFile.trim();
            if (testFile.isEmpty() || testFile.startsWith("#")) {
                continue; // Saltar líneas vacías o comentarios
            }

            totalCount++;
            System.out.println("\n" + "=" .repeat(50));
            System.out.println("PRUEBA " + totalCount + ": " + testFile);
            System.out.println("=" .repeat(50));

            try {
                compileTestFile(testFile);
                System.out.println(testFile + " - COMPILACIÓN EXITOSA");
                successCount++;
            } catch (Exception e) {
                System.out.println(testFile + " - ERROR: " + e.getMessage());
            }
        }

        System.out.println("\n" + "=" .repeat(50));
        System.out.println("RESUMEN DE PRUEBAS:");
        System.out.println("Archivos probados: " + totalCount);
        System.out.println("Exitosos: " + successCount);
        System.out.println("Fallidos: " + (totalCount - successCount));
        System.out.println("=" .repeat(50));
    }

    public static void compileTestFile(String testFile, boolean dumpIr, boolean optimize, String outputFile) throws Exception {
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
        MiniCParser parser = new MiniCParser(tokens);

        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, 
                                  int line, int charPositionInLine, String msg, RecognitionException e) {
                String errorMsg = String.format("Error de sintaxis en línea %d, posición %d: %s", 
                                              line, charPositionInLine, msg);
                throw new CompilationException(errorMsg, e);
            }
        });

        // Parseo y construcción del árbol
        MiniCParser.ProgramContext tree = parser.program();
        
        System.out.println("Construyendo AST...");
        AstBuilder astBuilder = new AstBuilder();
        AstNode ast = astBuilder.build(tree);

        // Compilar el AST con opciones
        Compiler.compile(ast, testFile, dumpIr, optimize, outputFile);
        
        System.out.println("✅ Proceso de compilación completado para: " + testFile);
    }

    // Método auxiliar para compilar desde un string (útil para pruebas)
    public static void compileFromString(String sourceCode, String fileName) throws Exception {
        System.out.println("Compilando código desde string...");
        CharStream input = CharStreams.fromString(sourceCode);

        // Crear lexer y parser
        MiniCLexer lexer = new MiniCLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MiniCParser parser = new MiniCParser(tokens); // Cambiado a MiniCParser

        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, 
                                  int line, int charPositionInLine, String msg, RecognitionException e) {
                String errorMsg = String.format("Error de sintaxis en línea %d, posición %d: %s", 
                                              line, charPositionInLine, msg);
                throw new CompilationException(errorMsg, e);
            }
        });

        // Parseo y construcción del árbol
        MiniCParser.ProgramContext tree = parser.program(); // Cambiado a MiniCParser.ProgramContext
        AstBuilder astBuilder = new AstBuilder();
        AstNode ast = astBuilder.build(tree);

        // Compilar el AST
        Compiler.compile(ast, fileName);
    }

    public static void compileTestFile(String testFile) throws Exception {
        compileTestFile(testFile, false, false, null);
    }
    
}