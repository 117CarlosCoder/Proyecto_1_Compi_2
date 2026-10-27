package org.compi2.ast.expresiones;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.Simbolo;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;

import org.compi2.tipos.TipoBase;
import org.compi2.tipos.Visibilidad;

import java.util.List;

@Getter
public class NodoIdentificador implements NodoAST {
    private final String nombre;
    private final int linea;
    private final int columna;

    public NodoIdentificador(String nombre, int linea, int columna) {
        this.nombre = nombre;
        this.linea = linea;
        this.columna = columna;
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Simbolo simbolo = tablaSimbolos.buscarSimbolo(nombre);

        boolean enLocal = false;
        if (simbolo != null) {
            String amb = simbolo.getAmbito();
            if (amb != null && !amb.equals("Global") && !amb.startsWith("Clase_")) {
                enLocal = true;
            }
        }

        if (!enLocal) {
            Simbolo simThis = tablaSimbolos.buscarSimbolo("this");
            if (simThis != null) {
                Tipo tipoClase = tablaSimbolos.resolverTipo(simThis.getTipo());
                if (tipoClase != null && tipoClase.getBase() == TipoBase.OBJETO_CLASE) {
                    Tipo tipoCampo = tipoClase.buscarCampo(nombre);
                    if (tipoCampo != null) {
                        Visibilidad vis = tipoClase.obtenerVisibilidadCampo(nombre);
                        String claseDeclarante = tipoClase.obtenerClaseDeclaranteCampo(nombre);
                        String claseActual = tablaSimbolos.getClaseActual();
                        if (!tablaSimbolos.esAccesible(vis, claseDeclarante, claseActual)) {
                            errores.add(new ErrorSemantico(
                                String.format("El campo '%s' de la clase '%s' no es accesible (visibilidad %s).",
                                    nombre, claseDeclarante, vis),
                                tablaSimbolos.getAmbitoActual().getNombreAmbito(), linea, columna));
                            return Tipo.ERROR;
                        }
                        return tipoCampo;
                    }
                }
            }
        }

        if (simbolo == null) {
            if ("super".equals(nombre)) {
                String claseActual = tablaSimbolos.getClaseActual();
                if (claseActual != null) {
                    Tipo tClase = tablaSimbolos.buscarTipoCompuesto(claseActual);
                    if (tClase != null && tClase.getTipoPadre() != null) {
                        return tClase.getTipoPadre();
                    }
                }
            }
            errores.add(new ErrorSemantico(
                "Identificador no declarado: '" + nombre + "'",
                tablaSimbolos.getAmbitoActual().getNombreAmbito(),
                linea,
                columna
            ));
            return Tipo.ERROR;
        }
        return simbolo.getTipo();
    }
}