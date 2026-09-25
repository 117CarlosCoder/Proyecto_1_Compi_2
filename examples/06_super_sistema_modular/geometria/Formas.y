% estructuras
estructura Rectangulo:
    entero base
    entero altura

estructura Caja:
    entero largo
    entero ancho
    entero alto

% funciones
area_rectangulo(Rectangulo r) -> entero:
    retornar r.base * r.altura

volumen_caja(Caja c) -> entero:
    retornar c.largo * c.ancho * c.alto
