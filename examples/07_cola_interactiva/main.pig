import Nodo.z
import Cola.z
import servicios.EstadisticasCola.y

VARIABILES>
esto miCola : novus Cola();
esto opcion : numerus -1;
esto elemento : numerus 0;
esto contador : numerus 0;

MAIOR>
imprimirEncabezado();

dum (opcion != 5) {
    >> "-----------------------------------------------";
    >> "Selecciona una operacion para la Cola:\n";
    >> "1. Encolar numero\n";
    >> "2. Desencolar numero\n";
    >> "3. Ver frente de la cola\n";
    >> "4. Imprimir cola completa\n";
    >> "5. Salir del programa\n";
    >> "-----------------------------------------------";
    opcion <<;

    si (opcion == 1) {
        >> "Ingresa el numero entero a encolar:\n";
        elemento <<;
        miCola.encolar(elemento);
        contador = contador + 1;
        >> "Elemento encolado correctamente.";
    } aliter (opcion == 2) {
        si (miCola.estaVacia()) {
            >> "La cola esta vacia, no se puede desencolar.";
        } aliter {
            elemento = miCola.desencolar();
            >> "Elemento desencolado: ";
            >> elemento;
        } finis;
    } aliter (opcion == 3) {
        si (miCola.estaVacia()) {
            >> "La cola esta vacia.";
        } aliter {
            elemento = miCola.verFrente();
            >> "Frente actual de la cola: ";
            >> elemento;
        } finis;
    } aliter (opcion == 4) {
        >> miCola.toString();
        >> formatearTotal(contador);
    } aliter (opcion == 5) {
        >> "Saliendo del programa de Cola. ¡Hasta pronto!";
    } aliter {
        >> "Opcion no valida. Intenta de nuevo.";
    } finis;

    >> "Presiona cualquier tecla para continuar...";
    elemento <<;
} finis;

FINIS;
