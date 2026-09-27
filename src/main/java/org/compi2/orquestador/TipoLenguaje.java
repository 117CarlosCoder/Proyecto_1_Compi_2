package org.compi2.orquestador;

import lombok.Getter;

@Getter
public enum TipoLenguaje {
    PIG_LATIN("pig"),
    Y("y"),
    ZETARIANO("z");

    private final String extension;

    TipoLenguaje(String extension) {
        this.extension = extension;
    }

    public static TipoLenguaje desdeExtension(String ext) {
        if (ext == null) return null;
        String e = ext.toLowerCase().trim();
        if (e.startsWith(".")) {
            e = e.substring(1);
        }
        for (TipoLenguaje tl : values()) {
            if (tl.extension.equalsIgnoreCase(e)) {
                return tl;
            }
        }
        return null;
    }
}
