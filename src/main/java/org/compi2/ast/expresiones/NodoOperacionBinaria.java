package org.compi2.ast.expresiones;

import lombok.Getter;
import org.compi2.analisis_semantico.ComprobadorTipos;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.List;

@Getter
public class NodoOperacionBinaria implements NodoAST {
    private final NodoAST izq;
    private final String operador;
    private final NodoAST der;
    private final int linea;
    private final int columna;

    public NodoOperacionBinaria(NodoAST izq, String operador, NodoAST der, int linea, int columna) {
        this.izq = izq;
        this.operador = operador;
        this.der = der;
        this.linea = linea;
        this.columna = columna;
    }

    private Tipo tipoIzq;
    private Tipo tipoDer;
    private Tipo tipoResultado;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        this.tipoIzq = izq.comprobar(tablaSimbolos, errores);
        this.tipoDer = der.comprobar(tablaSimbolos, errores);

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
        this.tipoResultado = res;
        return res;
    }
}