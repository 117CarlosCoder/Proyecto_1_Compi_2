package org.compi2.ast.expresiones;

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
public class NodoLlamadaMetodo implements NodoAST {
    private final NodoAST estructura;
    private final String nombreMetodo;
    private final List<NodoAST> argumentos;
    private final int linea;
    private final int columna;

    public NodoLlamadaMetodo(NodoAST estructura, String nombreMetodo, List<NodoAST> argumentos, int linea, int columna) {
        this.estructura = estructura;
        this.nombreMetodo = nombreMetodo;
        this.argumentos = argumentos;
        this.linea = linea;
        this.columna = columna;
    }

    private String claseObjetivoResuelta;
    private Tipo metodoResuelto;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        this.claseObjetivoResuelta = null;
        this.metodoResuelto = null;

        Tipo tipoEstructura = estructura.comprobar(tablaSimbolos, errores);
        if (tipoEstructura.getBase() == TipoBase.ERROR)
            return Tipo.ERROR;

        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";

        Tipo tipoEfectivo = tablaSimbolos.resolverTipo(tipoEstructura);

        if (tipoEfectivo.getBase() != TipoBase.OBJETO_CLASE) {
            errores.add(new ErrorSemantico(
                    "Solo las instancias de clase pueden invocar métodos. Tipo recibido: " + tipoEstructura,
                    ambito, linea, columna));
            return Tipo.ERROR;
        }

        if (!tipoEfectivo.existeMetodo(nombreMetodo)) {
            errores.add(new ErrorSemantico(
                    String.format("El método '%s' no está definido en la clase '%s'.",
                            nombreMetodo, tipoEstructura.getNombreTipo()),
                    ambito, linea, columna));
            return Tipo.ERROR;
        }

        List<Tipo> tiposArgs = new ArrayList<>();
        if (argumentos != null) {
            for (NodoAST arg : argumentos) {
                tiposArgs.add(arg.comprobar(tablaSimbolos, errores));
            }
        }

        Tipo metodo = tipoEfectivo.buscarMetodo(nombreMetodo, tiposArgs);
        if (metodo == null) {
            Tipo candAncestros = tipoEfectivo.buscarMetodoAncestrosCualquiera(nombreMetodo, tiposArgs);
            if (candAncestros != null) {
                Visibilidad vis = candAncestros.getVisibilidad();
                String claseDecl = candAncestros.getClaseDeclarante() != null ? candAncestros.getClaseDeclarante() : tipoEfectivo.getNombreTipo();
                if (!tablaSimbolos.esAccesible(vis, claseDecl)) {
                    errores.add(new ErrorSemantico(
                        String.format("El método '%s' de la clase '%s' no es accesible (visibilidad %s).",
                            nombreMetodo, claseDecl, vis),
                        ambito, linea, columna));
                    return Tipo.ERROR;
                }
            }

            errores.add(new ErrorSemantico(
                    String.format(
                            "No existe una sobrecarga del método '%s' en la clase '%s' que coincida con los argumentos: %s.",
                            nombreMetodo, tipoEstructura.getNombreTipo(), tiposArgs),
                    ambito, linea, columna));
            return Tipo.ERROR;
        }

        Visibilidad vis = metodo.getVisibilidad();
        String claseDeclarante = metodo.getClaseDeclarante() != null ? metodo.getClaseDeclarante() : tipoEfectivo.getNombreTipo();
        if (!tablaSimbolos.esAccesible(vis, claseDeclarante)) {
            errores.add(new ErrorSemantico(
                    String.format("El método '%s' de la clase '%s' no es accesible (visibilidad %s).",
                            nombreMetodo, claseDeclarante, vis),
                    ambito, linea, columna));
            return Tipo.ERROR;
        }

        this.claseObjetivoResuelta = tipoEfectivo.getNombreTipo();
        this.metodoResuelto = metodo;

        return (metodo.getTipoRetorno() != null) ? metodo.getTipoRetorno() : Tipo.VOID;
    }
}