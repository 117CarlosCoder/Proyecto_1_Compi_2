package org.compi2.errores;

import lombok.Getter;

@Getter
public class ErrorCompilacion {
    protected final TipoError tipo;
    protected final String mensaje;
    protected final String origen;
    protected final int linea;
    protected final int columna;

    public ErrorCompilacion(TipoError tipo, String mensaje, String origen, int linea, int columna) {
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.origen = origen;
        this.linea = linea;
        this.columna = columna;
    }

    public String getArchivo() {
        return origen;
    }

    public String getUbicacionArchivo() {
        String arch = getArchivo();
        if (arch != null && !arch.isBlank()) {
            return arch;
        }
        if (origen != null && !origen.isBlank()) {
            return origen;
        }
        return "—";
    }

    @Override
    public String toString() {
        return String.format("[%s] en '%s' (L:%d, C:%d): %s",
            tipo.getEtiqueta(), origen, linea, columna, mensaje);
    }
}
