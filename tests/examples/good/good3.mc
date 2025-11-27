int datos[5];

int cuadrado(int x) {
    return x * x;
}

int main() {
    int i;

    for (i = 0; i < 5; i = i + 1) {
        datos[i] = cuadrado(i);
    }

    return datos[4];   // Esperado: 16
}
