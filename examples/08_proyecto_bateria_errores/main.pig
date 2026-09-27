import modulos.ErroresZetariano.z
import modulos.ErroresY.y
import modulo_inexistente.z

VARIABILES>
esto objErrores : novus ErroresZetariano(10);
esto total : numerus 0;
esto mensaje : textum "inicio";
esto activo : booleanus verum;

MAIOR>
// Error de instanciacion con clase inexistente
esto objInvalido : novus ClaseFantasma();

// Invocacion a metodo inexistente en objeto
objErrores.metodoQueNoExiste();

// Invocacion con argumentos invalidos en cantidad y tipo
objErrores.sumar("incompatible", verum, 100);

// Acceso a campo inexistente
objErrores.campoInexistente = 999;

// Invocacion a funcion de Y con parametros incompatibles
calcularInvalido(10, 20);

// Operacion invalida entre boolean y texto
total = activo + mensaje;

// Variables no declaradas
variableInexistente = 42;
>> otraVariableFantasma;

// Salto fuera de bucle
interrumpe;
FINIS;
