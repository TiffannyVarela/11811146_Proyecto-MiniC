package org.minic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.minic.CompilationException;
import org.minic.ast.*;
import org.minic.semantic.*;

import semantic.SemanticChecker;

import org.minic.ir.*;
import org.minic.backend.mips.*;

public class Compiler {
    public static void compile(ParseTree tree, String sourceFile){
       try {
            System.out.println("=================== Análisis Semántico ==================");
            SemanticChecker semanticChecker = new SemanticChecker();
            semanticChecker.check(ast);

            System.out.println("=================== Generación de Código Intermedio ==================");
            IrGenerator irGenerator = new IrGenerator();
            Object irProgram = irGenerator.generate(ast);

            System.out.println("=================== Generación de Código MIPS ==================");
            MipsGenerator mipsGenerator = new MipsGenerator();
            String mipsCode = mipsGenerator.generate(irProgram, sourceFile);

            String outputFilePath = sourceFile.replace(".mc", ".s");
            writeToFile(mipsCode, outputFilePath);
            System.out.println("Código MIPS generado en: " + outputFilePath);

       } catch (CompilationException e) {
           throw e;
       } catch (Exception e) {
           throw new CompilationException("Error durante la compilación: " + e.getMessage(), e);
       }
    }

    private static void writeToFile(String content, String filePath){
        try {
            Files.write(Paths.get(filePath), content.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Error al escribir el archivo: " + e.getMessage());
        }
    }
}
