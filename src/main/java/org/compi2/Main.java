package org.compi2;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        System.setProperty("org.slf4j.simpleLogger.log.guru.nidi", "warn");
        try {
            java.util.logging.Logger.getLogger("com.kitfox.svg.Text").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("com.kitfox.svg").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("svgSalamandeLogger").setLevel(java.util.logging.Level.OFF);
        } catch (Throwable ignored) {}
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme.setup();
                UIManager.put("Button.arc", 8);
                UIManager.put("Component.arc", 8);
                UIManager.put("TextComponent.arc", 6);
                UIManager.put("ScrollBar.showButtons", true);
            } catch (Throwable e) {
                try {
                    com.formdev.flatlaf.FlatDarkLaf.setup();
                } catch (Throwable ex) {
                    System.err.println("No se pudo inicializar FlatLaf: " + ex.getMessage());
                }
            }

            VentanaPrincipal ventana = new VentanaPrincipal();
            ventana.setVisible(true);
        });
    }

}