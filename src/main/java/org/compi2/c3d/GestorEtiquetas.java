package org.compi2.c3d;

import java.util.LinkedHashSet;
import java.util.Set;

public class GestorEtiquetas {
    private int contador = 0;
    private final Set<String> etiquetasGeneradas = new LinkedHashSet<>();

    public GestorEtiquetas() {
        this.contador = 0;
    }

    public String nuevaEtiqueta() {
        String etiqueta = "L" + (contador++);
        etiquetasGeneradas.add(etiqueta);
        return etiqueta;
    }

    public void reiniciar() {
        this.contador = 0;
        this.etiquetasGeneradas.clear();
    }
}
