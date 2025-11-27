int contador;

int main() {
    contador = 0;

    while (contador < 5) {
        contador = contador + 1;
    }

    if (contador == 5) {
        contador = contador + 10;
    } else {
        contador = 0;
    }

    return contador;
}
