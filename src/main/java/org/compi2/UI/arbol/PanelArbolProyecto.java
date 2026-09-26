package org.compi2.UI.arbol;

import lombok.Getter;
import org.compi2.UI.util.FuentesUi;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;

public class PanelArbolProyecto extends JPanel {

    private static final Font FUENTE = FuentesUi.ui(Font.PLAIN, 12);
    private static final Font FUENTE_BOLD = FuentesUi.ui(Font.BOLD, 12);

    private final JTree arbol;
    private final DefaultTreeModel modeloArbol;
    private final JLabel etiquetaRaiz;
    @Getter
    private File carpetaRaiz = null;
    private final Consumer<File> alAbrirArchivo;

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
                if (e.getClickCount() == 2)
                    abrirSeleccion();
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

        JPanel miniBarra = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 3));
        miniBarra.add(crearBotonMini("📁", "Abrir carpeta de proyecto...", e -> seleccionarCarpetaRaiz()));
        miniBarra.add(crearBotonMini("🔄", "Actualizar árbol", e -> refrescar()));
        miniBarra.add(new JSeparator(SwingConstants.VERTICAL) {
            {
                setPreferredSize(new Dimension(1, 18));
            }
        });
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
                if (nodoDir.getChildCount() > 0) {
                    padre.add(nodoDir);
                }
            } else {
                padre.add(new DefaultMutableTreeNode(new NodoArchivo(hijo)));
            }
        }
    }

    private void abrirSeleccion() {
        TreePath ruta = arbol.getSelectionPath();
        if (ruta == null)
            return;
        DefaultMutableTreeNode nodo = (DefaultMutableTreeNode) ruta.getLastPathComponent();
        Object obj = nodo.getUserObject();
        if (!(obj instanceof NodoArchivo na)) {
            seleccionarCarpetaRaiz();
            return;
        }
        if (na.archivo.isDirectory()) {
            if (arbol.isExpanded(ruta))
                arbol.collapsePath(ruta);
            else
                arbol.expandPath(ruta);
        } else if (alAbrirArchivo != null) {
            alAbrirArchivo.accept(na.archivo);
        }
    }

    private void seleccionarCarpetaRaiz() {
        JFileChooser chooser = new JFileChooser(carpetaRaiz != null ? carpetaRaiz : new File("."));
        chooser.setDialogTitle("Abrir carpeta de proyecto");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            this.carpetaRaiz = chooser.getSelectedFile();
            refrescar();
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

    private JButton crearBotonMini(String texto, String tip, ActionListener action) {
        JButton btn = new JButton(texto);
        btn.setFont(FuentesUi.ui(Font.PLAIN, 11));
        btn.setFocusPainted(false);
        btn.setMargin(new Insets(2, 6, 2, 6));
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
            return (archivo.isDirectory() ? "📁 " : "\uD83D\uDCCB") + archivo.getName();
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
