package org.compi2.UI.editor;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class NumeroLinea extends JPanel implements CaretListener, DocumentListener, PropertyChangeListener {

    private final JTextComponent editor;
    private final Color colorActivo = new Color(255, 198, 0);
    private final Color colorInactivo = new Color(103, 139, 155);
    private final Color colorFondo = new Color(21, 35, 45);
    private final Color colorBorde = new Color(13, 58, 88);
    private int digitosMinimos = 3;

    public NumeroLinea(JTextComponent editor) {
        this.editor = editor;
        setOpaque(true);
        setBackground(colorFondo);
        setForeground(colorInactivo);
        setFont(editor.getFont());

        setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 0, 1, colorBorde),
                new EmptyBorder(0, 8, 0, 8)
        ));

        editor.getDocument().addDocumentListener(this);
        editor.addCaretListener(this);
        editor.addPropertyChangeListener("font", this);
        editor.addPropertyChangeListener("document", this);

        actualizarAncho();
    }

    private void actualizarAncho() {
        if (editor == null || editor.getDocument() == null) return;

        int totalLineas = editor.getDocument().getDefaultRootElement().getElementCount();
        int digitos = Math.max(String.valueOf(totalLineas).length(), digitosMinimos);

        FontMetrics fm = getFontMetrics(getFont() != null ? getFont() : editor.getFont());
        int ancho = getInsets().left + getInsets().right + (fm.charWidth('9') * digitos);
        int alto = Math.max(editor.getHeight(), totalLineas * fm.getHeight());

        Dimension d = new Dimension(ancho, alto);
        if (!d.equals(getPreferredSize())) {
            setPreferredSize(d);
            setSize(d);
            revalidate();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (editor == null || editor.getDocument() == null) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Rectangle clip = g2.getClipBounds();
            g2.setColor(getBackground());
            g2.fillRect(clip.x, clip.y, clip.width, clip.height);

            Element raiz = editor.getDocument().getDefaultRootElement();
            int total = raiz.getElementCount();
            if (total == 0) return;

            FontMetrics fm = g2.getFontMetrics(getFont() != null ? getFont() : editor.getFont());
            int anchoDisponible = getWidth() - getInsets().left - getInsets().right;

            int inicioOffset = Math.max(0, editor.viewToModel2D(new Point(0, Math.max(0, clip.y))));
            int finOffset = Math.max(0, editor.viewToModel2D(new Point(0, clip.y + clip.height)));

            int lineaInicio = Math.clamp(raiz.getElementIndex(inicioOffset), 0, total - 1);
            int lineaFin = Math.clamp(raiz.getElementIndex(finOffset), lineaInicio, total - 1);
            int lineaCaret = raiz.getElementIndex(editor.getCaretPosition());

            for (int i = lineaInicio; i <= lineaFin; i++) {
                Element elementoLinea = raiz.getElement(i);
                try {
                    Rectangle2D r = editor.modelToView2D(elementoLinea.getStartOffset());
                    if (r == null) continue;

                    String num = String.valueOf(i + 1);
                    int x = getInsets().left + (anchoDisponible - fm.stringWidth(num));
                    int y = (int) Math.round(r.getY()) + fm.getAscent();

                    g2.setColor(i == lineaCaret ? colorActivo : colorInactivo);
                    g2.drawString(num, x, y);
                } catch (BadLocationException e) {
                    break;
                }
            }
        } finally {
            g2.dispose();
        }
    }

    @Override public void caretUpdate(CaretEvent e) { repaint(); }
    @Override public void insertUpdate(DocumentEvent e) { refrescar(); }
    @Override public void removeUpdate(DocumentEvent e) { refrescar(); }
    @Override public void changedUpdate(DocumentEvent e) { refrescar(); }

    private void refrescar() {
        SwingUtilities.invokeLater(() -> {
            actualizarAncho();
            repaint();
        });
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("font".equals(evt.getPropertyName()) && evt.getNewValue() instanceof Font f) {
            setFont(f);
            refrescar();
        } else if ("document".equals(evt.getPropertyName())) {
            if (evt.getOldValue() instanceof Document viejo) viejo.removeDocumentListener(this);
            if (evt.getNewValue() instanceof Document nuevo) nuevo.addDocumentListener(this);
            refrescar();
        }
    }
}
