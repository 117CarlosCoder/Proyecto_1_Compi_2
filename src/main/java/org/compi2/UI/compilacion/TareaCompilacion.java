package org.compi2.UI.compilacion;

import gramaticas.y.yLexer;
import gramaticas.y.yParser;
import gramaticas.zetariano.zetarianoLexer;
import gramaticas.zetariano.zetarianoParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.tree.ParseTree;

import org.compi2.VentanaPrincipal;
import org.compi2.UI.consola.PanelConsola;
import org.compi2.UI.graficos.GeneradorDot;
import org.compi2.UI.tablas.ModeloTablaErrores;
import org.compi2.UI.tablas.ModeloTablaSimbolos;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.analisis_semantico.YSangriaToken;
import org.compi2.ast.sentencias.*;
import org.compi2.c3d.GeneradorC3D;
import org.compi2.constructores_ast.ConstructorAstY;
import org.compi2.constructores_ast.ConstructorAstZetariano;
import org.compi2.errores.CustomErrorListener;
import org.compi2.errores.ErrorCompilacion;
import org.compi2.orquestador.ArchivoFuente;
import org.compi2.orquestador.OrquestadorCompilacion;
import org.compi2.orquestador.ResultadoCompilacion;
import org.compi2.tipos.Tipo;

import javax.swing.SwingWorker;
import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TareaCompilacion extends SwingWorker<Void, Void> {

    private final String codigoFuente;
    private final String nombreArchivo;
    private final File archivoActual;
    private final File carpetaProyecto;
    private final PanelConsola panelConsola;
    private final ModeloTablaSimbolos modeloSimbolos;
    private final ModeloTablaErrores modeloErrores;
    private final VentanaPrincipal ventanaPrincipal;
    private final AtomicBoolean ejecucionCancelada;
    private final Consumer<String> salidaLog;

    private String codigoDot;
    private Map<String, String> mapaArbolesDot = new LinkedHashMap<>();
    private boolean exito = true;
    private int totalErrores = 0;
    private int totalSimbolos = 0;
    private ResultadoCompilacion resultadoCompilacion;

    public TareaCompilacion(String codigoFuente, String nombreArchivo, File archivoActual, File carpetaProyecto,
                            PanelConsola panelConsola, ModeloTablaSimbolos modeloSimbolos, ModeloTablaErrores modeloErrores,
                            VentanaPrincipal ventanaPrincipal, AtomicBoolean ejecucionCancelada, Consumer<String> salidaLog) {
        this.codigoFuente = codigoFuente;
        this.nombreArchivo = (nombreArchivo != null && !nombreArchivo.isBlank()) ? nombreArchivo : "codigo.pig";
        this.archivoActual = archivoActual;
        this.carpetaProyecto = carpetaProyecto;
        this.panelConsola = panelConsola;
        this.modeloSimbolos = modeloSimbolos;
        this.modeloErrores = modeloErrores;
        this.ventanaPrincipal = ventanaPrincipal;
        this.ejecucionCancelada = ejecucionCancelada;
        this.salidaLog = salidaLog;
    }

    private void log(String msg) {
        if (!ejecucionCancelada.get() && salidaLog != null) {
            salidaLog.accept(msg + "\n");
        }
    }

    @Override
    protected Void doInBackground() {
        log("================================================================================");
        log("                              COMPILANDO:                       " + nombreArchivo);
        log("================================================================================");

        if (ejecucionCancelada.get()) return null;

        String nombreMin = nombreArchivo.toLowerCase();
        if (nombreMin.endsWith(".y")) {
            compilarModuloY();
        } else if (nombreMin.endsWith(".z")) {
            compilarModuloZetariano();
        } else {
            compilarPigLatin(determinarDirectorioBase());
        }

        registrarResumenFinal();
        return null;
    }

    private Path determinarDirectorioBase() {
        if (archivoActual != null && archivoActual.getParentFile() != null) {
            return archivoActual.getParentFile().toPath();
        }
        if (carpetaProyecto != null && carpetaProyecto.exists() && carpetaProyecto.isDirectory()) {
            return carpetaProyecto.toPath();
        }
        return Path.of(".");
    }

    private void registrarResumenFinal() {
        totalErrores = resultadoCompilacion.getErrores().size();
        totalSimbolos = resultadoCompilacion.getTablaSimbolos().obtenerTodosLosSimbolos().size();
        exito = (totalErrores == 0);

        log("\n" + (exito ? "compilación completada " : "se encontraron errores de compilación"));
        log(String.format("Símbolos registrados : %d", totalSimbolos));
        log(String.format("Errores detectados   : %d", totalErrores));
        if (exito && resultadoCompilacion.getCodigoC3D() != null) {
            log("Código C3D : ");
        }
        if (!exito) {
            log("\nErrores");
            resultadoCompilacion.getErrores().forEach(err -> log(err.toString()));
        }
        log("================================================================================");
    }

    private void compilarPigLatin(Path dirBase) {
        log(">> Análisis Léxico y Sintáctico");

        OrquestadorCompilacion orquestador = new OrquestadorCompilacion();
        resultadoCompilacion = orquestador.compilar(nombreArchivo, codigoFuente, dirBase, null);

        log(String.format(">> Módulos e Imports (%d archivos importados)",
            resultadoCompilacion.getArchivosCompilados().size()));
        log(">> Análisis Semántico ");
        log(">> Generación Gráfica AST ");

        generarGraficosProyecto();
    }

    private void generarGraficosProyecto() {
        mapaArbolesDot = new LinkedHashMap<>();
        ParseTree arbolPrincipal = resultadoCompilacion.getParseTreePrincipal();

        if (arbolPrincipal == null) {
            this.codigoDot = GeneradorDot.generarDemoDot(nombreArchivo);
            mapaArbolesDot.put("Demostración", this.codigoDot);
            return;
        }

        this.codigoDot = GeneradorDot.generar(arbolPrincipal, nombreArchivo);
        Map<String, ArchivoFuente> importados = resultadoCompilacion.getArchivosCompilados();

        if (importados == null || importados.isEmpty()) {
            mapaArbolesDot.put("📄 " + nombreArchivo + " (Principal)", this.codigoDot);
            return;
        }

        String dotCompleto = GeneradorDot.generarProyectoCompleto(arbolPrincipal, nombreArchivo, importados);
        mapaArbolesDot.put("🌐 Proyecto Completo", dotCompleto);
        mapaArbolesDot.put("📄 " + nombreArchivo + " (Principal)", this.codigoDot);

        for (ArchivoFuente af : importados.values()) {
            if (af.getParseTree() != null) {
                String dotMod = GeneradorDot.generar(af.getParseTree(), af.getNombreArchivo());
                mapaArbolesDot.put("📦 " + af.getNombreArchivo() + " (" + af.getLenguaje() + ")", dotMod);
            }
        }
    }

    private void compilarModuloY() {
        log(">> Módulo Y Análisis Léxico y Sintáctico ");
        List<ErrorCompilacion> errores = new ArrayList<>();

        yLexer lexer = new yLexer(CharStreams.fromString(codigoFuente));
        configurarListener(lexer, errores);
        CommonTokenStream tokens = new CommonTokenStream(new YSangriaToken(lexer));
        yParser parser = new yParser(tokens);
        configurarListener(parser, errores);

        ParseTree tree = parser.programa();
        generarGraficoModulo(tree, "Módulo Y");

        NodoProgramaY ast = construirAst(tree, new ConstructorAstY(), NodoProgramaY.class, errores);

        resultadoCompilacion = procesarModulo(
            "Y", tree, errores,
            ast != null ? ast::registrarFirmas : null,
            ast != null ? ast::comprobarCuerpos : null,
            () -> {
                assert ast != null;
                return ast.getFunciones() != null ? ast.getFunciones() : Collections.emptyList();
            });
    }


    private void compilarModuloZetariano() {
        log(">> Módulo Zetariano Análisis Léxico y Sintáctico");
        List<ErrorCompilacion> errores = new ArrayList<>();

        zetarianoLexer lexer = new zetarianoLexer(CharStreams.fromString(codigoFuente));
        configurarListener(lexer, errores);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        zetarianoParser parser = new zetarianoParser(tokens);
        configurarListener(parser, errores);

        ParseTree tree = parser.unidadCompilacion();
        generarGraficoModulo(tree, "Módulo Zetariano");

        NodoUnidadCompilacionZ ast = construirAst(tree, new ConstructorAstZetariano(), NodoUnidadCompilacionZ.class, errores);

        resultadoCompilacion = procesarModulo(
            "Zetariano", tree, errores,
            ast != null ? ast::registrarFirmas : null,
            ast != null ? ast::comprobarCuerpos : null,
            () -> {
                assert ast != null;
                return extraerFuncionesDeClases(ast.getClases());
            });
    }

    private List<NodoFuncion> extraerFuncionesDeClases(List<NodoClase> clases) {
        List<NodoFuncion> funciones = new ArrayList<>();
        if (clases == null) return funciones;

        for (NodoClase clase : clases) {
            agregarMetodos(clase, funciones);
            agregarConstructoresComoFunciones(clase, funciones);
        }
        return funciones;
    }

    private void agregarMetodos(NodoClase clase, List<NodoFuncion> destino) {
        if (clase.getMetodos() == null) return;
        for (NodoFuncion metodo : clase.getMetodos()) {
            metodo.setEsMetodo(true);
            metodo.setClaseContenedora(clase.getNombre());
            destino.add(metodo);
        }
    }

    private void agregarConstructoresComoFunciones(NodoClase clase, List<NodoFuncion> destino) {
        if (clase.getConstructores() == null) return;
        for (NodoConstructor constructor : clase.getConstructores()) {
            NodoFuncion fn = new NodoFuncion(
                clase.getNombre() + "_constructor",
                constructor.getParametros(),
                Tipo.VOID,
                constructor.getCuerpo(),
                constructor.getLinea(),
                constructor.getColumna());
            fn.setEsMetodo(true);
            fn.setClaseContenedora(clase.getNombre());
            destino.add(fn);
        }
    }

    private void configurarListener(Lexer lexer, List<ErrorCompilacion> errores) {
        lexer.removeErrorListeners();
        lexer.addErrorListener(new CustomErrorListener(nombreArchivo, errores));
    }

    private void configurarListener(Parser parser, List<ErrorCompilacion> errores) {
        parser.removeErrorListeners();
        parser.addErrorListener(new CustomErrorListener(nombreArchivo, errores));
    }

    private void generarGraficoModulo(ParseTree tree, String etiquetaModulo) {
        this.codigoDot = GeneradorDot.generar(tree, nombreArchivo);
        mapaArbolesDot = new LinkedHashMap<>();
        mapaArbolesDot.put("📄 " + nombreArchivo + " (" + etiquetaModulo + ")", this.codigoDot);
    }

    private <T> T construirAst(ParseTree tree, org.antlr.v4.runtime.tree.ParseTreeVisitor<?> builder,
                               Class<T> tipoEsperado, List<ErrorCompilacion> errores) {
        try {
            return tipoEsperado.cast(builder.visit(tree));
        } catch (Exception e) {
            errores.add(new ErrorSemantico("Fallo al construir AST de '" + nombreArchivo + "': " + e.getMessage(),
                "AST", nombreArchivo, 1, 1));
            return null;
        }
    }

    private ResultadoCompilacion procesarModulo(String contexto, ParseTree tree, List<ErrorCompilacion> erroresSintacticos,
                                                BiConsumer<TablaSimbolos, List<ErrorSemantico>> registrarFirmas,
                                                BiConsumer<TablaSimbolos, List<ErrorSemantico>> comprobarCuerpos,
                                                Supplier<List<NodoFuncion>> obtenerFunciones) {

        TablaSimbolos tabla = new TablaSimbolos();
        tabla.setContextoOrigen(contexto, nombreArchivo);

        List<ErrorSemantico> erroresSemanticos = new ArrayList<>();
        if (registrarFirmas != null) {
            registrarFirmas.accept(tabla, erroresSemanticos);
            comprobarCuerpos.accept(tabla, erroresSemanticos);
        }

        List<ErrorCompilacion> errores = new ArrayList<>(erroresSintacticos);
        errores.addAll(erroresSemanticos);

        ResultadoCompilacion resultado = new ResultadoCompilacion(errores.isEmpty(), null, Collections.emptyMap(), tabla, errores);
        resultado.setParseTreePrincipal(tree);

        if (errores.isEmpty() && obtenerFunciones != null) {
            GeneradorC3D genC3D = new GeneradorC3D(tabla);
            String c3d = genC3D.compilarConFunciones(obtenerFunciones.get(), Collections.emptyList());
            resultado.setCodigoC3D(c3d);
            resultado.setContenedorC3D(genC3D.getC3d());
        }

        return resultado;
    }

    @Override
    protected void done() {
        if (ejecucionCancelada.get()) return;

        if (resultadoCompilacion == null) {
            ventanaPrincipal.notificarFinalizacion(false, 1, 0, Collections.emptyMap(), null, new TablaSimbolos(), Collections.emptyList());
            return;
        }

        modeloSimbolos.actualizarSimbolos(resultadoCompilacion.getTablaSimbolos().obtenerTodosLosSimbolos());
        modeloErrores.actualizarErrores(resultadoCompilacion.getErrores());
        ventanaPrincipal.notificarFinalizacion(
            exito, totalErrores, totalSimbolos, mapaArbolesDot,
            resultadoCompilacion.getCodigoC3D(),
            resultadoCompilacion.getTablaSimbolos(),
            resultadoCompilacion.getErrores());
    }
}