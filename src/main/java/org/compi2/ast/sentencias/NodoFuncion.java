package org.compi2.ast.sentencias;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.Simbolo;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NodoFuncion implements NodoAST {
    private final String nombre;
    private final List<Parametro> parametros;
    private final Tipo tipoRetorno;
    private final List<NodoAST> cuerpo;
    private final int linea;
    private final int columna;

    private boolean firmaRegistrada = false;
    private Tipo tipoFuncionCache;
    @Setter
    private boolean esMetodo = false;
    @Setter
    private String claseContenedora;

    public Tipo registrarFirma(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        if (firmaRegistrada && tipoFuncionCache != null && tablaSimbolos.buscarFuncionGlobal(nombre) != null) {
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
        firmaRegistrada = true;
        return tipoFuncionCache;
    }

    public void comprobarCuerpo(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        tablaSimbolos.abrirAmbitoFuncion("Funcion_" + nombre, tipoRetorno != null ? tipoRetorno : Tipo.VOID);
        try {
            if (parametros != null) {
                for (Parametro p : parametros) {
                    Tipo tipoParam = p.getTipo();
                    if (tipoParam != null && (tipoParam.getBase() == org.compi2.tipos.TipoBase.ESTRUCTURA || tipoParam.getBase() == org.compi2.tipos.TipoBase.OBJETO_CLASE)) {
                        Tipo compuesto = tablaSimbolos.buscarTipoCompuesto(tipoParam.getNombreTipo());
                        if (compuesto != null) {
                            tipoParam = compuesto;
                        }
                    }
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
        Tipo tf = registrarFirma(tablaSimbolos, errores);
        comprobarCuerpo(tablaSimbolos, errores);
        return tf;
    }
}
