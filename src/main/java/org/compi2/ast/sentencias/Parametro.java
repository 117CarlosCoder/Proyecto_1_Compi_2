package org.compi2.ast.sentencias;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.compi2.tipos.Tipo;

@Getter
@AllArgsConstructor
public class Parametro {
    private final String nombre;
    private final Tipo tipo;
    private final int linea;
    private final int columna;

    public Parametro(String nombre, Tipo tipo) {
        this(nombre, tipo, 0, 0);
    }
}
