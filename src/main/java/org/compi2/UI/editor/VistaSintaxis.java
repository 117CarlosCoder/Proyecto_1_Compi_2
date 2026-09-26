package org.compi2.UI.editor;

import org.compi2.analisis_semantico.Simbolo;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.PlainView;
import javax.swing.text.Segment;
import javax.swing.text.Utilities;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VistaSintaxis extends PlainView {

    public static final Color COLOR_KEYWORD = new Color(255, 157, 0);
    public static final Color COLOR_COMMENT = new Color(0, 136, 255);
    public static final Color COLOR_TYPE = new Color(42, 255, 223);
    public static final Color COLOR_STRING = new Color(58, 217, 0);
    public static final Color COLOR_NUMBER = new Color(255, 98, 140);
    public static final Color COLOR_SECTION = new Color(251, 148, 255);
    public static final Color COLOR_FUNCTION = new Color(255, 198, 0);
    public static final Color COLOR_OPERATOR = new Color(255, 157, 0);
    public static final Color COLOR_ARRAY = new Color(0, 204, 255);
    public static final Color COLOR_VARIABLE = new Color(158, 255, 255);
    public static final Color COLOR_DEFAULT = new Color(255, 255, 255);

    private static final Map<String, Color> PALABRAS_BASE = new HashMap<>();

    static {
        agregar(COLOR_SECTION, "variabiles>", "variables>", "maior>", "% estructuras", "% funciones");

        agregar(COLOR_KEYWORD,
                "import", "esto", "series", "novus", "si", "aliter", "dum", "facere", "per",
                "finis", "finis;", "interrumpe", "interrumpir", "perge", "verum", "falsus",
                "entonces", "sino", "contrario", "elegir", "caso", "siempre", "mientras",
                "hacer", "para", "romper", "continuar", "retornar", "verdadero", "falso",
                "class", "public", "void", "new", "null", "if", "else", "switch", "case",
                "default", "for", "while", "do", "break", "continue", "return", "true", "false");

        agregar(COLOR_TYPE,
                "numerus", "decimalis", "textum", "littera", "booleanus",
                "entero", "flotante", "caracter", "bool", "cadena",
                "int", "double", "char", "boolean", "string");

        agregar(COLOR_FUNCTION, "print", "println", "readln", "imprimir", "leer");

        agregar(COLOR_OPERATOR, ">>", "<<", "->");
    }

    private static void agregar(Color color, String... palabras) {
        for (String p : palabras) {
            PALABRAS_BASE.put(p.toLowerCase(), color);
        }
    }

    private static volatile Map<String, Color> mapaSimbolos = new HashMap<>();
    private static volatile List<int[]> comentariosBloque = new ArrayList<>();
    private static volatile boolean coloreadoActivo = true;

    private static final Pattern PATRON_LEXICO = Pattern.compile(
            "//.*|##.*|#.*|/\\*.*?\\*/"
                    + "|\"[^\"]*\"|'[^']*'"
                    + "|(?:VARIABILES|VARIABLES|MAIOR)>"
                    + "|%[ \\t]*(?:estructuras|funciones)"
                    + "|>>|<<|->"
                    + "|\\+\\+|--|\\+=|-=|\\*=|/=|%="
                    + "|==|!=|<=|>=|&&|\\|\\|"
                    + "|[a-zA-Z_][a-zA-Z0-9_]*"
                    + "|\\d+(?:\\.\\d+)?"
                    + "|[{}()\\[\\];,.%:?<>]"
                    + "|.");

    public VistaSintaxis(Element elemento) {
        super(elemento);
    }

    public static void actualizarSimbolosSemanticos(List<Simbolo> simbolos, String codigo) {
        mapaSimbolos = construirMapaSimbolos(simbolos);
        comentariosBloque = detectarComentariosBloque(codigo);
        coloreadoActivo = true;
    }

    public static void limpiarColoreado() {
        coloreadoActivo = false;
        mapaSimbolos = new HashMap<>();
        comentariosBloque = new ArrayList<>();
    }

    private static Map<String, Color> construirMapaSimbolos(List<Simbolo> simbolos) {
        Map<String, Color> mapa = new HashMap<>(PALABRAS_BASE);
        if (simbolos == null)
            return mapa;

        for (Simbolo s : simbolos) {
            if (s.getNombre() == null)
                continue;
            mapa.put(s.getNombre().toLowerCase(), colorSegunCategoria(s.getCategoria()));
        }
        return mapa;
    }

    private static Color colorSegunCategoria(String categoria) {
        String cat = categoria != null ? categoria.toLowerCase() : "";
        if (cat.contains("func") || cat.contains("metodo"))
            return COLOR_FUNCTION;
        if (cat.contains("clase") || cat.contains("estructura") || cat.contains("tipo"))
            return COLOR_TYPE;
        if (cat.contains("arreglo") || cat.contains("series"))
            return COLOR_ARRAY;
        return COLOR_VARIABLE;
    }

    private static List<int[]> detectarComentariosBloque(String codigo) {
        List<int[]> bloques = new ArrayList<>();
        if (codigo == null)
            return bloques;

        int inicio = codigo.indexOf("/*");
        while (inicio != -1) {
            int fin = codigo.indexOf("*/", inicio + 2);
            int finBloque = (fin != -1) ? fin + 2 : codigo.length();
            bloques.add(new int[] { inicio, finBloque });
            if (fin == -1)
                break;
            inicio = codigo.indexOf("/*", finBloque);
        }
        return bloques;
    }


    @Override
    protected float drawUnselectedText(Graphics2D g, float x, float y, int p0, int p1) throws BadLocationException {
        return dibujarTexto(g, x, y, p0, p1, false);
    }

    @Override
    protected float drawSelectedText(Graphics2D g, float x, float y, int p0, int p1) throws BadLocationException {
        return dibujarTexto(g, x, y, p0, p1, true);
    }

    private float dibujarTexto(Graphics2D g, float x, float y, int p0, int p1, boolean seleccionado)
            throws BadLocationException {

        String linea = getDocument().getText(p0, p1 - p0);
        if (linea.isEmpty())
            return x;

        g.setFont(getContainer() != null ? getContainer().getFont() : new Font("Consolas", Font.PLAIN, 14));

        if (seleccionado || !coloreadoActivo) {
            g.setColor(seleccionado ? Color.WHITE : COLOR_DEFAULT);
            return dibujarSegmento(g, linea, x, y, p0);
        }

        return dibujarLineaColoreada(g, linea, x, y, p0);
    }

    private float dibujarLineaColoreada(Graphics2D g, String linea, float x, float y, int p0)
            throws BadLocationException {

        Matcher matcher = PATRON_LEXICO.matcher(linea);
        float xActual = x;

        while (matcher.find()) {
            String token = matcher.group();
            int offsetGlobal = p0 + matcher.start();
            g.setColor(determinarColor(token, offsetGlobal));
            xActual = dibujarSegmento(g, token, xActual, y, offsetGlobal);
        }
        return xActual;
    }

    private float dibujarSegmento(Graphics2D g, String texto, float x, float y, int offset) throws BadLocationException {
        return Utilities.drawTabbedText(new Segment(texto.toCharArray(), 0, texto.length()), x, y, g, this, offset);
    }

    private static boolean enComentarioBloque(int offset) {
        for (int[] rango : comentariosBloque) {
            if (offset >= rango[0] && offset < rango[1])
                return true;
        }
        return false;
    }

    private static Color determinarColor(String token, int offset) {
        if (esComentario(token, offset))
            return COLOR_COMMENT;
        if (esSeccion(token))
            return COLOR_SECTION;
        if (token.startsWith("\"") || token.startsWith("'"))
            return COLOR_STRING;
        if (Character.isDigit(token.charAt(0)))
            return COLOR_NUMBER;

        Color color = mapaSimbolos.get(token.toLowerCase());
        if (color != null)
            return color;

        color = PALABRAS_BASE.get(token.toLowerCase());
        if (color != null)
            return color;

        if (Character.isLetter(token.charAt(0)) || token.startsWith("_"))
            return COLOR_VARIABLE;

        return COLOR_DEFAULT;
    }

    private static boolean esComentario(String token, int offset) {
        return enComentarioBloque(offset)
                || token.startsWith("//") || token.startsWith("/*") || token.startsWith("#");
    }

    private static boolean esSeccion(String token) {
        return token.startsWith("%") || token.startsWith("##") || token.toUpperCase().endsWith(">");
    }
}