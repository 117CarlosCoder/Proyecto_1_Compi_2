package org.compi2.orquestador;

import gramaticas.piglatin.piglatinLexer;
import gramaticas.piglatin.piglatinParser;
import gramaticas.y.yLexer;
import gramaticas.y.yParser;
import gramaticas.zetariano.zetarianoLexer;
import gramaticas.zetariano.zetarianoParser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.analisis_semantico.YSangriaToken;
import org.compi2.ast.NodoAST;
import org.compi2.ast.sentencias.NodoClase;
import org.compi2.ast.sentencias.NodoConstructor;
import org.compi2.ast.sentencias.NodoFuncion;
import org.compi2.ast.sentencias.NodoPrograma;
import org.compi2.ast.sentencias.NodoProgramaY;
import org.compi2.ast.sentencias.NodoUnidadCompilacionZ;
import org.compi2.c3d.GeneradorC3D;
import org.compi2.constructores_ast.ConstructorAstPigLatin;
import org.compi2.constructores_ast.ConstructorAstY;
import org.compi2.constructores_ast.ConstructorAstZetariano;
import org.compi2.errores.CustomErrorListener;
import org.compi2.errores.ErrorCompilacion;
import org.compi2.tipos.Tipo;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public class OrquestadorCompilacion {

    public ResultadoCompilacion compilarArchivo(File archivoPig) {
        return compilarArchivo(archivoPig.toPath());
    }

    public ResultadoCompilacion compilarArchivo(Path rutaArchivoPig) {
        List<ErrorCompilacion> errores = new ArrayList<>();
        Optional<String> contenido = leerContenidoArchivo(rutaArchivoPig, errores);

        if (contenido.isEmpty()) {
            return resultadoFallido(errores);
        }

        Path dirBase = rutaArchivoPig.toAbsolutePath().getParent();
        String nombreArchivo = rutaArchivoPig.getFileName().toString();
        return compilar(nombreArchivo, contenido.get(), dirBase, null);
    }

    public ResultadoCompilacion compilarEnMemoria(String codigoPig, Map<String, String> archivosVirtuales) {
        return compilar("principal.pig", codigoPig, null, archivosVirtuales);
    }

    public ResultadoCompilacion compilarEnMemoria(String nombreArchivoPig, String codigoPig,
                                                  Map<String, String> archivosVirtuales) {
        return compilar(nombreArchivoPig, codigoPig, null, archivosVirtuales);
    }

    public ResultadoCompilacion compilar(String nombrePig, String codigoPig, Path dirBase,
                                         Map<String, String> archivosVirtuales) {

        List<ErrorCompilacion> errores = new ArrayList<>();

        ParseTree treePig = parsearPigLatin(nombrePig, codigoPig, errores);
        NodoPrograma astPrincipal = construirAst(treePig, new ConstructorAstPigLatin(),
            NodoPrograma.class, nombrePig, errores);

        if (astPrincipal == null) {
            return resultadoFallido(errores);
        }

        Map<String, ArchivoFuente> archivos = procesarImportaciones(
            astPrincipal, dirBase, archivosVirtuales, nombrePig, errores);

        TablaSimbolos tabla = new TablaSimbolos();
        List<ErrorSemantico> erroresSemanticos = new ArrayList<>();

        procesarFase(archivos, tabla, erroresSemanticos, true);
        procesarFase(archivos, tabla, erroresSemanticos, false);

        ejecutarConContexto(tabla, erroresSemanticos, "Pig Latin", nombrePig,
            () -> astPrincipal.registrarFirmas(tabla, erroresSemanticos));
        ejecutarConContexto(tabla, erroresSemanticos, "Pig Latin", nombrePig,
            () -> astPrincipal.comprobarCuerpos(tabla, erroresSemanticos));

        errores.addAll(erroresSemanticos);

        boolean exitoso = errores.isEmpty();
        ResultadoCompilacion resultado = new ResultadoCompilacion(exitoso, astPrincipal, archivos, tabla, errores);
        resultado.setParseTreePrincipal(treePig);

        if (exitoso) {
            generarCodigoC3D(resultado, astPrincipal, archivos, tabla);
        }

        return resultado;
    }

    private Optional<String> leerContenidoArchivo(Path ruta, List<ErrorCompilacion> errores) {
        if (!Files.exists(ruta) || !Files.isRegularFile(ruta)) {
            errores.add(errorOrquestador(
                "El archivo raíz '" + ruta + "' no existe o no es un archivo válido.", ruta));
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(ruta, StandardCharsets.UTF_8));
        } catch (IOException e) {
            errores.add(errorOrquestador(
                "Error al leer el archivo raíz '" + ruta + "': " + e.getMessage(), ruta));
            return Optional.empty();
        }
    }

    private ErrorSemantico errorOrquestador(String mensaje, Path ruta) {
        return new ErrorSemantico(mensaje, "Orquestador", ruta.getFileName().toString(), 0, 0);
    }

    private ResultadoCompilacion resultadoFallido(List<ErrorCompilacion> errores) {
        return new ResultadoCompilacion(false, null, Collections.emptyMap(), new TablaSimbolos(), errores);
    }

    private ParseTree parsearPigLatin(String nombre, String codigo, List<ErrorCompilacion> errores) {
        piglatinLexer lexer = new piglatinLexer(CharStreams.fromString(codigo));
        configurarListener(lexer::removeErrorListeners, lexer::addErrorListener, nombre, errores);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        piglatinParser parser = new piglatinParser(tokens);
        configurarListener(parser::removeErrorListeners, parser::addErrorListener, nombre, errores);

        return parser.programa();
    }

    @SuppressWarnings("unchecked")
    private <T> T construirAst(ParseTree tree, ParseTreeVisitor<?> builder,
                               Class<T> tipoEsperado, String nombreArchivo, List<ErrorCompilacion> errores) {
        try {
            return tipoEsperado.cast(builder.visit(tree));
        } catch (Exception e) {
            errores.add(new ErrorSemantico(
                "Fallo al construir AST de '" + nombreArchivo + "': " + e.getMessage(),
                "AST", nombreArchivo, 1, 1));
            return null;
        }
    }

    private void configurarListener(Runnable removerListeners, Consumer<CustomErrorListener> agregarListener,
                                    String nombreArchivo, List<ErrorCompilacion> errores) {
        removerListeners.run();
        agregarListener.accept(new CustomErrorListener(nombreArchivo, errores));
    }

    private Map<String, ArchivoFuente> procesarImportaciones(NodoPrograma astPrincipal, Path dirBase,
                                                             Map<String, String> archivosVirtuales, String nombrePig, List<ErrorCompilacion> errores) {

        Map<String, ArchivoFuente> archivos = new LinkedHashMap<>();
        Set<String> importsProcesados = new HashSet<>();

        for (NodoPrograma.InfoImport info : astPrincipal.getImportacionesDetalladas()) {
            GestorImports.ResolucionImport res = GestorImports.resolverRutaImport(info.getRuta());

            if (!res.esValido()) {
                errores.add(new ErrorSemantico(res.getError(), "Importación", nombrePig,
                    info.getLinea(), info.getColumna()));
                continue;
            }

            if (importsProcesados.contains(res.getRutaRelativa())) {
                continue; // ya procesado exitosamente antes
            }

            ArchivoFuente archivo = GestorImports.cargarArchivo(res, dirBase, archivosVirtuales, errores,
                info.getLinea(), info.getColumna(), nombrePig);

            if (archivo == null) {
                continue;
            }

            importsProcesados.add(res.getRutaRelativa());
            procesarSegunLenguaje(archivo, errores);
            archivos.put(res.getRutaRelativa(), archivo);
        }

        return archivos;
    }

    private void procesarSegunLenguaje(ArchivoFuente archivo, List<ErrorCompilacion> errores) {
        if (archivo.esZetariano()) {
            procesarArchivoZetariano(archivo, errores);
        } else if (archivo.esY()) {
            procesarArchivoY(archivo, errores);
        }
    }

    private void procesarArchivoZetariano(ArchivoFuente archivo, List<ErrorCompilacion> errores) {
        String nombre = archivo.getNombreArchivo();

        zetarianoLexer lexer = new zetarianoLexer(CharStreams.fromString(archivo.getContenido()));
        configurarListener(lexer::removeErrorListeners, lexer::addErrorListener, nombre, errores);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        zetarianoParser parser = new zetarianoParser(tokens);
        configurarListener(parser::removeErrorListeners, parser::addErrorListener, nombre, errores);

        ParseTree tree = parser.unidadCompilacion();
        archivo.setParseTree(tree);

        NodoUnidadCompilacionZ astZ = construirAst(tree, new ConstructorAstZetariano(),
            NodoUnidadCompilacionZ.class, nombre, errores);

        if (astZ != null) {
            archivo.setAst(astZ);
            GestorImports.validarNombreClaseZetariano(archivo, astZ, errores);
        }
    }

    private void procesarArchivoY(ArchivoFuente archivo, List<ErrorCompilacion> errores) {
        String nombre = archivo.getNombreArchivo();

        yLexer lexer = new yLexer(CharStreams.fromString(archivo.getContenido()));
        configurarListener(lexer::removeErrorListeners, lexer::addErrorListener, nombre, errores);

        CommonTokenStream tokens = new CommonTokenStream(new YSangriaToken(lexer));
        yParser parser = new yParser(tokens);
        configurarListener(parser::removeErrorListeners, parser::addErrorListener, nombre, errores);

        ParseTree tree = parser.programa();
        archivo.setParseTree(tree);

        NodoProgramaY astY = construirAst(tree, new ConstructorAstY(), NodoProgramaY.class, nombre, errores);
        if (astY != null) {
            archivo.setAst(astY);
        }
    }

    private void procesarFase(Map<String, ArchivoFuente> archivos, TablaSimbolos tabla,
                              List<ErrorSemantico> errores, boolean esRegistroFirmas) {

        for (ArchivoFuente af : archivos.values()) {
            if (af.esY() && af.getAst() instanceof NodoProgramaY ny) {
                Runnable accion = esRegistroFirmas
                    ? () -> ny.registrarFirmas(tabla, errores)
                    : () -> ny.comprobarCuerpos(tabla, errores);
                ejecutarConContexto(tabla, errores, "Y?", af.getNombreArchivo(), accion);
            }
        }

        for (ArchivoFuente af : archivos.values()) {
            if (af.esZetariano() && af.getAst() instanceof NodoUnidadCompilacionZ nz) {
                Runnable accion = esRegistroFirmas
                    ? () -> nz.registrarFirmas(tabla, errores)
                    : () -> nz.comprobarCuerpos(tabla, errores);
                ejecutarConContexto(tabla, errores, "Zetariano", af.getNombreArchivo(), accion);
            }
        }
    }

    private void ejecutarConContexto(TablaSimbolos tabla, List<ErrorSemantico> errores,
                                     String contexto, String archivo, Runnable accion) {
        tabla.setContextoOrigen(contexto, archivo);
        int indiceAntes = errores.size();
        accion.run();
        etiquetarErroresConArchivo(errores, indiceAntes, archivo);
    }

    private void etiquetarErroresConArchivo(List<ErrorSemantico> errores, int indiceInicio, String nombreArchivo) {
        for (int i = indiceInicio; i < errores.size(); i++) {
            ErrorSemantico e = errores.get(i);
            if (e.getArchivo() == null || e.getArchivo().isEmpty()) {
                e.setArchivo(nombreArchivo);
            }
        }
    }


    private void generarCodigoC3D(ResultadoCompilacion resultado, NodoPrograma astPrincipal,
                                  Map<String, ArchivoFuente> archivos, TablaSimbolos tabla) {

        GeneradorC3D genC3D = new GeneradorC3D(tabla);
        List<NodoFuncion> todasLasFunciones = recopilarTodasLasFunciones(archivos);
        List<NodoAST> mainInstrucciones = recopilarInstruccionesPrincipales(astPrincipal);

        String codigoC3D = genC3D.compilarConFunciones(todasLasFunciones, mainInstrucciones);
        resultado.setCodigoC3D(codigoC3D);
        resultado.setContenedorC3D(genC3D.getC3d());
    }

    private List<NodoAST> recopilarInstruccionesPrincipales(NodoPrograma astPrincipal) {
        List<NodoAST> instrucciones = new ArrayList<>();
        if (astPrincipal.getDeclaracionesGlobales() != null) {
            instrucciones.addAll(astPrincipal.getDeclaracionesGlobales());
        }
        if (astPrincipal.getInstruccionesMaior() != null) {
            instrucciones.addAll(astPrincipal.getInstruccionesMaior());
        }
        return instrucciones;
    }

    private List<NodoFuncion> recopilarTodasLasFunciones(Map<String, ArchivoFuente> archivos) {
        List<NodoFuncion> funciones = new ArrayList<>();

        for (ArchivoFuente af : archivos.values()) {
            if (af.esY() && af.getAst() instanceof NodoProgramaY ny && ny.getFunciones() != null) {
                funciones.addAll(ny.getFunciones());
            }
        }

        for (ArchivoFuente af : archivos.values()) {
            if (af.esZetariano() && af.getAst() instanceof NodoUnidadCompilacionZ nz) {
                funciones.addAll(extraerFuncionesDeClases(nz.getClases()));
            }
        }

        return funciones;
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
}