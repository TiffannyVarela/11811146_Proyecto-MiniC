package org.minic.backend.mips;

public class MipsGenerator {
    
    public String generate(Object ir, String targetArch) {
        System.out.println("Generating MIPS code for target architecture: " + targetArch);
        // Implementar la lógica de generación de código MIPS aquí
        return "# Código MIPS generado para: " + targetArch + "\n" +
               ".text\n" +
               ".globl main\n" +
               "main:\n" +
               "    # TODO: Implementar generación de código\n" +
               "    jr $ra\n";
    }
}
