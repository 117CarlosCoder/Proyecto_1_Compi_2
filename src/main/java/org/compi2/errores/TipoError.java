package org.compi2.errores;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public enum TipoError {
    LEXICO("Léxico"),
    SINTACTICO("Sintáctico"),
    SEMANTICO("Semántico");

    private final String etiqueta;

    TipoError(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
