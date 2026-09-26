package org.compi2.ast.sentencias;

import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NodoClase implements NodoAST {
    private final String nombre;
    private final List<NodoDeclaracion> campos;
    private final List<NodoConstructor> constructores;
    private final List<NodoFuncion> metodos;
    private final int linea;
    private final int columna;

    private boolean firmasRegistradas = false;
    private Tipo tipoClaseCache;

    public Tipo registrarFirmas(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        if (firmasRegistradas && tipoClaseCache != null && tablaSimbolos.existeTipoCompuesto(nombre)) {
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
                List<Tipo> params = new ArrayList<>();
                if (c.getParametros() != null) {
                    for (Parametro p : c.getParametros()) {
                        params.add(p.getTipo());
                    }
                }
                tipoClaseCache.agregarConstructor(params);
            }
        }

        if (metodos != null) {
            for (NodoFuncion m : metodos) {
                List<Tipo> params = new ArrayList<>();
                if (m.getParametros() != null) {
                    for (Parametro p : m.getParametros()) {
                        params.add(p.getTipo());
                    }
                }
                Tipo tipoMetodo = new Tipo(m.getNombre(), params, m.getTipoRetorno() != null ? m.getTipoRetorno() : Tipo.VOID);
                tipoClaseCache.agregarMetodo(m.getNombre(), tipoMetodo);
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
                    List<Tipo> params = new ArrayList<>();
                    if (m.getParametros() != null) {
                        for (Parametro p : m.getParametros()) {
                            params.add(p.getTipo());
                        }
                    }
                    Tipo tipoMetodo = new Tipo(m.getNombre(), params, m.getTipoRetorno() != null ? m.getTipoRetorno() : Tipo.VOID);
                    tablaSimbolos.registrarMetodoClase(nombre, m.getNombre(), tipoMetodo, m.getLinea(), m.getColumna());
                }
            }
        }

        firmasRegistradas = true;
        return tipoClaseCache;
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

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Tipo tipo = registrarFirmas(tablaSimbolos, errores);
        comprobarCuerpos(tablaSimbolos, errores);
        return tipo;
    }
}
