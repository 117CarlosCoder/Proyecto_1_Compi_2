package org.compi2.ast.expresiones;

import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;
import org.compi2.tipos.Visibilidad;

import java.util.List;

@Getter
public class NodoAccesoMiembro implements NodoAST {
    private final NodoAST estructura;
    private final String nombreCampo;
    private final int linea;
    private final int columna;

    public NodoAccesoMiembro(NodoAST estructura, String nombreCampo, int linea, int columna) {
        this.estructura = estructura;
        this.nombreCampo = nombreCampo;
        this.linea = linea;
        this.columna = columna;
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Tipo tipoEstructura = estructura.comprobar(tablaSimbolos, errores);
        if (tipoEstructura.getBase() == TipoBase.ERROR)
            return Tipo.ERROR;

        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito() : "Global";

        if (tipoEstructura.getBase() != TipoBase.OBJETO_CLASE && tipoEstructura.getBase() != TipoBase.ESTRUCTURA) {
            errores.add(new ErrorSemantico(
                    "El tipo '" + tipoEstructura + "' no admite acceso a miembros con punto ('.').",
                    ambito, linea, columna));
            return Tipo.ERROR;
        }

        Tipo tipoEfectivo = tablaSimbolos.resolverTipo(tipoEstructura);
        Tipo tipoCampo = tipoEfectivo.buscarCampo(nombreCampo);
        if (tipoCampo == null) {
            errores.add(new ErrorSemantico(
                    String.format("El campo '%s' no existe en el tipo '%s'.",
                            nombreCampo, tipoEstructura.getNombreTipo()),
                    ambito, linea, columna));
            return Tipo.ERROR;
        }

        if (tipoEfectivo.getBase() == TipoBase.OBJETO_CLASE) {
            Visibilidad vis = tipoEfectivo.obtenerVisibilidadCampo(nombreCampo);
            String claseDeclarante = tipoEfectivo.obtenerClaseDeclaranteCampo(nombreCampo);
            if (!tablaSimbolos.esAccesible(vis, claseDeclarante)) {
                errores.add(new ErrorSemantico(
                        String.format("El campo '%s' de la clase '%s' no es accesible (visibilidad %s).",
                                nombreCampo, claseDeclarante, vis),
                        ambito, linea, columna));
                return Tipo.ERROR;
            }
        }

        return tipoCampo;
    }
}