package org.compi2.ast;

import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;

import java.util.List;

public interface ModuloSemantico extends NodoAST {
    void registrarEnTablaSimbolos(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores);
    void comprobarCuerpos(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores);
}
