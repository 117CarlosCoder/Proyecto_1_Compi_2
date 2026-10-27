package org.compi2.ast.sentencias;

import lombok.Getter;
import lombok.Setter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.Simbolo;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.Visibilidad;

import java.util.ArrayList;
import java.util.List;

@Getter
public class NodoFuncion implements NodoAST {
    private final String nombre;
    private final List<Parametro> parametros;
    private final Tipo tipoRetorno;
    private final List<NodoAST> cuerpo;
    private final int linea;
    private final int columna;

    public NodoFuncion(String nombre, List<Parametro> parametros, Tipo tipoRetorno, List<NodoAST> cuerpo, int linea, int columna) {
        this.nombre = nombre;
        this.parametros = parametros;
        this.tipoRetorno = tipoRetorno;
        this.cuerpo = cuerpo;
        this.linea = linea;
        this.columna = columna;
    }

    private boolean registradoEnTabla = false;
    private Tipo tipoFuncionCache;
    @Setter
    private boolean esMetodo = false;
    @Setter
    private String claseContenedora;
    @Setter
    private Visibilidad visibilidad = Visibilidad.PUBLIC;
    @Setter
    private boolean tieneOverride = false;

    public NodoFuncion(String nombre, Visibilidad visibilidad, boolean tieneOverride,
                       List<Parametro> parametros, Tipo tipoRetorno, List<NodoAST> cuerpo,
                       int linea, int columna) {
        this(nombre, parametros, tipoRetorno, cuerpo, linea, columna);
        this.visibilidad = visibilidad != null ? visibilidad : Visibilidad.PUBLIC;
        this.tieneOverride = tieneOverride;
    }

    public Tipo registrarEnTablaSimbolos(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        if (registradoEnTabla && tipoFuncionCache != null && tablaSimbolos.buscarFuncionGlobal(nombre) != null) {
            return tipoFuncionCache;
        }
        List<Tipo> tiposParams = new ArrayList<>();
        if (parametros != null) {
            for (Parametro p : parametros) {
                tiposParams.add(p.getTipo());
            }
        }
        tipoFuncionCache = new Tipo(nombre, tiposParams, tipoRetorno != null ? tipoRetorno : Tipo.VOID);

        if (tablaSimbolos.getAmbitoActual() != null && "Global".equals(tablaSimbolos.getAmbitoActual().getNombreAmbito())) {
            Simbolo f = tablaSimbolos.registrarFuncionGlobal(nombre, tipoFuncionCache, linea, columna);
            if (f == null) {
                errores.add(new ErrorSemantico(
                    "Ya existe una función o símbolo con el identificador '" + nombre + "' en el ámbito global.",
                    "Global", linea, columna
                ));
            }
        }
        registradoEnTabla = true;
        return tipoFuncionCache;
    }

    public void comprobarCuerpo(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Tipo retResuelto = (tipoRetorno != null) ? tablaSimbolos.resolverTipo(tipoRetorno) : Tipo.VOID;
        tablaSimbolos.abrirAmbitoFuncion("Funcion_" + nombre, retResuelto);
        try {
            if (parametros != null) {
                for (Parametro p : parametros) {
                    Tipo tipoParam = tablaSimbolos.resolverTipo(p.getTipo());
                    Simbolo sp = tablaSimbolos.registrarSimbolo(p.getNombre(), tipoParam, "parametro", p.getLinea(), p.getColumna());
                    if (sp == null) {
                        errores.add(new ErrorSemantico(
                            "Parámetro duplicado '" + p.getNombre() + "' en la función '" + nombre + "'.",
                            "Funcion_" + nombre, p.getLinea(), p.getColumna()
                        ));
                    }
                }
            }

            if (cuerpo != null) {
                for (NodoAST inst : cuerpo) {
                    if (inst != null) {
                        inst.comprobar(tablaSimbolos, errores);
                    }
                }
            }
        } finally {
            tablaSimbolos.cerrarAmbito();
        }
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Tipo tf = registrarEnTablaSimbolos(tablaSimbolos, errores);
        comprobarCuerpo(tablaSimbolos, errores);
        return tf;
    }
}
