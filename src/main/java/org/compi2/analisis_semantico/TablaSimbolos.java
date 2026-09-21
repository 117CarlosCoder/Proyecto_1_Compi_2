package org.compi2.analisis_semantico;

import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TablaSimbolos {
    private final Stack<Ambito> pilaAmbitos = new Stack<>();
    private final List<Simbolo> registroHistorico = new ArrayList<>();
    private int contadorId = 1;

    public TablaSimbolos() {
        reiniciar();
    }

    public void reiniciar() {
        pilaAmbitos.clear();
        registroHistorico.clear();
        contadorId = 1;
        pilaAmbitos.push(new Ambito(null, "Global", true));
    }

    public void abrirAmbito(String nombre) {
        abrirAmbito(nombre, false);
    }

    public void abrirAmbito(String nombre, boolean esNuevaFuncion) {
        Ambito actual = pilaAmbitos.peek();
        Ambito nuevo = new Ambito(actual, nombre, esNuevaFuncion);
        pilaAmbitos.push(nuevo);
    }

    public void cerrarAmbito() {
        if (pilaAmbitos.size() > 1) {
            pilaAmbitos.pop();
        }
    }

    public Ambito getAmbitoActual() {
        return pilaAmbitos.peek();
    }

    public Simbolo registrarSimbolo(String nombre, Tipo tipo, String categoria, int linea, int columna) {
        Ambito actual = pilaAmbitos.peek();
        if (actual.existeEnAmbitoActual(nombre)) {
            return null;
        }

        int tamanoTipo = (tipo.getBase() == TipoBase.DECIMAL) ? 8 : 4;
        int offset = actual.asignarDesplazamiento(tamanoTipo);

        Simbolo nuevo = new Simbolo(
            contadorId++,
            nombre,
            tipo,
            categoria,
            actual.getNombreAmbito(),
            linea,
            columna,
            offset
        );

        actual.insertar(nuevo);
        registroHistorico.add(nuevo);
        return nuevo;
    }

    public Simbolo buscarSimbolo(String nombre) {
        if (pilaAmbitos.isEmpty()) return null;
        return pilaAmbitos.peek().obtener(nombre);
    }

    public List<Simbolo> obtenerTodosLosSimbolos() {
        return new ArrayList<>(registroHistorico);
    }

    public String generarReporteTexto() {
        if (registroHistorico.isEmpty()) {
            return "Tabla de símbolos vacía.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== TABLA DE SÍMBOLOS (%d registrados) ===\n", registroHistorico.size()));
        sb.append(String.format("%-4s | %-16s | %-12s | %-16s | %-16s | %-6s | %-6s | %-6s\n",
            "ID", "Identificador", "Tipo", "Categoría", "Ámbito", "Línea", "Col.", "Offset"));
        sb.append("--------------------------------------------------------------------------------------------\n");
        for (Simbolo s : registroHistorico) {
            sb.append(String.format("%-4d | %-16s | %-12s | %-16s | %-16s | %-6d | %-6d | %-6d\n",
                s.getId(),
                s.getNombre(),
                s.getTipo().getNombreTipo(),
                s.getCategoria(),
                s.getAmbito(),
                s.getLinea(),
                s.getColumna(),
                s.getDesplazamiento()));
        }
        return sb.toString();
    }
}