package org.compi2.analisis_semantico;

import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.*;

public class TablaSimbolos {
    private final Stack<Ambito> pilaAmbitos = new Stack<>();
    private final List<Simbolo> registroHistorico = new ArrayList<>();
    private final Map<String, Tipo> catalogoTiposCompuestos = new HashMap<>();
    private final Map<String, Simbolo> catalogoFuncionesGlobales = new HashMap<>();
    private int contadorId = 1;

    public TablaSimbolos() {
        reiniciar();
    }

    public void reiniciar() {
        pilaAmbitos.clear();
        registroHistorico.clear();
        catalogoTiposCompuestos.clear();
        catalogoFuncionesGlobales.clear();
        contadorId = 1;
        pilaAmbitos.push(new Ambito(null, "Global", true));
    }

    public boolean registrarTipoCompuesto(Tipo tipo) {
        if (catalogoTiposCompuestos.containsKey(tipo.getNombreTipo())) {
            return false;
        }
        catalogoTiposCompuestos.put(tipo.getNombreTipo(), tipo);
        return true;
    }

    public Tipo buscarTipoCompuesto(String nombre) {
        return catalogoTiposCompuestos.get(nombre);
    }

    public boolean existeTipoCompuesto(String nombre) {
        return catalogoTiposCompuestos.containsKey(nombre);
    }

    public Simbolo registrarFuncionGlobal(String nombre, Tipo tipoFuncion, int linea, int columna) {
        if (catalogoFuncionesGlobales.containsKey(nombre)) {
            return null;
        }

        Ambito ambitoGlobal = pilaAmbitos.firstElement();
        if (ambitoGlobal.existeEnAmbitoActual(nombre)) {
            return null;
        }

        Simbolo f = new Simbolo(contadorId++, nombre, tipoFuncion, "funcion", "Global", linea, columna, 0);
        catalogoFuncionesGlobales.put(nombre, f);
        ambitoGlobal.insertar(f);
        registroHistorico.add(f);
        return f;
    }

    public Simbolo buscarFuncionGlobal(String nombre) {
        return catalogoFuncionesGlobales.get(nombre);
    }

    public void abrirAmbito(String nombre) {
        abrirAmbito(nombre, false);
    }

    public void abrirAmbito(String nombre, boolean esNuevoMarcoFuncion) {
        Ambito actual = pilaAmbitos.peek();
        pilaAmbitos.push(new Ambito(actual, nombre, esNuevoMarcoFuncion));
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
        return Collections.unmodifiableList(new ArrayList<>(registroHistorico));
    }

    public String generarReporteTexto() {
        if (registroHistorico.isEmpty()) return "Tabla de símbolos vacía.";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("=== TABLA DE SÍMBOLOS (%d registrados) ===\n", registroHistorico.size()));
        sb.append(String.format("%-4s | %-16s | %-14s | %-14s | %-16s | %-6s | %-6s | %-6s\n",
            "ID", "Identificador", "Tipo", "Categoría", "Ámbito", "Línea", "Col.", "Offset"));
        sb.append("----------------------------------------------------------------------------------------------------\n");
        for (Simbolo s : registroHistorico) {
            sb.append(String.format("%-4d | %-16s | %-14s | %-14s | %-16s | %-6d | %-6d | %-6d\n",
                s.getId(), s.getNombre(), s.getTipo().getNombreTipo(), s.getCategoria(),
                s.getAmbito(), s.getLinea(), s.getColumna(), s.getDesplazamiento()));
        }
        return sb.toString();
    }
}