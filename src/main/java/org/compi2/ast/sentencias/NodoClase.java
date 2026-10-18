package org.compi2.ast.sentencias;

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
public class NodoClase implements NodoAST {
    private final String nombre;
    private final List<NodoDeclaracion> campos;
    private final List<NodoConstructor> constructores;
    private final List<NodoFuncion> metodos;
    private final int linea;
    private final int columna;

    private boolean claseRegistrada = false;
    private Tipo tipoClaseCache;

    public Tipo registrarEnTablaSimbolos(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        if (claseRegistrada && tipoClaseCache != null && tablaSimbolos.existeTipoCompuesto(nombre)) {
            return tipoClaseCache;
        }

        tipoClaseCache = new Tipo(TipoBase.OBJETO_CLASE, nombre);

        if (campos != null) {
            for (NodoDeclaracion campo : campos) {
                tipoClaseCache.agregarCampo(campo.getNombre(), campo.getTipoDeclarado());
            }
        }

        if (constructores != null) {
            for (NodoConstructor c : constructores) {
                tipoClaseCache.agregarConstructor(obtenerTiposParametros(c.getParametros()));
            }
        }

        if (metodos != null) {
            for (NodoFuncion m : metodos) {
                tipoClaseCache.agregarMetodo(m.getNombre(), crearTipoMetodo(m));
            }
        }

        boolean ok = tablaSimbolos.registrarTipoCompuesto(tipoClaseCache);
        if (!ok) {
            errores.add(new ErrorSemantico(
                "La clase '" + nombre + "' ya fue definida previamente.",
                "Global", linea, columna
            ));
        } else {
            tablaSimbolos.registrarClase(nombre, tipoClaseCache, linea, columna);

            if (constructores != null) {
                for (NodoConstructor c : constructores) {
                    tablaSimbolos.registrarConstructorClase(nombre, tipoClaseCache, c.getLinea(), c.getColumna());
                }
            }

            if (metodos != null) {
                for (NodoFuncion m : metodos) {
                    tablaSimbolos.registrarMetodoClase(nombre, m.getNombre(), crearTipoMetodo(m), m.getLinea(), m.getColumna());
                }
            }
        }

        claseRegistrada = true;
        return tipoClaseCache;
    }

    private List<Tipo> obtenerTiposParametros(List<Parametro> parametros) {
        List<Tipo> tipos = new ArrayList<>();
        if (parametros != null) {
            for (Parametro p : parametros) {
                tipos.add(p.getTipo());
            }
        }
        return tipos;
    }

    private Tipo crearTipoMetodo(NodoFuncion m) {
        Tipo retorno = m.getTipoRetorno() != null ? m.getTipoRetorno() : Tipo.VOID;
        return new Tipo(m.getNombre(), obtenerTiposParametros(m.getParametros()), retorno);
    }

    public void comprobarCuerpos(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Tipo tipoClase = (tipoClaseCache != null) ? tipoClaseCache : tablaSimbolos.buscarTipoCompuesto(nombre);
        if (tipoClase == null) {
            tipoClase = new Tipo(TipoBase.OBJETO_CLASE, nombre);
        }

        tablaSimbolos.abrirAmbito("Clase_" + nombre, true);
        try {
            tablaSimbolos.registrarSimbolo("this", tipoClase, "variable", linea, columna);

            if (campos != null) {
                for (NodoDeclaracion campo : campos) {
                    campo.comprobar(tablaSimbolos, errores);
                }
            }

            if (constructores != null) {
                for (NodoConstructor c : constructores) {
                    c.comprobar(tablaSimbolos, errores);
                }
            }

            if (metodos != null) {
                for (NodoFuncion m : metodos) {
                    m.comprobar(tablaSimbolos, errores);
                }
            }
        } finally {
            tablaSimbolos.cerrarAmbito();
        }
    }

    public List<NodoFuncion> aFunciones() {
        List<NodoFuncion> funciones = new ArrayList<>();
        if (metodos != null) {
            for (NodoFuncion metodo : metodos) {
                metodo.setEsMetodo(true);
                metodo.setClaseContenedora(nombre);
                funciones.add(metodo);
            }
        }
        if (constructores != null) {
            for (NodoConstructor constructor : constructores) {
                NodoFuncion fn = new NodoFuncion(
                    nombre + "_constructor",
                    constructor.getParametros(),
                    Tipo.VOID,
                    constructor.getCuerpo(),
                    constructor.getLinea(),
                    constructor.getColumna());
                fn.setEsMetodo(true);
                fn.setClaseContenedora(nombre);
                funciones.add(fn);
            }
        }
        return funciones;
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Tipo tipo = registrarEnTablaSimbolos(tablaSimbolos, errores);
        comprobarCuerpos(tablaSimbolos, errores);
        return tipo;
    }
}
