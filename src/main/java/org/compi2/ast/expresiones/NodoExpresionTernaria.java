package org.compi2.ast.expresiones;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.compi2.analisis_semantico.ComprobadorTipos;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.List;

@Getter
@AllArgsConstructor
public class NodoExpresionTernaria implements NodoAST {
    private final NodoAST condicion;
    private final NodoAST expresionTrue;
    private final NodoAST expresionFalse;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";

        Tipo tipoCond = condicion.comprobar(tablaSimbolos, errores);
        if (tipoCond.getBase() != TipoBase.BOOLEANO && tipoCond.getBase() != TipoBase.ERROR) {
            errores.add(new ErrorSemantico(
                    "La condición del operador ternario ('?') debe ser de tipo booleano. Obtenido: " + tipoCond,
                    ambito, linea, columna));
        }

        Tipo tipoTrue = expresionTrue.comprobar(tablaSimbolos, errores);
        Tipo tipoFalse = expresionFalse.comprobar(tablaSimbolos, errores);

        if (tipoTrue.getBase() == TipoBase.ERROR || tipoFalse.getBase() == TipoBase.ERROR) {
            return Tipo.ERROR;
        }

        Tipo resultado = ComprobadorTipos.resolverTernario(tipoTrue, tipoFalse);
        if (resultado.getBase() == TipoBase.ERROR) {
            errores.add(new ErrorSemantico(
                    String.format("Incompatibilidad de tipos en operador ternario: no se puede unificar '%s' con '%s'.",
                            tipoTrue, tipoFalse),
                    ambito, linea, columna));
        }

        return resultado;
    }
}
