% estructuras
estructura Punto:
    entero x
    entero y

% funciones
sumar_coords(Punto p) -> entero:
    retornar p.x + p.y

describir_codigo_figura(entero codigo) -> cadena:
    elegir(codigo):
        caso 1:
            retornar "Punto"
        caso 2:
            retornar "Linea"
        caso 3:
            retornar "Triangulo"
        siempre:
            retornar "Poligono General"

