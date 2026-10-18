package org.compi2.errores;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorCompilacion {
    protected final TipoError tipo;
    protected final String mensaje;
    protected final String origen;
    protected final int linea;
    protected final int columna;

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
