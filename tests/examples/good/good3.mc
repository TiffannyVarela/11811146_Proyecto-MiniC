int main() {
    // Expresiones aritméticas básicas que deben plegarse
    int a = 5 + 3 * 2;        // 5 + 6 = 11
    int b = (10 - 2) / 4;     // 8 / 4 = 2
    int c = a * b;            // 11 * 2 = 22
    
    // Operaciones con diferentes tipos de constantes
    int d = 100 / 5;          // 20
    int e = 7 - 3 + 2;        // 6
    int f = (d + e) * 2;      // (20 + 6) * 2 = 52
    
    // Expresiones anidadas
    int g = ((4 + 2) * 3) - 5; // (6 * 3) - 5 = 18 - 5 = 13
    
    return c + f + g;
}