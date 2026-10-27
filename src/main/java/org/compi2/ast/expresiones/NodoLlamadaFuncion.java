package org.compi2.ast.expresiones;

import lombok.Getter;
import org.compi2.analisis_semantico.ComprobadorTipos;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.Simbolo;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;
import org.compi2.tipos.Visibilidad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class NodoLlamadaFuncion implements NodoAST {
    private final String nombreFuncion;
    private final List<NodoAST> argumentos;
    private final int linea;
    private final int columna;

    public NodoLlamadaFuncion(String nombreFuncion, List<NodoAST> argumentos, int linea, int columna) {
        this.nombreFuncion = nombreFuncion;
        this.argumentos = argumentos;
        this.linea = linea;
        this.columna = columna;
    }

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
        if (esMetodoThis) {
            tipoFuncion = tClase.buscarMetodo(nombreFuncion, tiposArgs);
            if (tipoFuncion == null) {
                Tipo candAncestros = tClase.buscarMetodoAncestrosCualquiera(nombreFuncion, tiposArgs);
                if (candAncestros != null) {
                    Visibilidad vis = candAncestros.getVisibilidad();
                    String claseDecl = candAncestros.getClaseDeclarante() != null ? candAncestros.getClaseDeclarante() : tClase.getNombreTipo();
                    if (!tablaSimbolos.esAccesible(vis, claseDecl)) {
                        errores.add(new ErrorSemantico(
                            String.format("El método '%s' de la clase '%s' no es accesible (visibilidad %s).",
                                nombreFuncion, claseDecl, vis),
                            ambito, linea, columna));
                        return Tipo.ERROR;
                    }
                }
                if (simboloFuncion != null) {
                    tipoFuncion = simboloFuncion.getTipo();
                    this.esMetodoDeClaseActual = false;
                    this.claseObjetivoResuelta = null;
                    this.funcionResuelta = tipoFuncion;
                } else {
                    errores.add(new ErrorSemantico(
                        String.format("No existe una sobrecarga del método '%s' en la clase '%s' que coincida con los argumentos: %s.",
                            nombreFuncion, tClase.getNombreTipo(), tiposArgs),
                        ambito, linea, columna));
                    return Tipo.ERROR;
                }
            } else {
                Visibilidad vis = tipoFuncion.getVisibilidad();
                String claseDeclarante = tipoFuncion.getClaseDeclarante() != null ? tipoFuncion.getClaseDeclarante() : tClase.getNombreTipo();
                if (!tablaSimbolos.esAccesible(vis, claseDeclarante)) {
                    errores.add(new ErrorSemantico(
                        String.format("El método '%s' de la clase '%s' no es accesible (visibilidad %s).",
                            nombreFuncion, claseDeclarante, vis),
                        ambito, linea, columna));
                    return Tipo.ERROR;
                }
                this.esMetodoDeClaseActual = true;
                this.claseObjetivoResuelta = tClase.getNombreTipo();
                this.funcionResuelta = tipoFuncion;
            }
        } else {
            tipoFuncion = simboloFuncion.getTipo();
            this.esMetodoDeClaseActual = false;
            this.claseObjetivoResuelta = null;
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