package org.compi2.ast.sentencias;

import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;

import java.util.ArrayList;
import java.util.List;

@Getter
public class NodoUnidadCompilacionZ implements NodoAST {
    private final List<NodoClase> clases = new ArrayList<>();

    public void agregarClase(NodoClase clase) {
        clases.add(clase);
    }

    public void registrarFirmas(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        for (NodoClase c : clases) {
            if (c != null) {
                c.registrarFirmas(tablaSimbolos, errores);
            }
        }
    }

    public void comprobarCuerpos(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        for (NodoClase c : clases) {
            if (c != null) {
                c.comprobarCuerpos(tablaSimbolos, errores);
            }
        }
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        registrarFirmas(tablaSimbolos, errores);
        comprobarCuerpos(tablaSimbolos, errores);
        return Tipo.VOID;
    }
}
