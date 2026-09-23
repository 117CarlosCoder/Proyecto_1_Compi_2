package org.compi2.ast.sentencias;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;

import java.util.List;

@Getter
@AllArgsConstructor
public class NodoBreak implements NodoAST {
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";

        if (!tablaSimbolos.estaEnBucle() && !tablaSimbolos.estaEnSwitch()) {
            errores.add(new ErrorSemantico(
                    "La sentencia de escape ('break', 'romper', 'interrumpe') debe encontrarse dentro de un ciclo o switch.",
                    ambito, linea, columna));
        }

        return Tipo.VOID;
    }
}
