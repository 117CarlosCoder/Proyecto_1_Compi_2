package org.compi2.UI.graficos;

import lombok.Getter;
import org.compi2.UI.util.FuentesUi;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;


public class PanelImagenZoom extends JPanel {

    @Getter
    private BufferedImage imagen;
    private double escala = 1.0;
    private int origenX = 0;
    private int origenY = 0;
    private Point puntoRaton;
    private JLabel etiquetaEstado;
    private String mensajeEstado = "El gráfico visual del AST aparecerá despues de compilar";

    public PanelImagenZoom() {
        setBackground(Color.WHITE);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                puntoRaton = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (puntoRaton != null) {
                    origenX += e.getX() - puntoRaton.x;
                    origenY += e.getY() - puntoRaton.y;
                    puntoRaton = e.getPoint();
                    repaint();
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (imagen != null) aplicarZoom(e.getWheelRotation() < 0 ? 1.15 : 0.85);
            }
        };

        addMouseListener(adapter);
        addMouseMotionListener(adapter);
        addMouseWheelListener(adapter);
    }

    public void setImagen(BufferedImage img) {
        this.imagen = img;
        this.escala = 1.0;
        this.origenX = 0;
        this.origenY = 0;
        actualizarEtiqueta();
        repaint();
    }

    public void setMensajeEstado(String msg) {
        this.mensajeEstado = msg;
        repaint();
    }

    public void acercar() { aplicarZoom(1.25); }
    public void alejar() { aplicarZoom(0.8); }
    public void restablecerZoom() {
        this.escala = 1.0;
        this.origenX = 0;
        this.origenY = 0;
        actualizarEtiqueta();
        repaint();
    }

    private void aplicarZoom(double factor) {
        double nueva = escala * factor;
        if (nueva >= 0.1 && nueva <= 6.0) {
            escala = nueva;
            actualizarEtiqueta();
            repaint();
        }
    }

    private void actualizarEtiqueta() {
        if (etiquetaEstado != null) {
            etiquetaEstado.setText("Zoom: " + Math.round(escala * 100) + "%");
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (imagen == null) {
            g.setColor(new Color(100, 100, 100));
            g.setFont(FuentesUi.ui(Font.ITALIC, 14));
            g.drawString(mensajeEstado != null ? mensajeEstado : "", 25, 35);
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = (int) (imagen.getWidth() * escala);
        int h = (int) (imagen.getHeight() * escala);
        int x = origenX + (getWidth() - w) / 2;
        int y = origenY + (getHeight() - h) / 2;

        g2.drawImage(imagen, x, y, w, h, null);
        g2.dispose();
    }
}
