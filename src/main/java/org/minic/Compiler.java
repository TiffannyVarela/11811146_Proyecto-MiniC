package org.minic;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.minic.ast.*;
import org.minic.backend.mips.MipsGenerator;
import org.minic.ir.IrGenerator;
import org.minic.optimizer.ConstantFolder;
import org.minic.semantic.SemanticChecker;

public class Compiler {
    // En Compiler.java, modifica el método compile:
public static void compile(AstNode ast, String sourceFile, boolean dumpIr, boolean optimize, String outputFile) {
       try {
            System.out.println("\n--- Análisis Semántico ---");
            SemanticChecker semanticChecker = new SemanticChecker();
            semanticChecker.check(ast);
            System.out.println("Análisis semántico completado sin errores");

            // OPTIMIZACIÓN (si está habilitada)
            AstNode processedAst = ast;
            if (optimize) {
                System.out.println("\n--- Optimización ---");
                processedAst = ConstantFolder.optimize(ast);
                System.out.println("Optimización de constantes completada");
            }

            // GENERACIÓN DE IR (si está habilitada)
            if (dumpIr) {
                System.out.println("\n--- Generación de Código Intermedio ---");
                IrGenerator irGenerator = new IrGenerator();
                List<String> irCode = irGenerator.generate(processedAst);
                
                String irFilePath = sourceFile.replace(".mc", ".ir");
                writeIrToFile(irCode, irFilePath);
                System.out.println("Código IR generado en: " + irFilePath);
            }

            System.out.println("\n--- Generación de Código MIPS ---");
            MipsGenerator mipsGenerator = new MipsGenerator();
            String mipsCode = mipsGenerator.generate(processedAst);
            
            // Usar archivo de salida personalizado si se especificó
            String outputFilePath = outputFile != null ? outputFile : sourceFile.replace(".mc", ".s");
            writeToFile(mipsCode, outputFilePath);
            System.out.println("Código MIPS generado en: " + outputFilePath);
            
            // Mostrar información del AST
            printAstInfo(processedAst);
            
       } catch (CompilationException e) {
           throw e;
       } catch (Exception e) {
           throw new CompilationException("Error durante la compilación: " + e.getMessage(), e);
       }
    }

    // === MÉTODO ORIGINAL (para compatibilidad) ===
    public static void compile(AstNode ast, String sourceFile) {
        compile(ast, sourceFile, false, false, null);
    }
    private static void printAstInfo(AstNode ast) {
        if (ast instanceof ProgramNode) {
            ProgramNode programNode = (ProgramNode) ast;
            System.out.println("\n--- INFORMACIÓN DEL PROGRAMA ---");
            System.out.println("Número de declaraciones: " + programNode.getDeclarationsNodes().size());
            
            int functionCount = 0;
            int globalVarCount = 0;
            
            for (AstNode decl : programNode.getDeclarationsNodes()) {
                if (decl instanceof FunctionNode) {
                    FunctionNode funcNode = (FunctionNode) decl;
                    String paramsInfo = funcNode.getParameters() != null ? 
                                       String.valueOf(funcNode.getParameters().size()) : "0";
                    System.out.println("Función: " + funcNode.getName() + 
                                     " -> " + funcNode.getReturnType() + 
                                     " (parámetros: " + paramsInfo + ")");
                    functionCount++;
                } else if (decl instanceof VarDeclNode) {
                    VarDeclNode varNode = (VarDeclNode) decl;
                    String arrayInfo = varNode.isArray() ? 
                                     "[tamaño: " + varNode.getArraySize() + "]" : "";
                    System.out.println("Variable: " + varNode.getName() + 
                                     " - tipo: " + varNode.getType() + " " + arrayInfo);
                    globalVarCount++;
                }
            }
            
            System.out.println("\n--- RESUMEN ---");
            System.out.println("• Funciones: " + functionCount);
            System.out.println("• Variables globales: " + globalVarCount);
            System.out.println("• Total de declaraciones: " + (functionCount + globalVarCount));
        }
    }

    private static void writeToFile(String content, String filePath) {
        try {
            Files.write(Paths.get(filePath), content.getBytes());
            System.out.println("Archivo guardado: " + filePath);
            
            // Mostrar tamaño del archivo generado
            long fileSize = Files.size(Paths.get(filePath));
            System.out.println("Tamaño del archivo: " + fileSize + " bytes");
            
        } catch (Exception e) {
            throw new RuntimeException("Error al escribir el archivo: " + filePath + ": " + e.getMessage());
        }
    }

    // === AGREGAR ESTE NUEVO MÉTODO PARA GUARDAR CÓDIGO IR ===
    private static void writeIrToFile(List<String> irCode, String filePath) {
        try {
            String content = String.join("\n", irCode);
            Files.write(Paths.get(filePath), content.getBytes());
            System.out.println("Archivo IR guardado: " + filePath);
            
            // Mostrar tamaño del archivo IR generado
            long fileSize = Files.size(Paths.get(filePath));
            System.out.println("Tamaño del archivo IR: " + fileSize + " bytes");
            
        } catch (Exception e) {
            throw new RuntimeException("Error al escribir el archivo IR: " + filePath + ": " + e.getMessage());
        }
    }
}