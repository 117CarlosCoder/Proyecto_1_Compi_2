import modulos.ErroresHerencia.z

VARIABILES>
esto baseObj : novus Base(10) : Base;
// Error: asignacion incompatible de superclase (Base) a variable de subclase (HijoInvalido)
esto hijoMalAsignado : novus Base(20) : HijoInvalido;

MAIOR>
// Error: acceso a campo y metodo protected desde fuera de la jerarquia de clases (.pig)
>> baseObj.codigoProtegido;
>> baseObj.validar();

// Error: acceso a campo y metodo default desde fuera de la clase declarante (.pig)
>> baseObj.secretoDefault;
baseObj.metodoDefault();
FINIS;

