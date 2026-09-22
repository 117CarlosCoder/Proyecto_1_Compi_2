package org.compi2.ast.expresiones;

import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;

import java.util.List;

@Getter
public class NodoLiteral implements NodoAST {
    private final Object valor;
    private final Tipo tipo;
    private final int linea;
    private final int columna;

    public NodoLiteral(Object valor, Tipo tipo, int linea, int columna) {
        this.valor = valor;
        this.tipo = tipo;
        this.linea = linea;
        this.columna = columna;
    }

    public NodoLiteral(int valor, int linea, int columna) {
        this(valor, Tipo.ENTERO, linea, columna);
    }

    public NodoLiteral(double valor, int linea, int columna) {
        this(valor, Tipo.DECIMAL, linea, columna);
    }

    public NodoLiteral(String valor, int linea, int columna) {
        this(valor, Tipo.CADENA, linea, columna);
    }

    public NodoLiteral(char valor, int linea, int columna) {
        this(valor, Tipo.CARACTER, linea, columna);
    }

    public NodoLiteral(boolean valor, int linea, int columna) {
        this(valor, Tipo.BOOLEANO, linea, columna);
    }

    @Override
    public Tipo comprobar(TablaSimbolos ts, List<ErrorSemantico> errores) {
        return this.tipo;
    }
}