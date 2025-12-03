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
    public static void compile(AstNode ast, String sourceFile, boolean dumpIr, boolean optimize, String outputFile) {
        try {
            System.out.println("\n--- Análisis Semántico ---");
            SemanticChecker semanticChecker = new SemanticChecker();
            semanticChecker.check(ast);
            System.out.println("Análisis semántico completado sin errores");
            
            AstNode processedAst = ast;
            List<String> irBeforeOptimization = null;
            List<String> irAfterOptimization = null;  // <-- AÑADIR esta variable

            if (dumpIr && optimize) {
                System.out.println("\n--- CÓDIGO IR (ANTES de optimización) ---");
                IrGenerator irGenerator = new IrGenerator();
                irBeforeOptimization = irGenerator.generate(ast);
                printIrSideBySide(irBeforeOptimization, null, "ANTES");
            }

            if (optimize) {
                System.out.println("\n--- Optimización ---");
                processedAst = ConstantFolder.optimize(ast);
                System.out.println("Optimización de constantes completada");
                
                if (dumpIr && optimize) {
                    System.out.println("\n--- CÓDIGO IR (DESPUÉS de optimización) ---");
                    IrGenerator irGenerator = new IrGenerator();
                    irAfterOptimization = irGenerator.generate(processedAst);
                    
                    if (irBeforeOptimization != null) {
                        printIrSideBySide(irBeforeOptimization, irAfterOptimization, "COMPARACIÓN");
                    } else {
                        printIrSideBySide(null, irAfterOptimization, "DESPUÉS");
                    }
                    
                    // Guardar IR optimizado en archivo
                    String irFilePath = sourceFile.replace(".mc", "_opt.ir");
                    writeIrToFile(irAfterOptimization, irFilePath);
                    System.out.println("Código IR optimizado guardado en: " + irFilePath);
                }
            }

            if (dumpIr && !optimize) {
                System.out.println("\n--- GENERACIÓN DE CÓDIGO INTERMEDIO ---");
                IrGenerator irGenerator = new IrGenerator();
                List<String> irCode = irGenerator.generate(processedAst);
                printIrSideBySide(irCode, null, "IR");
                String irFilePath = sourceFile.replace(".mc", ".ir");
                writeIrToFile(irCode, irFilePath);
                System.out.println("Código IR generado en: " + irFilePath);
            }

            System.out.println("\n--- Generación de Código MIPS ---");
            MipsGenerator mipsGenerator = new MipsGenerator();
            String mipsCode = mipsGenerator.generate(processedAst);
            String outputFilePath = outputFile != null ? outputFile : sourceFile.replace(".mc", ".s");
            writeToFile(mipsCode, outputFilePath);
            System.out.println("Código MIPS generado en: " + outputFilePath);
            printAstInfo(processedAst);
            
        } catch (CompilationException e) {
            throw e;
        } catch (Exception e) {
            throw new CompilationException("Error durante la compilación: " + e.getMessage(), e);
        }
    }

    private static void printIrSideBySide(List<String> irBefore, List<String> irAfter, String title) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("  " + title + " - CÓDIGO INTERMEDIO");
        System.out.println("=".repeat(80));
        
        if (irBefore == null && irAfter == null) {
            System.out.println("No hay código IR para mostrar");
            return;
        }
        
        if (irBefore != null && irAfter != null) {
            // Mostrar comparación lado a lado
            System.out.printf("%-40s | %-40s\n", "ANTES de optimización", "DESPUÉS de optimización");
            System.out.println("-".repeat(80));
            
            int maxLines = Math.max(irBefore.size(), irAfter.size());
            for (int i = 0; i < maxLines; i++) {
                String beforeLine = i < irBefore.size() ? irBefore.get(i) : "";
                String afterLine = i < irAfter.size() ? irAfter.get(i) : "";
                
                // Resaltar diferencias
                if (!beforeLine.equals(afterLine)) {
                    System.out.printf("\u001B[31m%-40s\u001B[0m | \u001B[32m%-40s\u001B[0m\n", 
                        truncate(beforeLine, 38), truncate(afterLine, 38));
                } else {
                    System.out.printf("%-40s | %-40s\n", 
                        truncate(beforeLine, 38), truncate(afterLine, 38));
                }
            }
            
            // Estadísticas
            System.out.println("\n" + "-".repeat(80));
            System.out.println("ESTADÍSTICAS DE OPTIMIZACIÓN:");
            System.out.println("  • Líneas antes: " + irBefore.size());
            System.out.println("  • Líneas después: " + irAfter.size());
            System.out.println("  • Líneas eliminadas: " + (irBefore.size() - irAfter.size()));
            System.out.println("  • Tasa de compresión: " + 
                String.format("%.1f%%", (1 - (double)irAfter.size()/irBefore.size()) * 100));
            
        } else if (irBefore != null) {
            // Solo mostrar "antes"
            System.out.println("Código IR (sin optimización):");
            System.out.println("-".repeat(80));
            for (String line : irBefore) {
                System.out.println("  " + line);
            }
        } else if (irAfter != null) {
            // Solo mostrar "después"
            System.out.println("Código IR (con optimización):");
            System.out.println("-".repeat(80));
            for (String line : irAfter) {
                System.out.println("  " + line);
            }
        }
        
        System.out.println("=".repeat(80));
    }

    private static String truncate(String str, int length) {
        if (str.length() <= length) return str;
        return str.substring(0, length - 3) + "...";
    }

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
            
            long fileSize = Files.size(Paths.get(filePath));
            System.out.println("Tamaño del archivo: " + fileSize + " bytes");
            
        } catch (Exception e) {
            throw new RuntimeException("Error al escribir el archivo: " + filePath + ": " + e.getMessage());
        }
    }

    private static void writeIrToFile(List<String> irCode, String filePath) {
        try {
            String content = String.join("\n", irCode);
            Files.write(Paths.get(filePath), content.getBytes());
            System.out.println("Archivo IR guardado: " + filePath);
            
            long fileSize = Files.size(Paths.get(filePath));
            System.out.println("Tamaño del archivo IR: " + fileSize + " bytes");
            
        } catch (Exception e) {
            throw new RuntimeException("Error al escribir el archivo IR: " + filePath + ": " + e.getMessage());
        }
    }
}