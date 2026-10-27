package org.compi2.orquestador;

import gramaticas.piglatin.piglatinLexer;
import gramaticas.piglatin.piglatinParser;
import gramaticas.y.yLexer;
import gramaticas.y.yParser;
import gramaticas.zetariano.zetarianoLexer;
import gramaticas.zetariano.zetarianoParser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.analisis_semantico.YSangriaToken;
import org.compi2.ast.ModuloSemantico;
import org.compi2.ast.NodoAST;
import org.compi2.ast.sentencias.NodoClase;
import org.compi2.ast.sentencias.NodoFuncion;
import org.compi2.ast.sentencias.NodoPrograma;
import org.compi2.ast.sentencias.NodoProgramaY;
import org.compi2.ast.sentencias.NodoUnidadCompilacionZ;
import org.compi2.c3d.GeneradorC3D;
import org.compi2.riscv.GeneradorRiscV;
import org.compi2.constructores_ast.ConstructorAstPigLatin;
import org.compi2.constructores_ast.ConstructorAstY;
import org.compi2.constructores_ast.ConstructorAstZetariano;
import org.compi2.errores.CustomErrorListener;
import org.compi2.errores.ErrorCompilacion;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class OrquestadorCompilacion {

    public ResultadoCompilacion compilarArchivo(File archivo) {
        return compilarArchivo(archivo.toPath());
    }

    public ResultadoCompilacion compilarArchivo(Path rutaArchivo) {
        List<ErrorCompilacion> errores = new ArrayList<>();
        Optional<String> contenido = leerContenidoArchivo(rutaArchivo, errores);

        if (contenido.isEmpty()) {
            return resultadoFallido(errores);
        }

        Path dirBase = rutaArchivo.toAbsolutePath().getParent();
        String nombreArchivo = rutaArchivo.getFileName().toString();
        return compilar(nombreArchivo, contenido.get(), dirBase, null);
    }

    public ResultadoCompilacion compilarEnMemoria(String codigoPig, Map<String, String> archivosVirtuales) {
        return compilar("principal.pig", codigoPig, null, archivosVirtuales);
    }

    public ResultadoCompilacion compilarEnMemoria(String nombreArchivo, String codigo,
                                                  Map<String, String> archivosVirtuales) {
        return compilar(nombreArchivo, codigo, null, archivosVirtuales);
    }

    public ResultadoCompilacion compilar(String nombre, String codigo, Path dirBase,
                                         Map<String, String> archivosVirtuales) {
        String nombreNormalizado = (nombre != null) ? nombre.toLowerCase(Locale.ROOT) : "";
        if (nombreNormalizado.endsWith(".y")) {
            return compilarModulo(nombre, codigo, TipoLenguaje.Y);
        }
        if (nombreNormalizado.endsWith(".z")) {
            return compilarModulo(nombre, codigo, TipoLenguaje.ZETARIANO);
        }
        return compilarPigLatin(nombre, codigo, dirBase, archivosVirtuales);
    }

    private ResultadoCompilacion compilarPigLatin(String nombrePig, String codigoPig, Path dirBase,
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
        enlazarHerencia(archivos, tabla, erroresSemanticos);
        procesarFase(archivos, tabla, erroresSemanticos, false);

        ejecutarConContexto(tabla, erroresSemanticos, "Pig Latin", nombrePig,
            () -> astPrincipal.registrarEnTablaSimbolos(tabla, erroresSemanticos));
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

    private ResultadoCompilacion compilarModulo(String nombreArchivo, String codigo, TipoLenguaje lenguaje) {
        List<ErrorCompilacion> errores = new ArrayList<>();
        ArchivoFuente modulo = new ArchivoFuente(nombreArchivo, null, nombreArchivo, lenguaje, codigo);
        procesarSegunLenguaje(modulo, errores);

        TablaSimbolos tabla = new TablaSimbolos();
        tabla.setContextoOrigen(lenguaje == TipoLenguaje.Y ? "Y" : "Zetariano", nombreArchivo);

        List<ErrorSemantico> erroresSemanticos = new ArrayList<>();
        if (modulo.getAst() instanceof ModuloSemantico moduloSemantico) {
            moduloSemantico.registrarEnTablaSimbolos(tabla, erroresSemanticos);
            if (modulo.getAst() instanceof NodoUnidadCompilacionZ uZ) {
                uZ.enlazarHerencia(tabla, erroresSemanticos);
            }
            moduloSemantico.comprobarCuerpos(tabla, erroresSemanticos);
        }
        etiquetarErroresConArchivo(erroresSemanticos, 0, nombreArchivo);

        errores.addAll(erroresSemanticos);
        boolean exitoso = errores.isEmpty();
        ResultadoCompilacion resultado = new ResultadoCompilacion(exitoso, null, Collections.emptyMap(), tabla, errores);
        resultado.setParseTreePrincipal(modulo.getParseTree());

        if (exitoso) {
            generarCodigoC3D(resultado, obtenerFuncionesModulo(modulo), Collections.emptyList(), tabla);
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
        configurarListener(lexer, nombre, errores);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        piglatinParser parser = new piglatinParser(tokens);
        configurarListener(parser, nombre, errores);

        return parser.programa();
    }

    private ParseTree parsearY(String nombre, String codigo, List<ErrorCompilacion> errores) {
        yLexer lexer = new yLexer(CharStreams.fromString(codigo));
        configurarListener(lexer, nombre, errores);

        CommonTokenStream tokens = new CommonTokenStream(new YSangriaToken(lexer));
        yParser parser = new yParser(tokens);
        configurarListener(parser, nombre, errores);

        return parser.programa();
    }

    private ParseTree parsearZetariano(String nombre, String codigo, List<ErrorCompilacion> errores) {
        zetarianoLexer lexer = new zetarianoLexer(CharStreams.fromString(codigo));
        configurarListener(lexer, nombre, errores);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        zetarianoParser parser = new zetarianoParser(tokens);
        configurarListener(parser, nombre, errores);

        return parser.unidadCompilacion();
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

    private void configurarListener(Recognizer<?, ?> recognizer, String nombreArchivo, List<ErrorCompilacion> errores) {
        recognizer.removeErrorListeners();
        recognizer.addErrorListener(new CustomErrorListener(nombreArchivo, errores));
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
            if (archivo.esZetariano() && archivo.getAst() instanceof NodoUnidadCompilacionZ programaZ) {
                GestorImports.validarNombreClaseZetariano(archivo, programaZ, errores);
            }
            archivos.put(res.getRutaRelativa(), archivo);
        }

        return archivos;
    }

    private void procesarSegunLenguaje(ArchivoFuente archivo, List<ErrorCompilacion> errores) {
        String nombre = archivo.getNombreArchivo();
        String contenido = archivo.getContenido();

        ParseTree tree;
        ParseTreeVisitor<?> builder;
        Class<? extends NodoAST> tipoEsperado;

        if (archivo.esY()) {
            tree = parsearY(nombre, contenido, errores);
            builder = new ConstructorAstY();
            tipoEsperado = NodoProgramaY.class;
        } else if (archivo.esZetariano()) {
            tree = parsearZetariano(nombre, contenido, errores);
            builder = new ConstructorAstZetariano();
            tipoEsperado = NodoUnidadCompilacionZ.class;
        } else {
            return;
        }

        archivo.setParseTree(tree);
        NodoAST ast = construirAst(tree, builder, tipoEsperado, nombre, errores);
        if (ast != null) {
            archivo.setAst(ast);
        }
    }

    private void procesarFase(Map<String, ArchivoFuente> archivos, TablaSimbolos tabla,
                              List<ErrorSemantico> errores, boolean esFaseRegistro) {
        procesarFaseLenguaje(archivos, tabla, errores, TipoLenguaje.Y, "Y", esFaseRegistro);
        procesarFaseLenguaje(archivos, tabla, errores, TipoLenguaje.ZETARIANO, "Zetariano", esFaseRegistro);
    }

    private void enlazarHerencia(Map<String, ArchivoFuente> archivos, TablaSimbolos tabla,
                                 List<ErrorSemantico> errores) {
        Map<String, NodoClase> mapaClases = new HashMap<>();
        for (ArchivoFuente af : archivos.values()) {
            if (af.esZetariano() && af.getAst() instanceof NodoUnidadCompilacionZ uZ) {
                if (uZ.getClases() != null) {
                    for (NodoClase c : uZ.getClases()) {
                        mapaClases.put(c.getNombre(), c);
                    }
                }
            }
        }

        for (ArchivoFuente af : archivos.values()) {
            if (af.esZetariano() && af.getAst() instanceof NodoUnidadCompilacionZ uZ) {
                ejecutarConContexto(tabla, errores, "Zetariano", af.getNombreArchivo(),
                    () -> uZ.enlazarHerencia(tabla, errores, mapaClases));
            }
        }
    }

    private void procesarFaseLenguaje(Map<String, ArchivoFuente> archivos, TablaSimbolos tabla,
                                      List<ErrorSemantico> errores, TipoLenguaje lenguaje,
                                      String contexto, boolean esFaseRegistro) {
        for (ArchivoFuente af : archivos.values()) {
            if (af.getLenguaje() == lenguaje && af.getAst() instanceof ModuloSemantico modulo) {
                Runnable accion = esFaseRegistro
                    ? () -> modulo.registrarEnTablaSimbolos(tabla, errores)
                    : () -> modulo.comprobarCuerpos(tabla, errores);
                ejecutarConContexto(tabla, errores, contexto, af.getNombreArchivo(), accion);
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

        List<NodoFuncion> todasLasFunciones = recopilarTodasLasFunciones(archivos);
        List<NodoAST> mainInstrucciones = recopilarInstruccionesPrincipales(astPrincipal);
        generarCodigoC3D(resultado, todasLasFunciones, mainInstrucciones, tabla);
    }

    private void generarCodigoC3D(ResultadoCompilacion resultado, List<NodoFuncion> funciones,
                                  List<NodoAST> instrucciones, TablaSimbolos tabla) {
        GeneradorC3D genC3D = new GeneradorC3D(tabla);
        String codigoC3D = genC3D.compilarConFunciones(funciones, instrucciones);
        resultado.setCodigoC3D(codigoC3D);
        resultado.setContenedorC3D(genC3D.getC3d());
        String codigoRiscV = GeneradorRiscV.generarCodigo(genC3D.getC3d());
        resultado.setCodigoRiscV(codigoRiscV);
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
            if (af.esY()) {
                funciones.addAll(obtenerFuncionesModulo(af));
            }
        }

        for (ArchivoFuente af : archivos.values()) {
            if (af.esZetariano()) {
                funciones.addAll(obtenerFuncionesModulo(af));
            }
        }

        return funciones;
    }

    private List<NodoFuncion> obtenerFuncionesModulo(ArchivoFuente modulo) {
        if (modulo.esY() && modulo.getAst() instanceof NodoProgramaY programaY && programaY.getFunciones() != null) {
            return programaY.getFunciones();
        }
        if (modulo.esZetariano() && modulo.getAst() instanceof NodoUnidadCompilacionZ programaZ) {
            return programaZ.aFunciones();
        }
        return Collections.emptyList();
    }
}
