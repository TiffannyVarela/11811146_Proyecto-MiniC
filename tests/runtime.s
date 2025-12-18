
.data
newline: .asciiz "\n"
true_str: .asciiz "true"
false_str: .asciiz "false"
err_uninit: .asciiz "ERROR: Variable no inicializada\n"

.text
.globl main, __start, print_int, print_char, print_bool, print_str, println
.globl read_int, read_char, read_str, exit, __init_stack

__start:
    jal __init_stack
    jal main
    # Si main retorna, terminar con código 0
    li $a0, 0
    jal exit

__init_stack:
    addiu $sp, $sp, -48
    
    sw $ra, 44($sp)
    sw $fp, 40($sp)
    sw $s0, 36($sp)
    sw $s1, 32($sp)
    
    move $fp, $sp
    
    li $t0, 0
    sw $t0, 0($sp)
    sw $t0, 4($sp)
    sw $t0, 8($sp)
    sw $t0, 12($sp)
    sw $t0, 16($sp)
    sw $t0, 20($sp)
    sw $t0, 24($sp)
    sw $t0, 28($sp)
    
    jr $ra

exit:
    # $a0 = código de salida
    # Imprime el valor antes de salir
    move $t0, $a0        # Guardar código
    
    # Imprimir "Result: "
    la $a0, result_msg
    li $v0, 4
    syscall
    
    # Imprimir valor numérico
    move $a0, $t0
    li $v0, 1
    syscall
    
    # Nueva línea
    la $a0, newline
    li $v0, 4
    syscall
    
    # Terminar programa
    move $a0, $t0
    li $v0, 17          # syscall: exit2
    syscall
    
print_int:
    move $a0, $a0
    li $v0, 1
    syscall
    jr $ra

print_char:
    move $a0, $a0
    li $v0, 11
    syscall
    jr $ra

print_bool:
    beqz $a0, print_false
    la $a0, true_str
    li $v0, 4
    syscall
    jr $ra
print_false:
    la $a0, false_str
    li $v0, 4
    syscall
    jr $ra

print_str:
    move $a0, $a0
    li $v0, 4
    syscall
    jr $ra

println:
    la $a0, newline
    li $v0, 4
    syscall
    jr $ra

read_int:
    li $v0, 5
    syscall
    jr $ra

read_char:
    li $v0, 12
    syscall
    jr $ra

read_str:
    addiu $sp, $sp, -8
    sw $a0, 0($sp)
    sw $a1, 4($sp)
    
    move $a0, $a0
    move $a1, $a1
    li $v0, 8
    syscall
    
    lw $a1, 4($sp)
    lw $a0, 0($sp)
    addiu $sp, $sp, 8
    
    move $t0, $a0
read_str_loop:
    lb $t1, 0($t0)
    beqz $t1, read_str_end
    addiu $t0, $t0, 1
    j read_str_loop
read_str_end:
    addiu $t0, $t0, -1
    li $t1, '\n'
    lb $t2, 0($t0)
    bne $t2, $t1, read_str_no_newline
    sb $zero, 0($t0)
read_str_no_newline:
    jr $ra

#      addu $t0, $zero, $v0  # valor cargado
load_var:
    # Esta función carga de (-12($fp)) convirtiendo offset negativo
    # $a0 = offset negativo (ej: -12)
    addu $t0, $fp, $a0    # Calcula dirección real
    lw $v0, 0($t0)        # Carga valor
    jr $ra

store_var:
    # $a0 = offset negativo (ej: -16)
    # $a1 = valor a guardar
    addu $t0, $fp, $a0    # Calcula dirección real
    sw $a1, 0($t0)        # Guarda valor
    jr $ra

.data
result_msg: .asciiz "Result: "