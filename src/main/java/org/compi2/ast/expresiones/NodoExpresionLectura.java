package org.compi2.ast.expresiones;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class NodoExpresionLectura implements NodoAST {
    private final int linea;
    private final int columna;

    private Tipo tipoResuelto = Tipo.CADENA;

    public void aplicarTipoEsperado(Tipo tipoEsperado) {
        if (tipoEsperado == null || tipoEsperado.getBase() == null) {
            return;
        }
        switch (tipoEsperado.getBase()) {
            case CADENA, ENTERO, DECIMAL, CARACTER, BOOLEANO -> this.tipoResuelto = tipoEsperado;
            default -> {}
        }
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        return tipoResuelto;
    }
}
