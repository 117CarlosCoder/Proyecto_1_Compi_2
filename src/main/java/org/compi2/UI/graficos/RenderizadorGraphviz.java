package org.compi2.UI.graficos;

import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;

import java.awt.image.BufferedImage;

public class RenderizadorGraphviz {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        System.setProperty("org.slf4j.simpleLogger.log.guru.nidi", "warn");
        try {
            java.util.logging.Logger.getLogger("com.kitfox.svg.Text").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("com.kitfox.svg").setLevel(java.util.logging.Level.OFF);
            java.util.logging.Logger.getLogger("svgSalamandeLogger").setLevel(java.util.logging.Level.OFF);
        } catch (Throwable ignored) {}
    }

    public static BufferedImage renderizarDotAImagen(String codigoDot) {
        if (codigoDot == null || codigoDot.isBlank()) {
            return null;
        }

        try {
            return Graphviz.fromString(codigoDot)
                    .render(Format.PNG)
                    .toImage();
        } catch (Throwable t) {
            System.err.println("No se pudo renderizar el grafo DOT: " + t.getMessage());
            return null;
        }
    }
}
