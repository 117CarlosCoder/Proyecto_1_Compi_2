package org.compi2.errores;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoError {
    LEXICO("Léxico"),
    SINTACTICO("Sintáctico"),
    SEMANTICO("Semántico");

    private final String etiqueta;

    @Override
    public String toString() {
        return etiqueta;
    }
}
