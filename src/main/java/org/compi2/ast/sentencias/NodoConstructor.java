package org.compi2.ast.sentencias;

import lombok.Getter;
import lombok.Setter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.Simbolo;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.Visibilidad;

import java.util.List;

@Getter
public class NodoConstructor implements NodoAST {
    private final String nombreClase;
    @Setter
    private Visibilidad visibilidad;
    private final List<Parametro> parametros;
    private final List<NodoAST> cuerpo;
    private final int linea;
    private final int columna;

    public NodoConstructor(String nombreClase, Visibilidad visibilidad, List<Parametro> parametros, List<NodoAST> cuerpo, int linea, int columna) {
        this.nombreClase = nombreClase;
        this.visibilidad = visibilidad;
        this.parametros = parametros;
        this.cuerpo = cuerpo;
        this.linea = linea;
        this.columna = columna;
    }

    public NodoConstructor(String nombreClase, List<Parametro> parametros, List<NodoAST> cuerpo, int linea, int columna) {
        this(nombreClase, Visibilidad.PUBLIC, parametros, cuerpo, linea, columna);
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        tablaSimbolos.abrirAmbitoFuncion("Constructor_" + nombreClase, Tipo.VOID);
        try {
            if (parametros != null) {
                for (Parametro p : parametros) {
                    Tipo tipoParam = tablaSimbolos.resolverTipo(p.getTipo());
                    Simbolo sp = tablaSimbolos.registrarSimbolo(p.getNombre(), tipoParam, "parametro", p.getLinea(), p.getColumna());
                    if (sp == null) {
                        errores.add(new ErrorSemantico(
                            "Parámetro duplicado '" + p.getNombre() + "' en constructor de '" + nombreClase + "'.",
                            "Constructor_" + nombreClase, p.getLinea(), p.getColumna()
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
        return Tipo.VOID;
    }
}
