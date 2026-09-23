package org.compi2.ast.sentencias;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;

import java.util.List;

@Getter
@AllArgsConstructor
public class NodoImprimir implements NodoAST {
    private final List<NodoAST> expresiones;
    private final boolean conSaltoLinea;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        if (expresiones != null) {
            for (NodoAST expr : expresiones) {
                if (expr != null) {
                    expr.comprobar(tablaSimbolos, errores);
                }
            }
        }
        return Tipo.VOID;
    }
}
