package org.compi2.analisis_semantico;

import lombok.Getter;
import org.compi2.tipos.Tipo;

@Getter
public class Simbolo {
    private final int id;
    private final String nombre;
    private final Tipo tipo;
    private final String categoria;
    private final String ambito;
    private final int linea;
    private final int columna;
    private final int desplazamiento;
    private final String lenguaje;
    private final String archivoOrigen;

    public Simbolo(int id, String nombre, Tipo tipo, String categoria, String ambito, int linea, int columna, int desplazamiento) {
        this(id, nombre, tipo, categoria, ambito, linea, columna, desplazamiento, "—", "—");
    }

    public Simbolo(int id, String nombre, Tipo tipo, String categoria, String ambito, int linea, int columna, int desplazamiento, String lenguaje, String archivoOrigen) {
        this.id = id;
        this.nombre = nombre;
        this.tipo = tipo;
        this.categoria = categoria;
        this.ambito = ambito;
        this.linea = linea;
        this.columna = columna;
        this.desplazamiento = desplazamiento;
        this.lenguaje = (lenguaje != null && !lenguaje.isEmpty()) ? lenguaje : "—";
        this.archivoOrigen = (archivoOrigen != null && !archivoOrigen.isEmpty()) ? archivoOrigen : "—";
    }

    @Override
    public String toString() {
        return String.format("[%d] %s (%s, %s) en %s [%s] - L:%d C:%d | Offset:%d",
            id, nombre, tipo != null ? tipo.getNombreTipo() : "void", categoria, ambito, lenguaje, linea, columna, desplazamiento);
    }
}