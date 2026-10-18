package org.compi2.c3d;

public class GestorEtiquetas {
    private int contador = 0;

    public String nuevaEtiqueta() {
        String etiqueta = "L" + contador;
        contador++;
        return etiqueta;
    }

    public void reiniciar() {
        contador = 0;
    }
}
