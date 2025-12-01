.data
newline: .asciiz "\n"
true_str: .asciiz "true"
false_str: .asciiz "false"

.text
.globl main
main:
  addiu $sp, $sp, -8
  sw $ra, 4($sp)
  sw $fp, 0($sp)
  move $fp, $sp
  addiu $sp, $sp, -28
  li $t0, 0
  li $t1, 0
  add $t2, $t0, $t1
  move $v0, $t2
  li $v0, 10
  syscall
  addiu $sp, $sp, 28
  lw $fp, 0($sp)
  lw $ra, 4($sp)
  addiu $sp, $sp, 8
  jr $ra