package org.compi2.tipos;

public enum Visibilidad {
    PUBLIC,
    PRIVATE,
    PROTECTED,
    DEFAULT;

    public static Visibilidad desdeTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return DEFAULT;
        }
        return switch (texto.trim()) {
            case "public" -> PUBLIC;
            case "private" -> PRIVATE;
            case "protected" -> PROTECTED;
            default -> DEFAULT;
        };
    }

    public int nivelAcceso() {
        return switch (this) {
            case PRIVATE -> 1;
            case DEFAULT -> 2;
            case PROTECTED -> 3;
            case PUBLIC -> 4;
        };
    }

    public boolean reduceVisibilidadRespectoA(Visibilidad padre) {
        if (padre == null) {
            return false;
        }
        return this.nivelAcceso() < padre.nivelAcceso();
    }
}
