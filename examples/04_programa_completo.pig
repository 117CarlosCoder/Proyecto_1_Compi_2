import Geometria.y;
import Calculadora.z;

VARIABILES>
esto pt : Punto;
esto calc : Calculadora;
esto resultado : numerus 0;

MAIOR>
pt = novus Punto();
pt.x = 10;
pt.y = 25;
calc = novus Calculadora(2);
resultado = calc.multiplicar(sumar_coords(pt));
>> resultado;
>> "Figura:";
>> describir_codigo_figura(1);
FINIS;
