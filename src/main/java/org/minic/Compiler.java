package org.minic;

import java.nio.file.Files;
import java.nio.file.Path;
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
            ErrorManager.cleanErrors();
            System.out.println("\n--- Análisis Semántico ---");
            SemanticChecker semanticChecker = new SemanticChecker();
            semanticChecker.check(ast);

            if (ErrorManager.hasErrors()) {
                ErrorManager.throwIfErrors();
            }

            System.out.println("Análisis semántico completado");
            
            AstNode processedAst = ast;
            List<String> irBeforeOptimization = null;
            List<String> irAfterOptimization = null;

            //Obtener nombre base del archivo
            Path sourcePath = Paths.get(sourceFile);
            String sourceName = sourcePath.getFileName().toString();
            String baseName = sourceName.replace(".mc", "");

            //Ruta base de archivos generados
            Path baseOutputDir = Paths.get("tests/output");
            Path programOutputDir = baseOutputDir.resolve(baseName);

            //Generar IR sin optimizar
            System.out.println("\n--- CODIGO IR SIN OPTIMIZAR ---");
            IrGenerator irGenerator = new IrGenerator();
            List<String> irNoOpt = irGenerator.generate(ast);

            //Guardar IR sin optimizar
            writeIrToFile(irNoOpt, programOutputDir, baseName, "");
            System.out.println("Codigo IR sin optimizar guardado en: " + programOutputDir.resolve(baseName + ".ir"));

            if (dumpIr && optimize) {
                System.out.println("\n--- CÓDIGO IR (ANTES de optimización) ---");
                irBeforeOptimization = irNoOpt;//Generado antes
                printIrSideBySide(irBeforeOptimization, null, "ANTES");
            }

            if (optimize) {
                System.out.println("\n--- Optimización ---");
                processedAst = ConstantFolder.optimize(ast);
                System.out.println("Optimización de constantes completada");

                //Generar IR optimizado
                System.out.println("\n--- Generando Codigo IR Optimizado ---");
                IrGenerator irGeneratorOp = new IrGenerator();
                irAfterOptimization = irGeneratorOp.generate(processedAst);

                //Guardar IR optimizado
                writeIrToFile(irAfterOptimization, programOutputDir, baseName, "_opt");
                System.out.println("Codigo IR optimizado guardado en: " + programOutputDir.resolve(baseName + "_opt.ir"));
                
                if (dumpIr && optimize) {
                    System.out.println("\n--- CÓDIGO IR (DESPUÉS de optimización) ---");

                    if (irBeforeOptimization != null) {
                        printIrSideBySide(irBeforeOptimization, irAfterOptimization, "COMPARACIÓN");
                    } else {
                        printIrSideBySide(null, irAfterOptimization, "DESPUÉS");
                    }
                }
            }

            if (dumpIr && !optimize) {
                System.out.println("\n--- GENERACIÓN DE CÓDIGO INTERMEDIO ---");
                printIrSideBySide(irNoOpt, null, "IR SIN OPTIMIZAR");
            }

            System.out.println("\n--- Generación de Código MIPS ---");
            MipsGenerator mipsGenerator = new MipsGenerator();
            String mipsCode = mipsGenerator.generate(processedAst);

            //Determinar nombre del .s
            String sFileName;
            if (outputFile != null) {
                Path outputPath = Paths.get(outputFile);
                sFileName = outputPath.getFileName().toString();
            } else {
                sFileName = baseName + ".s";
            }
            writeToFile(mipsCode, programOutputDir, sFileName);

            printAstInfo(processedAst);
            
        } catch (CompilationException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Error durante la compilación: " + e.getMessage());
            e.printStackTrace();
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
            System.out.println("- Funciones: " + functionCount);
            System.out.println("- Variables globales: " + globalVarCount);
            System.out.println("- Total de declaraciones: " + (functionCount + globalVarCount));
        }
    }

    private static void writeToFile(String content, Path outputDir, String fileName) {
        try {            
            // Crear carpeta tests/output si no existe
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            
            // Mantener el mismo nombre del archivo
            Path outputPath = outputDir.resolve(fileName);
            
            Files.write(outputPath, content.getBytes());
            System.out.println("Archivo guardado: " + outputPath);
            
            long fileSize = Files.size(outputPath);
            System.out.println("Tamaño del archivo: " + fileSize + " bytes");
            
        } catch (Exception e) {
            throw new RuntimeException("Error al escribir el archivo: " + e.getMessage());
        }
    }

    private static void writeIrToFile(List<String> irCode, Path outputDir, String baseName, String sufijo) {
        try {
            // Crear carpeta tests/output si no existe
            if (!Files.exists(outputDir)) {
                Files.createDirectories(outputDir);
            }
            
            // Crear nombre de archivo
            String fileName = baseName + sufijo + ".ir";
            Path outputPath = outputDir.resolve(fileName);
            
            String content = String.join("\n", irCode);
            Files.write(outputPath, content.getBytes());
            System.out.println("Archivo IR guardado: " + outputPath);
            
            long fileSize = Files.size(outputPath);
            System.out.println("Tamaño del archivo IR: " + fileSize + " bytes");
            
        } catch (Exception e) {
            throw new RuntimeException("Error al escribir el archivo IR: " +  e.getMessage());
        }
    }
}