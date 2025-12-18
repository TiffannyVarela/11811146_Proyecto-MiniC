package org.minic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.minic.ast.*;
import org.minic.backend.mips.MipsGenerator;
import org.minic.ir.IrGenerator;
import org.minic.optimizer.*;
import org.minic.semantic.SemanticChecker;

/*
Compiler

Clase principal que coordina todas las fases del compilador MiniC:
  - Construcción y verificación semántica del AST
  - Generación de IR intermedio
  - Optimización (Constant Folding)
  - Generación de código MIPS
 */

public class Compiler {

    // ANSI para colorear la salida en terminal al mostrar IR
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";

    /*
     * Compila un AST con opciones avanzadas.
     * 
     * @param generateMips indica si genera código MIPS
     * 
     * @param dumpIr indica si imprime IR antes/después de optimización
     * 
     * @param optimize indica si aplica optimización sobre el AST
     * 
     * @param outputFile nombre de salida (opcional)
     */

    public static void compile(AstNode ast,
            ParseTree parseTree,
            Parser parser,
            String sourceFile,
            boolean generateMips,
            boolean dumpIr,
            boolean optimize,
            String outputFile) {

        compileInternal(ast, parseTree, parser,
                sourceFile, generateMips, dumpIr, optimize, outputFile);
    }

    // Comilacion simple sin opciones avanzadas (Solo MIPs y sin optimización)
    public static void compile(AstNode ast,
            ParseTree parseTree,
            Parser parser,
            String sourceFile) {

        compileInternal(ast, parseTree, parser,
                sourceFile, true, false, false, null);
    }

    // Compila código fuente dado como string
    public static void compileFromString(String sourceCode, String fileName) {
        try {
            ErrorManager.cleanErrors();
            // Usar un nombre base para archivos temporales
            String baseName = (fileName != null && !fileName.isEmpty())
                    ? fileName.replace(".mc", "")
                    : "string_code_" + System.currentTimeMillis();
            // Tokenizar y parsear el código fuente
            org.antlr.v4.runtime.CharStream input = org.antlr.v4.runtime.CharStreams.fromString(sourceCode, fileName);
            org.minic.MiniCLexer lexer = new org.minic.MiniCLexer(input);
            org.antlr.v4.runtime.CommonTokenStream tokens = new org.antlr.v4.runtime.CommonTokenStream(lexer);

            org.minic.MiniCParser parser = new org.minic.MiniCParser(tokens);
            ParseTree tree = parser.program();

            if (ErrorManager.hasErrors()) {
                ErrorManager.throwIfErrors();
            }
            // Construir el AST
            AstBuilder astBuilder = new AstBuilder();
            AstNode ast = astBuilder.build(tree);

            compileInternal(ast, tree, parser,
                    baseName + ".mc", true, false, false, null);

        } catch (Exception e) {
            throw new CompilationException("Error compilando desde string", e);
        }
    }

    /*
     * Fase interna de compilación.
     * Aquí se ejecutan todas las fases: semántica, IR, optimización y MIPS.
     */
    private static void compileInternal(AstNode ast,
            ParseTree parseTree,
            Parser parser,
            String sourceFile,
            boolean generateMips,
            boolean dumpIr,
            boolean optimize,
            String outputFile) {

        try {
            ErrorManager.cleanErrors();

            Path sourcePath = Paths.get(sourceFile);
            String baseName = sourcePath.getFileName().toString().replace(".mc", "");
            Path outputDir = Paths.get("tests/output").resolve(baseName);
            // Fase Semántica
            SemanticChecker checker = new SemanticChecker();
            String sourceText = Files.readString(Paths.get(sourceFile));
            ErrorManager.setSourceText(sourceText);
            checker.check(ast);

            if (ErrorManager.hasErrors()) {
                System.out.println("Errores semánticos encontrados:");
                ErrorManager.throwIfErrors();
                return;
            }
            // Crear directorio de salida y guardar árbol de análisis
            Files.createDirectories(outputDir);
            TreePrinter.saveToFile(
                    parseTree,
                    parser,
                    outputDir.resolve(baseName + ".txt").toString());
            // Generar IR original
            IrGenerator irGenOriginal = new IrGenerator();
            List<String> irOriginal = irGenOriginal.generate(ast);

            Files.write(
                    outputDir.resolve(baseName + ".ir"),
                    irOriginal);
            AstNode processedAst = ast;
            List<String> irProcessed = irOriginal;
            // Fase de optimización
            if (optimize) {
                System.out.println("=== INICIANDO OPTIMIZACIÓN ===");
                processedAst = ConstantFolder.optimize(ast);
                IrGenerator irGenOptimized = new IrGenerator();
                irProcessed = irGenOptimized.generate(processedAst);
                if (dumpIr) {
                    Files.write(
                            outputDir.resolve(baseName + "_opt.ir"),
                            irProcessed);
                    printIrComparison(irOriginal, irProcessed);
                }
                System.out.println("=== FIN OPTIMIZACIÓN ===");
            }
            // Generar código MIPS
            if (generateMips) {
                MipsGenerator mipsGenerator = new MipsGenerator();
                String mipsCode = mipsGenerator.generate(processedAst);

                Files.write(
                        outputDir.resolve(baseName + ".s"),
                        mipsCode.getBytes());
            }

            System.out.println("Compilación finalizada correctamente");

        } catch (Exception e) {
            throw new CompilationException("Error durante compilación", e);
        }
    }

    // Imprime comparación lado a lado del IR antes y después de la optimización
    private static void printIrComparison(List<String> irBefore, List<String> irAfter) {
        System.out.println("========================================");
        System.out.println("Antes de la optimización                 | Después de la optimización");
        System.out.println("========================================");

        int maxLines = Math.max(irBefore.size(), irAfter.size());
        for (int i = 0; i < maxLines; i++) {
            String left = i < irBefore.size() ? irBefore.get(i) : "";
            String right = i < irAfter.size() ? irAfter.get(i) : "";
            System.out.printf("%s%-40s%s | %s%-40s%s%n",
                    ANSI_RED, left, ANSI_RESET,
                    ANSI_GREEN, right, ANSI_RESET);
        }
        System.out.println("========================================");
    }
}
