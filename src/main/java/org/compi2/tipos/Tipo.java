package org.compi2.tipos;

import lombok.Getter;

import java.util.List;

@Getter
public class Tipo {
    public static final Tipo ENTERO = new Tipo(TipoBase.ENTERO);
    public static final Tipo DECIMAL = new Tipo(TipoBase.DECIMAL);
    public static final Tipo CARACTER = new Tipo(TipoBase.CARACTER);
    public static final Tipo BOOLEANO = new Tipo(TipoBase.BOOLEANO);
    public static final Tipo CADENA = new Tipo(TipoBase.CADENA);
    public static final Tipo VOID = new Tipo(TipoBase.VOID);
    public static final Tipo ERROR = new Tipo(TipoBase.ERROR);

    private final TipoBase base;
    private final String nombreTipo;
    private final Tipo tipoComponente;
    private final int dimensiones;
    private final List<Tipo> parametros;
    private final Tipo tipoRetorno;

    public Tipo(TipoBase base) {
        this(base, base.name().toLowerCase(), null, 0, null, null);
    }

    public Tipo(TipoBase base, String nombreTipo) {
        this(base, nombreTipo, null, 0, null, null);
    }

    public Tipo(Tipo tipoComponente, int dimensiones) {
        this(TipoBase.ARREGLO, tipoComponente.getNombreTipo() + "[]".repeat(dimensiones), tipoComponente, dimensiones, null, null);
    }

    public Tipo(TipoBase base, String nombreTipo, Tipo tipoComponente, int dimensiones, List<Tipo> parametros, Tipo tipoRetorno) {
        this.base = base;
        this.nombreTipo = nombreTipo;
        this.tipoComponente = tipoComponente;
        this.dimensiones = dimensiones;
        this.parametros = parametros;
        this.tipoRetorno = tipoRetorno;
    }

    @Override
    public String toString() {
        return nombreTipo;
    }

    public boolean esMismoTipo(Tipo otro) {
        if (otro == null) return false;
        if (this.base != otro.base) return false;

        return switch (this.base) {
            case ARREGLO -> this.dimensiones == otro.dimensiones &&
                this.tipoComponente.esMismoTipo(otro.tipoComponente);
            case OBJETO_CLASE, ESTRUCTURA -> this.nombreTipo.equals(otro.nombreTipo);
            default -> true;
        };
    }
}