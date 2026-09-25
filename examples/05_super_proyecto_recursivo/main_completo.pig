import libreria.EstructurasYFunciones.y;
import servicios.ProcesadorAvanzado.z;

VARIABILES>
// =========================================================================
// 1. VARIABLES VALIDAS: PRIMITIVAS, ESTRUCTURAS, OBJETOS Y ARREGLOS
// =========================================================================
esto n : numerus 5;
esto fact : numerus 0;
esto fibo : numerus 0;
esto sumaRec : numerus 0;
esto pot : numerus 0;
esto mcdVal : numerus 0;
esto tasaDecimal : decimalis 3.1416;
esto letraCodigo : littera 'R';
esto banderaActiva : booleanus verum;
esto descripcion : textum "Super Prueba Integral de Compilacion";

esto nodo : NodoRecursivo;
esto proc : ProcesadorAvanzado;
series arreglo[5] : numerus {10, 20, 30, 40, 50};
esto indice : numerus 0;
esto acumulado : numerus 0;
esto resulNodo : numerus 0;

// =========================================================================
// 2. VARIABLES CON FALLAS LOGICAS Y SEMANTICAS ADREDE EN DECLARACION
// =========================================================================
esto varErrorTipo : booleanus 9999;
esto variableDuplicada : numerus 100;
esto variableDuplicada : textum "segunda_declaracion_con_mismo_nombre";

MAIOR>
>> "=================================================================";
>> "          EJECUCION DEL SUPER ARCHIVO DE PRUEBA COMPLETO         ";
>> "=================================================================";

// -------------------------------------------------------------------------
// COMPROBACION 1: RECURSIVIDAD EN FUNCIONES MODULARES (Y?)
// -------------------------------------------------------------------------
>> "1. Probando Factorial Recursivo en Y?:";
fact = factorial(n);
>> fact;

>> "2. Probando Serie de Fibonacci Recursiva en Y?:";
fibo = fibonacci(7);
>> fibo;

>> "3. Probando Sumatoria Recursiva en Y?:";
sumaRec = suma_recursiva(10);
>> sumaRec;

// -------------------------------------------------------------------------
// COMPROBACION 2: RECURSIVIDAD Y METODOS EN CLASES POO (ZETARIANO)
// -------------------------------------------------------------------------
>> "4. Instanciando clase Zetariano y ejecutando potencia recursiva:";
proc = novus ProcesadorAvanzado(5);
pot = proc.potenciaRecursiva(2, 6);
>> "2 elevado a la 6 es:";
>> pot;

>> "5. Probando Maximo Comun Divisor recursivo (Euclides):";
mcdVal = proc.mcdRecursivo(48, 18);
>> "MCD entre 48 y 18 es:";
>> mcdVal;

// -------------------------------------------------------------------------
// COMPROBACION 3: ESTRUCTURAS, ACCESO A MIEMBROS Y FUNCIONES CON STRUCTS
// -------------------------------------------------------------------------
>> "6. Instanciando y manipulando struct NodoRecursivo de Y?:";
nodo = novus NodoRecursivo();
nodo.id = 101;
nodo.valor = 50;
nodo.profundidad = 4;
resulNodo = procesar_nodo(nodo);
>> "Resultado de procesar nodo:";
>> resulNodo;

// -------------------------------------------------------------------------
// COMPROBACION 4: ARREGLOS (SERIES) Y CICLOS
// -------------------------------------------------------------------------
>> "7. Recorriendo arreglo e iterando con ciclo dum:";
dum (indice < 5) {
    acumulado = acumulado + arreglo[indice];
    >> arreglo[indice];
    indice = indice + 1;
}
finis;
>> "Total acumulado del arreglo:";
>> acumulado;

// -------------------------------------------------------------------------
// COMPROBACION 5: CONDICIONALES
// -------------------------------------------------------------------------
si (fact > 100) {
    >> "El factorial calculado es mayor a 100";
} aliter {
    >> "El factorial calculado es menor o igual a 100";
}
finis;

// =========================================================================
// 3. SECCION DE COMPROBACION DE FALLAS LOGICAS Y SEMANTICAS ADREDE
//    (Estas lineas deben fallar para comprobar que el compilador captura
//     inconsistencias de tipos, ambitos, miembros y parametros)
// =========================================================================
>> ">>> PROVOCANDO FALLOS LOGICOS Y SEMANTICOS INTENCIONADOS <<<";

// Falla A: Asignacion incompatible (entero = entero + cadena)
fact = fact + "texto_incompatible_que_no_se_puede_sumar_a_entero";

// Falla B: Acceso a miembro inexistente en struct
nodo.campoInexistenteQueDebeFallar = 777;

// Falla C: Invocar constructor con signatura inexistente
proc = novus ProcesadorAvanzado("parametro_de_tipo_incorrecto", 10, 20, 30);

// Falla D: Argumento incompatible en llamada a funcion recursiva
sumaRec = suma_recursiva("texto_invalido_como_parametro_recursivo");

// Falla E: Indexar arreglo con tipo de dato no entero
arreglo["indice_cadena_invalido"] = 999;

// Falla F: Asignacion a identificador que nunca fue declarado
identificadorFantasmaTotalmenteInexistente = 404;

// Falla G: Error interno provocado en modulo Y? (variable inexistente)
fact = funcion_con_error_logico(10);

// Falla H: Error interno provocado en clase Zetariano (variable no declarada y retorno erroneo)
fibo = proc.metodoConFaltaDeLogica(20);

FINIS;
