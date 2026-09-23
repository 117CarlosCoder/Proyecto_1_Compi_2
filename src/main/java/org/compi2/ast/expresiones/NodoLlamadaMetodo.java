package org.compi2.ast.expresiones;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.compi2.analisis_semantico.ComprobadorTipos;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public class NodoLlamadaMetodo implements NodoAST {
    private final NodoAST estructura;
    private final String nombreMetodo;
    private final List<NodoAST> argumentos;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Tipo tipoEstructura = estructura.comprobar(tablaSimbolos, errores);
        if (tipoEstructura.getBase() == TipoBase.ERROR) return Tipo.ERROR;

        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito() : "Global";

        if (tipoEstructura.getBase() != TipoBase.OBJETO_CLASE) {
            errores.add(new ErrorSemantico(
                "Solo las instancias de clase pueden invocar métodos. Tipo recibido: " + tipoEstructura,
                ambito, linea, columna));
            return Tipo.ERROR;
        }

        Tipo metodo = tipoEstructura.buscarMetodo(nombreMetodo);
        if (metodo == null) {
            errores.add(new ErrorSemantico(
                String.format("El método '%s' no está definido en la clase '%s'.",
                    nombreMetodo, tipoEstructura.getNombreTipo()),
                ambito, linea, columna));
            return Tipo.ERROR;
        }

        List<Tipo> tiposArgs = new ArrayList<>();
        if (argumentos != null) {
            for (NodoAST arg : argumentos) {
                tiposArgs.add(arg.comprobar(tablaSimbolos, errores));
            }
        }

        List<Tipo> esperados = metodo.getParametros() != null ? metodo.getParametros() : Collections.emptyList();
        if (esperados.size() != tiposArgs.size()) {
            errores.add(new ErrorSemantico(
                String.format("Número de argumentos incorrecto para el método '%s'. Esperados: %d, Recibidos: %d.",
                    nombreMetodo, esperados.size(), tiposArgs.size()),
                ambito, linea, columna));
            return Tipo.ERROR;
        }

        for (int i = 0; i < esperados.size(); i++) {
            Tipo esp = esperados.get(i);
            Tipo act = tiposArgs.get(i);
            if (act.getBase() != TipoBase.ERROR && !ComprobadorTipos.esCompatibleAsignacion(esp, act)) {
                errores.add(new ErrorSemantico(
                    String.format("Argumento %d incompatible en el método '%s': se esperaba %s y se recibió %s.",
                        i + 1, nombreMetodo, esp, act),
                    ambito, linea, columna));
            }
        }

        return (metodo.getTipoRetorno() != null) ? metodo.getTipoRetorno() : Tipo.VOID;
    }
}