package org.minic;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.minic.ast.*;
import org.minic.semantic.SemanticChecker;

public class Compiler {
    public static void compile(AstNode ast, String sourceFile){
       try {
            System.out.println("=================== Generación del Árbol AST ==================");
            System.out.println("AST generado correctamente.");
            System.out.println("=================== Análisis Semántico ==================");
            SemanticChecker semanticChecker = new SemanticChecker();
            semanticChecker.check(ast);

            System.out.println("=================== Generación de Código Intermedio ==================");
            //IrGenerator irGenerator = new IrGenerator();
            //Object irProgram = irGenerator.generate(ast);

            System.out.println("=================== Generación de Código MIPS ==================");
            //MipsGenerator mipsGenerator = new MipsGenerator();
            //String mipsCode = mipsGenerator.generate(irProgram, sourceFile);

            //String outputFilePath = sourceFile.replace(".mc", ".s");
            //writeToFile(mipsCode, outputFilePath);
            //System.out.println("Código MIPS generado en: " + outputFilePath);
            printAstInfo(ast);
       } catch (CompilationException e) {
           throw e;
       } catch (Exception e) {
           throw new CompilationException("Error durante la compilación: " + e.getMessage(), e);
       }
    }

    private static void printAstInfo(AstNode ast){
        if(ast instanceof ProgramNode){
            ProgramNode programNode = (ProgramNode) ast;
            System.out.println("Número de declaraciones en el programa: " + programNode.getDeclarationsNodes().size());
            for(AstNode decl : programNode.getDeclarationsNodes()){
                if(decl instanceof FunctionNode){
                    FunctionNode funcNode = (FunctionNode) decl;
                    System.out.println("Función: " + funcNode.getName() +" -> " + funcNode.getReturnType() + ", Parámetros: " + funcNode.getParameters().size());
                }
                else if(decl instanceof VarDeclNode){
                    VarDeclNode varNode = (VarDeclNode) decl;
                    System.out.println("Variable Global: " + varNode.getName() + " Tipo: " + varNode.getType());
                }
            }
        }
    }

    private static void writeToFile(String content, String filePath){
        try {
            Files.write(Paths.get(filePath), content.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Error al escribir el archivo: "+filePath + ": " + e.getMessage());
        }
    }
}
