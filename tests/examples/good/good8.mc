// Prueba todas las funciones de E/S
int main() {
    int num;
    char letra;
    string nombre[50];
    
    print_str("Ingrese un numero: ");
    num = read_int();
    
    print_str("Ingrese una letra: ");
    letra = read_char();
    
    print_str("Ingrese su nombre: ");
    read_str(nombre, 50);
    
    println();
    print_str("Numero: ");
    print_int(num);
    println();
    
    print_str("Letra: ");
    print_char(letra);
    println();
    
    print_str("Nombre: ");
    print_str(nombre);
    println();
    
    // Booleano
    bool es_mayor = num > 10;
    print_str("Es mayor que 10? ");
    print_bool(es_mayor);
    println();
    
    return 0;
}