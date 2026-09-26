package org.compi2.ast.sentencias;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.Simbolo;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;

import java.util.List;

@Getter
@AllArgsConstructor
public class NodoConstructor implements NodoAST {
    private final String nombreClase;
    private final List<Parametro> parametros;
    private final List<NodoAST> cuerpo;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        tablaSimbolos.abrirAmbitoFuncion("Constructor_" + nombreClase, Tipo.VOID);
        try {
            if (parametros != null) {
                for (Parametro p : parametros) {
                    Simbolo sp = tablaSimbolos.registrarSimbolo(p.getNombre(), p.getTipo(), "parametro", p.getLinea(), p.getColumna());
                    if (sp == null) {
                        errores.add(new ErrorSemantico(
                            "Parámetro duplicado '" + p.getNombre() + "' en constructor de '" + nombreClase + "'.",
                            "Constructor_" + nombreClase, p.getLinea(), p.getColumna()
                        ));
                    }
                }
            }

            if (cuerpo != null) {
                for (NodoAST inst : cuerpo) {
                    if (inst != null) {
                        inst.comprobar(tablaSimbolos, errores);
                    }
                }
            }
        } finally {
            tablaSimbolos.cerrarAmbito();
        }
        return Tipo.VOID;
    }
}
