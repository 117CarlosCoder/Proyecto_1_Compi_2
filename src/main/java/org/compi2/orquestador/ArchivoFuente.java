package org.compi2.orquestador;

import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.compi2.ast.NodoAST;

import java.nio.file.Path;

@Getter
public class ArchivoFuente {
    private final String identificador;
    private final Path rutaAbsoluta;
    private final String nombreArchivo;
    private final String nombreBase;
    private final TipoLenguaje lenguaje;
    private final String contenido;

    @Setter
    private ParseTree parseTree;

    @Setter
    private NodoAST ast;

    public ArchivoFuente(String identificador, Path rutaAbsoluta, String nombreArchivo, TipoLenguaje lenguaje, String contenido) {
        this.identificador = normalizarSeparadores(identificador);
        this.rutaAbsoluta = rutaAbsoluta;
        this.nombreArchivo = nombreArchivo;
        this.lenguaje = lenguaje;
        this.contenido = contenido;

        if (nombreArchivo != null && nombreArchivo.contains(".")) {
            this.nombreBase = nombreArchivo.substring(0, nombreArchivo.lastIndexOf('.'));
        } else {
            this.nombreBase = nombreArchivo != null ? nombreArchivo : "";
        }
    }

    public boolean esZetariano() {
        return lenguaje == TipoLenguaje.ZETARIANO;
    }

    public boolean esY() {
        return lenguaje == TipoLenguaje.Y;
    }

    public boolean esPigLatin() {
        return lenguaje == TipoLenguaje.PIG_LATIN;
    }

    public static String normalizarSeparadores(String ruta) {
        if (ruta == null) return "";
        return ruta.replace('\\', '/').trim();
    }
}
