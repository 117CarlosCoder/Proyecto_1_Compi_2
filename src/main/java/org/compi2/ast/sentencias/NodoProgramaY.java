package org.compi2.ast.sentencias;

import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.ModuloSemantico;
import org.compi2.tipos.Tipo;

import java.util.ArrayList;
import java.util.List;

@Getter
public class NodoProgramaY implements ModuloSemantico {
    private final List<NodoEstructura> estructuras = new ArrayList<>();
    private final List<NodoFuncion> funciones = new ArrayList<>();

    public void agregarEstructura(NodoEstructura est) {
        estructuras.add(est);
    }

    public void agregarFuncion(NodoFuncion fn) {
        funciones.add(fn);
    }

    @Override
    public void registrarEnTablaSimbolos(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        for (NodoEstructura est : estructuras) {
            if (est != null) {
                est.registrarEnTablaSimbolos(tablaSimbolos, errores);
            }
        }
        for (NodoFuncion fn : funciones) {
            if (fn != null) {
                fn.registrarEnTablaSimbolos(tablaSimbolos, errores);
            }
        }
    }

    @Override
    public void comprobarCuerpos(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        for (NodoFuncion fn : funciones) {
            if (fn != null) {
                fn.comprobarCuerpo(tablaSimbolos, errores);
            }
        }
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        registrarEnTablaSimbolos(tablaSimbolos, errores);
        comprobarCuerpos(tablaSimbolos, errores);
        return Tipo.VOID;
    }
}
