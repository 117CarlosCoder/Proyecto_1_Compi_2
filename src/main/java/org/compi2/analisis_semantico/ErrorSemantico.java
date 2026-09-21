package org.compi2.analisis_semantico;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErrorSemantico {
    private final String mensaje;
    private final String ambito;
    private final int linea;
    private final int columna;

    @Override
    public String toString() {
        return String.format("[%d:%d] Error semántico en '%s': %s", linea, columna, ambito, mensaje);
    }
}