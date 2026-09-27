import modelos.Estudiante.z;
import utilidades.Estadisticas.y;

VARIABILES>
esto alumno1 : Estudiante;
esto alumno2 : Estudiante;
esto reporte : ReporteGrupo;
esto prom1 : numerus 0;
esto prom2 : numerus 0;
esto estado1 : textum "";
esto escala1 : textum "";
esto tasa : numerus 0;

MAIOR>
>> "=== PROYECTO 04: CONTROL ACADEMICO ===";
alumno1 = novus Estudiante(2023001, 85, 90, 78);
alumno2 = novus Estudiante(2023002, 50, 45, 60);

prom1 = alumno1.calcularPromedio();
prom2 = alumno2.calcularPromedio();

>> "Promedio Alumno 1:";
>> prom1;
estado1 = evaluar_estado(prom1);
>> "Estado Alumno 1:";
>> estado1;
escala1 = clasificar_desempeno(1);
>> "Desempeno Alumno 1 (elegir Y?):";
>> escala1;

>> "Promedio Alumno 2:";
>> prom2;
>> "Estado Alumno 2:";
>> evaluar_estado(prom2);
>> "Desempeno Alumno 2 (elegir Y?):";
>> clasificar_desempeno(4);

reporte = novus ReporteGrupo();
reporte.totalAlumnos = 2;
reporte.aprobados = 1;
reporte.reprobados = 1;

tasa = calcular_tasa_aprobacion(reporte.totalAlumnos, reporte.aprobados);
>> "Tasa de aprobacion (%):";
>> tasa;
FINIS;
