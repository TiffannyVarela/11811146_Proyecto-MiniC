int main() {
    int x = 5 + 3 * 2;      // Debe optimizarse a 11
    int y = (10 - 2) / 4;   // Debe optimizarse a 2
    return x + y;           // Debe optimizarse a return 13;
}