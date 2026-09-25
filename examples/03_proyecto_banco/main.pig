import entidades.CuentaBancaria.z;
import servicios.CalculadoraIntereses.y;

VARIABILES>
esto cuenta : CuentaBancaria;
esto resumen : ResumenFinanciero;
esto saldoActual : numerus 0;
esto interesGenerado : numerus 0;

MAIOR>
>> "=== PROYECTO 03: SISTEMA BANCARIO ===";
cuenta = novus CuentaBancaria(1001, 500);

>> "Saldo inicial:";
>> cuenta.consultarSaldo();

saldoActual = cuenta.depositar(250);
>> "Saldo tras deposito de 250:";
>> saldoActual;

saldoActual = cuenta.retirar(100);
>> "Saldo tras retiro de 100:";
>> saldoActual;

resumen = novus ResumenFinanciero();
resumen.totalCapital = cuenta.consultarSaldo();
resumen.tasaPorcentaje = 5;

interesGenerado = calcular_interes(resumen.totalCapital, resumen.tasaPorcentaje);
>> "Interes generado al 5%:";
>> interesGenerado;

saldoActual = cuenta.depositar(interesGenerado);
>> "Saldo final con intereses:";
>> saldoActual;
FINIS;
