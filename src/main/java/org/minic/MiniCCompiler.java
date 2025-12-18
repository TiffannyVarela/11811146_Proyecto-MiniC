package org.minic;

import java.nio.file.*;
import java.util.*;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.minic.ast.AstBuilder;
import org.minic.ast.AstNode;

public class MiniCCompiler {

    private static boolean generateMips = false;
    private static boolean dumpTree = false;

    private static final String[] SEARCH_PATHS = {
        "",
        "tests/examples/good/",
        "tests/examples/bad/",
        "tests/examples/",
        "examples/good/",
        "examples/bad/",
        "examples/"
    };
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

            for (String arg : args) {
                if (arg.equals("--help")) {
                    showHelp();
                    return;
                }
            }

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--dump-tree":
                        dumpTree = true;
                        break;
                    case "-S":
                        generateMips = true;
                        break;
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
                        if (!args[i].startsWith("-") && inputFile == null) {
                            inputFile = args[i];
                            if (!inputFile.endsWith(".mc")) {
                                inputFile += ".mc";
                            }
                        }
                        break;
                }
            }

            if (inputFile == null) {
                System.err.println("Error: No se especificó archivo de entrada");
                System.exit(1);
            }

            String foundFile = findFile(inputFile);
            if (foundFile == null) {
                System.err.println("Error: No se pudo encontrar el archivo '" + inputFile + "'");
                System.exit(1);
            }

            compileTestFile(foundFile, dumpIr, optimize, outputFile);

        } catch (Exception e) {
            System.err.println("Error durante la compilación: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void compileTestFile(String testFile,
                                   boolean dumpIr,
                                   boolean optimize,
                                   String outputFile) throws Exception {

    System.out.println("Compilando archivo: " + testFile);

    Path filePath = Paths.get(testFile);
    
    String sourceText = Files.readString(filePath);
    ErrorManager.setSourceText(sourceText);
    CharStream input = CharStreams.fromString(sourceText, filePath.toString());
    ErrorManager.cleanErrors();
    System.out.println("Análisis léxico...");
    MiniCLexer lexer = new MiniCLexer(input);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    System.out.println("Análisis sintáctico...");
    MiniCParser parser = new MiniCParser(tokens);
    parser.removeErrorListeners();
    lexer.removeErrorListeners();
    SyntaxErrorListener syntaxErrorListener = new SyntaxErrorListener();
    parser.addErrorListener(syntaxErrorListener);
    lexer.addErrorListener(syntaxErrorListener);
    ParseTree tree = parser.program();
    if (ErrorManager.hasErrors()) {
        ErrorManager.throwIfErrors();
        return;
    }

    System.out.println("Construyendo AST...");
    AstBuilder astBuilder = new AstBuilder();
    AstNode ast = astBuilder.build(tree);
    Compiler.compile(
            ast,
            tree,
            parser,
            testFile,
            generateMips,
            dumpIr,
            optimize,
            outputFile
    );

    if (dumpTree) {
        TreePrinter.printToConsole(tree, parser);
        TreePrinter.printStats(tree, parser);
    }
    System.out.println("Compilación completada para: " + testFile);
}
    public static void compileTestFile(String testFile) throws Exception {
        compileTestFile(testFile, false, false, null);
    }

    private static String findFile(String fileName) {
        for (String path : SEARCH_PATHS) {
            Path file = Paths.get(path + fileName);
            if (Files.exists(file) && Files.isRegularFile(file)) {
                return file.toString();
            }
        }
        return null;
    }

    private static void showHelp() {
        System.out.println("MiniC Compiler");
        System.out.println("=================================================");
        System.out.println();
        System.out.println("Uso:");
        System.out.println("  run.bat <archivo.mc> [opciones]");
        System.out.println();
        System.out.println("Opciones:");
        System.out.println("  -S               Generar código ensamblador MIPS (.s)");
        System.out.println("  -o <archivo>     Especificar archivo de salida");
        System.out.println("  --dump-tree      Mostrar árbol de parse (ANTLR)");
        System.out.println("  --dump-ir        Mostrar código intermedio (IR)");
        System.out.println("  -O               Habilitar optimizaciones");
        System.out.println("  --test <lista>   Ejecutar suite de pruebas");
        System.out.println("  --help           Mostrar esta ayuda");
        System.out.println();
        System.out.println("Ejemplos:");
        System.out.println("  run.bat good1.mc");
        System.out.println("  run.bat good1.mc -O");
        System.out.println("  run.bat good1.mc --dump-tree --dump-ir");
        System.out.println("  run.bat good1.mc --dump-tree");
        System.out.println("  run.bat good1.mc --dump-ir -O");
        System.out.println("  run.bat good1.mc --dump-ir -S");
        System.out.println("  run.bat good1.mc -S -o good1.s");
        System.out.println("  run.bat good1.mc --dump-tree -S");
        System.out.println("  run.bat good1.mc --dump-tree --dump-ir -O");
        System.out.println("  run.bat good1.mc --dump-tree --dump-ir -S -O");
        System.out.println("  run.bat good1.mc --dump-tree --dump-ir -S -O -o good1.s");
    }

    public static void runTestSuite(String testListFile) throws Exception {
        List<String> tests = Files.readAllLines(Paths.get(findFile(testListFile)));
        for (String test : tests) {
            if (!test.trim().isEmpty() && !test.startsWith("#")) {
                compileTestFile(test.trim());
            }
        }
    }
}
