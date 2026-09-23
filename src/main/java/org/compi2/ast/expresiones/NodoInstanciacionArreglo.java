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
public class NodoInstanciacionArreglo implements NodoAST {
    private final Tipo tipoComponente;
    private final List<NodoAST> tamanosDimensiones;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";

        if (tamanosDimensiones == null || tamanosDimensiones.isEmpty()) {
            errores.add(new ErrorSemantico("Un arreglo nuevo debe especificar al menos el tamaño de una dimensión.",
                    ambito, linea, columna));
            return Tipo.ERROR;
        }

        for (int i = 0; i < tamanosDimensiones.size(); i++) {
            NodoAST tam = tamanosDimensiones.get(i);
            Tipo tTam = tam.comprobar(tablaSimbolos, errores);
            if (tTam.getBase() != TipoBase.ENTERO && tTam.getBase() != TipoBase.ERROR) {
                errores.add(new ErrorSemantico(
                        String.format("El tamaño de la dimensión %d del arreglo debe ser un entero. Obtenido: %s.",
                                i + 1, tTam),
                        ambito, linea, columna));
            }
        }

        return new Tipo(tipoComponente, tamanosDimensiones.size());
    }
}
