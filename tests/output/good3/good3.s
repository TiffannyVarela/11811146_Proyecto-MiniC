.data
newline: .asciiz "\n"
true_str: .asciiz "true"
false_str: .asciiz "false"

.text
.globl main

# =========================================
# RUNTIME FUNCTIONS (provided by runtime.s)
# =========================================

# =========================================
# FUNCTION: main
# =========================================
main:
  # === PROLOGUE ===
  addiu $sp, $sp, -8
  sw $ra, 4($sp)
  sw $fp, 0($sp)
  move $fp, $sp
  addiu $sp, $sp, -8
  # === Process parameters ===
  li $t0, 11
  sw $t0, -16($fp)
  li $t0, 2
  sw $t0, -20($fp)
  lw $t0, -12($fp)
  li $t1, 11
  mul $t2, $t0, $t1
  sw $t2, -24($fp)
  li $t2, 20
  sw $t2, -28($fp)
  li $t2, 6
  sw $t2, -32($fp)
  lw $t2, -24($fp)
  li $t1, 20
  addu $t0, $t2, $t1
  li $t1, 2
  mul $t2, $t0, $t1
  sw $t2, -36($fp)
  li $t2, 8
  lw $t1, -36($fp)
  addu $t0, $t2, $t1
  move $v0, $t0

  # === EPILOGUE ===
  move $sp, $fp
  lw $fp, 0($sp)
  lw $ra, 4($sp)
  addiu $sp, $sp, 8
  jr $ra