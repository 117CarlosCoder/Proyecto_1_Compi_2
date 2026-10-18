package org.compi2.c3d;

public class GestorTemporales {
    private int contador = 0;

    public String nuevoTemporal() {
        String temporal = "t" + contador;
        contador++;
        return temporal;
    }

    public String generarDeclaracionC() {
        if (contador == 0) {
            return "";
        }
        StringBuilder declaracion = new StringBuilder("double ");
        for (int i = 0; i < contador; i++) {
            if (i > 0) {
                declaracion.append(", ");
            }
            declaracion.append("t").append(i);
        }
        return declaracion.append(";").toString();
    }

    public void reiniciar() {
        contador = 0;
    }
}
