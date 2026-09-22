package org.compi2.ast.expresiones;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.Simbolo;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;

import java.util.List;

@Getter
@AllArgsConstructor
public class NodoIdentificador implements NodoAST {
    private final String nombre;
    private final int linea;
    private final int columna;

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Simbolo simbolo = tablaSimbolos.buscarSimbolo(nombre);
        if (simbolo == null) {
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