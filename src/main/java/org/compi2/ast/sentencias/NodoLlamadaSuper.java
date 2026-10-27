package org.compi2.ast.sentencias;

import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;
import org.compi2.tipos.Visibilidad;

import java.util.ArrayList;
import java.util.List;

@Getter
public class NodoLlamadaSuper implements NodoAST {
    private final List<NodoAST> argumentos;
    private final int linea;
    private final int columna;

    public NodoLlamadaSuper(List<NodoAST> argumentos, int linea, int columna) {
        this.argumentos = argumentos;
        this.linea = linea;
        this.columna = columna;
    }

    private String clasePadreResuelta;
    private Tipo constructorResuelto;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        this.clasePadreResuelta = null;
        this.constructorResuelto = null;

        String ambito = (tablaSimbolos.getAmbitoActual() != null)
                ? tablaSimbolos.getAmbitoActual().getNombreAmbito() : "Global";

        String claseActual = tablaSimbolos.getClaseActual();
        if (claseActual == null || !tablaSimbolos.estaEnConstructor() || !ambito.startsWith("Constructor_")) {
            errores.add(new ErrorSemantico(
                "La llamada a super(...) solo es válida dentro del constructor de una clase.",
                ambito, linea, columna));
            return Tipo.ERROR;
        }

        Tipo tipoClase = tablaSimbolos.buscarTipoCompuesto(claseActual);
        String clasePadre = (tipoClase != null) ? tipoClase.getClasePadre() : null;
        if (clasePadre == null) {
            errores.add(new ErrorSemantico(
                String.format("No se puede invocar a super(...) porque la clase '%s' no tiene una superclase.", claseActual),
                ambito, linea, columna));
            return Tipo.ERROR;
        }

        Tipo tipoPadre = tablaSimbolos.buscarTipoCompuesto(clasePadre);
        if (tipoPadre == null) {
            errores.add(new ErrorSemantico(
                String.format("La clase padre '%s' no está definida.", clasePadre),
                ambito, linea, columna));
            return Tipo.ERROR;
        }

        List<Tipo> tiposArgs = new ArrayList<>();
        boolean errorEnArgs = false;
        if (argumentos != null) {
            for (NodoAST arg : argumentos) {
                if (arg != null) {
                    Tipo tArg = arg.comprobar(tablaSimbolos, errores);
                    if (tArg.getBase() == TipoBase.ERROR) {
                        errorEnArgs = true;
                    }
                    tiposArgs.add(tArg);
                }
            }
        }

        if (errorEnArgs) {
            return Tipo.ERROR;
        }

        List<Tipo> paramsCtor = tipoPadre.buscarConstructor(tiposArgs);
        if (paramsCtor == null) {
            errores.add(new ErrorSemantico(
                String.format("No existe un constructor en la superclase '%s' que coincida con los argumentos: %s.",
                    clasePadre, tiposArgs),
                ambito, linea, columna));
            return Tipo.ERROR;
        }

        Visibilidad vis = tipoPadre.obtenerVisibilidadConstructor(paramsCtor);
        if (!tablaSimbolos.esAccesible(vis, clasePadre, claseActual)) {
            errores.add(new ErrorSemantico(
                String.format("El constructor de la superclase '%s' no es accesible (visibilidad %s).",
                    clasePadre, vis),
                ambito, linea, columna));
            return Tipo.ERROR;
        }

        this.clasePadreResuelta = clasePadre;
        this.constructorResuelto = new Tipo(clasePadre + "_constructor", paramsCtor, Tipo.VOID);
        return Tipo.VOID;
    }
}
