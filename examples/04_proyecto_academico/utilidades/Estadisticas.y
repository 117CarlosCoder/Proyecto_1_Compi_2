% estructuras
estructura ReporteGrupo:
    entero totalAlumnos
    entero aprobados
    entero reprobados

% funciones
evaluar_estado(entero promedio) -> cadena:
    si (promedio >= 61) entonces
        retornar "APROBADO"
    contrario
        retornar "REPROBADO"

calcular_tasa_aprobacion(entero total, entero aprobados) -> entero:
    retornar (aprobados * 100) / total
