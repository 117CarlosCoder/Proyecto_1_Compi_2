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

clasificar_desempeno(entero escala) -> cadena:
    elegir(escala):
        caso 1:
            retornar "SOBRESALIENTE"
        caso 2:
            retornar "NOTABLE"
        caso 3:
            retornar "APROBADO"
        siempre:
            retornar "REPROBADO"

