package org.compi2.UI.arbol;

import lombok.Getter;
import lombok.Setter;
import org.compi2.UI.util.FuentesUi;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class PanelArbolProyecto extends JPanel {

    private static final Font FUENTE = FuentesUi.ui(Font.PLAIN, 12);
    private static final Font FUENTE_BOLD = FuentesUi.ui(Font.BOLD, 12);

    private final JTree arbol;
    private final DefaultTreeModel modeloArbol;
    private final JLabel etiquetaRaiz;
    @Getter
    private File carpetaRaiz = null;

    private final Consumer<File> alAbrirArchivo;
    @Setter
    private Runnable alGuardarArchivo;
    @Setter
    private Consumer<File> alEliminarArchivo;
    @Setter
    private BiConsumer<File, File> alRenombrarArchivo;
    @Setter
    private BiConsumer<String, Color> notificadorEstado;

    public PanelArbolProyecto(Consumer<File> alAbrirArchivo) {
        super(new BorderLayout());
        this.alAbrirArchivo = alAbrirArchivo;

        DefaultMutableTreeNode raizNodo = new DefaultMutableTreeNode("📁 (Ningún proyecto abierto)");
        modeloArbol = new DefaultTreeModel(raizNodo);
        arbol = new JTree(modeloArbol);
        arbol.setFont(FUENTE);
        arbol.setRowHeight(22);
        arbol.setRootVisible(true);
        arbol.setShowsRootHandles(true);
        arbol.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        arbol.setCellRenderer(new RendererNodoArchivo());
        arbol.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    abrirSeleccion();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                evaluarPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                evaluarPopup(e);
            }
        });

        arbol.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "abrir");
        arbol.getActionMap().put("abrir", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abrirSeleccion();
            }
        });

        etiquetaRaiz = new JLabel("  📂 (Sin proyecto abierto)");
        etiquetaRaiz.setFont(FUENTE_BOLD);
        etiquetaRaiz.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel miniBarra = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 3));
        miniBarra.add(crearBotonMini("📦", "Nuevo Proyecto...", e -> crearNuevoProyecto()));
        miniBarra.add(crearBotonMini("📄+", "Nuevo Archivo en el proyecto...", e -> crearNuevoArchivo()));
        miniBarra.add(crearBotonMini("📁+", "Nueva Carpeta en el proyecto...", e -> crearNuevaCarpeta()));
        miniBarra.add(crearBotonMini("📂", "Abrir carpeta de proyecto...", e -> seleccionarCarpetaRaiz()));

        miniBarra.add(new JSeparator(SwingConstants.VERTICAL) {
            {
                setPreferredSize(new Dimension(1, 18));
            }
        });

        miniBarra.add(crearBotonMini("💾", "Guardar archivo actual", e -> {
            if (alGuardarArchivo != null)
                alGuardarArchivo.run();
        }));
        miniBarra.add(
                crearBotonMini("📥", "Descargar / Exportar proyecto (ZIP)...", e -> descargarOExportarProyectoZip()));

        miniBarra.add(new JSeparator(SwingConstants.VERTICAL) {
            {
                setPreferredSize(new Dimension(1, 18));
            }
        });

        miniBarra.add(crearBotonMini("🔄", "Actualizar árbol", e -> refrescar()));
        miniBarra.add(crearBotonMini("⊞", "Expandir todo", e -> expandirTodo()));
        miniBarra.add(crearBotonMini("⊟", "Colapsar todo", e -> colapsarTodo()));

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.add(etiquetaRaiz, BorderLayout.NORTH);
        cabecera.add(miniBarra, BorderLayout.CENTER);

        add(cabecera, BorderLayout.NORTH);
        add(new JScrollPane(arbol), BorderLayout.CENTER);
    }

    public void setCarpetaRaiz(File carpeta) {
        this.carpetaRaiz = carpeta;
        refrescar();
    }

    public void refrescar() {
        if (carpetaRaiz == null || !carpetaRaiz.exists() || !carpetaRaiz.isDirectory()) {
            DefaultMutableTreeNode vacio = new DefaultMutableTreeNode("📁 (Ningún proyecto abierto)");
            modeloArbol.setRoot(vacio);
            etiquetaRaiz.setText("  📂 (Sin proyecto abierto)");
            return;
        }

        DefaultMutableTreeNode raiz = new DefaultMutableTreeNode(new NodoArchivo(carpetaRaiz));
        poblarNodo(raiz, carpetaRaiz, 0);
        modeloArbol.setRoot(raiz);
        arbol.expandRow(0);
        etiquetaRaiz.setText(" 📂 " + carpetaRaiz.getName());
    }

    private void poblarNodo(DefaultMutableTreeNode padre, File dir, int nivel) {
        if (nivel > 6)
            return;
        File[] hijos = dir.listFiles();
        if (hijos == null)
            return;

        Arrays.sort(hijos, Comparator.<File, Boolean>comparing(f -> !f.isDirectory())
                .thenComparing(f -> f.getName().toLowerCase()));

        for (File hijo : hijos) {
            if (hijo.isHidden())
                continue;
            String n = hijo.getName();
            if (n.equals("target") || n.equals(".git") || n.equals(".idea") || n.equals(".vscode") || n.startsWith("."))
                continue;

            if (hijo.isDirectory()) {
                DefaultMutableTreeNode nodoDir = new DefaultMutableTreeNode(new NodoArchivo(hijo));
                poblarNodo(nodoDir, hijo, nivel + 1);
                padre.add(nodoDir);
            } else {
                padre.add(new DefaultMutableTreeNode(new NodoArchivo(hijo)));
            }
        }
    }

    public void crearNuevoProyecto() {
        JFileChooser chooser = new JFileChooser(carpetaRaiz != null ? carpetaRaiz.getParentFile() : new File("."));
        chooser.setDialogTitle("Selecciona la ubicación donde crear el Nuevo Proyecto");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File carpetaPadre = chooser.getSelectedFile();
        String nombreProyecto = JOptionPane.showInputDialog(
                this,
                "Nombre del nuevo proyecto:",
                "Crear Nuevo Proyecto",
                JOptionPane.PLAIN_MESSAGE);

        if (nombreProyecto == null || nombreProyecto.trim().isEmpty()) {
            return;
        }

        nombreProyecto = nombreProyecto.trim();
        File nuevoProyectoDir = new File(carpetaPadre, nombreProyecto);

        if (nuevoProyectoDir.exists()) {
            JOptionPane.showMessageDialog(this, "Ya existe un directorio con ese nombre en esa ubicación.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!nuevoProyectoDir.mkdirs()) {
            JOptionPane.showMessageDialog(this, "No se pudo crear el directorio del proyecto.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File mainPig = new File(nuevoProyectoDir, "main.pig");
        String plantilla = """
                // Programa Principal de PigLatin
                VARIABILES>
                esto saludo : textum "¡Hola Mundo desde PigLatin!";

                MAIOR>
                >> saludo;
                FINIS;
                """;
        try {
            Files.writeString(mainPig.toPath(), plantilla, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("No se pudo escribir plantilla main.pig: " + e.getMessage());
        }

        setCarpetaRaiz(nuevoProyectoDir);
        if (alAbrirArchivo != null && mainPig.exists()) {
            alAbrirArchivo.accept(mainPig);
        }

        notificar("Proyecto creado: " + nuevoProyectoDir.getName(), new Color(40, 167, 69));
    }

    public void crearNuevoArchivo() {
        File dirDestino = obtenerDirectorioParaNuevosElementos();
        if (dirDestino == null) {
            JOptionPane.showMessageDialog(this, "Primero abre o crea un proyecto para agregar archivos.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String nombre = JOptionPane.showInputDialog(
                this,
                "Nombre del nuevo archivo (ej. Principal.pig, Clases.z, Calculos.y):",
                "Nuevo Archivo en " + dirDestino.getName(),
                JOptionPane.PLAIN_MESSAGE);

        if (nombre == null || nombre.trim().isEmpty()) {
            return;
        }

        nombre = nombre.trim();
        if (!nombre.contains(".")) {
            nombre += ".pig";
        }

        File nuevoArchivo = new File(dirDestino, nombre);
        if (nuevoArchivo.exists()) {
            JOptionPane.showMessageDialog(this, "El archivo ya existe: " + nombre, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (nuevoArchivo.createNewFile()) {
                refrescar();
                if (alAbrirArchivo != null) {
                    alAbrirArchivo.accept(nuevoArchivo);
                }
                notificar("Archivo creado: " + nuevoArchivo.getName(), new Color(40, 167, 69));
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al crear archivo: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void crearNuevaCarpeta() {
        File dirDestino = obtenerDirectorioParaNuevosElementos();
        if (dirDestino == null) {
            JOptionPane.showMessageDialog(this, "Primero abre o crea un proyecto para agregar carpetas.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String nombre = JOptionPane.showInputDialog(
                this,
                "Nombre de la nueva carpeta:",
                "Nueva Carpeta en " + dirDestino.getName(),
                JOptionPane.PLAIN_MESSAGE);

        if (nombre == null || nombre.trim().isEmpty()) {
            return;
        }

        File nuevaCarpeta = new File(dirDestino, nombre.trim());
        if (nuevaCarpeta.exists()) {
            JOptionPane.showMessageDialog(this, "La carpeta ya existe: " + nombre, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (nuevaCarpeta.mkdirs()) {
            refrescar();
            notificar("Carpeta creada: " + nuevaCarpeta.getName(), new Color(40, 167, 69));
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo crear la carpeta.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void renombrarSeleccion() {
        File seleccionado = getArchivoSeleccionado();
        if (seleccionado == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un archivo o carpeta para renombrar.",
                    "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String nuevoNombre = JOptionPane.showInputDialog(
                this,
                "Nuevo nombre para '" + seleccionado.getName() + "':",
                seleccionado.getName());

        if (nuevoNombre == null || nuevoNombre.trim().isEmpty() || nuevoNombre.trim().equals(seleccionado.getName())) {
            return;
        }

        nuevoNombre = nuevoNombre.trim();
        File destino = new File(seleccionado.getParentFile(), nuevoNombre);
        if (destino.exists()) {
            JOptionPane.showMessageDialog(this, "Ya existe un elemento con el nombre '" + nuevoNombre + "'.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean exito = seleccionado.renameTo(destino);
        if (exito) {
            if (seleccionado.equals(carpetaRaiz)) {
                carpetaRaiz = destino;
            }
            if (alRenombrarArchivo != null) {
                alRenombrarArchivo.accept(seleccionado, destino);
            }
            refrescar();
            notificar("Renombrado a: " + destino.getName(), new Color(0, 136, 255));
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo renombrar el elemento.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public void eliminarSeleccion() {
        File seleccionado = getArchivoSeleccionado();
        if (seleccionado == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un archivo o carpeta para eliminar.",
                    "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String tipo = seleccionado.isDirectory() ? "la carpeta y todo su contenido" : "el archivo";
        int resp = JOptionPane.showConfirmDialog(
                this,
                "¿Estás seguro de que deseas eliminar " + tipo + ":\n" + seleccionado.getName()
                        + "?\nEsta acción no se puede deshacer.",
                "Confirmar Eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (resp != JOptionPane.YES_OPTION) {
            return;
        }

        boolean exito = eliminarRecursivo(seleccionado);
        if (exito) {
            if (seleccionado.equals(carpetaRaiz)) {
                carpetaRaiz = null;
            }
            if (alEliminarArchivo != null) {
                alEliminarArchivo.accept(seleccionado);
            }
            refrescar();
            notificar("Eliminado: " + seleccionado.getName(), new Color(220, 53, 69));
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo eliminar el elemento.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean eliminarRecursivo(File f) {
        if (!f.exists())
            return true;
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

    public void descargarOExportarSeleccion() {
        File seleccionado = getArchivoSeleccionado();
        if (seleccionado == null) {
            descargarOExportarProyectoZip();
            return;
        }

        if (seleccionado.isDirectory()) {
            descargarCarpetaZip(seleccionado);
        } else {
            descargarArchivoCopia(seleccionado);
        }
    }

    public void descargarOExportarProyectoZip() {
        if (carpetaRaiz == null || !carpetaRaiz.exists()) {
            JOptionPane.showMessageDialog(this, "No hay ningún proyecto abierto para descargar.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        descargarCarpetaZip(carpetaRaiz);
    }

    private void descargarCarpetaZip(File carpeta) {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Descargar / Exportar Carpeta como ZIP");
        chooser.setSelectedFile(new File(carpeta.getName() + ".zip"));
        chooser.setFileFilter(new FileNameExtensionFilter("Archivos Comprimidos (*.zip)", "zip"));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File destinoZip = chooser.getSelectedFile();
        if (!destinoZip.getName().toLowerCase().endsWith(".zip")) {
            destinoZip = new File(destinoZip.getAbsolutePath() + ".zip");
        }

        try {
            empaquetarEnZip(carpeta, destinoZip);
            JOptionPane.showMessageDialog(this,
                    "Carpeta / Proyecto descargado exitosamente como ZIP en:\n" + destinoZip.getAbsolutePath(),
                    "Descarga Completada", JOptionPane.INFORMATION_MESSAGE);
            notificar("Descargado ZIP: " + destinoZip.getName(), new Color(40, 167, 69));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al generar ZIP: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void descargarArchivoCopia(File archivo) {
        JFileChooser chooser = new JFileChooser(".");
        chooser.setDialogTitle("Descargar / Guardar copia del archivo");
        chooser.setSelectedFile(new File(archivo.getName()));

        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File destino = chooser.getSelectedFile();
        try {
            Files.copy(archivo.toPath(), destino.toPath(), StandardCopyOption.REPLACE_EXISTING);
            JOptionPane.showMessageDialog(this,
                    "Archivo descargado / exportado exitosamente en:\n" + destino.getAbsolutePath(),
                    "Descarga Completada", JOptionPane.INFORMATION_MESSAGE);
            notificar("Descargado archivo: " + destino.getName(), new Color(40, 167, 69));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al descargar archivo: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void empaquetarEnZip(File directorioRaiz, File archivoDestino) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(archivoDestino))) {
            Path rutaBase = directorioRaiz.toPath();
            try (var stream = Files.walk(rutaBase)) {
                stream.forEach(path -> {
                    if (Files.isDirectory(path))
                        return;
                    String entradaRelativa = rutaBase.relativize(path).toString().replace('\\', '/');
                    try {
                        zos.putNextEntry(new ZipEntry(entradaRelativa));
                        Files.copy(path, zos);
                        zos.closeEntry();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        }
    }

    public void abrirEnExplorador() {
        File seleccionado = getArchivoSeleccionado();
        File dir = (seleccionado != null) ? (seleccionado.isDirectory() ? seleccionado : seleccionado.getParentFile())
                : carpetaRaiz;
        if (dir == null || !dir.exists())
            return;
        try {
            Desktop.getDesktop().open(dir);
        } catch (IOException e) {
            System.err.println("No se pudo abrir explorador: " + e.getMessage());
        }
    }

    private void evaluarPopup(MouseEvent e) {
        if (!e.isPopupTrigger())
            return;

        int row = arbol.getClosestRowForLocation(e.getX(), e.getY());
        if (row >= 0) {
            arbol.setSelectionRow(row);
        }

        File seleccionado = getArchivoSeleccionado();
        JPopupMenu popup = new JPopupMenu();

        if (seleccionado != null) {
            boolean esDir = seleccionado.isDirectory();

            JMenuItem itemNuevoArch = new JMenuItem("📄 Nuevo Archivo...");
            itemNuevoArch.addActionListener(ev -> crearNuevoArchivo());
            popup.add(itemNuevoArch);

            JMenuItem itemNuevaCarpeta = new JMenuItem("📁 Nueva Carpeta...");
            itemNuevaCarpeta.addActionListener(ev -> crearNuevaCarpeta());
            popup.add(itemNuevaCarpeta);

            popup.addSeparator();

            JMenuItem itemRenombrar = new JMenuItem("✏ Renombrar...");
            itemRenombrar.addActionListener(ev -> renombrarSeleccion());
            popup.add(itemRenombrar);

            JMenuItem itemEliminar = new JMenuItem("🗑 Eliminar");
            itemEliminar.addActionListener(ev -> eliminarSeleccion());
            popup.add(itemEliminar);

            popup.addSeparator();

            JMenuItem itemDescargar = new JMenuItem(
                    esDir ? "📥 Descargar Carpeta (ZIP)..." : "📥 Descargar / Exportar Copia...");
            itemDescargar.addActionListener(ev -> descargarOExportarSeleccion());
            popup.add(itemDescargar);

            JMenuItem itemExplorador = new JMenuItem("📂 Abrir en Explorador del Sistema");
            itemExplorador.addActionListener(ev -> abrirEnExplorador());
            popup.add(itemExplorador);
        } else {
            JMenuItem itemNuevoProyecto = new JMenuItem("📦 Nuevo Proyecto");
            itemNuevoProyecto.addActionListener(ev -> crearNuevoProyecto());
            popup.add(itemNuevoProyecto);

            JMenuItem itemAbrir = new JMenuItem("📁 Abrir Proyecto");
            itemAbrir.addActionListener(ev -> seleccionarCarpetaRaiz());
            popup.add(itemAbrir);

            if (carpetaRaiz != null) {
                JMenuItem itemDescargarZip = new JMenuItem("📥 Descargar Proyecto Completo (ZIP)");
                itemDescargarZip.addActionListener(ev -> descargarOExportarProyectoZip());
                popup.add(itemDescargarZip);
            }

            popup.addSeparator();

            JMenuItem itemRefrescar = new JMenuItem("🔄 Actualizar Árbol");
            itemRefrescar.addActionListener(ev -> refrescar());
            popup.add(itemRefrescar);
        }

        popup.show(arbol, e.getX(), e.getY());
    }

    public File getArchivoSeleccionado() {
        TreePath ruta = arbol.getSelectionPath();
        if (ruta == null)
            return null;
        DefaultMutableTreeNode nodo = (DefaultMutableTreeNode) ruta.getLastPathComponent();
        Object obj = nodo.getUserObject();
        return (obj instanceof NodoArchivo na) ? na.archivo : null;
    }

    private File obtenerDirectorioParaNuevosElementos() {
        File sel = getArchivoSeleccionado();
        if (sel != null) {
            return sel.isDirectory() ? sel : sel.getParentFile();
        }
        return carpetaRaiz;
    }

    private void abrirSeleccion() {
        File f = getArchivoSeleccionado();
        if (f == null) {
            seleccionarCarpetaRaiz();
            return;
        }
        TreePath ruta = arbol.getSelectionPath();
        if (f.isDirectory()) {
            if (arbol.isExpanded(ruta))
                arbol.collapsePath(ruta);
            else
                arbol.expandPath(ruta);
        } else if (alAbrirArchivo != null) {
            alAbrirArchivo.accept(f);
        }
    }

    public void seleccionarCarpetaRaiz() {
        JFileChooser chooser = new JFileChooser(carpetaRaiz != null ? carpetaRaiz : new File("."));
        chooser.setDialogTitle("Abrir carpeta de proyecto");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            setCarpetaRaiz(chooser.getSelectedFile());
            notificar("Proyecto abierto: " + chooser.getSelectedFile().getName(), new Color(0, 136, 255));
        }
    }

    private void expandirTodo() {
        for (int i = 0; i < arbol.getRowCount(); i++)
            arbol.expandRow(i);
    }

    private void colapsarTodo() {
        for (int i = arbol.getRowCount() - 1; i > 0; i--)
            arbol.collapseRow(i);
    }

    private void notificar(String mensaje, Color color) {
        if (notificadorEstado != null) {
            notificadorEstado.accept(mensaje, color);
        }
    }

    private JButton crearBotonMini(String texto, String tip, ActionListener action) {
        JButton btn = new JButton(texto);
        btn.setFont(FuentesUi.ui(Font.PLAIN, 11));
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(2, 5, 2, 5));
        btn.setToolTipText(tip);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);
        return btn;
    }

    public static class NodoArchivo {
        public final File archivo;

        public NodoArchivo(File archivo) {
            this.archivo = archivo;
        }

        @Override
        public String toString() {
            return (archivo.isDirectory() ? "📁 " : "\uD83D\uDCCB ") + archivo.getName();
        }
    }

    private static class RendererNodoArchivo extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            setFont(FUENTE);
            setIcon(null);
            return this;
        }
    }
}
