% estructuras
estructura Punto2D:
    entero x
    entero y

estructura Punto3D:
    entero x
    entero y
    entero z

% funciones
crear_punto_2d(entero x, entero y) -> entero:
    retornar x + y

distancia_manhattan(Punto2D p1, Punto2D p2) -> entero:
    retornar (p1.x - p2.x) + (p1.y - p2.y)
