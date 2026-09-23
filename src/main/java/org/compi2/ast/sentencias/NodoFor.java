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
public class NodoFor implements NodoAST {
    private final NodoAST inicializacion;
    private final NodoAST condicion;
    private final NodoAST paso;
    private final List<NodoAST> cuerpo;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";

        tablaSimbolos.abrirAmbitoBucle("Bloque_For");

        if (inicializacion != null) {
            inicializacion.comprobar(tablaSimbolos, errores);
        }

        if (condicion != null) {
            Tipo tipoCond = condicion.comprobar(tablaSimbolos, errores);
            if (tipoCond.getBase() != TipoBase.BOOLEANO && tipoCond.getBase() != TipoBase.ERROR) {
                errores.add(new ErrorSemantico(
                        "La condición del ciclo ('for', 'per', 'para') debe ser booleana. Obtenido: " + tipoCond,
                        ambito, linea, columna));
            }
        }

        if (paso != null) {
            paso.comprobar(tablaSimbolos, errores);
        }

        if (cuerpo != null) {
            for (NodoAST inst : cuerpo) {
                if (inst != null) {
                    inst.comprobar(tablaSimbolos, errores);
                }
            }
        }

        tablaSimbolos.cerrarAmbito();
        return Tipo.VOID;
    }
}
