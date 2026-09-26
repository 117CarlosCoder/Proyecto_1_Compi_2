package org.compi2.UI.consola;

import lombok.Getter;
import org.compi2.UI.util.FuentesUi;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

@Getter
public class PanelConsola extends JPanel {

    private final JTextArea areaConsola;

    public PanelConsola() {
        setLayout(new BorderLayout());

        areaConsola = new JTextArea();
        areaConsola.setFont(FuentesUi.mono(Font.BOLD, 14));
        areaConsola.setBackground(new Color(18, 39, 56));
        areaConsola.setForeground(new Color(42, 255, 223));
        areaConsola.setCaretColor(new Color(255, 198, 0));
        areaConsola.setEditable(false);
        areaConsola.setMargin(new Insets(8, 10, 8, 10));

        JScrollPane scroll = new JScrollPane(areaConsola);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(new Color(18, 39, 56));

        add(scroll, BorderLayout.CENTER);
    }

    public void agregarTexto(String texto) {
        if (texto != null) {
            areaConsola.append(texto);
            areaConsola.setCaretPosition(areaConsola.getDocument().getLength());
        }
    }

    public void limpiar() {
        areaConsola.setText("");
    }

}
