package org.compi2.tipos;

import lombok.Getter;
import org.compi2.analisis_semantico.ComprobadorTipos;

import java.util.*;

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

    private final Map<String, Tipo> campos = new HashMap<>();
    private final Map<String, Tipo> metodos = new HashMap<>();
    private final List<List<Tipo>> constructores = new ArrayList<>();

    public Tipo(TipoBase base) {
        this(base, base.name().toLowerCase(), null, 0, null, null);
    }

    public Tipo(TipoBase base, String nombreTipo) {
        this(base, nombreTipo, null, 0, null, null);
    }

    public Tipo(Tipo tipoComponente, int dimensiones) {
        this(TipoBase.ARREGLO, tipoComponente.getNombreTipo() + "[]".repeat(dimensiones), tipoComponente, dimensiones, null, null);
    }

    public Tipo(String nombreTipo, List<Tipo> parametros, Tipo tipoRetorno) {
        this(TipoBase.FUNCION, nombreTipo, null, 0, parametros != null ? parametros : Collections.emptyList(), tipoRetorno);
    }

    public Tipo(TipoBase base, String nombreTipo, Tipo tipoComponente, int dimensiones, List<Tipo> parametros, Tipo tipoRetorno) {
        this.base = base;
        this.nombreTipo = nombreTipo;
        this.tipoComponente = tipoComponente;
        this.dimensiones = dimensiones;
        this.parametros = parametros;
        this.tipoRetorno = tipoRetorno;
    }

    public void agregarCampo(String nombre, Tipo tipo) {
        campos.put(nombre, tipo);
    }

    public Tipo buscarCampo(String nombre) {
        return campos.get(nombre);
    }

    public boolean existeCampo(String nombre) {
        return campos.containsKey(nombre);
    }

    public void agregarMetodo(String nombre, Tipo tipoFuncion) {
        metodos.put(nombre, tipoFuncion);
    }

    public Tipo buscarMetodo(String nombre) {
        return metodos.get(nombre);
    }

    public boolean existeMetodo(String nombre) {
        return metodos.containsKey(nombre);
    }

    public void agregarConstructor(List<Tipo> params) {
        constructores.add(params != null ? new ArrayList<>(params) : Collections.emptyList());
    }

    public boolean existeConstructor(List<Tipo> args) {
        List<Tipo> listaArgs = (args != null) ? args : Collections.emptyList();

        if (constructores.isEmpty() && listaArgs.isEmpty()) {
            return true;
        }

        for (List<Tipo> firma : constructores) {
            if (firma.size() == listaArgs.size()) {
                boolean coincide = true;
                for (int i = 0; i < firma.size(); i++) {
                    Tipo esperado = firma.get(i);
                    Tipo recibido = listaArgs.get(i);

                    if (!esperado.esMismoTipo(recibido) && !ComprobadorTipos.esCompatibleAsignacion(esperado, recibido)) {
                        coincide = false;
                        break;
                    }
                }
                if (coincide) return true;
            }
        }
        return false;
    }

    public boolean esMismoTipo(Tipo otro) {
        if (otro == null) return false;
        if (this.base != otro.base) return false;

        return switch (this.base) {
            case ARREGLO -> this.dimensiones == otro.dimensiones &&
                this.tipoComponente != null &&
                this.tipoComponente.esMismoTipo(otro.tipoComponente);
            case OBJETO_CLASE, ESTRUCTURA -> this.nombreTipo.equals(otro.nombreTipo);
            case FUNCION -> {
                if (this.tipoRetorno != null && !this.tipoRetorno.esMismoTipo(otro.tipoRetorno)) yield false;
                if (this.parametros == null || otro.parametros == null) yield this.parametros == otro.parametros;
                if (this.parametros.size() != otro.parametros.size()) yield false;
                for (int i = 0; i < this.parametros.size(); i++) {
                    if (!this.parametros.get(i).esMismoTipo(otro.parametros.get(i))) yield false;
                }
                yield true;
            }
            default -> true;
        };
    }

    @Override
    public String toString() {
        return nombreTipo;
    }
}