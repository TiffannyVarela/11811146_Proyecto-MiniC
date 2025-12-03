// Programas con errores para probar diagnostico
int main() {
    int x = 5;
    string s = "hola";
    
    // Error: tipos incompatibles
    x = s;
    
    // Error: función no declarada
    funcion_inexistente();
    
    // Error: arreglo fuera de límites (constante)
    int arr[5];
    arr[10] = 1;
    
    // Error: falta return en función no-void
    // (el compilador debería detectarlo)
    
    return 0;
}