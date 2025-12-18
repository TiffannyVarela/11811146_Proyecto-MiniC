int main() {

    int a[5];
    int b[3][4];

    /* Arreglo 1D */
    a[0] = 10;
    a[1] = 20;
    a[2] = a[0] + a[1];
    a[3] = a[2] * 2;
    a[4] = a[1 + 3 - 2];

    /* Arreglo 2D */
    b[0][0] = 1;
    b[0][1] = 2;
    b[1][0] = 3;
    b[1][1] = b[0][0] + b[0][1];

    b[2][3] = b[1][1] * 2;

    /* Usar valores de arreglos */
    int x;
    x = a[2] + b[1][1];

    return x;
}
