import geometria.Puntos.y;
import geometria.Formas.y;
import matematica.Aritmetica.y;
import matematica.Estadistica.y;
import entidades.Estudiante.z;
import entidades.Profesor.z;
import sistema.Logger.z;
import sistema.Seguridad.z;

VARIABILES>
esto p1 : Punto2D;
esto p2 : Punto2D;
esto r1 : Rectangulo;
esto caja1 : Caja;
esto alumno : Estudiante;
esto catedratico : Profesor;
esto log : Logger;
esto sec : Seguridad;

esto dist : numerus 0;
esto areaR : numerus 0;
esto volC : numerus 0;
esto potVal : numerus 0;
esto factVal : numerus 0;
esto promVal : numerus 0;
esto rangoVal : numerus 0;
esto promAlumno : numerus 0;
esto cargaProf : numerus 0;
esto eventos : numerus 0;
esto accesoOk : numerus 0;

MAIOR>
>> "==========================================================";
>> "  SISTEMA MODULAR INTEGRAL CON MULTIPLES IMPORTACIONES    ";
>> "==========================================================";

>> "[1] Probando Modulo geometria.Puntos.y";
p1 = novus Punto2D();
p1.x = 100;
p1.y = 50;

p2 = novus Punto2D();
p2.x = 20;
p2.y = 10;

dist = distancia_manhattan(p1, p2);
>> "Distancia Manhattan entre p1 y p2:";
>> dist;

>> "[2] Probando Modulo geometria.Formas.y";
r1 = novus Rectangulo();
r1.base = 15;
r1.altura = 8;
areaR = area_rectangulo(r1);
>> "Area de rectangulo:";
>> areaR;

caja1 = novus Caja();
caja1.largo = 4;
caja1.ancho = 5;
caja1.alto = 6;
volC = volumen_caja(caja1);
>> "Volumen de caja:";
>> volC;

>> "[3] Probando Modulos de Matematicas (Aritmetica y Estadistica)";
potVal = potencia(2, 5);
>> "2 elevado a la 5:";
>> potVal;

factVal = factorial(5);
>> "Factorial de 5:";
>> factVal;
>> "Operacion modo multiplicacion (elegir Y?):";
>> operar_segun_modo(3, 7, 8);

promVal = promedio_tres(80, 90, 100);
>> "Promedio de tres notas:";
>> promVal;

rangoVal = rango_valores(95, 45);
>> "Rango de valores (max - min):";
>> rangoVal;

>> "[4] Probando Modulos de Entidades POO en Zetariano";
alumno = novus Estudiante(2026001, 85, 95);
promAlumno = alumno.calcularPromedio();
>> "Carnet del estudiante:";
>> alumno.carnet;
>> "Promedio calculado por metodo:";
>> promAlumno;

catedratico = novus Profesor(9876, 4, 6);
cargaProf = catedratico.calcularCargaSemanal();
>> "Carga semanal del catedratico:";
>> cargaProf;

>> "[5] Probando Modulos del Sistema (Logger y Seguridad)";
log = novus Logger(1);
eventos = log.registrarEvento(101);
eventos = log.registrarEvento(102);
>> "Total eventos registrados:";
>> eventos;

sec = novus Seguridad(1234);
accesoOk = sec.verificarAcceso(1234);
>> "Acceso con pin correcto (1 = Exito, 0 = Fallo):";
>> accesoOk;

>> "==========================================================";
>> "  COMPILACION Y EJECUCION MODULAR COMPLETADA CON EXITO    ";
>> "==========================================================";
FINIS;
