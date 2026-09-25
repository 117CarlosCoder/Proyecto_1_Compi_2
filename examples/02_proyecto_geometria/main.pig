import figuras.Puntos.y;
import graficos.PlanoCartesiano.z;

VARIABILES>
esto p1 : Punto;
esto p2 : Punto;
esto r : Rectangulo;
esto plano : PlanoCartesiano;
esto totalArea : numerus 0;
esto distancia : numerus 0;

MAIOR>
>> "=== PROYECTO 02: GEOMETRIA MODULAR ===";
p1 = novus Punto();
p1.x = 4;
p1.y = 8;

p2 = novus Punto();
p2.x = 2;
p2.y = 3;

r = novus Rectangulo();
r.ancho = 10;
r.alto = 5;

totalArea = area(r);
>> "Area calculada con funcion de modulo Y:";
>> totalArea;

plano = novus PlanoCartesiano(0, 0);
distancia = plano.calcularDistanciaOrigen(p1.x, p1.y);
>> "Distancia obtenida con metodo de clase Zetariano:";
>> distancia;
FINIS;
