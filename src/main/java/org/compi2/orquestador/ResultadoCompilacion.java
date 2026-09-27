package org.compi2.orquestador;

import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.tree.ParseTree;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.sentencias.NodoPrograma;
import org.compi2.c3d.ContenedorC3D;
import org.compi2.errores.ErrorCompilacion;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
public class ResultadoCompilacion {
    private final boolean exitoso;
    private final NodoPrograma astPrincipal;
    private final Map<String, ArchivoFuente> archivosCompilados;
    private final TablaSimbolos tablaSimbolos;
    private final List<ErrorCompilacion> errores;

    @Setter
    private ParseTree parseTreePrincipal;

    @Setter
    private String codigoC3D;

    @Setter
    private ContenedorC3D contenedorC3D;

    public ResultadoCompilacion(boolean exitoso, NodoPrograma astPrincipal,
            Map<String, ArchivoFuente> archivosCompilados, TablaSimbolos tablaSimbolos,
            List<ErrorCompilacion> errores) {
        this(exitoso, astPrincipal, archivosCompilados, tablaSimbolos, errores, null);
    }

    public ResultadoCompilacion(boolean exitoso, NodoPrograma astPrincipal,
            Map<String, ArchivoFuente> archivosCompilados, TablaSimbolos tablaSimbolos, List<ErrorCompilacion> errores,
            ParseTree parseTreePrincipal) {
        this.exitoso = exitoso;
        this.astPrincipal = astPrincipal;
        this.archivosCompilados = archivosCompilados;
        this.tablaSimbolos = tablaSimbolos;
        this.errores = errores;
        this.parseTreePrincipal = parseTreePrincipal;
    }

    public boolean tieneErrores() {
        return errores != null && !errores.isEmpty();
    }

    public List<ErrorCompilacion> getErrores() {
        return errores != null ? Collections.unmodifiableList(errores) : Collections.emptyList();
    }

    public String generarReporteErrores() {
        if (!tieneErrores()) {
            return "Compilación exitosa sin errores.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== REPORTE DE ERRORES DE COMPILACIÓN (%d errores) ===\n", errores.size()));
        for (int i = 0; i < errores.size(); i++) {
            sb.append(String.format("[%d] %s\n", i + 1, errores.get(i).toString()));
        }
        return sb.toString();
    }
}
