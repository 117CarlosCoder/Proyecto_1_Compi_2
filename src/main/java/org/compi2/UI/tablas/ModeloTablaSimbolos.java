package org.compi2.UI.tablas;

import org.compi2.analisis_semantico.Simbolo;

import javax.swing.table.DefaultTableModel;
import java.util.List;

public class ModeloTablaSimbolos extends DefaultTableModel {

    private static final String[] ENCABEZADOS = {
        "N°", "Nombre", "Tipo", "Categoría", "Ámbito", "Lenguaje", "Línea", "Columna", "Offset"
    };

    public ModeloTablaSimbolos() {
        super(ENCABEZADOS, 0);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public void actualizarSimbolos(List<Simbolo> simbolos) {
        setRowCount(0);
        if (simbolos == null) return;
        for (Simbolo s : simbolos) {
            addRow(new Object[]{
                s.getId(),
                s.getNombre(),
                s.getTipo() != null ? s.getTipo().getNombreTipo() : "—",
                s.getCategoria(),
                s.getAmbito(),
                s.getLenguaje(),
                s.getLinea(),
                s.getColumna(),
                s.getDesplazamiento()
            });
        }
    }
}
