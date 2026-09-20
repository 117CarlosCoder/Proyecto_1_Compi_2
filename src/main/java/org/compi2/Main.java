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

public class Main {

    public static void main(String[] args) {
        probarPigLatin();
        probarZetariano();
        probarY();
    }

    public static void probarPigLatin() {
        System.out.println("========================================");
        System.out.println(">>> PROBANDO PIG LATIN (.pig)");
        System.out.println("========================================");

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
        reportar("Pig Latin", parser.getNumberOfSyntaxErrors(), arbol, parser);
    }

    public static void probarZetariano() {
        System.out.println("\n========================================");
        System.out.println(">>> PROBANDO ZETARIANO (.z)");
        System.out.println("========================================");

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
        reportar("Zetariano", parser.getNumberOfSyntaxErrors(), arbol, parser);
    }


    public static void probarY() {
        System.out.println("\n========================================");
        System.out.println(">>> PROBANDO Y (.y)");
        System.out.println("========================================");

        String codigoY = """
            % estructuras
            
            % funciones
            """;

        yLexer lexer = new yLexer(CharStreams.fromString(codigoY));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        yParser parser = new yParser(tokens);

        ParseTree arbol = parser.programa();
        reportar("Y?", parser.getNumberOfSyntaxErrors(), arbol, parser);
    }

    private static void reportar(String lenguaje, int errores, ParseTree arbol, org.antlr.v4.runtime.Parser parser) {
        if (errores == 0) {
            System.out.println("[" + lenguaje + "] Estado: Sintaxis valida (0 errores).");
            System.out.println("[" + lenguaje + "] Arbol: " + arbol.toStringTree(parser));
        } else {
            System.err.println("[" + lenguaje + "] Fallo: Se detectaron " + errores + " errores sintacticos.");
        }
    }
}