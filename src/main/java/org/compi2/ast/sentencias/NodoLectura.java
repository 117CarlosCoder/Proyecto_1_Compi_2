package org.compi2.ast.sentencias;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;

import java.util.List;

@Getter
public class NodoLectura implements NodoAST {
    private final NodoAST destino;
    private final int linea;
    private final int columna;

    public NodoLectura(NodoAST destino, int linea, int columna) {
        this.destino = destino;
        this.linea = linea;
        this.columna = columna;
    }

    private Tipo tipoDestino;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        this.tipoDestino = (destino != null) ? destino.comprobar(tablaSimbolos, errores) : null;
        return Tipo.VOID;
    }
}
