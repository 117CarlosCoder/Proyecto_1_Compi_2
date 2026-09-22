package org.compi2.ast;

import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.tipos.Tipo;

import java.util.List;

public interface NodoAST {
    Tipo comprobar(TablaSimbolos ts, List<ErrorSemantico> errores);
}