package org.compi2.UI.graficos;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.compi2.orquestador.ArchivoFuente;

import java.util.Map;

public class GeneradorDot {

    private static final String COLOR_TERMINAL = "#CEEDC7";
    private static final String COLOR_RAIZ = "#B4D8E7";
    private static final String COLOR_PRINCIPAL = "#FCE38A";
    private static final String COLOR_MODULO_Y = "#A8E6CF";
    private static final String COLOR_MODULO_OTRO = "#FFD3B6";

    private GeneradorDot() {}

    public static String generar(ParseTree arbol, String titulo) {
        if (arbol == null) return null;

        StringBuilder sb = new StringBuilder();
        escribirEncabezado(sb, titulo != null ? titulo : "AST");
        construir(sb, arbol, new int[]{0});
        sb.append("}\n");
        return sb.toString();
    }

    public static String generarProyectoCompleto(ParseTree arbolPrincipal, String nombrePrincipal, Map<String, ArchivoFuente> archivosImportados) {

        if (arbolPrincipal == null) return null;

        StringBuilder sb = new StringBuilder();
        escribirEncabezado(sb, "ProyectoCompleto");

        int[] idContador = {0};
        int idRaiz = nodo(sb, idContador, "🌐 PROYECTO COMPLETO", "folder", COLOR_RAIZ, "13");

        String nombre = nombrePrincipal != null ? nombrePrincipal : "Principal";
        int idMain = nodo(sb, idContador, "📄 " + nombre, "component", COLOR_PRINCIPAL, "11");
        arista(sb, idRaiz, idMain);

        int idArbolMain = construir(sb, arbolPrincipal, idContador);
        arista(sb, idMain, idArbolMain);

        if (archivosImportados != null) {
            for (ArchivoFuente archivo : archivosImportados.values()) {
                agregarModuloImportado(sb, idContador, idRaiz, archivo);
            }
        }

        sb.append("}\n");
        return sb.toString();
    }

    private static void agregarModuloImportado(StringBuilder sb, int[] idContador, int idRaiz, ArchivoFuente archivo) {
        if (archivo.getParseTree() == null) return;

        String etiqueta = "📦 " + archivo.getNombreArchivo() + " (" + archivo.getLenguaje() + ")";
        String color = archivo.esY() ? COLOR_MODULO_Y : COLOR_MODULO_OTRO;

        int idMod = nodo(sb, idContador, etiqueta, "component", color, "11");
        arista(sb, idRaiz, idMod);

        int idArbolMod = construir(sb, archivo.getParseTree(), idContador);
        arista(sb, idMod, idArbolMod);
    }

    private static int construir(StringBuilder sb, ParseTree nodo, int[] idContador) {
        int id = idContador[0]++;

        if (nodo instanceof TerminalNode) {
            escribirNodo(sb, id, escapar(nodo.getText()), "box", COLOR_TERMINAL, null);
        } else {
            String nombre = nodo.getClass().getSimpleName().replace("Context", "");
            escribirNodoSimple(sb, id, escapar(nombre));
        }

        for (int i = 0; i < nodo.getChildCount(); i++) {
            int idHijo = construir(sb, nodo.getChild(i), idContador);
            arista(sb, id, idHijo);
        }
        return id;
    }

    private static void escribirEncabezado(StringBuilder sb, String titulo) {
        sb.append("digraph \"").append(escapar(titulo)).append("\" {\n");
        sb.append("    rankdir=TB;\n");
        sb.append("    node [shape=oval, fontname=\"Dialog\", fontsize=11];\n");
        sb.append("    edge [arrowhead=vee];\n\n");
    }

    private static int nodo(StringBuilder sb, int[] idContador, String etiqueta, String shape, String color, String fontSize) {
        int id = idContador[0]++;
        sb.append("    n").append(id)
            .append(" [label=\"").append(escapar(etiqueta)).append("\"")
            .append(", shape=").append(shape)
            .append(", style=filled, fillcolor=\"").append(color).append("\"")
            .append(", fontsize=").append(fontSize)
            .append(", fontname=\"Dialog\"];\n");
        return id;
    }

    private static void escribirNodo(StringBuilder sb, int id, String etiquetaEscapada, String shape, String color, String fontSize) {
        sb.append("    n").append(id)
            .append(" [label=\"").append(etiquetaEscapada).append("\"")
            .append(", shape=").append(shape)
            .append(", style=filled, fillcolor=\"").append(color).append("\"];\n");
    }

    private static void escribirNodoSimple(StringBuilder sb, int id, String etiquetaEscapada) {
        sb.append("    n").append(id).append(" [label=\"").append(etiquetaEscapada).append("\"];\n");
    }

    private static void arista(StringBuilder sb, int idOrigen, int idDestino) {
        sb.append("    n").append(idOrigen).append(" -> n").append(idDestino).append(";\n");
    }


    public static String generarDemoDot(String titulo) {
        StringBuilder sb = new StringBuilder();
        escribirEncabezado(sb, titulo != null ? titulo : "AST_Demo");

        escribirNodoSimple(sb, 0, "Programa");
        escribirNodoSimple(sb, 1, "Declaracion");
        escribirNodo(sb, 2, "int", "box", COLOR_TERMINAL, null);
        escribirNodo(sb, 3, "total", "box", COLOR_TERMINAL, null);
        escribirNodo(sb, 4, "=", "box", COLOR_TERMINAL, null);
        escribirNodoSimple(sb, 5, "Suma");
        escribirNodo(sb, 6, "10", "box", COLOR_TERMINAL, null);
        escribirNodo(sb, 7, "+", "box", COLOR_TERMINAL, null);
        escribirNodo(sb, 8, "20", "box", COLOR_TERMINAL, null);
        escribirNodoSimple(sb, 9, "Print");
        escribirNodo(sb, 10, "'Resultado: '", "box", COLOR_TERMINAL, null);
        escribirNodo(sb, 11, "total", "box", COLOR_TERMINAL, null);
        sb.append("\n");

        arista(sb, 0, 1);
        arista(sb, 1, 2);
        arista(sb, 1, 3);
        arista(sb, 1, 4);
        arista(sb, 1, 5);
        arista(sb, 5, 6);
        arista(sb, 5, 7);
        arista(sb, 5, 8);
        arista(sb, 0, 9);
        arista(sb, 9, 10);
        arista(sb, 9, 11);

        sb.append("}\n");
        return sb.toString();
    }

    public static String escapar(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}