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
import org.compi2.analisis_semantico.VisitorSemanticoPigLatin;
import org.compi2.tipos.Tipo;


public class Main {

    public static void main(String[] args) {
        probarMotorSemanticoAislado();
        probarPigLatinConSemantica();
        probarZetarianoSintaxis();
        probarYSintaxis();
    }

    public static void probarMotorSemanticoAislado() {
        System.out.println("=================================================");
        System.out.println(">>> 1. PRUEBA AISLADA DEL MOTOR SEMÁNTICO");
        System.out.println("=================================================");

        TablaSimbolos ts = new TablaSimbolos();

        ts.registrarSimbolo("global_cont", Tipo.ENTERO, "variable", 1, 1);     // Offset: 0 (+4)
        ts.registrarSimbolo("global_pi", Tipo.DECIMAL, "variable", 2, 1);       // Offset: 4 (+8)
        ts.registrarSimbolo("global_titulo", Tipo.CADENA, "variable", 3, 1);    // Offset: 12 (+4)

        ts.abrirAmbito("calcular()", true);
        ts.registrarSimbolo("param_x", Tipo.ENTERO, "parametro", 5, 5);        // Offset: 0 (+4)
        ts.registrarSimbolo("temp_res", Tipo.DECIMAL, "variable", 6, 5);       // Offset: 4 (+8)

        ts.abrirAmbito("Bloque_Si", false);
        ts.registrarSimbolo("local_flag", Tipo.BOOLEANO, "variable", 8, 9);     // Offset: 12 (+4)

        System.out.println("[Búsqueda] 'local_flag' desde Si: " + (ts.buscarSimbolo("local_flag") != null ? "ENCONTRADO" : "FALLÓ"));
        System.out.println("[Búsqueda] 'global_pi' desde Si: " + (ts.buscarSimbolo("global_pi") != null ? "ENCONTRADO" : "FALLÓ"));

        ts.cerrarAmbito();
        System.out.println("[Búsqueda post-cierre] 'local_flag' fuera de Si: " + (ts.buscarSimbolo("local_flag") == null ? "NO VISIBLE (CORRECTO)" : "ERROR DE SCOPE"));
        ts.cerrarAmbito();

        Tipo sumaNumDec = ComprobadorTipos.resolverSuma(Tipo.ENTERO, Tipo.DECIMAL);
        Tipo sumaStrNum = ComprobadorTipos.resolverSuma(Tipo.CADENA, Tipo.ENTERO);
        Tipo multInvalida = ComprobadorTipos.resolverAritmetica(Tipo.CADENA, Tipo.ENTERO);

        System.out.println("\n[ComprobadorTipos] ENTERO + DECIMAL -> " + sumaNumDec + " (Esperado: decimal)");
        System.out.println("[ComprobadorTipos] CADENA + ENTERO  -> " + sumaStrNum + " (Esperado: cadena)");
        System.out.println("[ComprobadorTipos] CADENA * ENTERO  -> " + multInvalida + " (Esperado: error)");

        System.out.println("\n" + ts.generarReporteTexto());
    }

    public static void probarPigLatinConSemantica() {
        System.out.println("\n=================================================");
        System.out.println(">>> 2. PRUEBA INTEGRAL PIG LATIN (AST + SEMÁNTICA)");
        System.out.println("=================================================");

        String codigoPigLatin = """
            import carpeta.Objeto1.z;
            import carpeta.Funciones.y;

            VARIABILES>
            esto edad : numerus 20;
            esto nombre : textum "Resistencia";
            series mis_enteros[2] : numerus {1, 1};

            MAIOR>
            >> "Hola comandante!";
            >> "Ingresa tu edad";
            edad <<;
            si (edad >= 18) {
                fuerza = 12;
                contador++;
            } finis;
            >> "Fin de la transmision";
            FINIS;
            """;

        piglatinLexer lexer = new piglatinLexer(CharStreams.fromString(codigoPigLatin));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        piglatinParser parser = new piglatinParser(tokens);

        ParseTree arbol = parser.programa();

        int erroresSintacticos = parser.getNumberOfSyntaxErrors();
        if (erroresSintacticos > 0) {
            System.err.println("Fallo sintáctico en Pig Latin con " + erroresSintacticos + " errores.");
            return;
        }
        System.out.println("Sintaxis de Pig Latin validada con éxito (0 errores).");

        VisitorSemanticoPigLatin visitor = new VisitorSemanticoPigLatin();
        visitor.visit(arbol);

        System.out.println("\n" + visitor.getTablaSimbolos().generarReporteTexto());

        if (visitor.getErrores().isEmpty()) {
            System.out.println("Análisis Semántico exitoso sin inconsistencias.");
        } else {
            System.err.println("Se detectaron " + visitor.getErrores().size() + " inconsistencias semánticas:");
            for (ErrorSemantico err : visitor.getErrores()) {
                System.err.println("  -> " + err);
            }
        }
    }

    public static void probarZetarianoSintaxis() {
        System.out.println("\n=================================================");
        System.out.println(">>> 3. PRUEBA SINTÁCTICA ZETARIANO (.z)");
        System.out.println("=================================================");

        String codigoZetariano = """
            public class Guerrero {
                public String nombre;
                public int nivel;
                public double vida;

                public Guerrero(String nombre, int nivel) {
                    this.nombre = nombre;
                    this.nivel = nivel;
                    this.vida = 100.0;
                }
    
                public void entrenar(int incremento) {
                    this.nivel += incremento;
                    println("Nivel actual: " + this.nivel);

                    if (this.nivel >= 50) {
                        println("Guerrero avanzado");
                    } else {
                        println("Entrenamiento basico");
                    }
                }

                public int obtenerNivel() {
                    return this.nivel;
                }
            }
            """;

        zetarianoLexer lexer = new zetarianoLexer(CharStreams.fromString(codigoZetariano));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        zetarianoParser parser = new zetarianoParser(tokens);

        ParseTree arbol = parser.unidadCompilacion();
        reportarSintaxis("Zetariano", parser.getNumberOfSyntaxErrors(), arbol, parser);
    }

    public static void probarYSintaxis() {
        System.out.println("\n=================================================");
        System.out.println(">>> 4. PRUEBA SINTÁCTICA Y? (.y)");
        System.out.println("=================================================");

        String codigoY = """
            % estructuras
            
            % funciones
            """;

        yLexer lexer = new yLexer(CharStreams.fromString(codigoY));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        yParser parser = new yParser(tokens);

        ParseTree arbol = parser.programa();
        reportarSintaxis("Y?", parser.getNumberOfSyntaxErrors(), arbol, parser);
    }

    private static void reportarSintaxis(String lenguaje, int errores, ParseTree arbol, org.antlr.v4.runtime.Parser parser) {
        if (errores == 0) {
            System.out.println("[" + lenguaje + "] Sintaxis válida (0 errores).");
        } else {
            System.err.println("[" + lenguaje + "] Se detectaron " + errores + " errores sintácticos.");
        }
    }
}