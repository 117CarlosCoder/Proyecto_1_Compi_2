package org.compi2.UI.editor;

import org.compi2.analisis_semantico.Simbolo;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.util.List;

public class PanelEditorTexto extends JPanel {

    private final JEditorPane editor;
    private static final Font FUENTE_MONO = new Font("Consolas", Font.PLAIN, 14);

    public PanelEditorTexto() {
        setLayout(new BorderLayout());

        editor = new JEditorPane();
        editor.setEditorKit(new EditorKitPlantilla());
        editor.setFont(FUENTE_MONO);
        editor.setBackground(new Color(25, 53, 73));
        editor.setForeground(new Color(255, 255, 255));
        editor.setCaretColor(new Color(255, 198, 0));
        editor.setSelectionColor(new Color(0, 80, 131));
        editor.setSelectedTextColor(Color.WHITE);
        editor.setMargin(new Insets(6, 6, 6, 6));

        editor.getDocument().putProperty(PlainDocument.tabSizeAttribute, 4);

        JScrollPane scroll = new JScrollPane(editor);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(new Color(25, 53, 73));
        scroll.setRowHeaderView(new NumeroLinea(editor));

        add(scroll, BorderLayout.CENTER);
    }

    public String getTexto() {
        return editor.getText();
    }

    public void setTexto(String texto) {
        VistaSintaxis.limpiarColoreado();
        editor.setText(texto != null ? texto : "");
        editor.setCaretPosition(0);
        editor.repaint();
    }

    public void actualizarColoresSimbolos(List<Simbolo> simbolos, String codigoCompleto) {
        VistaSintaxis.actualizarSimbolosSemanticos(simbolos, codigoCompleto);
        editor.repaint();
    }

    public JTextComponent getEditorComponente() {
        return editor;
    }
}
