package org.compi2;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import gramaticas.piglatin.piglatinLexer;
import gramaticas.piglatin.piglatinParser;
import gramaticas.zetariano.zetarianoLexer;
import gramaticas.zetariano.zetarianoParser;
import gramaticas.y.yLexer;
import gramaticas.y.yParser;

import org.compi2.analisis_semantico.ComprobadorTipos;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.analisis_semantico.YSangriaToken;
import org.compi2.tipos.Tipo;

import org.compi2.ast.NodoAST;
import org.compi2.constructores_ast.ConstructorAstPigLatin;
import org.compi2.constructores_ast.ConstructorAstZetariano;
import org.compi2.constructores_ast.ConstructorAstY;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        probarMotorSemanticoAislado();
        probarAstExpresionesPigLatin();
        probarAstExpresionesZetariano();
        probarAstExpresionesY();
        probarSintaxisProgramasCompletos();
    }

    public static void probarMotorSemanticoAislado() {
        System.out.println("=================================================");
        System.out.println(">>> 1. PRUEBA AISLADA DEL MOTOR SEMÁNTICO");
        System.out.println("=================================================");

        TablaSimbolos ts = new TablaSimbolos();

        ts.registrarSimbolo("global_cont", Tipo.ENTERO, "variable", 1, 1);
        ts.registrarSimbolo("global_pi", Tipo.DECIMAL, "variable", 2, 1);
        ts.registrarSimbolo("global_titulo", Tipo.CADENA, "variable", 3, 1);

        ts.abrirAmbito("calcular()", true);
        ts.registrarSimbolo("param_x", Tipo.ENTERO, "parametro", 5, 5);
        ts.registrarSimbolo("temp_res", Tipo.DECIMAL, "variable", 6, 5);

        ts.abrirAmbito("Bloque_Si", false);
        ts.registrarSimbolo("local_flag", Tipo.BOOLEANO, "variable", 8, 9);

        System.out.println("[Búsqueda] 'local_flag' en ámbito local: " + (ts.buscarSimbolo("local_flag") != null ? "ENCONTRADO" : "FALLÓ"));
        System.out.println("[Búsqueda] 'global_pi' desde ámbito anidado: " + (ts.buscarSimbolo("global_pi") != null ? "ENCONTRADO" : "FALLÓ"));

        ts.cerrarAmbito();
        System.out.println("[Búsqueda post-cierre] 'local_flag' fuera de bloque: " + (ts.buscarSimbolo("local_flag") == null ? "NO VISIBLE (CORRECTO)" : "ERROR DE ALCANCE"));
        ts.cerrarAmbito();

        Tipo sumaNumDec = ComprobadorTipos.resolverSuma(Tipo.ENTERO, Tipo.DECIMAL);
        Tipo sumaStrNum = ComprobadorTipos.resolverSuma(Tipo.CADENA, Tipo.ENTERO);
        Tipo multInvalida = ComprobadorTipos.resolverAritmetica(Tipo.CADENA, Tipo.ENTERO);

        System.out.println("\n[ComprobadorTipos] ENTERO + DECIMAL -> " + sumaNumDec);
        System.out.println("[ComprobadorTipos] CADENA + ENTERO  -> " + sumaStrNum);
        System.out.println("[ComprobadorTipos] CADENA * ENTERO  -> " + multInvalida);

        System.out.println("\n" + ts.generarReporteTexto());
    }

    public static void probarAstExpresionesPigLatin() {
        System.out.println("\n=================================================");
        System.out.println(">>> 2. PRUEBA AST EXPRESIONES EN PIG LATIN");
        System.out.println("=================================================");

        TablaSimbolos ts = new TablaSimbolos();
        ts.registrarSimbolo("edad", Tipo.ENTERO, "variable", 1, 1);
        ts.registrarSimbolo("factor", Tipo.DECIMAL, "variable", 2, 1);

        String exprValida = "edad * 2 + factor >= 30.5";
        ejecutarExpresionPig(exprValida, ts, "booleano");

        String exprInvalida = "\"Texto\" * 5";
        ejecutarExpresionPig(exprInvalida, ts, "error");
    }

    public static void probarAstExpresionesZetariano() {
        System.out.println("\n=================================================");
        System.out.println(">>> 3. PRUEBA AST EXPRESIONES EN ZETARIANO");
        System.out.println("=================================================");

        TablaSimbolos ts = new TablaSimbolos();
        ts.registrarSimbolo("nivel", Tipo.ENTERO, "variable", 1, 1);
        ts.registrarSimbolo("vida", Tipo.DECIMAL, "variable", 2, 1);

        String exprValida = "nivel >= 50 && vida > 0.0";
        ejecutarExpresionZetariano(exprValida, ts, "booleano");

        String exprInvalida = "nivel + mana";
        ejecutarExpresionZetariano(exprInvalida, ts, "error (identificador no declarado)");
    }

    public static void probarAstExpresionesY() {
        System.out.println("\n=================================================");
        System.out.println(">>> 4. PRUEBA AST EXPRESIONES EN Y?");
        System.out.println("=================================================");

        TablaSimbolos ts = new TablaSimbolos();
        ts.registrarSimbolo("total", Tipo.ENTERO, "variable", 1, 1);

        String exprValida1 = "total / 2 == 50 || verdadero";
        ejecutarExpresionY(exprValida1, ts, "booleano");

        String exprValida2 = "\"Resultado: \" + total";
        ejecutarExpresionY(exprValida2, ts, "cadena");
    }

    public static void probarSintaxisProgramasCompletos() {
        System.out.println("\n=================================================");
        System.out.println(">>> 5. VALIDACIÓN SINTÁCTICA DE PROGRAMAS COMPLETOS");
        System.out.println("=================================================");

        String codigoPigLatin = """
            import carpeta.Guerrero.z;
            import carpeta.Funciones.y;

            VARIABILES>
            esto edad : numerus 20;

            MAIOR>
            >> "Hola comandante!";
            FINIS;
            """;
        piglatinLexer lexerPig = new piglatinLexer(CharStreams.fromString(codigoPigLatin));
        piglatinParser parserPig = new piglatinParser(new CommonTokenStream(lexerPig));
        parserPig.programa();
        reportarSintaxis("Pig Latin", parserPig.getNumberOfSyntaxErrors());

        String codigoZetariano = """
            public class Guerrero {
                public String nombre;
                public int nivel;

                public Guerrero(String nombre, int nivel) {
                    this.nombre = nombre;
                    this.nivel = nivel;
                }

                public int obtenerNivel() {
                    return this.nivel;
                }
            }
            """;
        zetarianoLexer lexerZ = new zetarianoLexer(CharStreams.fromString(codigoZetariano));
        zetarianoParser parserZ = new zetarianoParser(new CommonTokenStream(lexerZ));
        parserZ.unidadCompilacion();
        reportarSintaxis("Zetariano", parserZ.getNumberOfSyntaxErrors());

        String codigoY = """
    %estructuras
    estructura Punto:
        entero x
        entero y
    %funciones
    calcularPoder(entero base) -> entero:
        retornar base * 2
    """;
        yLexer lexerY = new yLexer(CharStreams.fromString(codigoY));
        YSangriaToken sangriaSource = new YSangriaToken(lexerY);
        yParser parserY = new yParser(new CommonTokenStream(sangriaSource));
        parserY.programa();
        reportarSintaxis("Y?", parserY.getNumberOfSyntaxErrors());
    }

    private static void ejecutarExpresionPig(String codigo, TablaSimbolos ts, String esperado) {
        piglatinLexer lexer = new piglatinLexer(CharStreams.fromString(codigo));
        piglatinParser parser = new piglatinParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.expresion();

        ConstructorAstPigLatin builder = new ConstructorAstPigLatin();
        NodoAST ast = builder.visit(tree);

        List<ErrorSemantico> errores = new ArrayList<>();
        Tipo obtenido = ast.comprobar(ts, errores);
        imprimirResultado("Pig Latin", codigo, obtenido, esperado, errores);
    }

    private static void ejecutarExpresionZetariano(String codigo, TablaSimbolos ts, String esperado) {
        zetarianoLexer lexer = new zetarianoLexer(CharStreams.fromString(codigo));
        zetarianoParser parser = new zetarianoParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.expresion();

        ConstructorAstZetariano builder = new ConstructorAstZetariano();
        NodoAST ast = builder.visit(tree);

        List<ErrorSemantico> errores = new ArrayList<>();
        Tipo obtenido = ast.comprobar(ts, errores);
        imprimirResultado("Zetariano", codigo, obtenido, esperado, errores);
    }

    private static void ejecutarExpresionY(String codigo, TablaSimbolos ts, String esperado) {
        yLexer lexer = new yLexer(CharStreams.fromString(codigo));
        yParser parser = new yParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.expresion();

        ConstructorAstY builder = new ConstructorAstY();
        NodoAST ast = builder.visit(tree);

        List<ErrorSemantico> errores = new ArrayList<>();
        Tipo obtenido = ast.comprobar(ts, errores);
        imprimirResultado("Y?", codigo, obtenido, esperado, errores);
    }

    private static void imprimirResultado(String lenguaje, String codigo, Tipo obtenido, String esperado, List<ErrorSemantico> errores) {
        System.out.printf("[%s] '%s' -> Tipo: %s (Esperado: %s)\n", lenguaje, codigo, obtenido, esperado);
        for (ErrorSemantico err : errores) {
            System.out.printf("   [Error detectado] %s (L:%d, C:%d)\n", err.getMensaje(), err.getLinea(), err.getColumna());
        }
    }

    private static void reportarSintaxis(String lenguaje, int errores) {
        if (errores == 0) {
            System.out.println("[" + lenguaje + "] Sintaxis válida (0 errores).");
        } else {
            System.err.println("[" + lenguaje + "] Se detectaron " + errores + " errores sintácticos.");
        }
    }
}