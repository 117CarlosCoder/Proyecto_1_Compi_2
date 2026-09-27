package org.compi2;

import org.compi2.UI.arbol.PanelArbolProyecto;
import org.compi2.UI.compilacion.TareaCompilacion;
import org.compi2.UI.consola.PanelConsola;
import org.compi2.UI.editor.PanelEditorTexto;
import org.compi2.UI.graficos.PanelImagenZoom;
import org.compi2.UI.graficos.RenderizadorGraphviz;
import org.compi2.UI.tablas.DialogoTabla;
import org.compi2.UI.tablas.ModeloTablaErrores;
import org.compi2.UI.tablas.ModeloTablaSimbolos;
import org.compi2.UI.util.FuentesUi;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.errores.ErrorCompilacion;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class VentanaPrincipal extends JFrame {

    private static final Color COLOR_VERDE = new Color(40, 167, 69);
    private static final Color COLOR_ROJO = new Color(220, 53, 69);
    private static final Color COLOR_AZUL = new Color(0, 136, 255);

    private static final Font FUENTE_UI = FuentesUi.ui(Font.PLAIN, 12);
    private static final Font FUENTE_UI_BOLD = FuentesUi.ui(Font.BOLD, 13);

    private final PanelEditorTexto panelEditor = new PanelEditorTexto();
    private final PanelConsola panelConsola = new PanelConsola();
    private final ModeloTablaSimbolos modeloSimbolos = new ModeloTablaSimbolos();
    private final ModeloTablaErrores modeloErrores = new ModeloTablaErrores();
    private final PanelImagenZoom panelImagenZoom = new PanelImagenZoom();
    private final JTextArea areaCodigoDot = new JTextArea("(Arbol disponible)");
    private final JTextArea areaCodigoC3D = new JTextArea("(Compila para generar el código C3D.)");
    private final JComboBox<String> comboSelectorArbol = new JComboBox<>();
    private final Map<String, String> mapaArbolesDot = new LinkedHashMap<>();
    private final JLabel etiquetaZoom = new JLabel("100%");
    private final JTabbedPane panelPestanas = new JTabbedPane();

    private final JLabel etiquetaEstado = new JLabel("Listo.");
    private final JLabel etiquetaEditor = new JLabel(" 📝 Editor de Código");
    private final JButton botonCompilar = crearBoton("▶  Compilar", COLOR_VERDE, Color.WHITE);
    private final JButton botonDetener = crearBoton("⏹  Detener", COLOR_ROJO, Color.WHITE);
    private final JProgressBar barraProgreso = new JProgressBar();

    private final PanelArbolProyecto panelArbol;
    private JSplitPane splitPrincipal;
    private boolean arbolVisible = true;

    private TablaSimbolos tablaSimbolos = new TablaSimbolos();
    private final List<ErrorCompilacion> listaErrores = new ArrayList<>();
    private final AtomicBoolean ejecucionCancelada = new AtomicBoolean(false);
    private TareaCompilacion tareaActual;
    private File archivoActual;

    public VentanaPrincipal() {
        super("💻 Proyecto 1");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1260, 780);
        setMinimumSize(new Dimension(960, 580));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        areaCodigoDot.setEditable(false);
        areaCodigoDot.setFont(new Font("Consolas", Font.PLAIN, 12));
        areaCodigoDot.setBackground(new Color(25, 53, 73));
        areaCodigoDot.setForeground(new Color(225, 239, 250));
        areaCodigoDot.setCaretColor(new Color(255, 198, 0));
        areaCodigoDot.setMargin(new Insets(8, 10, 8, 10));

        areaCodigoC3D.setEditable(false);
        areaCodigoC3D.setFont(new Font("Consolas", Font.PLAIN, 13));
        areaCodigoC3D.setBackground(new Color(20, 24, 35));
        areaCodigoC3D.setForeground(new Color(230, 240, 255));
        areaCodigoC3D.setCaretColor(new Color(255, 198, 0));
        areaCodigoC3D.setSelectedTextColor(Color.WHITE);
        areaCodigoC3D.setSelectionColor(new Color(40, 80, 130));
        areaCodigoC3D.setMargin(new Insets(10, 12, 10, 12));

        etiquetaZoom.setFont(FUENTE_UI);
        botonDetener.setEnabled(false);
        botonCompilar.addActionListener(this::accionCompilar);
        panelArbol = new PanelArbolProyecto(this::cargarArchivoEnEditor);
        panelArbol.setAlGuardarArchivo(this::guardarArchivo);
        panelArbol.setAlEliminarArchivo(archivoEliminado -> {
            if (archivoActual != null && archivoActual.equals(archivoEliminado)) {
                limpiarEntorno();
            }
        });
        panelArbol.setAlRenombrarArchivo((anterior, nuevo) -> {
            if (archivoActual != null && archivoActual.equals(anterior)) {
                archivoActual = nuevo;
                etiquetaEditor.setText(" 📝 Editor — " + nuevo.getName());
            }
        });
        panelArbol.setNotificadorEstado(this::actualizarEstado);
        barraProgreso.setVisible(false);
        barraProgreso.setPreferredSize(new Dimension(150, 14));

        comboSelectorArbol.setFont(FUENTE_UI);
        comboSelectorArbol.setBackground(new Color(245, 248, 255));
        comboSelectorArbol.setForeground(new Color(20, 30, 45));
        comboSelectorArbol.setPreferredSize(new Dimension(320, 26));
        comboSelectorArbol.addActionListener(e -> cambiarArbolSeleccionado());

        configurarPestanas();
        add(crearBarraSuperior(), BorderLayout.NORTH);
        add(crearDivisorCentral(), BorderLayout.CENTER);
        add(crearBarraInferior(), BorderLayout.SOUTH);

        panelEditor.getEditorComponente().getInputMap().put(KeyStroke.getKeyStroke("ctrl ENTER"), "compilar");
        panelEditor.getEditorComponente().getActionMap().put("compilar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                accionCompilar(e);
            }
        });

        panelEditor.setTexto("");
        actualizarEstado("Listo. Abre un archivo o escribe tu código.", new Color(200, 200, 200));
    }

    private void configurarPestanas() {
        panelPestanas.setFont(FUENTE_UI_BOLD);
        panelPestanas.addTab("🖥 Consola", panelConsola);
        panelPestanas.addTab("📊 Tabla de Símbolos", new JScrollPane(crearTablaEstilizada(modeloSimbolos)));
        panelPestanas.addTab("⚠ Errores", new JScrollPane(crearTablaEstilizada(modeloErrores)));
        panelPestanas.addTab("Código C3D (C)", crearPanelC3D());

        JTabbedPane subAst = new JTabbedPane();
        subAst.addTab("Gráfico Visual", crearPanelGraficoAst());
        subAst.addTab("Código DOT", new JScrollPane(areaCodigoDot));
        panelPestanas.addTab("🌲 AST / ParseTree", subAst);
    }

    private JToolBar crearBarraSuperior() {
        JToolBar b = new JToolBar();
        b.setFloatable(false);
        b.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        b.add(crearBotonSecundario("💻 Proyecto 1", e -> toggleArbol()));
        b.addSeparator(new Dimension(10, 0));

        b.add(botonCompilar);
        b.add(Box.createHorizontalStrut(4));
        b.add(botonDetener);
        b.add(Box.createHorizontalStrut(4));
        b.add(crearBotonSecundario("✕ Limpiar", e -> limpiarEntorno()));
        b.addSeparator();

        b.add(crearBotonSecundario("📦 Nuevo Proy.", e -> panelArbol.crearNuevoProyecto()));
        b.add(crearBotonSecundario("📂 Abrir", e -> abrirArchivo()));
        b.add(crearBotonSecundario("💾 Guardar", e -> guardarArchivo()));
        b.add(crearBotonSecundario("💾 Guardar Como", e -> guardarArchivoComo()));
        b.add(crearBotonSecundario("📥 Descargar ZIP", e -> panelArbol.descargarOExportarProyectoZip()));

        b.add(Box.createGlue());
        b.add(crearBotonSecundario("📊 Símbolos",
                e -> DialogoTabla.mostrarSimbolos(this, tablaSimbolos.obtenerTodosLosSimbolos())));
        b.add(crearBotonSecundario("🔤 Tipos", e -> DialogoTabla.mostrarTipos(this)));
        b.add(crearBotonSecundario("⚠ Errores", e -> DialogoTabla.mostrarErrores(this, listaErrores)));
        b.add(Box.createHorizontalStrut(8));
        return b;
    }

    private JSplitPane crearDivisorCentral() {
        JPanel wrapperEditor = new JPanel(new BorderLayout());
        wrapperEditor.add(etiquetaEditor, BorderLayout.NORTH);
        wrapperEditor.add(panelEditor, BorderLayout.CENTER);

        JSplitPane splitEditorTabs = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, wrapperEditor, panelPestanas);
        splitEditorTabs.setDividerLocation(580);
        splitEditorTabs.setContinuousLayout(true);
        splitEditorTabs.setResizeWeight(0.52);

        splitPrincipal = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelArbol, splitEditorTabs);
        splitPrincipal.setDividerLocation(220);
        splitPrincipal.setContinuousLayout(true);
        return splitPrincipal;
    }

    private JPanel crearBarraInferior() {
        JPanel barra = new JPanel(new BorderLayout());
        barra.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        izq.add(etiquetaEstado);

        JPanel der = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JLabel hint = new JLabel("Ctrl+Enter = Compilar");
        hint.setFont(FUENTE_UI);
        der.add(hint);
        der.add(barraProgreso);

        barra.add(izq, BorderLayout.WEST);
        barra.add(der, BorderLayout.EAST);
        return barra;
    }

    private JPanel crearPanelGraficoAst() {
        JPanel p = new JPanel(new BorderLayout());
        JPanel mini = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JLabel lblVista = new JLabel(" Árbol:");
        lblVista.setFont(FUENTE_UI_BOLD);
        mini.add(lblVista);
        mini.add(comboSelectorArbol);
        mini.add(Box.createHorizontalStrut(8));
        mini.add(crearBotonSecundario("🔍+", e -> {
            panelImagenZoom.acercar();
            etiquetaZoom.setText("Zoom+");
        }));
        mini.add(crearBotonSecundario("🔍-", e -> {
            panelImagenZoom.alejar();
            etiquetaZoom.setText("Zoom-");
        }));
        mini.add(crearBotonSecundario("100%", e -> {
            panelImagenZoom.restablecerZoom();
            etiquetaZoom.setText("100%");
        }));
        mini.add(etiquetaZoom);
        mini.add(Box.createHorizontalStrut(8));
        mini.add(crearBotonSecundario("💾 PNG", e -> guardarImagenAst()));
        mini.add(crearBotonSecundario("💾 DOT", e -> guardarCodigoDot()));

        p.add(mini, BorderLayout.NORTH);
        p.add(panelImagenZoom, BorderLayout.CENTER);
        return p;
    }

    private void cambiarArbolSeleccionado() {
        String seleccion = (String) comboSelectorArbol.getSelectedItem();
        if (seleccion != null && mapaArbolesDot.containsKey(seleccion)) {
            String dot = mapaArbolesDot.get(seleccion);
            areaCodigoDot.setText(dot != null ? dot : "(Sin árbol DOT)");
            renderizarDotEnImagen(dot);
        }
    }

    private void renderizarDotEnImagen(String dot) {
        if (dot != null && !dot.isBlank()) {
            panelImagenZoom.setMensajeEstado("Renderizando árbol con Graphviz...");
            new Thread(() -> {
                BufferedImage img = RenderizadorGraphviz.renderizarDotAImagen(dot);
                SwingUtilities.invokeLater(() -> {
                    if (img != null)
                        panelImagenZoom.setImagen(img);
                    else
                        panelImagenZoom.setMensajeEstado("Graphviz no pudo generar imagen. Revisa pestaña DOT.");
                });
            }).start();
        } else {
            panelImagenZoom.setImagen(null);
        }
    }

    private void toggleArbol() {
        arbolVisible = !arbolVisible;
        panelArbol.setVisible(arbolVisible);
        splitPrincipal.setDividerLocation(arbolVisible ? 220 : 0);
        splitPrincipal.setDividerSize(arbolVisible ? 5 : 0);
    }

    private void accionCompilar(ActionEvent e) {
        String codigo = panelEditor.getTexto().trim();
        if (codigo.isEmpty()) {
            actualizarEstado("⚠ Editor vacío. Escribe o abre un archivo primero.", COLOR_ROJO);
            return;
        }

        limpiarEntornoSinBorrarEditor();
        ejecucionCancelada.set(false);
        botonCompilar.setEnabled(false);
        botonDetener.setEnabled(true);
        barraProgreso.setVisible(true);
        actualizarEstado("⏳ Compilando código...", COLOR_AZUL);

        String nombre = (archivoActual != null) ? archivoActual.getName() : "principal.pig";
        tareaActual = new TareaCompilacion(
                codigo,
                nombre,
                archivoActual,
                panelArbol.getCarpetaRaiz(),
                panelConsola,
                modeloSimbolos,
                modeloErrores,
                this,
                ejecucionCancelada,
                linea -> SwingUtilities.invokeLater(() -> panelConsola.agregarTexto(linea)));
        tareaActual.execute();
    }

    private void detenerEjecucion() {
        ejecucionCancelada.set(true);
        if (tareaActual != null && !tareaActual.isDone())
            tareaActual.cancel(true);
        restablecerControles();
        actualizarEstado("Compilación detenida.", COLOR_ROJO);
    }

    public void notificarFinalizacion(boolean exito, int totalErrores, int totalSimbolos,
            Map<String, String> mapaArboles, String codigoC3D,
            TablaSimbolos nuevaTabla, List<ErrorCompilacion> nuevosErrores) {
        restablecerControles();
        this.tablaSimbolos = (nuevaTabla != null) ? nuevaTabla : new TablaSimbolos();
        this.listaErrores.clear();
        if (nuevosErrores != null)
            this.listaErrores.addAll(nuevosErrores);

        this.mapaArbolesDot.clear();
        this.comboSelectorArbol.removeAllItems();

        if (mapaArboles != null && !mapaArboles.isEmpty()) {
            this.mapaArbolesDot.putAll(mapaArboles);
            for (String clave : mapaArboles.keySet()) {
                this.comboSelectorArbol.addItem(clave);
            }
            if (this.comboSelectorArbol.getItemCount() > 0) {
                this.comboSelectorArbol.setSelectedIndex(0);
            }
        } else {
            areaCodigoDot.setText("(Sin árbol disponible)");
            panelImagenZoom.setImagen(null);
        }

        if (codigoC3D != null && !codigoC3D.isBlank()) {
            areaCodigoC3D.setText(codigoC3D);
            areaCodigoC3D.setCaretPosition(0);
        } else {
            areaCodigoC3D.setText("(Sin código C3D generado debido a errores de compilación)");
        }

        if (exito) {
            panelEditor.actualizarColoresSimbolos(tablaSimbolos.obtenerTodosLosSimbolos(), panelEditor.getTexto());
            panelPestanas.setSelectedIndex(3);
            actualizarEstado(
                    String.format("Compilación exitosa — %d símbolo(s)", totalSimbolos),
                    COLOR_VERDE);
        } else {
            panelImagenZoom.setImagen(null);
            panelPestanas.setSelectedIndex(2);
            actualizarEstado(String.format("Errores detectados (%d)", totalErrores), COLOR_ROJO);
        }
    }

    private JPanel crearPanelC3D() {
        JPanel panel = new JPanel(new BorderLayout());

        JPanel barraAcciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        barraAcciones.setBackground(new Color(32, 40, 54));

        JLabel titulo = new JLabel("Código de Tres Direcciones");
        titulo.setFont(FUENTE_UI_BOLD);
        titulo.setForeground(new Color(225, 235, 250));
        barraAcciones.add(titulo);

        barraAcciones.add(Box.createHorizontalStrut(16));

        JButton btnCopiar = crearBotonSecundario("📋 Copiar C3D", e -> {
            String c3d = areaCodigoC3D.getText();
            if (c3d != null && !c3d.isBlank() && !c3d.startsWith("(Sin")) {
                StringSelection ss = new StringSelection(c3d);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
                actualizarEstado("📋 Código C3D copiado al portapapeles.", COLOR_VERDE);
            } else {
                actualizarEstado("No hay código C3D.", COLOR_ROJO);
            }
        });
        barraAcciones.add(btnCopiar);

        JButton btnExportar = crearBotonSecundario("💾 Exportar archivo .c", e -> exportarCodigoC());
        barraAcciones.add(btnExportar);

        panel.add(barraAcciones, BorderLayout.NORTH);
        panel.add(new JScrollPane(areaCodigoC3D), BorderLayout.CENTER);
        return panel;
    }

    private void exportarCodigoC() {
        String c3d = areaCodigoC3D.getText();
        if (c3d == null || c3d.isBlank() || c3d.startsWith("(Sin")) {
            JOptionPane.showMessageDialog(this, "No hay código C3D para exportar.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Guardar código C3D (.c)");
        fc.setSelectedFile(new File("traduccion_c3d.c"));
        fc.setFileFilter(new FileNameExtensionFilter("Archivos en C (*.c)", "c"));

        int res = fc.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File destino = fc.getSelectedFile();
            if (!destino.getName().toLowerCase().endsWith(".c")) {
                destino = new File(destino.getAbsolutePath() + ".c");
            }
            try {
                Files.writeString(destino.toPath(), c3d, StandardCharsets.UTF_8);
                actualizarEstado("💾 C3D guardado exitosamente en: " + destino.getName(), COLOR_VERDE);
                JOptionPane.showMessageDialog(this,
                        "Archivo .c generado exitosamente en:\n" + destino.getAbsolutePath(), "Exportación Exitosa",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                actualizarEstado("Error al guardar archivo .c: " + ex.getMessage(), COLOR_ROJO);
                JOptionPane.showMessageDialog(this, "Error al escribir el archivo: " + ex.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void restablecerControles() {
        botonCompilar.setEnabled(true);
        botonDetener.setEnabled(false);
        barraProgreso.setVisible(false);
        tareaActual = null;
    }

    private void limpiarEntorno() {
        limpiarEntornoSinBorrarEditor();
        panelEditor.setTexto("");
        archivoActual = null;
        etiquetaEditor.setText(" 📝 Editor de Código");
        actualizarEstado("Entorno limpio.", new Color(120, 120, 120));
    }

    private void limpiarEntornoSinBorrarEditor() {
        panelConsola.limpiar();
        listaErrores.clear();
        tablaSimbolos = new TablaSimbolos();
        modeloSimbolos.setRowCount(0);
        modeloErrores.setRowCount(0);
        mapaArbolesDot.clear();
        comboSelectorArbol.removeAllItems();
        areaCodigoDot.setText("(Árbol no disponible)");
        areaCodigoC3D.setText("(Compila para generar el código C3D");
        panelImagenZoom.setImagen(null);
        panelImagenZoom.setMensajeEstado("El árbol aparecerá aquí tras compilar");
    }

    private void actualizarEstado(String msg, Color c) {
        etiquetaEstado.setText(msg);
        etiquetaEstado.setForeground(c);
    }

    private void cargarArchivoEnEditor(File archivo) {
        try {
            String texto = Files.readString(archivo.toPath(), StandardCharsets.UTF_8);
            archivoActual = archivo;
            panelEditor.setTexto(texto);
            if (tablaSimbolos != null) {
                panelEditor.actualizarColoresSimbolos(tablaSimbolos.obtenerTodosLosSimbolos(), texto);
            }
            etiquetaEditor.setText(" 📝 Editor — " + archivo.getName());
            actualizarEstado("Abierto: " + archivo.getName(), COLOR_AZUL);
        } catch (IOException e) {
            actualizarEstado("Error al leer archivo.", COLOR_ROJO);
        }
    }

    private void abrirArchivo() {
        JFileChooser ch = new JFileChooser(".");
        ch.setFileFilter(new FileNameExtensionFilter("Archivos del Compilador (*.pig, *.y, *.z, *.txt)", "pig", "y",
                "z", "txt"));
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            cargarArchivoEnEditor(ch.getSelectedFile());
    }

    private void guardarArchivo() {
        if (archivoActual == null) {
            guardarArchivoComo();
            return;
        }
        try {
            Files.writeString(archivoActual.toPath(), panelEditor.getTexto(), StandardCharsets.UTF_8);
            actualizarEstado("Guardado: " + archivoActual.getName(), COLOR_VERDE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void guardarArchivoComo() {
        JFileChooser ch = new JFileChooser(".");
        ch.setFileFilter(new FileNameExtensionFilter("Archivos Pig Latin (*.pig)", "pig"));
        if (ch.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            archivoActual = ch.getSelectedFile();
            guardarArchivo();
            etiquetaEditor.setText(" 📝 Editor — " + archivoActual.getName());
        }
    }

    private void guardarImagenAst() {
        BufferedImage img = panelImagenZoom.getImagen();
        if (img == null) {
            JOptionPane.showMessageDialog(this, "No hay imagen disponible.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser ch = new JFileChooser(".");
        if (ch.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = ch.getSelectedFile();
                if (!f.getName().toLowerCase().endsWith(".png"))
                    f = new File(f.getAbsolutePath() + ".png");
                javax.imageio.ImageIO.write(img, "png", f);
                actualizarEstado("Imagen guardada.", COLOR_VERDE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al guardar imagen: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void guardarCodigoDot() {
        String dot = areaCodigoDot.getText();
        if (dot.isBlank() || dot.startsWith("("))
            return;
        JFileChooser ch = new JFileChooser(".");
        if (ch.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File f = ch.getSelectedFile();
                if (!f.getName().toLowerCase().endsWith(".dot"))
                    f = new File(f.getAbsolutePath() + ".dot");
                Files.writeString(f.toPath(), dot, StandardCharsets.UTF_8);
                actualizarEstado("DOT guardado.", COLOR_VERDE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error al guardar DOT: " + e.getMessage(), "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JTable crearTablaEstilizada(javax.swing.table.TableModel m) {
        JTable t = new JTable(m);
        t.setFont(FUENTE_UI);
        t.setRowHeight(26);
        t.setAutoCreateRowSorter(true);
        JTableHeader h = t.getTableHeader();
        h.setFont(FUENTE_UI_BOLD);
        return t;
    }

    private JButton crearBoton(String txt, Color bg, Color fg) {
        JButton b = new JButton(txt);
        b.setFont(FUENTE_UI_BOLD);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setMargin(new Insets(6, 14, 6, 14));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Color hover = bg.darker();
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (b.isEnabled())
                    b.setBackground(hover);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setBackground(bg);
            }
        });
        return b;
    }

    private JButton crearBotonSecundario(String txt, java.awt.event.ActionListener al) {
        JButton b = new JButton(txt);
        b.setFont(FUENTE_UI);
        b.setFocusPainted(false);
        b.setMargin(new Insets(4, 10, 4, 10));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(al);
        return b;
    }
}
