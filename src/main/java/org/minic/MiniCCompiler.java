package org.minic;

import java.nio.file.*;
import java.util.*;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.minic.ast.AstBuilder;
import org.minic.ast.AstNode;

public class MiniCCompiler {
    // Indica si se debe generar código MIPS
    private static boolean generateMips = false;
    // Indica si se debe mostrar el árbol de parse
    private static boolean dumpTree = false;
    // Rutas de búsqueda para archivos de prueba
    private static final String[] SEARCH_PATHS = {
            "",
            "tests/examples/good/",
            "tests/examples/bad/",
            "tests/examples/",
            "examples/good/",
            "examples/bad/",
            "examples/"
    };

    // Punto de entrada principal del compilador
    public static void main(String[] args) {
        if (args.length == 0) {
            showHelp();
            System.exit(1);
        }
        try {
            boolean dumpIr = false; // Indica si se debe mostrar el IR
            boolean optimize = false; // Indica si se deben aplicar optimizaciones
            String outputFile = null; // Nombre del archivo de salida
            String inputFile = null; // Nombre del archivo de entrada
            // Procesa los argumentos de línea de comandos
            for (String arg : args) {
                if (arg.equals("--help")) {
                    showHelp();
                    return;
                }
            }
            // Procesa los argumentos de línea de comandos
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
                            outputFile = args[++i]; // Nombre del archivo de salida
                        }
                        break;
                    case "--test":
                        if (i + 1 < args.length) {
                            runTestSuite(args[++i]); // Ejecuta la suite de pruebas
                            return;
                        }
                        break;
                    default:
                        if (!args[i].startsWith("-") && inputFile == null) {
                            inputFile = args[i];
                            if (!inputFile.endsWith(".mc")) {
                                inputFile += ".mc"; // Añade extensión si falta
                            }
                        }
                        break;
                }
            }
            // Verifica que se haya especificado un archivo de entrada
            if (inputFile == null) {
                System.err.println("Error: No se especificó archivo de entrada");
                System.exit(1);
            }
            // Busca el archivo de entrada en las rutas especificadas
            String foundFile = findFile(inputFile);
            if (foundFile == null) {
                System.err.println("Error: No se pudo encontrar el archivo '" + inputFile + "'");
                System.exit(1);
            }
            // Compila el archivo encontrado con las opciones especificadas
            compileTestFile(foundFile, dumpIr, optimize, outputFile);

        } catch (Exception e) {
            System.err.println("Error durante la compilación: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    // Compila un archivo de prueba con opciones específicas
    public static void compileTestFile(String testFile,
            boolean dumpIr,
            boolean optimize,
            String outputFile) throws Exception {

        System.out.println("Compilando archivo: " + testFile);

        Path filePath = Paths.get(testFile);
        // Lee el contenido del archivo fuente
        String sourceText = Files.readString(filePath);
        ErrorManager.setSourceText(sourceText);
        // Preparar flujo de entrada para ANTLR
        CharStream input = CharStreams.fromString(sourceText, filePath.toString());
        ErrorManager.cleanErrors();
        System.out.println("Análisis léxico...");
        MiniCLexer lexer = new MiniCLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        System.out.println("Análisis sintáctico...");
        MiniCParser parser = new MiniCParser(tokens);
        parser.removeErrorListeners();
        lexer.removeErrorListeners();
        // Agregar oyente de errores personalizado
        SyntaxErrorListener syntaxErrorListener = new SyntaxErrorListener();
        parser.addErrorListener(syntaxErrorListener);
        lexer.addErrorListener(syntaxErrorListener);
        // Generar árbol de parseo
        ParseTree tree = parser.program();
        // Si hay errores léxico-sintácticos, lanzar excepción
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
                outputFile);
        // Mostrar árbol de parse si se solicitó
        if (dumpTree) {
            TreePrinter.printToConsole(tree, parser);
            TreePrinter.printStats(tree, parser);
        }
        System.out.println("Compilación completada para: " + testFile);
    }

    public static void compileTestFile(String testFile) throws Exception {
        compileTestFile(testFile, false, false, null);
    }

    // Busca un archivo en las rutas de búsqueda definidas
    private static String findFile(String fileName) {
        for (String path : SEARCH_PATHS) {
            Path file = Paths.get(path + fileName);
            if (Files.exists(file) && Files.isRegularFile(file)) {
                return file.toString();
            }
        }
        return null;
    }

    // Muestra la ayuda del compilador
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

    // Ejecuta una suite de pruebas desde un archivo de lista
    public static void runTestSuite(String testListFile) throws Exception {
        List<String> tests = Files.readAllLines(Paths.get(findFile(testListFile)));
        for (String test : tests) {
            if (!test.trim().isEmpty() && !test.startsWith("#")) {
                compileTestFile(test.trim());
            }
        }
    }
}
