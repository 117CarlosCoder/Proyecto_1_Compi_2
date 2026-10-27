package org.compi2.ast.sentencias;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.ModuloSemantico;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;

import java.util.ArrayList;
import java.util.List;

@Getter
public class NodoPrograma implements ModuloSemantico {

    @Getter
    public static class InfoImport {
        private final String ruta;
        private final int linea;
        private final int columna;

        public InfoImport(String ruta, int linea, int columna) {
            this.ruta = ruta;
            this.linea = linea;
            this.columna = columna;
        }
    }

    private final List<String> importaciones = new ArrayList<>();
    private final List<InfoImport> importacionesDetalladas = new ArrayList<>();
    private final List<NodoAST> declaracionesGlobales = new ArrayList<>();
    private final List<NodoAST> instruccionesMaior = new ArrayList<>();

    public void agregarImport(String ruta, int linea, int columna) {
        importaciones.add(ruta);
        importacionesDetalladas.add(new InfoImport(ruta, linea, columna));
    }

    public void agregarDeclaracionGlobal(NodoAST decl) {
        declaracionesGlobales.add(decl);
    }

    public void agregarInstruccionMaior(NodoAST inst) {
        instruccionesMaior.add(inst);
    }

    @Override
    public void registrarEnTablaSimbolos(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        for (NodoAST decl : declaracionesGlobales) {
            if (decl != null) {
                decl.comprobar(tablaSimbolos, errores);
            }
        }
    }

    @Override
    public void comprobarCuerpos(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        tablaSimbolos.abrirAmbito("MAIOR", true);
        try {
            for (NodoAST inst : instruccionesMaior) {
                if (inst != null) {
                    inst.comprobar(tablaSimbolos, errores);
                }
            }
        } finally {
            tablaSimbolos.cerrarAmbito();
        }
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        registrarEnTablaSimbolos(tablaSimbolos, errores);
        comprobarCuerpos(tablaSimbolos, errores);
        return Tipo.VOID;
    }
}