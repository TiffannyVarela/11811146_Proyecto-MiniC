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