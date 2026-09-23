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
public class NodoAsignacion implements NodoAST {
    private final NodoAST destino;
    private final NodoAST valor;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Tipo tipoDestino = destino.comprobar(tablaSimbolos, errores);
        Tipo tipoValor = valor.comprobar(tablaSimbolos, errores);

        if (tipoDestino.getBase() == TipoBase.ERROR || tipoValor.getBase() == TipoBase.ERROR) {
            return Tipo.VOID;
        }

        if (!ComprobadorTipos.esCompatibleAsignacion(tipoDestino, tipoValor)) {
            String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito() : "Global";
            errores.add(new ErrorSemantico(
                String.format("No se puede asignar un valor de tipo %s a un destino de tipo %s.",
                    tipoValor, tipoDestino),
                ambito, linea, columna));
        }

        return Tipo.VOID;
    }
}