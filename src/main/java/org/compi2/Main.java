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
import org.compi2.ast.expresiones.*;
import org.compi2.ast.sentencias.*;
import org.compi2.constructores_ast.ConstructorAstPigLatin;
import org.compi2.constructores_ast.ConstructorAstZetariano;
import org.compi2.constructores_ast.ConstructorAstY;

import org.compi2.errores.CustomErrorListener;
import org.compi2.errores.ErrorCompilacion;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        probarMotorSemanticoAislado();
        probarAstExpresionesPigLatin();
        probarAstExpresionesZetariano();
        probarAstExpresionesY();
        probarSintaxisProgramasCompletos();
        probarCustomErrorListener();
        probarNuevasSentenciasYExpresiones();
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

        System.out.println("[Búsqueda] 'local_flag' en ámbito local: "
                + (ts.buscarSimbolo("local_flag") != null ? "ENCONTRADO" : "FALLÓ"));
        System.out.println("[Búsqueda] 'global_pi' desde ámbito anidado: "
                + (ts.buscarSimbolo("global_pi") != null ? "ENCONTRADO" : "FALLÓ"));

        ts.cerrarAmbito();
        System.out.println("[Búsqueda post-cierre] 'local_flag' fuera de bloque: "
                + (ts.buscarSimbolo("local_flag") == null ? "NO VISIBLE (CORRECTO)" : "ERROR DE ALCANCE"));
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

    private static void imprimirResultado(String lenguaje, String codigo, Tipo obtenido, String esperado,
            List<ErrorSemantico> errores) {
        System.out.printf("[%s] '%s' -> Tipo: %s (Esperado: %s)\n", lenguaje, codigo, obtenido, esperado);
        for (ErrorSemantico err : errores) {
            System.out.printf("   [Error detectado] %s (L:%d, C:%d)\n", err.getMensaje(), err.getLinea(),
                    err.getColumna());
        }
    }

    private static void reportarSintaxis(String lenguaje, int errores) {
        if (errores == 0) {
            System.out.println("[" + lenguaje + "] Sintaxis válida (0 errores).");
        } else {
            System.err.println("[" + lenguaje + "] Se detectaron " + errores + " errores sintácticos.");
        }
    }

    public static void probarCustomErrorListener() {
        System.out.println("\n=================================================");
        System.out.println(">>> 6. VALIDACIÓN DE CAPTURA DE ERRORES (CustomErrorListener)");
        System.out.println("=================================================");

        List<ErrorCompilacion> erroresCapturados = new ArrayList<>();

        String codigoConErrorLexico = """
                MAIOR>
                esto x : numerus 10 @;
                FINIS;
                """;
        piglatinLexer lexerPig = new piglatinLexer(CharStreams.fromString(codigoConErrorLexico));
        piglatinParser parserPig = new piglatinParser(new CommonTokenStream(lexerPig));
        CustomErrorListener.asociar(lexerPig, parserPig, "programa_lexico.pig", erroresCapturados);
        parserPig.programa();

        String codigoConErrorSintactico = """
                public class Prueba {
                    public int suma(int a, int b {
                        return a + b;
                    }
                }
                """;
        zetarianoLexer lexerZ = new zetarianoLexer(CharStreams.fromString(codigoConErrorSintactico));
        zetarianoParser parserZ = new zetarianoParser(new CommonTokenStream(lexerZ));
        CustomErrorListener.asociar(lexerZ, parserZ, "Prueba_sintactico.z", erroresCapturados);
        parserZ.unidadCompilacion();

        System.out.printf("\nTotal de errores capturados para UI: %d\n", erroresCapturados.size());
        for (ErrorCompilacion err : erroresCapturados) {
            System.out.println("  -> " + err);
        }
    }

    public static void probarNuevasSentenciasYExpresiones() {
        System.out.println("\n=================================================");
        System.out.println(">>> 7. VALIDACIÓN DE NUEVAS SENTENCIAS Y EXPRESIONES (Fase 2)");
        System.out.println("=================================================");

        TablaSimbolos ts = new TablaSimbolos();
        List<ErrorSemantico> errores = new ArrayList<>();

        NodoExpresionTernaria ternaria = new NodoExpresionTernaria(
                new NodoOperacionBinaria(new NodoLiteral(10, 1, 1), ">", new NodoLiteral(5, 1, 1), 1, 1),
                new NodoLiteral(100, 1, 1),
                new NodoLiteral(200, 1, 1),
                1, 1);
        Tipo tipoTernaria = ternaria.comprobar(ts, errores);
        System.out.println("[Ternario] '(10 > 5) ? 100 : 200' -> Tipo: " + tipoTernaria);

        NodoBreak breakIlegal = new NodoBreak(10, 5);
        breakIlegal.comprobar(ts, errores);

        NodoFor cicloFor = new NodoFor(
                new NodoDeclaracion("i", Tipo.ENTERO, new NodoLiteral(0, 1, 1), 12, 1),
                new NodoOperacionBinaria(new NodoIdentificador("i", 12, 1), "<", new NodoLiteral(10, 12, 1), 12, 1),
                new NodoOperacionUnaria("++", new NodoIdentificador("i", 12, 1), 12, 1),
                List.of(
                        new NodoImprimir(List.of(new NodoIdentificador("i", 13, 5)), true, 13, 5),
                        new NodoBreak(14, 9) // Válido dentro de for
                ),
                12, 1);
        cicloFor.comprobar(ts, errores);
        System.out.println("[For & Break] Comprobación de ciclo 'for' con 'break' interno procesada.");

        NodoSwitch switchPrueba = new NodoSwitch(
                new NodoLiteral(1, 20, 1), // Tipo entero
                List.of(
                        new NodoSwitch.CasoSwitch(new NodoLiteral(1, 21, 5), List.of(new NodoBreak(21, 15)), 21, 5),
                        new NodoSwitch.CasoSwitch(new NodoLiteral("textoInvalido", 22, 5),
                                List.of(new NodoBreak(22, 15)), 22, 5) // Error: String en switch entero
                ),
                null,
                20, 1);
        switchPrueba.comprobar(ts, errores);

        ts.abrirAmbitoFuncion("calcular()", Tipo.ENTERO);
        NodoRetorno retCorrecto = new NodoRetorno(new NodoLiteral(42, 30, 5), 30, 5);
        retCorrecto.comprobar(ts, errores);
        NodoRetorno retIncompatible = new NodoRetorno(new NodoLiteral("noEsNumero", 31, 5), 31, 5); // Error: retorno
                                                                                                    // incompatible
        retIncompatible.comprobar(ts, errores);
        ts.cerrarAmbito();

        NodoInstanciacionArreglo nuevoArr = new NodoInstanciacionArreglo(
                Tipo.ENTERO,
                List.of(new NodoLiteral(5, 40, 1)),
                40, 1);
        Tipo tipoArr = nuevoArr.comprobar(ts, errores);
        System.out.println("[Arreglo Nuevo] 'new int[5]' -> Tipo: " + tipoArr);

        System.out.printf("\nTotal de errores semánticos detectados en pruebas: %d\n", errores.size());
        for (ErrorSemantico err : errores) {
            System.out.println("  -> " + err);
        }
    }
}