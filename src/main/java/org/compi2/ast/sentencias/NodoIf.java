package org.compi2.ast.sentencias;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.List;

@Getter
@AllArgsConstructor
public class NodoIf implements NodoAST {
    private final NodoAST condicion;
    private final List<NodoAST> instruccionesSi;
    private final List<NodoAST> instruccionesAliter;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        String ambitoActual = (tablaSimbolos.getAmbitoActual() != null)
            ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
            : "Global";

        Tipo tipoCond = condicion.comprobar(tablaSimbolos, errores);
        if (tipoCond.getBase() != TipoBase.BOOLEANO && tipoCond.getBase() != TipoBase.ERROR) {
            errores.add(new ErrorSemantico(
                "La condición del condicional debe ser booleana. Se obtuvo: " + tipoCond,
                ambitoActual,
                linea,
                columna
            ));
        }

        tablaSimbolos.abrirAmbito("Bloque_Si");
        if (instruccionesSi != null) {
            for (NodoAST inst : instruccionesSi) {
                if (inst != null) {
                    inst.comprobar(tablaSimbolos, errores);
                }
            }
        }
        tablaSimbolos.cerrarAmbito();

        if (instruccionesAliter != null && !instruccionesAliter.isEmpty()) {
            tablaSimbolos.abrirAmbito("Bloque_Aliter");
            for (NodoAST inst : instruccionesAliter) {
                if (inst != null) {
                    inst.comprobar(tablaSimbolos, errores);
                }
            }
            tablaSimbolos.cerrarAmbito();
        }

        return Tipo.VOID;
    }
}