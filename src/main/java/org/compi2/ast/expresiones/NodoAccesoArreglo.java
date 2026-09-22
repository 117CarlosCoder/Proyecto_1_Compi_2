package org.compi2.ast.expresiones;

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
public class NodoAccesoArreglo implements NodoAST {
    private final NodoAST estructura;
    private final NodoAST indice;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Tipo tipoEstructura = estructura.comprobar(tablaSimbolos, errores);
        Tipo tipoIndice = indice.comprobar(tablaSimbolos, errores);

        if (tipoEstructura.getBase() == TipoBase.ERROR) return Tipo.ERROR;

        if (tipoEstructura.getBase() != TipoBase.ARREGLO) {
            errores.add(new ErrorSemantico(
                "Intento de indexar con '[]' una variable que no es un arreglo: " + tipoEstructura,
                tablaSimbolos.getAmbitoActual().getNombreAmbito(),
                linea,
                columna
            ));
            return Tipo.ERROR;
        }

        if (tipoIndice.getBase() != TipoBase.ENTERO && tipoIndice.getBase() != TipoBase.ERROR) {
            errores.add(new ErrorSemantico(
                "El índice del arreglo debe ser de tipo entero. Obtenido: " + tipoIndice,
                tablaSimbolos.getAmbitoActual().getNombreAmbito(),
                linea,
                columna
            ));
        }

        if (tipoEstructura.getDimensiones() > 1) {
            return new Tipo(tipoEstructura.getTipoComponente(), tipoEstructura.getDimensiones() - 1);
        }
        return tipoEstructura.getTipoComponente() != null ? tipoEstructura.getTipoComponente() : Tipo.ERROR;
    }
}