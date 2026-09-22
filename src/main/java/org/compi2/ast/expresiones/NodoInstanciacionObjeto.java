package org.compi2.ast.expresiones;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
public class NodoInstanciacionObjeto implements NodoAST {
    private final String nombreClase;
    private final List<NodoAST> argumentos;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Tipo tipoClase = tablaSimbolos.buscarTipoCompuesto(nombreClase);
        if (tipoClase == null || tipoClase.getBase() != TipoBase.OBJETO_CLASE) {
            errores.add(new ErrorSemantico(
                "La clase '" + nombreClase + "' no está definida ni importada desde ningún archivo .z.",
                tablaSimbolos.getAmbitoActual().getNombreAmbito(), linea, columna));
            return Tipo.ERROR;
        }

        List<Tipo> tiposArgs = new ArrayList<>();
        boolean contieneErrorEnArgumentos = false;

        if (argumentos != null) {
            for (NodoAST arg : argumentos) {
                Tipo tArg = arg.comprobar(tablaSimbolos, errores);
                if (tArg.getBase() == TipoBase.ERROR) {
                    contieneErrorEnArgumentos = true;
                }
                tiposArgs.add(tArg);
            }
        }

        if (contieneErrorEnArgumentos) {
            return Tipo.ERROR;
        }

        if (!tipoClase.existeConstructor(tiposArgs)) {
            errores.add(new ErrorSemantico(
                String.format("No existe un constructor en la clase '%s' que coincida con los argumentos: %s.",
                    nombreClase, tiposArgs),
                tablaSimbolos.getAmbitoActual().getNombreAmbito(), linea, columna));
            return Tipo.ERROR;
        }

        return tipoClase;
    }
}