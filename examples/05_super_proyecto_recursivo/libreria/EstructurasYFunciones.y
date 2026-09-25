% estructuras
estructura NodoRecursivo:
    entero id
    entero valor
    entero profundidad

estructura Estadistica:
    entero maximo
    entero minimo
    flotante promedio

% funciones
factorial(entero n) -> entero:
    si (n <= 1) entonces
        retornar 1
    contrario
        retornar n * factorial(n - 1)

fibonacci(entero n) -> entero:
    si (n <= 0) entonces
        retornar 0
    si (n == 1) entonces
        retornar 1
    contrario
        retornar fibonacci(n - 1) + fibonacci(n - 2)

suma_recursiva(entero n) -> entero:
    si (n <= 0) entonces
        retornar 0
    contrario
        retornar n + suma_recursiva(n - 1)

procesar_nodo(NodoRecursivo nodo) -> entero:
    retornar nodo.valor * nodo.profundidad

funcion_con_error_logico(entero x) -> entero:
    retornar x + variable_inexistente_en_y
