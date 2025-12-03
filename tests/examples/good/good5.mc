// Programa específico para mostrar optimizaciones side-by-side

int main() {
    // Expresiones que se optimizan
    int x = (2 + 3) * 4;          // Optimiza a 20
    int y = 10 / 2 + 5;           // Optimiza a 10
    int z = (100 - 50) * 0;       // Optimiza a 0
    
    // Expresiones booleanas optimizadas
    bool a = true && false;       // Optimiza a false
    bool b = (5 > 3) || false;    // Optimiza a true
    
    // Expresión compleja
    int result = x + y * z - (a ? 10 : 20) + (b ? 5 : 0);
    
    print_int(result);
    println();
    
    return 0;
}