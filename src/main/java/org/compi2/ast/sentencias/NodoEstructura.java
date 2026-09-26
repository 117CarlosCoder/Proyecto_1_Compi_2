package org.compi2.ast.sentencias;

import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
public class NodoEstructura implements NodoAST {
    private final String nombre;
    private final LinkedHashMap<String, Tipo> campos;
    private final int linea;
    private final int columna;

    private boolean firmaRegistrada = false;
    private Tipo tipoEstructuraCache;

    public NodoEstructura(String nombre, Map<String, Tipo> campos, int linea, int columna) {
        this.nombre = nombre;
        this.campos = (campos instanceof LinkedHashMap)
                ? (LinkedHashMap<String, Tipo>) campos
                : new LinkedHashMap<>(campos != null ? campos : Collections.emptyMap());
        this.linea = linea;
        this.columna = columna;
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        return registrarFirma(tablaSimbolos, errores);
    }

    public Tipo registrarFirma(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        if (firmaRegistrada && tipoEstructuraCache != null && tablaSimbolos.existeTipoCompuesto(nombre)) {
            return tipoEstructuraCache;
        }

        tipoEstructuraCache = new Tipo(TipoBase.ESTRUCTURA, nombre);
        if (campos != null) {
            for (Map.Entry<String, Tipo> campo : campos.entrySet()) {
                tipoEstructuraCache.agregarCampo(campo.getKey(), campo.getValue());
            }
        }

        boolean ok = tablaSimbolos.registrarTipoCompuesto(tipoEstructuraCache);
        if (!ok) {
            errores.add(new ErrorSemantico(
                "La estructura '" + nombre + "' ya fue definida previamente.",
                "Global", linea, columna
            ));
            return Tipo.ERROR;
        }

        tablaSimbolos.registrarEstructura(nombre, tipoEstructuraCache, linea, columna);
        if (campos != null) {
            int off = 0;
            for (Map.Entry<String, Tipo> campo : campos.entrySet()) {
                tablaSimbolos.registrarCampoEstructura(nombre, campo.getKey(), campo.getValue(), linea, columna, off++);
            }
        }

        firmaRegistrada = true;
        return tipoEstructuraCache;
    }
}
