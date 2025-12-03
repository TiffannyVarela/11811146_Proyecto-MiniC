// Prueba arreglos 2D, strings y E/S completa
string mensaje = "Resultado matriz:";

int main() {
    int matriz[3][4];
    int i, j, cont = 1;
    
    // Llenar matriz
    for (i = 0; i < 3; i = i + 1) {
        for (j = 0; j < 4; j = j + 1) {
            matriz[i][j] = cont;
            cont = cont + 1;
        }
    }
    
    // Imprimir
    print_str(mensaje);
    println();
    
    for (i = 0; i < 3; i = i + 1) {
        for (j = 0; j < 4; j = j + 1) {
            print_int(matriz[i][j]);
            print_str(" ");
        }
        println();
    }
    
    return 0;
}