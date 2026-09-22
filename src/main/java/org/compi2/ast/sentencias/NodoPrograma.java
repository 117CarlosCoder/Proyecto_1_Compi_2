package org.compi2.ast.sentencias;

import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;

import java.util.ArrayList;
import java.util.List;

@Getter
public class NodoPrograma implements NodoAST {
    private final List<String> importaciones = new ArrayList<>();
    private final List<NodoAST> declaracionesGlobales = new ArrayList<>();
    private final List<NodoAST> instruccionesMaior = new ArrayList<>();

    public void agregarImport(String ruta) {
        importaciones.add(ruta);
    }

    public void agregarDeclaracionGlobal(NodoAST decl) {
        declaracionesGlobales.add(decl);
    }

    public void agregarInstruccionMaior(NodoAST inst) {
        instruccionesMaior.add(inst);
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        for (NodoAST decl : declaracionesGlobales) {
            if (decl != null) {
                decl.comprobar(tablaSimbolos, errores);
            }
        }

        tablaSimbolos.abrirAmbito("MAIOR", true);
        for (NodoAST inst : instruccionesMaior) {
            if (inst != null) {
                inst.comprobar(tablaSimbolos, errores);
            }
        }
        tablaSimbolos.cerrarAmbito();

        return Tipo.VOID;
    }
}