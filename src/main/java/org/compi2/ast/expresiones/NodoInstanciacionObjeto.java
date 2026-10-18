package org.compi2.ast.expresiones;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class NodoInstanciacionObjeto implements NodoAST {
    private final String nombreClase;
    private final List<NodoAST> argumentos;
    private final int linea;
    private final int columna;

    private Tipo constructorResuelto;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        this.constructorResuelto = null;
        Tipo tipoClase = tablaSimbolos.buscarTipoCompuesto(nombreClase);
        if (tipoClase == null || (tipoClase.getBase() != TipoBase.OBJETO_CLASE && tipoClase.getBase() != TipoBase.ESTRUCTURA)) {
            errores.add(new ErrorSemantico(
                "El tipo '" + nombreClase + "' no está definido ni importado desde ningún archivo .z o .y.",
                tablaSimbolos.getAmbitoActual().getNombreAmbito(), linea, columna));
            return Tipo.ERROR;
        }

        if (tipoClase.getBase() == TipoBase.ESTRUCTURA) {
            return tipoClase;
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

        List<Tipo> paramsCtor = tipoClase.buscarConstructor(tiposArgs);
        if (paramsCtor == null) {
            errores.add(new ErrorSemantico(
                String.format("No existe un constructor en la clase '%s' que coincida con los argumentos: %s.",
                    nombreClase, tiposArgs),
                tablaSimbolos.getAmbitoActual().getNombreAmbito(), linea, columna));
            return Tipo.ERROR;
        }

        this.constructorResuelto = new Tipo(nombreClase + "_constructor", paramsCtor, Tipo.VOID);

        return tipoClase;
    }
}