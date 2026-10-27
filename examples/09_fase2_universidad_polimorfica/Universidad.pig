import servicios.Finanzas.y
import modelos.MiembroUniversitario.z
import modelos.EmpleadoAcademico.z
import modelos.ProfesorTitular.z
import modelos.EstudianteBecado.z
import servicios.Facultad.z

VARIABILES>
esto decano : novus ProfesorTitular(101, "Dr. Turing", 4000, 10, 6) : ProfesorTitular;
esto coord : novus EmpleadoAcademico(102, "Ing. Lovelace", 6) : EmpleadoAcademico;
esto becadoExcelencia : novus EstudianteBecado(201, "Ada", 95, 100.5) : EstudianteBecado;
esto becadoRegular : novus EstudianteBecado(202, "Babbage", 82, 150.25) : EstudianteBecado;
esto facultad : novus Facultad("Ingenieria") : Facultad;
esto buscado : null : MiembroUniversitario;

MAIOR>
// Sobrecarga de metodos y asignacion de supervisor (polimorfismo Persona/Empleado)
decano.asignarBono(300);
coord.asignarBono(0.10, verum);
coord.asignarSupervisor(decano);

facultad.registrar(decano);
facultad.registrar(coord);
facultad.registrar(becadoExcelencia);
facultad.registrar(becadoRegular);

facultad.generarReporte();

buscado = facultad.buscarPorId(999);
si (buscado == null) {
    >> "Busqueda 999: No encontrado";
} finis;

// Llamada encadenada sobre retorno polimorfico y covariante
>> "Representante de 102 via cadena: " >> facultad.buscarPorId(102).obtenerRepresentante().obtenerRol();
>> "Impuesto global .y sobre 1000: " >> calcularImpuesto(1000);
FINIS;

