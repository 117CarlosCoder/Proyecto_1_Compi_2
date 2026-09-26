package org.compi2.analisis_semantico;

import lombok.Getter;
import lombok.Setter;
import org.compi2.errores.ErrorCompilacion;
import org.compi2.errores.TipoError;

@Getter
public class ErrorSemantico extends ErrorCompilacion {
    @Setter
    private String archivo;

    public ErrorSemantico(String mensaje, String ambito, int linea, int columna) {
        super(TipoError.SEMANTICO, mensaje, ambito, linea, columna);
        this.archivo = "";
    }

    public ErrorSemantico(String mensaje, String ambito, String archivo, int linea, int columna) {
        super(TipoError.SEMANTICO, mensaje, ambito, linea, columna);
        this.archivo = archivo != null ? archivo : "";
    }

    public String getAmbito() {
        return origen;
    }

    @Override
    public String toString() {
        if (archivo != null && !archivo.isEmpty()) {
            return String.format("[%s][%d:%d] Error semántico en '%s': %s", archivo, linea, columna, origen, mensaje);
        }
        return String.format("[%d:%d] Error semántico en '%s': %s", linea, columna, origen, mensaje);
    }
}