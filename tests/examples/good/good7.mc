// Prueba punteros y paso por referencia
void incrementar(int* ptr) {
    *ptr = *ptr + 1;
}

int main() {
    int x = 5;
    int* px = &x;
    
    print_str("Antes: ");
    print_int(x);
    println();
    
    incrementar(&x);
    
    print_str("Despues: ");
    print_int(x);
    println();
    
    // Acceso a través de puntero
    *px = *px * 2;
    print_str("Doble: ");
    print_int(x);
    println();
    
    return 0;
}