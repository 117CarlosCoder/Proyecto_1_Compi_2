package org.compi2.c3d;

import java.util.LinkedHashSet;
import java.util.Set;

public class GestorTemporales {
    private int contador = 0;
    private final Set<String> temporalesUsados = new LinkedHashSet<>();

    public GestorTemporales() {
        this.contador = 0;
    }

    public String nuevoTemporal() {
        String temporal = "t" + (contador++);
        temporalesUsados.add(temporal);
        return temporal;
    }

    public String generarDeclaracionC() {
        if (temporalesUsados.isEmpty()) {
            return "";
        }
        return "double " + String.join(", ", temporalesUsados) + ";";
    }

    public void reiniciar() {
        this.contador = 0;
        this.temporalesUsados.clear();
    }
}
