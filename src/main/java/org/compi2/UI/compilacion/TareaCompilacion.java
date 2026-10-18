package org.compi2.UI.compilacion;

import org.antlr.v4.runtime.tree.ParseTree;

import org.compi2.UI.VentanaPrincipal;
import org.compi2.UI.consola.PanelConsola;
import org.compi2.UI.graficos.GeneradorDot;
import org.compi2.UI.tablas.ModeloTablaErrores;
import org.compi2.UI.tablas.ModeloTablaSimbolos;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.orquestador.ArchivoFuente;
import org.compi2.orquestador.OrquestadorCompilacion;
import org.compi2.orquestador.ResultadoCompilacion;

import javax.swing.SwingWorker;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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

    private static String mensajeError(Throwable t) {
        String mensaje = t.getMessage();
        return (mensaje != null && !mensaje.isBlank()) ? mensaje : t.toString();
    }

    @Override
    protected Void doInBackground() {
        try {
            log("================================================================================");
            log("                              COMPILANDO:                       " + nombreArchivo);
            log("================================================================================");

            if (ejecucionCancelada.get()) return null;

            String nombreNormalizado = nombreArchivo.toLowerCase(Locale.ROOT);
            boolean moduloY = nombreNormalizado.endsWith(".y");
            boolean moduloZ = nombreNormalizado.endsWith(".z");

            if (moduloY) {
                log(">> Módulo Y Análisis Léxico y Sintáctico");
            } else if (moduloZ) {
                log(">> Módulo Zetariano Análisis Léxico y Sintáctico");
            } else {
                log(">> Análisis Léxico y Sintáctico");
            }

            OrquestadorCompilacion orquestador = new OrquestadorCompilacion();
            resultadoCompilacion = orquestador.compilar(nombreArchivo, codigoFuente, determinarDirectorioBase(), null);

            try {
                if (moduloY) {
                    generarGraficoModulo(resultadoCompilacion.getParseTreePrincipal(), "Módulo Y");
                } else if (moduloZ) {
                    generarGraficoModulo(resultadoCompilacion.getParseTreePrincipal(), "Módulo Zetariano");
                } else {
                    log(String.format(">> Módulos e Imports (%d archivos importados)",
                        resultadoCompilacion.getArchivosCompilados().size()));
                    log(">> Análisis Semántico");
                    log(">> Generación Gráfica AST");
                    generarGraficosProyecto();
                }
            } catch (Throwable t) {
                log("No se pudo generar el gráfico del AST: " + mensajeError(t));
            }

            registrarResumenFinal();
            return null;
        } catch (Throwable t) {
            exito = false;
            log("Error inesperado durante la compilación: " + mensajeError(t));
            return null;
        }
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
            log("Código C3D : generado (ver pestaña \"Código C3D (C)\")");
        }
        if (!exito) {
            log("\nErrores");
            resultadoCompilacion.getErrores().forEach(err -> log(err.toString()));
        }
        log("================================================================================");
    }

    private void generarGraficosProyecto() {
        mapaArbolesDot = new LinkedHashMap<>();
        ParseTree arbolPrincipal = resultadoCompilacion.getParseTreePrincipal();

        if (arbolPrincipal == null) {
            mapaArbolesDot.put("Demostración", GeneradorDot.generarDemoDot(nombreArchivo));
            return;
        }

        String dotPrincipal = GeneradorDot.generar(arbolPrincipal, nombreArchivo);
        Map<String, ArchivoFuente> importados = resultadoCompilacion.getArchivosCompilados();

        if (importados == null || importados.isEmpty()) {
            mapaArbolesDot.put("📄 " + nombreArchivo + " (Principal)", dotPrincipal);
            return;
        }

        mapaArbolesDot.put("🌐 Proyecto Completo",
            GeneradorDot.generarProyectoCompleto(arbolPrincipal, nombreArchivo, importados));
        mapaArbolesDot.put("📄 " + nombreArchivo + " (Principal)", dotPrincipal);

        for (ArchivoFuente af : importados.values()) {
            if (af.getParseTree() != null) {
                String dotModulo = GeneradorDot.generar(af.getParseTree(), af.getNombreArchivo());
                mapaArbolesDot.put("📦 " + af.getNombreArchivo() + " (" + af.getLenguaje() + ")", dotModulo);
            }
        }
    }

    private void generarGraficoModulo(ParseTree tree, String etiquetaModulo) {
        mapaArbolesDot = new LinkedHashMap<>();

        if (tree == null) {
            mapaArbolesDot.put("Demostración", GeneradorDot.generarDemoDot(nombreArchivo));
            return;
        }

        mapaArbolesDot.put("📄 " + nombreArchivo + " (" + etiquetaModulo + ")",
            GeneradorDot.generar(tree, nombreArchivo));
    }

    @Override
    protected void done() {
        if (ejecucionCancelada.get()) return;

        if (resultadoCompilacion == null) {
            try {
                get();
            } catch (Exception ex) {
                Throwable causa = (ex.getCause() != null) ? ex.getCause() : ex;
                panelConsola.agregarTexto("\nError inesperado al compilar: " + mensajeError(causa) + "\n");
            }
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
