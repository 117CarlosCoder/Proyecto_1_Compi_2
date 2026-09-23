package org.compi2.ast.sentencias;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.compi2.analisis_semantico.ComprobadorTipos;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.Simbolo;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.List;

@Getter
@AllArgsConstructor
public class NodoDeclaracion implements NodoAST {
    private final String nombre;
    private final Tipo tipoDeclarado;
    private final NodoAST valorInicial;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";

        if (tipoDeclarado.getBase() == TipoBase.OBJETO_CLASE || tipoDeclarado.getBase() == TipoBase.ESTRUCTURA) {
            Tipo definido = tablaSimbolos.buscarTipoCompuesto(tipoDeclarado.getNombreTipo());
            if (definido == null) {
                errores.add(new ErrorSemantico(
                        "El tipo '" + tipoDeclarado.getNombreTipo() + "' no está definido ni importado.",
                        ambito, linea, columna));
                return Tipo.ERROR;
            }
        }

        if (valorInicial != null) {
            Tipo tipoVal = valorInicial.comprobar(tablaSimbolos, errores);
            if (tipoVal.getBase() != TipoBase.ERROR
                    && !ComprobadorTipos.esCompatibleAsignacion(tipoDeclarado, tipoVal)) {
                errores.add(new ErrorSemantico(
                        String.format(
                                "Incompatibilidad de tipos en la inicialización de '%s': se esperaba %s y se obtuvo %s.",
                                nombre, tipoDeclarado, tipoVal),
                        ambito, linea, columna));
            }
        }

        Simbolo s = tablaSimbolos.registrarSimbolo(nombre, tipoDeclarado, "variable", linea, columna);
        if (s == null) {
            errores.add(new ErrorSemantico(
                    "La variable '" + nombre + "' ya ha sido declarada en el ámbito actual (" + ambito + ").",
                    ambito, linea, columna));
            return Tipo.ERROR;
        }

        return Tipo.VOID;
    }
}