package org.minic;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.minic.ast.*;
import org.minic.backend.mips.MipsGenerator;
import org.minic.semantic.SemanticChecker;

public class Compiler {
    public static void compile(AstNode ast, String sourceFile) {
       try {
            System.out.println("\n--- Análisis Semántico ---");
            SemanticChecker semanticChecker = new SemanticChecker();
            semanticChecker.check(ast);
            System.out.println("Análisis semántico completado sin errores");

            System.out.println("\n--- Generación de Código MIPS ---");
            MipsGenerator mipsGenerator = new MipsGenerator();
            String mipsCode = mipsGenerator.generate(ast);
            
            String outputFilePath = sourceFile.replace(".mc", ".s");
            writeToFile(mipsCode, outputFilePath);
            System.out.println("Código MIPS generado en: " + outputFilePath);
            
            // Mostrar información del AST
            printAstInfo(ast);
            
       } catch (CompilationException e) {
           throw e;
       } catch (Exception e) {
           throw new CompilationException("Error durante la compilación: " + e.getMessage(), e);
       }
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
}