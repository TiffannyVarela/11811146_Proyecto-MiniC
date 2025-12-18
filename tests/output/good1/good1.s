.data
newline: .asciiz "\n"
true_str: .asciiz "true"
false_str: .asciiz "false"
result_msg: .asciiz "Result: "

.text
.globl main, __start, print_int, print_char, print_bool, print_str, println, read_int, read_char, read_str, exit

# =========================================
# RUNTIME FUNCTIONS
# =========================================

__start:
  # === SYSTEM INITIALIZATION ===
  addiu $sp, $sp, -64    # Reservar stack para sistema
  sw $ra, 60($sp)        # Guardar return address
  sw $fp, 56($sp)        # Guardar frame pointer
  move $fp, $sp          # Establecer frame pointer
  
  # === CALL MAIN ===
  jal main               # Ejecutar programa principal
  
  # === DISPLAY RESULT ===
  move $t0, $v0          # Guardar resultado
  la $a0, result_msg     # Cargar mensaje 'Result: '
  li $v0, 4              # syscall: print string
  syscall
  move $a0, $t0          # Cargar resultado numérico
  li $v0, 1              # syscall: print integer
  syscall
  la $a0, newline        # Nueva línea
  li $v0, 4
  syscall
  
  # === EXIT PROGRAM ===
  li $v0, 10             # syscall: exit
  syscall

print_int:
  move $a0, $a0          # Argumento ya está en $a0
  li $v0, 1              # syscall: print integer
  syscall
  jr $ra                 # Retornar

print_char:
  move $a0, $a0
  li $v0, 11             # syscall: print character
  syscall
  jr $ra

print_bool:
  beqz $a0, print_false  # Si es 0, imprimir false
  la $a0, true_str       # Cargar 'true'
  li $v0, 4
  syscall
  jr $ra
print_false:
  la $a0, false_str      # Cargar 'false'
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
  li $v0, 5              # syscall: read integer
  syscall
  jr $ra

read_char:
  li $v0, 12             # syscall: read character
  syscall
  jr $ra

read_str:
  addiu $sp, $sp, -8     # Guardar registros
  sw $a0, 0($sp)
  sw $a1, 4($sp)
  move $a0, $a0          # Buffer
  move $a1, $a1          # Máxima longitud
  li $v0, 8              # syscall: read string
  syscall
  lw $a1, 4($sp)         # Restaurar registros
  lw $a0, 0($sp)
  addiu $sp, $sp, 8
  jr $ra

exit:
  move $a0, $a0          # Código de salida
  li $v0, 17             # syscall: exit2
  syscall
  # No retorna


# =========================================
# FUNCTION: main
# =========================================
main:
  # === PROLOGUE ===
  move $fp, $sp          # $fp apunta al tope actual del stack
  addiu $sp, $sp, -32  # Reservar espacio para variables locales
  # Inicializar todas las variables locales a 0
  sw $zero, 0($sp)
  sw $zero, 4($sp)
  sw $zero, 8($sp)
  sw $zero, 12($sp)
  sw $zero, 16($sp)
  sw $zero, 20($sp)
  sw $zero, 24($sp)
  sw $zero, 28($sp)
  li $t0, 5
  sw $t0, 4($sp)
  li $t0, 5
  sw $t0, 4($fp)
  lw $t0, 0($sp)
  lw $t1, 4($sp)
  addu $t2, $t0, $t1
  sw $t2, 8($fp)
  li $t2, 0
  move $v0, $t2

  # === EPILOGUE ===
  addiu $sp, $sp, 32  # Liberar variables locales
  jr $ra  # Retorna a __start