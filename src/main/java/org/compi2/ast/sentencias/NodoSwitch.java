package org.compi2.ast.sentencias;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.compi2.analisis_semantico.ComprobadorTipos;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.List;

@Getter
@AllArgsConstructor
public class NodoSwitch implements NodoAST {
    private final NodoAST expresionControl;
    private final List<CasoSwitch> casos;
    private final List<NodoAST> casoDefecto;
    private final int linea;
    private final int columna;

    @Getter
    @AllArgsConstructor
    public static class CasoSwitch {
        private final NodoAST valorCaso;
        private final List<NodoAST> instrucciones;
        private final int linea;
        private final int columna;
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";

        Tipo tipoControl = expresionControl.comprobar(tablaSimbolos, errores);

        tablaSimbolos.abrirAmbitoSwitch("Bloque_Switch");

        if (casos != null) {
            for (CasoSwitch caso : casos) {
                if (caso.getValorCaso() != null) {
                    Tipo tCaso = caso.getValorCaso().comprobar(tablaSimbolos, errores);
                    if (tipoControl.getBase() != TipoBase.ERROR && tCaso.getBase() != TipoBase.ERROR) {
                        if (!ComprobadorTipos.esCompatibleAsignacion(tipoControl, tCaso)
                                && !tipoControl.esMismoTipo(tCaso)) {
                            errores.add(new ErrorSemantico(
                                    String.format(
                                            "El tipo del caso (%s) no es compatible con el tipo evaluado en el switch (%s).",
                                            tCaso, tipoControl),
                                    ambito, caso.getLinea(), caso.getColumna()));
                        }
                    }
                }

                if (caso.getInstrucciones() != null) {
                    for (NodoAST inst : caso.getInstrucciones()) {
                        if (inst != null) {
                            inst.comprobar(tablaSimbolos, errores);
                        }
                    }
                }
            }
        }

        if (casoDefecto != null) {
            for (NodoAST inst : casoDefecto) {
                if (inst != null) {
                    inst.comprobar(tablaSimbolos, errores);
                }
            }
        }

        tablaSimbolos.cerrarAmbito();
        return Tipo.VOID;
    }
}
