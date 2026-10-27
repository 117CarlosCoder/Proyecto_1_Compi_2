package org.compi2.c3d;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContenedorC3D {
    private final List<Cuarteta> cuartetas = new ArrayList<>();
    @Getter
    private final GestorTemporales gestorTemporales;
    @Getter
    private final GestorEtiquetas gestorEtiquetas;

    public ContenedorC3D() {
        this.gestorTemporales = new GestorTemporales();
        this.gestorEtiquetas = new GestorEtiquetas();
    }

    public ContenedorC3D(GestorTemporales gestorTemporales, GestorEtiquetas gestorEtiquetas) {
        this.gestorTemporales = (gestorTemporales != null) ? gestorTemporales : new GestorTemporales();
        this.gestorEtiquetas = (gestorEtiquetas != null) ? gestorEtiquetas : new GestorEtiquetas();
    }

    public void emitir(Cuarteta c) {
        if (c != null) {
            cuartetas.add(c);
        }
    }

    public String nuevoTemporal() {
        return gestorTemporales.nuevoTemporal();
    }

    public String nuevaEtiqueta() {
        return gestorEtiquetas.nuevaEtiqueta();
    }

    public void emitirAsignacion(String resultado, String arg1) {
        emitir(new Cuarteta(TipoOperadorC3D.ASIGNACION, arg1, null, resultado));
    }

    public void emitirOperacion(TipoOperadorC3D operador, String resultado, String arg1, String arg2) {
        emitir(new Cuarteta(operador, arg1, arg2, resultado));
    }

    public void emitirEtiqueta(String etiqueta) {
        emitir(new Cuarteta(TipoOperadorC3D.ETIQUETA, null, null, etiqueta));
    }

    public void emitirGoto(String etiqueta) {
        emitir(new Cuarteta(TipoOperadorC3D.GOTO, null, null, etiqueta));
    }

    public void emitirIf(TipoOperadorC3D operadorRelacional, String arg1, String arg2, String etiqueta) {
        emitir(new Cuarteta(operadorRelacional, arg1, arg2, etiqueta));
    }

    public void emitirAccesoStack(String temporalResultado, String posicion) {
        emitir(new Cuarteta(TipoOperadorC3D.ACCESO_STACK, posicion, null, temporalResultado));
    }

    public void emitirAsignacionStack(String posicion, String valor) {
        emitir(new Cuarteta(TipoOperadorC3D.ASIGNACION_STACK, valor, null, posicion));
    }

    public void emitirAccesoHeap(String temporalResultado, String posicion) {
        emitir(new Cuarteta(TipoOperadorC3D.ACCESO_HEAP, posicion, null, temporalResultado));
    }

    public void emitirAsignacionHeap(String posicion, String valor) {
        emitir(new Cuarteta(TipoOperadorC3D.ASIGNACION_HEAP, valor, null, posicion));
    }

    public void emitirPrintInt(String valor) {
        emitir(new Cuarteta(TipoOperadorC3D.PRINT_INT, valor, null, null));
    }

    public void emitirPrintDouble(String valor) {
        emitir(new Cuarteta(TipoOperadorC3D.PRINT_DOUBLE, valor, null, null));
    }

    public void emitirPrintChar(String valor) {
        emitir(new Cuarteta(TipoOperadorC3D.PRINT_CHAR, valor, null, null));
    }

    public void emitirAccesoBuffer(String temporalResultado, String posicion) {
        emitir(new Cuarteta(TipoOperadorC3D.ACCESO_BUFFER, posicion, null, temporalResultado));
    }

    public void emitirAsignacionBuffer(String posicion, String valor) {
        emitir(new Cuarteta(TipoOperadorC3D.ASIGNACION_BUFFER, valor, null, posicion));
    }

    public enum TipoRutinaRiscV {
        READ_DECIMAL,
        DECIMAL_TO_HEAP
    }

    public record RangoRutinaRiscV(
            TipoRutinaRiscV tipo,
            int inicioInclusive,
            int finExclusive,
            String arg,
            String resultado) {
    }

    public enum AccionCuartetaRiscV {
        ESCALAR_ARG1_X10000,
        ESCALAR_ARG2_X10000,
        MUL_DECIMAL,
        DIV_DECIMAL
    }

    private final java.util.Map<Integer, RangoRutinaRiscV> rutinasRiscVPorInicio = new java.util.HashMap<>();
    private final java.util.Map<Integer, java.util.EnumSet<AccionCuartetaRiscV>> accionesRiscVPorCuarteta = new java.util.HashMap<>();

    public int getCantidadCuartetas() {
        return cuartetas.size();
    }

    public void registrarRutinaRiscV(RangoRutinaRiscV rango) {
        if (rango != null) {
            rutinasRiscVPorInicio.put(rango.inicioInclusive(), rango);
        }
    }

    public RangoRutinaRiscV obtenerRutinaRiscVEnInicio(int indiceCuarteta) {
        return rutinasRiscVPorInicio.get(indiceCuarteta);
    }

    public void marcarAccionRiscV(int indiceCuarteta, AccionCuartetaRiscV accion) {
        if (indiceCuarteta >= 0 && indiceCuarteta < cuartetas.size() && accion != null) {
            accionesRiscVPorCuarteta.computeIfAbsent(indiceCuarteta, k -> java.util.EnumSet.noneOf(AccionCuartetaRiscV.class)).add(accion);
        }
    }

    public void marcarAccionUltimaCuartetaRiscV(AccionCuartetaRiscV accion) {
        if (!cuartetas.isEmpty()) {
            marcarAccionRiscV(cuartetas.size() - 1, accion);
        }
    }

    public java.util.Set<AccionCuartetaRiscV> obtenerAccionesRiscV(int indiceCuarteta) {
        java.util.Set<AccionCuartetaRiscV> res = accionesRiscVPorCuarteta.get(indiceCuarteta);
        return (res != null) ? res : Collections.emptySet();
    }

    public List<Cuarteta> getCuartetas() {
        return Collections.unmodifiableList(cuartetas);
    }

    public int size() {
        return cuartetas.size();
    }

    public void limpiar() {
        cuartetas.clear();
        gestorTemporales.reiniciar();
        gestorEtiquetas.reiniciar();
        rutinasRiscVPorInicio.clear();
        accionesRiscVPorCuarteta.clear();
    }

    public String generarCodigoC(String indentacion) {
        StringBuilder codigo = new StringBuilder();
        for (Cuarteta cuarteta : cuartetas) {
            String linea = cuarteta.toCodigoC();
            if (linea.isBlank()) {
                continue;
            }
            if (cuarteta.getOperador() != TipoOperadorC3D.ETIQUETA) {
                codigo.append(indentacion);
            }
            codigo.append(linea).append("\n");
        }
        return codigo.toString();
    }

}
