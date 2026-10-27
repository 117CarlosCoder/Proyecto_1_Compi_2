package org.compi2.ast.sentencias;

import lombok.Getter;
import org.compi2.analisis_semantico.ComprobadorTipos;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.ast.expresiones.NodoExpresionLectura;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.List;

@Getter
public class NodoRetorno implements NodoAST {
    private final NodoAST expresion;
    private final int linea;
    private final int columna;

    public NodoRetorno(NodoAST expresion, int linea, int columna) {
        this.expresion = expresion;
        this.linea = linea;
        this.columna = columna;
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";
        Tipo esperado = tablaSimbolos.getTipoRetornoEsperado();
        if (expresion instanceof NodoExpresionLectura lectura) {
            lectura.aplicarTipoEsperado(esperado);
        }
        Tipo obtenido = (expresion != null) ? expresion.comprobar(tablaSimbolos, errores) : Tipo.VOID;

        if (esperado != null && obtenido.getBase() != TipoBase.ERROR) {
            boolean compatible = (esperado.getBase() == TipoBase.VOID)
                    ? (expresion == null)
                    : (expresion != null && (ComprobadorTipos.esCompatibleAsignacion(esperado, obtenido)
                            || (obtenido.getBase() == TipoBase.NULO && esperado.getBase().esNumerico())));
            if (!compatible) {
                errores.add(new ErrorSemantico(
                        String.format("Tipo de retorno incompatible: se esperaba '%s' pero se retornó '%s'.", esperado,
                                obtenido),
                        ambito, linea, columna));
            }
        }
        return obtenido;
    }
}