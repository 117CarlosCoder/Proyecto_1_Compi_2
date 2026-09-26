package org.compi2.ast.expresiones;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.compi2.analisis_semantico.ComprobadorTipos;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.Simbolo;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public class NodoLlamadaFuncion implements NodoAST {
    private final String nombreFuncion;
    private final List<NodoAST> argumentos;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Simbolo simboloFuncion = tablaSimbolos.buscarFuncionGlobal(nombreFuncion);
        Tipo tipoFuncion = (simboloFuncion != null) ? simboloFuncion.getTipo() : null;
        if (tipoFuncion == null) {
            Simbolo sThis = tablaSimbolos.buscarSimbolo("this");
            if (sThis != null && sThis.getTipo() != null) {
                Tipo tClase = tablaSimbolos.buscarTipoCompuesto(sThis.getTipo().getNombreTipo());
                if (tClase != null) {
                    tipoFuncion = tClase.buscarMetodo(nombreFuncion);
                }
            }
        }

        if (tipoFuncion == null) {
            String ambito = (tablaSimbolos.getAmbitoActual() != null)
                    ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                    : "Global";
            errores.add(new ErrorSemantico(
                "La función o método '" + nombreFuncion + "' no ha sido definida.",
                ambito, linea, columna));
            return Tipo.ERROR;
        }

        List<Tipo> tiposArgs = new ArrayList<>();
        if (argumentos != null) {
            for (NodoAST arg : argumentos) {
                tiposArgs.add(arg.comprobar(tablaSimbolos, errores));
            }
        }

        List<Tipo> parametrosEsperados = tipoFuncion.getParametros();
        if (parametrosEsperados == null) {
            parametrosEsperados = Collections.emptyList();
        }

        if (parametrosEsperados.size() != tiposArgs.size()) {
            String ambito = (tablaSimbolos.getAmbitoActual() != null)
                    ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                    : "Global";
            errores.add(new ErrorSemantico(
                String.format("Cantidad de argumentos inválida para '%s'. Se esperaban %d pero se recibieron %d.",
                    nombreFuncion, parametrosEsperados.size(), tiposArgs.size()),
                ambito, linea, columna));
            return Tipo.ERROR;
        }

        for (int i = 0; i < parametrosEsperados.size(); i++) {
            Tipo esperado = parametrosEsperados.get(i);
            Tipo actual = tiposArgs.get(i);

            if (actual.getBase() != TipoBase.ERROR && !ComprobadorTipos.esCompatibleAsignacion(esperado, actual)) {
                String ambito = (tablaSimbolos.getAmbitoActual() != null)
                        ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                        : "Global";
                errores.add(new ErrorSemantico(
                    String.format("Argumento %d incompatible en '%s': se esperaba %s y se recibió %s.",
                        i + 1, nombreFuncion, esperado, actual),
                    ambito, linea, columna));
            }
        }

        Tipo retorno = tipoFuncion.getTipoRetorno();
        return (retorno != null) ? retorno : Tipo.VOID;
    }
}