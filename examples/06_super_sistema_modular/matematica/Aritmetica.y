% funciones
potencia(entero base, entero exp) -> entero:
    si (exp <= 0) entonces
        retornar 1
    contrario
        retornar base * potencia(base, exp - 1)

factorial(entero n) -> entero:
    si (n <= 1) entonces
        retornar 1
    contrario
        retornar n * factorial(n - 1)
