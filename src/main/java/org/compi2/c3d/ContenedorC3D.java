package org.compi2.c3d;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class ContenedorC3D {
    private final List<Cuarteta> cuartetas = new ArrayList<>();
    private final GestorTemporales gestorTemporales;
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

    public void emitirCall(String nombreFuncion) {
        emitir(new Cuarteta(TipoOperadorC3D.CALL, null, null, nombreFuncion));
    }

    public void emitirReturn() {
        emitir(new Cuarteta(TipoOperadorC3D.RETURN, null, null, null));
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

    public void emitirPrintStr(String punteroHeap) {
        emitir(new Cuarteta(TipoOperadorC3D.PRINT_STR, punteroHeap, null, null));
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
    }

    public String generarCodigoC(String indentacion) {
        return generarCodigoC(indentacion, false);
    }

    public String generarCodigoC(String indentacion, boolean conComentarios) {
        StringBuilder sb = new StringBuilder();
        for (Cuarteta c : cuartetas) {
            String linea = c.toCodigoC();
            if (linea.isBlank()) {
                continue;
            }
            if (c.getOperador() == TipoOperadorC3D.ETIQUETA) {
                sb.append(linea).append("\n");
            } else {
                sb.append(indentacion).append(linea).append("\n");
            }
        }
        return sb.toString();
    }
}