package org.compi2.orquestador;

import lombok.Getter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.ast.sentencias.NodoClase;
import org.compi2.ast.sentencias.NodoUnidadCompilacionZ;
import org.compi2.errores.ErrorCompilacion;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GestorImports {

    @Getter
    public static class ResolucionImport {
        private final String rutaRelativa;
        private final String nombreArchivo;
        private final String nombreBase;
        private final TipoLenguaje lenguaje;
        private final String error;

        public ResolucionImport(String rutaRelativa, String nombreArchivo, String nombreBase,
                                TipoLenguaje lenguaje, String error) {
            this.rutaRelativa = rutaRelativa;
            this.nombreArchivo = nombreArchivo;
            this.nombreBase = nombreBase;
            this.lenguaje = lenguaje;
            this.error = error;
        }

        public boolean esValido() {
            return error == null;
        }

        private static ResolucionImport invalida(String mensajeError) {
            return new ResolucionImport(null, null, null, null, mensajeError);
        }
    }

    public static ResolucionImport resolverRutaImport(String importTexto) {
        if (importTexto == null || importTexto.trim().isEmpty()) {
            return ResolucionImport.invalida("La ruta de importación no puede estar vacía.");
        }

        String limpia = quitarPuntoYComaFinal(importTexto.trim());
        String[] partes = limpia.split("\\.");

        if (partes.length < 2) {
            return ResolucionImport.invalida(
                "Formato de importación inválido: '" + importTexto
                    + "'. Debe incluir nombre de archivo y extensión (.y o .z).");
        }

        String extension = partes[partes.length - 1].trim();
        TipoLenguaje lenguaje = TipoLenguaje.desdeExtension(extension);

        if (lenguaje != TipoLenguaje.Y && lenguaje != TipoLenguaje.ZETARIANO) {
            return ResolucionImport.invalida(
                "Extensión de importación no reconocida: '." + extension
                    + "' (únicamente se permite importar archivos .y o .z).");
        }

        String nombreBase = partes[partes.length - 2].trim();
        String nombreArchivo = nombreBase + "." + extension;
        String rutaRelativa = construirRutaRelativa(partes, extension);

        return new ResolucionImport(rutaRelativa, nombreArchivo, nombreBase, lenguaje, null);
    }

    private static String quitarPuntoYComaFinal(String texto) {
        return texto.endsWith(";") ? texto.substring(0, texto.length() - 1).trim() : texto;
    }

    private static String construirRutaRelativa(String[] partes, String extension) {
        String directorios = Arrays.stream(partes, 0, partes.length - 1)
            .map(String::trim)
            .collect(Collectors.joining("/"));
        return directorios + "." + extension;
    }

    public static ArchivoFuente cargarArchivo(
        ResolucionImport resolucion,
        Path directorioBase,
        Map<String, String> fuentesVirtuales,
        List<ErrorCompilacion> errores,
        int linea,
        int columna,
        String archivoOrigen) {

        if (!resolucion.esValido()) {
            errores.add(new ErrorSemantico(resolucion.getError(), "Importación", archivoOrigen, linea, columna));
            return null;
        }

        String rutaRelativa = resolucion.getRutaRelativa();
        String rutaNormalizada = ArchivoFuente.normalizarSeparadores(rutaRelativa);

        ArchivoFuente encontradoVirtual = buscarEnFuentesVirtuales(rutaNormalizada, resolucion, fuentesVirtuales);
        if (encontradoVirtual != null) {
            return encontradoVirtual;
        }

        ArchivoFuente encontradoEnDisco = buscarEnDisco(
            rutaRelativa, rutaNormalizada, resolucion, directorioBase, errores, archivoOrigen, linea, columna);
        if (encontradoEnDisco != null) {
            return encontradoEnDisco;
        }

        errores.add(new ErrorSemantico(
            "No se encontró el archivo importado '" + rutaRelativa + "'.",
            "Importación", archivoOrigen, linea, columna));
        return null;
    }

    private static ArchivoFuente buscarEnFuentesVirtuales(String rutaNormalizada, ResolucionImport resolucion,
                                                          Map<String, String> fuentesVirtuales) {

        if (fuentesVirtuales == null) {
            return null;
        }

        for (Map.Entry<String, String> entrada : fuentesVirtuales.entrySet()) {
            String claveNormalizada = ArchivoFuente.normalizarSeparadores(entrada.getKey());
            boolean coincide = claveNormalizada.equalsIgnoreCase(rutaNormalizada)
                || claveNormalizada.endsWith("/" + rutaNormalizada)
                || rutaNormalizada.endsWith("/" + claveNormalizada);

            if (coincide) {
                return new ArchivoFuente(
                    rutaNormalizada, null,
                    resolucion.getNombreArchivo(), resolucion.getLenguaje(),
                    entrada.getValue());
            }
        }
        return null;
    }

    private static ArchivoFuente buscarEnDisco(String rutaRelativa, String rutaNormalizada,
                                               ResolucionImport resolucion, Path directorioBase, List<ErrorCompilacion> errores,
                                               String archivoOrigen, int linea, int columna) {

        if (directorioBase == null) {
            return null;
        }

        Path rutaArchivo = directorioBase.resolve(rutaRelativa).normalize();
        if (!Files.exists(rutaArchivo) || !Files.isRegularFile(rutaArchivo)) {
            return null;
        }

        try {
            String contenido = Files.readString(rutaArchivo, StandardCharsets.UTF_8);
            return new ArchivoFuente(
                rutaNormalizada, rutaArchivo,
                resolucion.getNombreArchivo(), resolucion.getLenguaje(),
                contenido);
        } catch (IOException e) {
            errores.add(new ErrorSemantico(
                "Error de E/S al leer el archivo '" + rutaRelativa + "': " + e.getMessage(),
                "Importación", archivoOrigen, linea, columna));
            return null;
        }
    }


    public static void validarNombreClaseZetariano(ArchivoFuente archivo, NodoUnidadCompilacionZ astZ,
                                                   List<ErrorCompilacion> errores) {

        if (archivo == null || astZ == null || !archivo.esZetariano()) {
            return;
        }

        String nombreEsperado = archivo.getNombreBase();
        List<String> nombresClases = new ArrayList<>();
        boolean coincide = false;

        for (NodoClase clase : astZ.getClases()) {
            if (clase == null) continue;

            nombresClases.add(clase.getNombre());
            if (clase.getNombre().equals(nombreEsperado)) {
                coincide = true;
                break;
            }
        }

        if (!coincide) {
            errores.add(new ErrorSemantico(construirMensajeNombreClase(archivo, nombresClases),
                "Clase", archivo.getNombreArchivo(), 1, 1));
        }
    }

    private static String construirMensajeNombreClase(ArchivoFuente archivo, List<String> nombresClases) {
        String clasesEncontradas = nombresClases.isEmpty() ? "ninguna" : String.join(", ", nombresClases);
        return String.format(
            "El nombre del archivo '%s' debe coincidir exactamente con la clase declarada en su interior (declarada: %s).",
            archivo.getNombreArchivo(), clasesEncontradas);
    }
}