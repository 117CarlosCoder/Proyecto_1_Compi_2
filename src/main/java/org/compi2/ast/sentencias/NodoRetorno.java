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
public class NodoRetorno implements NodoAST {
    private final NodoAST expresion; // null si es 'return;' vacío
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos ts, List<ErrorSemantico> errores) {
        if (expresion != null) {
            return expresion.comprobar(ts, errores);
        }
        return Tipo.VOID;
    }
}