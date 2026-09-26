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
public class NodoLlamadaMetodo implements NodoAST {
    private final NodoAST estructura;
    private final String nombreMetodo;
    private final List<NodoAST> argumentos;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Tipo tipoEstructura = estructura.comprobar(tablaSimbolos, errores);
        if (tipoEstructura.getBase() == TipoBase.ERROR)
            return Tipo.ERROR;

        String ambito = (tablaSimbolos.getAmbitoActual() != null) ? tablaSimbolos.getAmbitoActual().getNombreAmbito()
                : "Global";

        Tipo compuesto = tablaSimbolos.buscarTipoCompuesto(tipoEstructura.getNombreTipo());
        Tipo tipoEfectivo = (compuesto != null) ? compuesto : tipoEstructura;

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
            errores.add(new ErrorSemantico(
                    String.format(
                            "No existe una sobrecarga del método '%s' en la clase '%s' que coincida con los argumentos: %s.",
                            nombreMetodo, tipoEstructura.getNombreTipo(), tiposArgs),
                    ambito, linea, columna));
            return Tipo.ERROR;
        }

        return (metodo.getTipoRetorno() != null) ? metodo.getTipoRetorno() : Tipo.VOID;
    }
}