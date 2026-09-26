package org.compi2.ast.sentencias;

import lombok.Getter;
import org.compi2.analisis_semantico.ComprobadorTipos;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.Simbolo;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import org.compi2.ast.expresiones.NodoListaValores;
import java.util.Collections;
import java.util.List;

@Getter
public class NodoDeclaracion implements NodoAST {
    private final String nombre;
    private final Tipo tipoDeclarado;
    private final NodoAST valorInicial;
    private final List<NodoAST> dimensionesDeclaradas;
    private final int linea;
    private final int columna;

    public NodoDeclaracion(String nombre, Tipo tipoDeclarado, NodoAST valorInicial, int linea, int columna) {
        this(nombre, tipoDeclarado, valorInicial, Collections.emptyList(), linea, columna);
    }

    public NodoDeclaracion(String nombre, Tipo tipoDeclarado, NodoAST valorInicial, List<NodoAST> dimensionesDeclaradas, int linea, int columna) {
        this.nombre = nombre;
        this.tipoDeclarado = tipoDeclarado;
        this.valorInicial = valorInicial;
        this.dimensionesDeclaradas = dimensionesDeclaradas != null ? dimensionesDeclaradas : Collections.emptyList();
        this.linea = linea;
        this.columna = columna;
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";

        Tipo tipoFinal = tipoDeclarado;
        if (tipoDeclarado.getBase() == TipoBase.OBJETO_CLASE || tipoDeclarado.getBase() == TipoBase.ESTRUCTURA) {
            Tipo definido = tablaSimbolos.buscarTipoCompuesto(tipoDeclarado.getNombreTipo());
            if (definido == null) {
                errores.add(new ErrorSemantico(
                        "El tipo '" + tipoDeclarado.getNombreTipo() + "' no está definido ni importado.",
                        ambito, linea, columna));
                return Tipo.ERROR;
            }
            tipoFinal = definido;
        }

        if (tipoFinal.getBase() == TipoBase.ARREGLO && dimensionesDeclaradas != null) {
            for (int i = 0; i < dimensionesDeclaradas.size(); i++) {
                NodoAST dim = dimensionesDeclaradas.get(i);
                Tipo tDim = dim.comprobar(tablaSimbolos, errores);
                if (tDim.getBase() != TipoBase.ENTERO && tDim.getBase() != TipoBase.ERROR) {
                    errores.add(new ErrorSemantico(
                            String.format("El tamaño de la dimensión %d del arreglo '%s' debe ser un entero. Obtenido: %s.",
                                    i + 1, nombre, tDim),
                            ambito, linea, columna));
                } else {
                    Integer valConst = NodoListaValores.evaluarConstanteEntera(dim);
                    if (valConst != null && valConst <= 0) {
                        errores.add(new ErrorSemantico(
                                String.format("El tamaño de la dimensión %d del arreglo '%s' debe ser mayor a cero. Obtenido: %d.",
                                        i + 1, nombre, valConst),
                                ambito, linea, columna));
                    }
                }
            }
        }

        if (valorInicial != null) {
            if (valorInicial instanceof NodoListaValores lista) {
                if (tipoFinal.getBase() == TipoBase.ESTRUCTURA || tipoFinal.getBase() == TipoBase.OBJETO_CLASE) {
                    lista.comprobarEstructura(tipoFinal, tablaSimbolos, errores);
                } else if (tipoFinal.getBase() == TipoBase.ARREGLO) {
                    lista.comprobarArreglo(tipoFinal, dimensionesDeclaradas, 0, tablaSimbolos, errores);
                } else {
                    Tipo tipoVal = valorInicial.comprobar(tablaSimbolos, errores);
                    if (tipoVal.getBase() != TipoBase.ERROR
                            && !ComprobadorTipos.esCompatibleAsignacion(tipoFinal, tipoVal)) {
                        errores.add(new ErrorSemantico(
                                String.format(
                                        "Tipos no compatibles en la inicialización de '%s': se esperaba %s y se obtuvo %s.",
                                        nombre, tipoFinal, tipoVal),
                                ambito, linea, columna));
                    }
                }
            } else {
                Tipo tipoVal = valorInicial.comprobar(tablaSimbolos, errores);
                if (tipoVal.getBase() != TipoBase.ERROR
                        && !ComprobadorTipos.esCompatibleAsignacion(tipoFinal, tipoVal)) {
                    errores.add(new ErrorSemantico(
                            String.format(
                                    "Tipos no compatibles en la inicialización de '%s': se esperaba %s y se obtuvo %s.",
                                    nombre, tipoFinal, tipoVal),
                            ambito, linea, columna));
                }
            }
        }

        Simbolo s = tablaSimbolos.registrarSimbolo(nombre, tipoFinal, "variable", linea, columna);
        if (s == null) {
            errores.add(new ErrorSemantico(
                    "La variable '" + nombre + "' ya ha sido declarada en el ámbito actual (" + ambito + ").",
                    ambito, linea, columna));
            return Tipo.ERROR;
        }

        return Tipo.VOID;
    }
}