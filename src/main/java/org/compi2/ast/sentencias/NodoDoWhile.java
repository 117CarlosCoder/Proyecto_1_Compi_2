package org.compi2.ast.sentencias;

import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.List;

@Getter
public class NodoDoWhile implements NodoAST {
    private final NodoAST condicion;
    private final List<NodoAST> cuerpo;
    private final int linea;
    private final int columna;

    public NodoDoWhile(NodoAST condicion, List<NodoAST> cuerpo, int linea, int columna) {
        this.condicion = condicion;
        this.cuerpo = cuerpo;
        this.linea = linea;
        this.columna = columna;
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";

        tablaSimbolos.abrirAmbitoBucle("Ambito_DoWhile");
        try {
            if (cuerpo != null) {
                for (NodoAST inst : cuerpo) {
                    if (inst != null) {
                        inst.comprobar(tablaSimbolos, errores);
                    }
                }
            }
        } finally {
            tablaSimbolos.cerrarAmbito();
        }

        Tipo tipoCond = condicion.comprobar(tablaSimbolos, errores);
        if (tipoCond.getBase() != TipoBase.BOOLEANO && tipoCond.getBase() != TipoBase.ERROR) {
            errores.add(new ErrorSemantico(
                    "La condición del ciclo ('do-while', 'facere', 'hacer') debe ser de tipo booleano. Obtenido: "
                            + tipoCond,
                    ambito, linea, columna));
        }

        return Tipo.VOID;
    }
}
