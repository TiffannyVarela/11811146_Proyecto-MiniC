# runtime.s - Runtime library for MiniC
# Funciones de E/S para MIPS32 (ABI O32)

.data
newline: .asciiz "\n"
true_str: .asciiz "true"
false_str: .asciiz "false"

.text
.globl print_int, print_char, print_bool, print_str, println
.globl read_int, read_char

# void print_int(int x)
print_int:
    move $a0, $a0        # El argumento ya está en $a0
    li $v0, 1            # syscall: print integer
    syscall
    jr $ra

# void print_char(char c)
print_char:
    move $a0, $a0        # El argumento ya está en $a0
    li $v0, 11           # syscall: print character
    syscall
    jr $ra

# void print_bool(bool b)
print_bool:
    beqz $a0, print_false
    # Print "true"
    la $a0, true_str
    li $v0, 4            # syscall: print string
    syscall
    jr $ra
print_false:
    # Print "false"
    la $a0, false_str
    li $v0, 4            # syscall: print string
    syscall
    jr $ra

# void print_str(const char* s)
print_str:
    move $a0, $a0        # El argumento ya está en $a0
    li $v0, 4            # syscall: print string
    syscall
    jr $ra

# void println()
println:
    la $a0, newline
    li $v0, 4            # syscall: print string
    syscall
    jr $ra

# int read_int()
read_int:
    li $v0, 5            # syscall: read integer
    syscall
    jr $ra

# char read_char()
read_char:
    li $v0, 12           # syscall: read character
    syscall
    jr $ra

# void read_str(char* buf, int maxlen)
read_str:
    # $a0 = buf (dirección del buffer)
    # $a1 = maxlen (máximo de caracteres a leer)
    
    # Guardar $a0 y $a1 porque syscall los modifica
    addiu $sp, $sp, -8
    sw $a0, 0($sp)
    sw $a1, 4($sp)
    
    # Leer string con syscall 8
    move $a0, $a0        # Buffer ya está en $a0
    move $a1, $a1        # Maxlen ya está en $a1
    li $v0, 8            # syscall: read string
    syscall
    
    # Restaurar registros
    lw $a1, 4($sp)
    lw $a0, 0($sp)
    addiu $sp, $sp, 8
    
    # Eliminar el newline al final si existe
    move $t0, $a0        # $t0 = puntero al buffer
read_str_loop:
    lb $t1, 0($t0)       # Cargar carácter actual
    beqz $t1, read_str_end # Fin del string
    addiu $t0, $t0, 1    # Siguiente carácter
    j read_str_loop
read_str_end:
    # Reemplazar newline con null
    addiu $t0, $t0, -1
    li $t1, '\n'
    lb $t2, 0($t0)
    bne $t2, $t1, read_str_no_newline
    sb $zero, 0($t0)     # Reemplazar \n con \0
read_str_no_newline:
    jr $ra

# Agregar al .globl
.globl read_str