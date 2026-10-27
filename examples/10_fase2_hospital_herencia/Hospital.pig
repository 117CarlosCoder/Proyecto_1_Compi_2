import hospital.Persona.z
import hospital.Paciente.z
import hospital.PacienteCritico.z
import hospital.SalaEmergencia.z

VARIABILES>
esto sala : novus SalaEmergencia("Trauma Central") : SalaEmergencia;
esto p1 : novus Paciente(501, "Carlos", 65, 2, 500) : Paciente;
esto p2 : novus PacienteCritico(502, "Elena", 45, 800, 3, verum) : Paciente;
esto p3 : novus Paciente("Visitante Express", 4) : Paciente;

MAIOR>
sala.ingresar(p1);
sala.ingresar(p2);
sala.ingresar(p3);
sala.atenderCola();
FINIS;

