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

    public boolean comprobarArreglo(Tipo tipoEsperado, TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";
        Tipo tipoComp = tipoEsperado.getTipoComponente();

        if (tipoComp == null)
            return false;

        boolean valido = true;
        for (int i = 0; i < elementos.size(); i++) {
            NodoAST elem = elementos.get(i);
            Tipo tipoElemento = elem.comprobar(tablaSimbolos, errores);

            if (tipoElemento.getBase() != TipoBase.ERROR
                    && !ComprobadorTipos.esCompatibleAsignacion(tipoComp, tipoElemento)) {
                errores.add(new ErrorSemantico(
                        String.format(
                                "Elemento %d de la lista incompatible con el arreglo: se esperaba '%s' y se obtuvo '%s'.",
                                i + 1, tipoComp, tipoElemento),
                        ambito, linea, columna));
                valido = false;
            }
        }
        return valido;
    }
}
