package org.compi2.errores;

public enum TipoError {
    LEXICO("Léxico"),
    SINTACTICO("Sintáctico"),
    SEMANTICO("Semántico");

    private final String etiqueta;

    TipoError(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
