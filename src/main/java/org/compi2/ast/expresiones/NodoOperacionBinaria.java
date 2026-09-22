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
public class NodoOperacionBinaria implements NodoAST {
    private final NodoAST izq;
    private final String operador;
    private final NodoAST der;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Tipo tipoIzq = izq.comprobar(tablaSimbolos, errores);
        Tipo tipoDer = der.comprobar(tablaSimbolos, errores);

        Tipo res = switch (operador) {
            case "+" -> ComprobadorTipos.resolverSuma(tipoIzq, tipoDer);
            case "-", "*", "/", "%" -> ComprobadorTipos.resolverAritmetica(tipoIzq, tipoDer);
            case ">", "<", ">=", "<=" -> ComprobadorTipos.resolverRelacional(tipoIzq, tipoDer);
            case "==", "!=" -> ComprobadorTipos.resolverIgualdad(tipoIzq, tipoDer);
            case "&&", "||" -> ComprobadorTipos.resolverLogico(tipoIzq, tipoDer);
            default -> Tipo.ERROR;
        };

        if (res.getBase() == TipoBase.ERROR && tipoIzq.getBase() != TipoBase.ERROR && tipoDer.getBase() != TipoBase.ERROR) {
            String ambito = (tablaSimbolos.getAmbitoActual() != null)
                ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";

            errores.add(new ErrorSemantico(
                String.format("Operación '%s' inválida entre tipos %s y %s.", operador, tipoIzq, tipoDer),
                ambito,
                linea,
                columna
            ));
        }
        return res;
    }
}