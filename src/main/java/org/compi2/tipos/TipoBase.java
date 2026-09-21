package org.compi2.tipos;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TipoBase {
    BOOLEANO(1),
    CARACTER(2),
    ENTERO(3),
    DECIMAL(4),
    CADENA(5),
    VOID(-1),
    ERROR(-2),
    ARREGLO(0),
    OBJETO_CLASE(0),
    ESTRUCTURA(0),
    FUNCION(0);

    private final int jerarquia;

    public boolean esNumerico() {
        return this == ENTERO || this == DECIMAL || this == CARACTER;
    }
}