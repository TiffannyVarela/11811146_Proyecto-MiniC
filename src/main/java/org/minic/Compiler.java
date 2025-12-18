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

public class Compiler {

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";

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

    public static void compile(AstNode ast,
                               ParseTree parseTree,
                               Parser parser,
                               String sourceFile) {

        compileInternal(ast, parseTree, parser,
                sourceFile, true, false, false, null);
    }

    public static void compileFromString(String sourceCode, String fileName) {
        try {
            ErrorManager.cleanErrors();

            String baseName = (fileName != null && !fileName.isEmpty())
                    ? fileName.replace(".mc", "")
                    : "string_code_" + System.currentTimeMillis();

            org.antlr.v4.runtime.CharStream input =
                    org.antlr.v4.runtime.CharStreams.fromString(sourceCode, fileName);
            org.minic.MiniCLexer lexer = new org.minic.MiniCLexer(input);
            org.antlr.v4.runtime.CommonTokenStream tokens =
                    new org.antlr.v4.runtime.CommonTokenStream(lexer);

            org.minic.MiniCParser parser = new org.minic.MiniCParser(tokens);
            ParseTree tree = parser.program();

            if (ErrorManager.hasErrors()) {
                ErrorManager.throwIfErrors();
            }

            AstBuilder astBuilder = new AstBuilder();
            AstNode ast = astBuilder.build(tree);

            compileInternal(ast, tree, parser,
                    baseName + ".mc", true, false, false, null);

        } catch (Exception e) {
            throw new CompilationException("Error compilando desde string", e);
        }
    }

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
        

        SemanticChecker checker = new SemanticChecker();
        String sourceText = Files.readString(Paths.get(sourceFile));
        ErrorManager.setSourceText(sourceText);
        checker.check(ast);

        if (ErrorManager.hasErrors()) {
            System.out.println("Errores semánticos encontrados:");
            ErrorManager.throwIfErrors();
            return;
        }
        Files.createDirectories(outputDir);
        
        TreePrinter.saveToFile(
                parseTree,
                parser,
                outputDir.resolve(baseName + ".tree").toString()
        );

        IrGenerator irGenOriginal = new IrGenerator();
        List<String> irOriginal = irGenOriginal.generate(ast);
        
        Files.write(
            outputDir.resolve(baseName + ".ir"),
            irOriginal
        );
        AstNode processedAst = ast;
        List<String> irProcessed = irOriginal;

        if (optimize) {
            System.out.println("=== INICIANDO OPTIMIZACIÓN ===");
            processedAst = ConstantFolder.optimize(ast);
            IrGenerator irGenOptimized = new IrGenerator();
            irProcessed = irGenOptimized.generate(processedAst);
            if (dumpIr) {
                Files.write(
                    outputDir.resolve(baseName + "_opt.ir"),
                    irProcessed
                );
                printIrComparison(irOriginal, irProcessed);
            }

            System.out.println("=== FIN OPTIMIZACIÓN ===");
        }
        if (generateMips) {
            MipsGenerator mipsGenerator = new MipsGenerator();
            String mipsCode = mipsGenerator.generate(processedAst);

            Files.write(
                    outputDir.resolve(baseName + ".s"),
                    mipsCode.getBytes()
            );
        }

        System.out.println("Compilación finalizada correctamente");

    } catch (Exception e) {
        throw new CompilationException("Error durante compilación", e);
    }
}


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
