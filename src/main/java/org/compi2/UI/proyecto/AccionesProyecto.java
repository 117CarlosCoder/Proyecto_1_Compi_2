package org.compi2.UI.proyecto;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class AccionesProyecto {

    private final Component padre;

    public AccionesProyecto(Component padre) {
        this.padre = padre;
    }

    public File crearArchivo(File dirDestino) {
        if (dirDestino == null) {
            JOptionPane.showMessageDialog(padre, "Primero abre o crea un proyecto para agregar archivos.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        String nombre = JOptionPane.showInputDialog(
                padre,
                "Nombre del nuevo archivo (ej. Principal.pig, Clases.z, Calculos.y):",
                "Nuevo Archivo en " + dirDestino.getName(),
                JOptionPane.PLAIN_MESSAGE);

        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }

        nombre = nombre.trim();
        if (!nombre.contains(".")) {
            nombre += ".pig";
        }

        File nuevoArchivo = new File(dirDestino, nombre);
        if (nuevoArchivo.exists()) {
            JOptionPane.showMessageDialog(padre, "El archivo ya existe: " + nombre, "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        try {
            if (nuevoArchivo.createNewFile()) {
                return nuevoArchivo;
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(padre, "Error al crear archivo: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    public File crearCarpeta(File dirDestino) {
        if (dirDestino == null) {
            JOptionPane.showMessageDialog(padre, "Primero abre o crea un proyecto para agregar carpetas.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        String nombre = JOptionPane.showInputDialog(
                padre,
                "Nombre de la nueva carpeta:",
                "Nueva Carpeta en " + dirDestino.getName(),
                JOptionPane.PLAIN_MESSAGE);

        if (nombre == null || nombre.trim().isEmpty()) {
            return null;
        }

        File nuevaCarpeta = new File(dirDestino, nombre.trim());
        if (nuevaCarpeta.exists()) {
            JOptionPane.showMessageDialog(padre, "La carpeta ya existe: " + nombre, "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        if (nuevaCarpeta.mkdirs()) {
            return nuevaCarpeta;
        } else {
            JOptionPane.showMessageDialog(padre, "No se pudo crear la carpeta.", "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public File renombrar(File seleccionado) {
        if (seleccionado == null) {
            JOptionPane.showMessageDialog(padre, "Selecciona un archivo o carpeta para renombrar.",
                    "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }

        String nuevoNombre = JOptionPane.showInputDialog(
                padre,
                "Nuevo nombre para '" + seleccionado.getName() + "':",
                seleccionado.getName());

        if (nuevoNombre == null || nuevoNombre.trim().isEmpty() || nuevoNombre.trim().equals(seleccionado.getName())) {
            return null;
        }

        nuevoNombre = nuevoNombre.trim();
        File destino = new File(seleccionado.getParentFile(), nuevoNombre);
        if (destino.exists()) {
            JOptionPane.showMessageDialog(padre, "Ya existe un elemento con el nombre '" + nuevoNombre + "'.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        boolean exito = seleccionado.renameTo(destino);
        if (exito) {
            return destino;
        } else {
            JOptionPane.showMessageDialog(padre, "No se pudo renombrar el elemento.", "Error",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
    }

    public boolean eliminar(File seleccionado) {
        if (seleccionado == null) {
            JOptionPane.showMessageDialog(padre, "Selecciona un archivo o carpeta para eliminar.",
                    "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }

        String tipo = seleccionado.isDirectory() ? "la carpeta y todo su contenido" : "el archivo";
        int resp = JOptionPane.showConfirmDialog(
                padre,
                "¿Estás seguro de que deseas eliminar " + tipo + ":\n" + seleccionado.getName()
                        + "?\nEsta acción no se puede deshacer.",
                "Confirmar Eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (resp != JOptionPane.YES_OPTION) {
            return false;
        }

        boolean exito = eliminarRecursivo(seleccionado);
        if (exito) {
            return true;
        } else {
            JOptionPane.showMessageDialog(padre, "No se pudo eliminar el elemento.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean eliminarRecursivo(File f) {
        if (!f.exists()) {
            return true;
        }
        if (f.isDirectory()) {
            File[] hijos = f.listFiles();
            if (hijos != null) {
                for (File h : hijos) {
                    eliminarRecursivo(h);
                }
            }
        }
        return f.delete();
    }

    public File seleccionarArchivo() {
        JFileChooser ch = new JFileChooser(".");
        ch.setFileFilter(new FileNameExtensionFilter("Archivos del Compilador (*.pig, *.y, *.z, *.txt)", "pig", "y",
                "z", "txt"));
        if (ch.showOpenDialog(padre) == JFileChooser.APPROVE_OPTION) {
            return ch.getSelectedFile();
        }
        return null;
    }

    public File seleccionarArchivoParaGuardar() {
        JFileChooser ch = new JFileChooser(".");
        ch.setFileFilter(new FileNameExtensionFilter("Archivos Pig Latin (*.pig)", "pig"));
        if (ch.showSaveDialog(padre) == JFileChooser.APPROVE_OPTION) {
            return ch.getSelectedFile();
        }
        return null;
    }

    public String leerArchivo(File archivo) throws IOException {
        return Files.readString(archivo.toPath(), StandardCharsets.UTF_8);
    }

    public void guardarArchivo(File archivo, String contenido) throws IOException {
        Files.writeString(archivo.toPath(), contenido, StandardCharsets.UTF_8);
    }
}
