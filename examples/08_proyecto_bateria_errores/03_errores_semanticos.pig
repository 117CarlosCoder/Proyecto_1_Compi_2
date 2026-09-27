VARIABILES>
esto numero : numerus 10;
esto numero : numerus 20;
esto bandera : booleanus verum;
esto cadenaTexto : textum "hola";
series arregloNumeros[3] : numerus {1, 2, 3};

MAIOR>
// Asignacion incompatible
numero = bandera;
bandera = 500;
cadenaTexto = falsus;

// Operaciones invalidas
numero = cadenaTexto * 5;
bandera = 10 / cadenaTexto;

// Acceso con indice no entero
numero = arregloNumeros["indiceInvalido"];

// Acceso a variables no declaradas
variableInexistente = 99;
>> variableFantasma;

// Llamadas a funciones no declaradas y argumentos incorrectos
funcionQueNoExiste(1, 2, 3);

// Sentencias de salto fuera de bucle
interrumpe;
perge;
FINIS;
