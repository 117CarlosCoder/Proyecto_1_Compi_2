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
public class NodoWhile implements NodoAST {
    private final NodoAST condicion;
    private final List<NodoAST> instrucciones;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";

        Tipo tipoCond = condicion.comprobar(tablaSimbolos, errores);
        if (tipoCond.getBase() != TipoBase.BOOLEANO && tipoCond.getBase() != TipoBase.ERROR) {
            errores.add(new ErrorSemantico(
                    "La condición del bucle debe ser de tipo booleano. Obtenido: " + tipoCond,
                    ambito, linea, columna));
        }

        tablaSimbolos.abrirAmbitoBucle("Bloque_While");
        try {
            if (instrucciones != null) {
                for (NodoAST inst : instrucciones) {
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