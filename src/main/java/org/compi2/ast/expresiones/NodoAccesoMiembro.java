package org.compi2.ast.expresiones;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.List;

@Getter
@AllArgsConstructor
public class NodoAccesoMiembro implements NodoAST {
    private final NodoAST estructura;
    private final String nombreCampo;
    private final int linea;
    private final int columna;

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

        Tipo tipoCampo = tipoEstructura.buscarCampo(nombreCampo);
        if (tipoCampo == null) {
            errores.add(new ErrorSemantico(
                    String.format("El campo '%s' no existe en el tipo compuesto '%s'.",
                            nombreCampo, tipoEstructura.getNombreTipo()),
                    ambito, linea, columna));
            return Tipo.ERROR;
        }

        return tipoCampo;
    }
}