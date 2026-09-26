package org.compi2.ast.expresiones;

import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.List;

@Getter
public class NodoOperacionUnaria implements NodoAST {
    private final String operador;
    private final NodoAST expresion;
    private final int linea;
    private final int columna;
    private final boolean esPrefijo;

    public NodoOperacionUnaria(String operador, NodoAST expresion, int linea, int columna, boolean esPrefijo) {
        this.operador = operador;
        this.expresion = expresion;
        this.linea = linea;
        this.columna = columna;
        this.esPrefijo = esPrefijo;
    }

    public NodoOperacionUnaria(String operador, NodoAST expresion, int linea, int columna) {
        this(operador, expresion, linea, columna, true);
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Tipo tipoExp = expresion.comprobar(tablaSimbolos, errores);
        if (tipoExp.getBase() == TipoBase.ERROR)
            return Tipo.ERROR;

        String ambito = (tablaSimbolos.getAmbitoActual() != null)
                ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";

        return switch (operador) {
            case "!" -> {
                if (tipoExp.getBase() != TipoBase.BOOLEANO) {
                    errores.add(new ErrorSemantico(
                            "El operador '!' requiere un operando booleano. Obtenido: " + tipoExp,
                            ambito, linea, columna));
                    yield Tipo.ERROR;
                }
                yield Tipo.BOOLEANO;
            }
            case "-", "+" -> {
                if (!tipoExp.getBase().esNumerico()) {
                    errores.add(new ErrorSemantico(
                            String.format("El operador unario '%s' requiere un operando numérico. Obtenido: %s.",
                                    operador, tipoExp),
                            ambito, linea, columna));
                    yield Tipo.ERROR;
                }
                yield tipoExp;
            }
            case "++", "--" -> {
                if (!tipoExp.getBase().esNumerico()) {
                    errores.add(new ErrorSemantico(
                            String.format("El operador '%s' requiere un operando numérico. Obtenido: %s.", operador,
                                    tipoExp),
                            ambito, linea, columna));
                    yield Tipo.ERROR;
                }
                yield tipoExp;
            }
            default -> {
                errores.add(new ErrorSemantico(
                        "Operador unario no reconocido: '" + operador + "'.",
                        ambito, linea, columna));
                yield Tipo.ERROR;
            }
        };
    }
}