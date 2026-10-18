package org.compi2.ast.expresiones;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class NodoLlamadaFuncion implements NodoAST {
    private final String nombreFuncion;
    private final List<NodoAST> argumentos;
    private final int linea;
    private final int columna;

    private boolean esMetodoDeClaseActual;
    private String claseObjetivoResuelta;
    private Tipo funcionResuelta;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        this.esMetodoDeClaseActual = false;
        this.claseObjetivoResuelta = null;
        this.funcionResuelta = null;

        String ambito = (tablaSimbolos.getAmbitoActual() != null)
                ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";

        Simbolo simboloFuncion = tablaSimbolos.buscarFuncionGlobal(nombreFuncion);
        Simbolo sThis = tablaSimbolos.buscarSimbolo("this");
        Tipo tClase = (sThis != null && sThis.getTipo() != null)
                ? tablaSimbolos.buscarTipoCompuesto(sThis.getTipo().getNombreTipo())
                : null;
        boolean esMetodoThis = (tClase != null && tClase.existeMetodo(nombreFuncion));

        if (simboloFuncion == null && !esMetodoThis) {
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

        Tipo tipoFuncion;
        if (simboloFuncion != null) {
            tipoFuncion = simboloFuncion.getTipo();
            this.esMetodoDeClaseActual = false;
            this.claseObjetivoResuelta = null;
            this.funcionResuelta = tipoFuncion;
        } else {
            tipoFuncion = tClase.buscarMetodo(nombreFuncion, tiposArgs);
            if (tipoFuncion == null) {
                errores.add(new ErrorSemantico(
                    String.format("No existe una sobrecarga del método '%s' en la clase '%s' que coincida con los argumentos: %s.",
                        nombreFuncion, tClase.getNombreTipo(), tiposArgs),
                    ambito, linea, columna));
                return Tipo.ERROR;
            }
            this.esMetodoDeClaseActual = true;
            this.claseObjetivoResuelta = tClase.getNombreTipo();
            this.funcionResuelta = tipoFuncion;
        }

        List<Tipo> parametrosEsperados = tipoFuncion.getParametros();
        if (parametrosEsperados == null) {
            parametrosEsperados = Collections.emptyList();
        }

        if (parametrosEsperados.size() != tiposArgs.size()) {
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