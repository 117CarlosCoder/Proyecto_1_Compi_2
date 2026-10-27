package org.compi2.ast.sentencias;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.compi2.analisis_semantico.ComprobadorTipos;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.List;

@Getter
public class NodoSwitch implements NodoAST {
    private final NodoAST expresionControl;
    private final List<CasoSwitch> casos;
    private final List<NodoAST> casoDefecto;
    private final int linea;
    private final int columna;

    public NodoSwitch(NodoAST expresionControl, List<CasoSwitch> casos, List<NodoAST> casoDefecto, int linea, int columna) {
        this.expresionControl = expresionControl;
        this.casos = casos;
        this.casoDefecto = casoDefecto;
        this.linea = linea;
        this.columna = columna;
    }

    private Tipo tipoControl;

    @Getter
    public static class CasoSwitch {
        private final NodoAST valorCaso;
        private final List<NodoAST> instrucciones;
        private final int linea;
        private final int columna;

        public CasoSwitch(NodoAST valorCaso, List<NodoAST> instrucciones, int linea, int columna) {
            this.valorCaso = valorCaso;
            this.instrucciones = instrucciones;
            this.linea = linea;
            this.columna = columna;
        }
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";

        this.tipoControl = expresionControl.comprobar(tablaSimbolos, errores);

        tablaSimbolos.abrirAmbitoSwitch("Bloque_Switch");
        try {
            if (casos != null) {
                for (CasoSwitch caso : casos) {
                    if (caso.getValorCaso() != null) {
                        Tipo tCaso = caso.getValorCaso().comprobar(tablaSimbolos, errores);
                        if (tipoControl.getBase() != TipoBase.ERROR && tCaso.getBase() != TipoBase.ERROR) {
                            if (!ComprobadorTipos.esCompatibleAsignacion(tipoControl, tCaso)
                                    && !tipoControl.esMismoTipo(tCaso)) {
                                errores.add(new ErrorSemantico(
                                        String.format(
                                                "El tipo (%s) no es compatible con el tipo evaluado en el switch (%s).",
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
        } finally {
            tablaSimbolos.cerrarAmbito();
        }
        return Tipo.VOID;
    }
}
