package org.compi2.analisis_semantico;

import lombok.Getter;
import org.compi2.tipos.Tipo;
import java.util.HashMap;
import java.util.Map;

@Getter
public class Ambito {
    private final Ambito padre;
    private final String nombreAmbito;
    private final int nivel;
    private final Map<String, Simbolo> tablaSimbolos;
    private int desplazamientoActual;
    private final boolean esBucle;
    private final boolean esSwitch;
    private final Tipo tipoRetornoEsperado;

    public Ambito(Ambito padre, String nombreAmbito, boolean reiniciarDesplazamiento) {
        this(padre, nombreAmbito, reiniciarDesplazamiento, false, false, null);
    }

    public Ambito(Ambito padre, String nombreAmbito, boolean reiniciarDesplazamiento, boolean esBucle, boolean esSwitch, Tipo tipoRetornoEsperado) {
        this.padre = padre;
        this.nombreAmbito = nombreAmbito;
        this.nivel = (padre == null) ? 0 : padre.getNivel() + 1;
        this.tablaSimbolos = new HashMap<>();
        this.desplazamientoActual = (reiniciarDesplazamiento || padre == null) ? 0 : padre.desplazamientoActual;
        this.esBucle = esBucle;
        this.esSwitch = esSwitch;
        this.tipoRetornoEsperado = (tipoRetornoEsperado != null) ? tipoRetornoEsperado : (padre != null ? padre.tipoRetornoEsperado : null);
    }

    public boolean estaEnBucle() {
        for (Ambito actual = this; actual != null; actual = actual.padre) {
            if (actual.esBucle) return true;
        }
        return false;
    }

    public boolean estaEnSwitch() {
        for (Ambito actual = this; actual != null; actual = actual.padre) {
            if (actual.esSwitch) return true;
        }
        return false;
    }

    public Tipo obtenerTipoRetornoEsperado() {
        return this.tipoRetornoEsperado;
    }

    public int asignarDesplazamiento(int tamanoBytes) {
        int posicionAsignada = this.desplazamientoActual;
        this.desplazamientoActual += tamanoBytes;
        if (padre != null && this.nivel > padre.nivel) {
            padre.desplazamientoActual = this.desplazamientoActual;
        }
        return posicionAsignada;
    }

    public boolean existeEnAmbitoActual(String nombre) {
        return tablaSimbolos.containsKey(nombre);
    }

    public boolean insertar(Simbolo s) {
        if (existeEnAmbitoActual(s.getNombre())) {
            return false;
        }
        tablaSimbolos.put(s.getNombre(), s);
        return true;
    }

    public Simbolo obtenerActual(String nombre) {
        return tablaSimbolos.get(nombre);
    }

    public Simbolo obtener(String nombre) {
        for (Ambito actual = this; actual != null; actual = actual.padre) {
            Simbolo s = actual.tablaSimbolos.get(nombre);
            if (s != null) return s;
        }
        return null;
    }
}