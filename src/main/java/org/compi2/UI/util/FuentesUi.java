package org.compi2.UI.util;

import java.awt.Font;

public final class FuentesUi {

    private FuentesUi() {}

    public static Font ui(int estilo, int tamano) {
        return new Font(Font.DIALOG, estilo, tamano);
    }

    public static Font mono(int estilo, int tamano) {
        return new Font(Font.MONOSPACED, estilo, tamano);
    }
}
