.data
newline: .asciiz "\n"
true_str: .asciiz "true"
false_str: .asciiz "false"

.text
.globl main
main:
  # === PRÓLOGO ===
  addiu $sp, $sp, -40
  sw $ra, 36($sp)
  sw $fp, 32($sp)
  move $fp, $sp
  # Guardar registros callee-saved usados
  sw $s0, 28($sp)
  sw $s1, 24($sp)
  sw $s2, 20($sp)
  sw $s3, 16($sp)
  sw $s4, 12($sp)
  sw $s5, 8($sp)
  sw $s6, 4($sp)
  sw $s7, 0($sp)
  addiu $sp, $sp, -24
  li $t0, 0
  li $t1, 0
  add $t2, $t0, $t1
  move $v0, $t2
  addiu $sp, $sp, 24
  # Restaurar registros callee-saved
  lw $s7, 0($sp)
  lw $s6, 4($sp)
  lw $s5, 8($sp)
  lw $s4, 12($sp)
  lw $s3, 16($sp)
  lw $s2, 20($sp)
  lw $s1, 24($sp)
  lw $s0, 28($sp)
  lw $fp, 32($sp)
  lw $ra, 36($sp)
  addiu $sp, $sp, 40
  jr $ra