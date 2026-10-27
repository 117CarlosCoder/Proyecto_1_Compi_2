package org.compi2.ast.sentencias;

import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;

import java.util.ArrayList;
import java.util.List;

@Getter
public class NodoImprimir implements NodoAST {
    private final List<NodoAST> expresiones;
    private final boolean conSaltoLinea;
    private final int linea;
    private final int columna;

    public NodoImprimir(List<NodoAST> expresiones, boolean conSaltoLinea, int linea, int columna) {
        this.expresiones = expresiones;
        this.conSaltoLinea = conSaltoLinea;
        this.linea = linea;
        this.columna = columna;
    }

    private final List<Tipo> tiposExpresiones = new ArrayList<>();

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        tiposExpresiones.clear();
        if (expresiones != null) {
            for (NodoAST expr : expresiones) {
                tiposExpresiones.add(expr != null ? expr.comprobar(tablaSimbolos, errores) : null);
            }
        }
        return Tipo.VOID;
    }
}
