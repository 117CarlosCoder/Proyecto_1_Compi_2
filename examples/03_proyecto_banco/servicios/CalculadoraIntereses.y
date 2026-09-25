% estructuras
estructura ResumenFinanciero:
    entero totalCapital
    entero tasaPorcentaje

% funciones
calcular_interes(entero capital, entero porcentaje) -> entero:
    retornar (capital * porcentaje) / 100

aplicar_comision(entero saldo, entero comision) -> entero:
    retornar saldo - comision
