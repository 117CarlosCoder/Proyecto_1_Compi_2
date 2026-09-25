% estructuras
estructura Punto:
    entero x
    entero y

estructura Rectangulo:
    entero ancho
    entero alto

% funciones
area(Rectangulo r) -> entero:
    retornar r.ancho * r.alto

suma_puntos(Punto p1, Punto p2) -> entero:
    retornar p1.x + p2.x + p1.y + p2.y
