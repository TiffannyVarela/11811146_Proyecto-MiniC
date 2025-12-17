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
  li $t0, 2
  sw $t0, -16($fp)
  li $t0, 4
  sw $t0, -20($fp)
  li $t0, 2
  sw $t0, -24($fp)
  li $t0, 5
  sw $t0, -28($fp)
  li $t0, 2
  sw $t0, -32($fp)
  li $t0, 2
  sw $t0, -36($fp)
  li $t0, 2
  move $v0, $t0

  # === EPILOGUE ===
  move $sp, $fp
  lw $fp, 0($sp)
  lw $ra, 4($sp)
  addiu $sp, $sp, 8
  jr $ra