package org.compi2.analisis_semantico;

import lombok.Getter;
import org.compi2.errores.ErrorCompilacion;
import org.compi2.errores.TipoError;

@Getter
public class ErrorSemantico extends ErrorCompilacion {

    public ErrorSemantico(String mensaje, String ambito, int linea, int columna) {
        super(TipoError.SEMANTICO, mensaje, ambito, linea, columna);
    }

    public String getAmbito() {
        return origen;
    }

    @Override
    public String toString() {
        return String.format("[%d:%d] Error semántico en '%s': %s", linea, columna, origen, mensaje);
    }
}