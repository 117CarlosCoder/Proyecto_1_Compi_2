package org.compi2.ast.sentencias;

import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.ModuloSemantico;
import org.compi2.tipos.Tipo;

import java.util.ArrayList;
import java.util.List;

@Getter
public class NodoUnidadCompilacionZ implements ModuloSemantico {
    private final List<NodoClase> clases = new ArrayList<>();

    public void agregarClase(NodoClase clase) {
        clases.add(clase);
    }

    public List<NodoFuncion> aFunciones() {
        List<NodoFuncion> funciones = new ArrayList<>();
        for (NodoClase clase : clases) {
            funciones.addAll(clase.aFunciones());
        }
        return funciones;
    }

    @Override
    public void registrarEnTablaSimbolos(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        for (NodoClase c : clases) {
            if (c != null) {
                c.registrarEnTablaSimbolos(tablaSimbolos, errores);
            }
        }
    }

    public void enlazarHerencia(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        enlazarHerencia(tablaSimbolos, errores, null);
    }

    public void enlazarHerencia(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores, java.util.Map<String, NodoClase> mapaClases) {
        for (NodoClase c : clases) {
            if (c != null) {
                c.enlazarHerencia(tablaSimbolos, errores, mapaClases);
            }
        }
    }

    @Override
    public void comprobarCuerpos(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        for (NodoClase c : clases) {
            if (c != null) {
                c.comprobarCuerpos(tablaSimbolos, errores);
            }
        }
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        registrarEnTablaSimbolos(tablaSimbolos, errores);
        enlazarHerencia(tablaSimbolos, errores);
        comprobarCuerpos(tablaSimbolos, errores);
        return Tipo.VOID;
    }
}
