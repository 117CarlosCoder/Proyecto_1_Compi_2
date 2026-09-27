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
public class NodoListaValores implements NodoAST {
    private final List<NodoAST> elementos;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        if (elementos == null || elementos.isEmpty()) {
            return new Tipo(TipoBase.ARREGLO, "[]", Tipo.VOID, 1, null, null);
        }

        Tipo tipoPrimerElemento = null;
        for (NodoAST elem : elementos) {
            Tipo tipoElemento = elem.comprobar(tablaSimbolos, errores);
            if (tipoPrimerElemento == null && tipoElemento.getBase() != TipoBase.ERROR) {
                tipoPrimerElemento = tipoElemento;
            }
        }

        if (tipoPrimerElemento == null) {
            return Tipo.ERROR;
        }

        return new Tipo(tipoPrimerElemento, 1);
    }

    public static Integer evaluarConstanteEntera(NodoAST nodo) {
        if (nodo == null) return null;
        if (nodo instanceof NodoLiteral lit) {
            if (lit.getValor() instanceof Integer i) return i;
            if (lit.getValor() instanceof Number num) return num.intValue();
        } else if (nodo instanceof NodoOperacionUnaria unaria) {
            Integer sub = evaluarConstanteEntera(unaria.getExpresion());
            if (sub != null) {
                if ("-".equals(unaria.getOperador())) return -sub;
                if ("+".equals(unaria.getOperador())) return sub;
            }
        } else if (nodo instanceof NodoOperacionBinaria bin) {
            Integer izq = evaluarConstanteEntera(bin.getIzq());
            Integer der = evaluarConstanteEntera(bin.getDer());
            if (izq != null && der != null) {
                return switch (bin.getOperador()) {
                    case "+" -> izq + der;
                    case "-" -> izq - der;
                    case "*" -> izq * der;
                    case "/" -> der != 0 ? izq / der : null;
                    case "%" -> der != 0 ? izq % der : null;
                    default -> null;
                };
            }
        }
        return null;
    }

    public boolean comprobarArreglo(Tipo tipoEsperado, TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        return comprobarArreglo(tipoEsperado, java.util.Collections.emptyList(), 0, tablaSimbolos, errores);
    }

    public boolean comprobarArreglo(Tipo tipoEsperado, List<NodoAST> dimensionesDeclaradas, int nivelDimension, TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";
        Tipo tipoComp = tipoEsperado.getTipoComponente();

        if (tipoComp == null)
            return false;

        boolean valido = true;

        if (dimensionesDeclaradas != null && nivelDimension < dimensionesDeclaradas.size()) {
            NodoAST dimNode = dimensionesDeclaradas.get(nivelDimension);
            Integer capMax = evaluarConstanteEntera(dimNode);
            if (capMax != null) {
                if (elementos.size() > capMax) {
                    errores.add(new ErrorSemantico(
                            String.format(
                                    "La lista de inicialización contiene %d elementos, superando el tamaño declarado de %d para la dimensión %d del arreglo.",
                                    elementos.size(), capMax, nivelDimension + 1),
                            ambito, linea, columna));
                    valido = false;
                }
            }
        }

        Tipo tipoElementoEsperado = (tipoEsperado.getDimensiones() > 1)
                ? new Tipo(tipoComp, tipoEsperado.getDimensiones() - 1)
                : tipoComp;

        for (int i = 0; i < elementos.size(); i++) {
            NodoAST elem = elementos.get(i);
            if (elem instanceof NodoListaValores subLista && tipoElementoEsperado.getBase() == TipoBase.ARREGLO) {
                boolean subOk = subLista.comprobarArreglo(tipoElementoEsperado, dimensionesDeclaradas, nivelDimension + 1, tablaSimbolos, errores);
                if (!subOk) valido = false;
                continue;
            }

            Tipo tipoElemento = elem.comprobar(tablaSimbolos, errores);

            if (tipoElemento.getBase() != TipoBase.ERROR
                    && !ComprobadorTipos.esCompatibleAsignacion(tipoElementoEsperado, tipoElemento)) {
                errores.add(new ErrorSemantico(
                        String.format(
                                "Elemento %d de la lista incompatible con el arreglo: se esperaba '%s' y se obtuvo '%s'.",
                                i + 1, tipoElementoEsperado, tipoElemento),
                        ambito, linea, columna));
                valido = false;
            }
        }
        return valido;
    }

    public boolean comprobarEstructura(Tipo tipoEstructura, TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";

        if (tipoEstructura == null || (tipoEstructura.getBase() != TipoBase.ESTRUCTURA && tipoEstructura.getBase() != TipoBase.OBJETO_CLASE)) {
            return false;
        }

        int camposEsperados = tipoEstructura.getCampos().size();
        int valoresRecibidos = (elementos != null) ? elementos.size() : 0;

        if (valoresRecibidos != camposEsperados) {
            errores.add(new ErrorSemantico(
                    String.format("La estructura '%s' requiere %d valores, pero se proporcionaron %d.",
                            tipoEstructura.getNombreTipo(), camposEsperados, valoresRecibidos),
                    ambito, linea, columna));
            return false;
        }

        boolean valido = true;
        for (int i = 0; i < camposEsperados; i++) {
            NodoAST elem = elementos.get(i);
            Tipo tipoCampo = tipoEstructura.obtenerTipoCampoPorIndice(i);
            String nombreCampo = tipoEstructura.obtenerNombreCampoPorIndice(i);

            if (elem instanceof NodoListaValores subLista) {
                if (tipoCampo != null && (tipoCampo.getBase() == TipoBase.ESTRUCTURA || tipoCampo.getBase() == TipoBase.OBJETO_CLASE)) {
                    Tipo tipoCompuestoReal = tablaSimbolos.buscarTipoCompuesto(tipoCampo.getNombreTipo());
                    boolean subOk = subLista.comprobarEstructura(tipoCompuestoReal != null ? tipoCompuestoReal : tipoCampo, tablaSimbolos, errores);
                    if (!subOk) valido = false;
                    continue;
                } else if (tipoCampo != null && tipoCampo.getBase() == TipoBase.ARREGLO) {
                    boolean subOk = subLista.comprobarArreglo(tipoCampo, tablaSimbolos, errores);
                    if (!subOk) valido = false;
                    continue;
                }
            }

            Tipo tipoElemento = elem.comprobar(tablaSimbolos, errores);
            if (tipoElemento.getBase() != TipoBase.ERROR && tipoCampo != null) {
                if (!ComprobadorTipos.esCompatibleAsignacion(tipoCampo, tipoElemento)) {
                    errores.add(new ErrorSemantico(
                            String.format("Incompatibilidad en el campo '%s' (posición %d) de la estructura '%s': se esperaba '%s' y se obtuvo '%s'.",
                                    nombreCampo, i + 1, tipoEstructura.getNombreTipo(), tipoCampo, tipoElemento),
                            ambito, linea, columna));
                    valido = false;
                }
            }
        }

        return valido;
    }
}
