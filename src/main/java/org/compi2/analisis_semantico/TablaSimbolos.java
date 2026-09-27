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
    private String lenguajeActual = "Pig Latin";
    private String archivoActual = "principal.pig";

    public TablaSimbolos() {
        reiniciar();
    }

    public void reiniciar() {
        pilaAmbitos.clear();
        registroHistorico.clear();
        catalogoTiposCompuestos.clear();
        catalogoFuncionesGlobales.clear();
        contadorId = 1;
        lenguajeActual = "Pig Latin";
        archivoActual = "principal.pig";
        pilaAmbitos.push(new Ambito(null, "Global", true));
    }

    public void setContextoOrigen(String lenguaje, String archivo) {
        this.lenguajeActual = (lenguaje != null && !lenguaje.isEmpty()) ? lenguaje : "Pig Latin";
        this.archivoActual = (archivo != null && !archivo.isEmpty()) ? archivo : "—";
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

    public Simbolo registrarEstructura(String nombre, Tipo tipoEstructura, int linea, int columna) {
        Simbolo s = new Simbolo(contadorId++, nombre, tipoEstructura, "estructura", "Global", linea, columna, 0, lenguajeActual, archivoActual);
        registroHistorico.add(s);
        return s;
    }

    public Simbolo registrarCampoEstructura(String nombreEstructura, String nombreCampo, Tipo tipoCampo, int linea, int columna, int offsetEnStruct) {
        Simbolo s = new Simbolo(contadorId++, nombreCampo, tipoCampo, "campo_estructura", "Estructura_" + nombreEstructura, linea, columna, offsetEnStruct, lenguajeActual, archivoActual);
        registroHistorico.add(s);
        return s;
    }

    public Simbolo registrarClase(String nombre, Tipo tipoClase, int linea, int columna) {
        Simbolo s = new Simbolo(contadorId++, nombre, tipoClase, "clase", "Global", linea, columna, 0, lenguajeActual, archivoActual);
        registroHistorico.add(s);
        return s;
    }

    public Simbolo registrarCampoClase(String nombreClase, String nombreCampo, Tipo tipoCampo, int linea, int columna, int offsetEnClase) {
        Simbolo s = new Simbolo(contadorId++, nombreCampo, tipoCampo, "campo_clase", "Clase_" + nombreClase, linea, columna, offsetEnClase, lenguajeActual, archivoActual);
        registroHistorico.add(s);
        return s;
    }

    public Simbolo registrarMetodoClase(String nombreClase, String nombreMetodo, Tipo tipoMetodo, int linea, int columna) {
        Simbolo s = new Simbolo(contadorId++, nombreMetodo, tipoMetodo, "metodo", "Clase_" + nombreClase, linea, columna, 0, lenguajeActual, archivoActual);
        registroHistorico.add(s);
        return s;
    }

    public Simbolo registrarConstructorClase(String nombreClase, Tipo tipoConstructor, int linea, int columna) {
        Simbolo s = new Simbolo(contadorId++, nombreClase, tipoConstructor, "constructor", "Clase_" + nombreClase, linea, columna, 0, lenguajeActual, archivoActual);
        registroHistorico.add(s);
        return s;
    }

    public boolean tieneConstructor(String nombreClase) {
        if (nombreClase == null) return false;
        for (Simbolo s : registroHistorico) {
            if ("constructor".equals(s.getCategoria()) && ("Clase_" + nombreClase).equals(s.getAmbito())) {
                return true;
            }
        }
        return false;
    }

    public Simbolo registrarFuncionGlobal(String nombre, Tipo tipoFuncion, int linea, int columna) {
        if (catalogoFuncionesGlobales.containsKey(nombre)) {
            return null;
        }

        Ambito ambitoGlobal = pilaAmbitos.firstElement();
        if (ambitoGlobal.existeEnAmbitoActual(nombre)) {
            return null;
        }

        Simbolo f = new Simbolo(contadorId++, nombre, tipoFuncion, "funcion", "Global", linea, columna, 0, lenguajeActual, archivoActual);
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

    public void abrirAmbitoBucle(String nombre) {
        Ambito actual = pilaAmbitos.peek();
        pilaAmbitos.push(new Ambito(actual, nombre, false, true, false, null));
    }

    public void abrirAmbitoSwitch(String nombre) {
        Ambito actual = pilaAmbitos.peek();
        pilaAmbitos.push(new Ambito(actual, nombre, false, false, true, null));
    }

    public void abrirAmbitoFuncion(String nombre, Tipo tipoRetorno) {
        Ambito actual = pilaAmbitos.peek();
        pilaAmbitos.push(new Ambito(actual, nombre, true, false, false, tipoRetorno));
    }

    public boolean estaEnBucle() {
        return !pilaAmbitos.isEmpty() && pilaAmbitos.peek().estaEnBucle();
    }

    public boolean estaEnSwitch() {
        return !pilaAmbitos.isEmpty() && pilaAmbitos.peek().estaEnSwitch();
    }

    public Tipo getTipoRetornoEsperado() {
        return !pilaAmbitos.isEmpty() ? pilaAmbitos.peek().obtenerTipoRetornoEsperado() : null;
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
            offset,
            lenguajeActual,
            archivoActual
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