VARIABILES>
esto a : numerus 15;
esto b : numerus 5;
esto suma : numerus 0;
esto mensaje : textum "El resultado de la operacion es: ";
esto activo : booleanus verum;
series valores[3] : numerus {10, 20, 30};

MAIOR>
>> "=== PROYECTO 01: CALCULOS BASICOS ===";
suma = a + b * 2;
>> mensaje;
>> suma;

si (suma > 20) {
    >> "La suma supera el limite de 20";
} aliter {
    >> "La suma es menor o igual a 20";
}
finis;

dum (a > 10) {
    >> a;
    a = a - 2;
}
finis;

valores[0] = suma;
>> valores[0];
FINIS;
