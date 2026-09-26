package org.compi2.UI.tablas;

import org.compi2.errores.ErrorCompilacion;

import javax.swing.table.DefaultTableModel;
import java.util.List;

public class ModeloTablaErrores extends DefaultTableModel {

    private static final String[] ENCABEZADOS = {
        "N°", "Tipo de Error", "Archivo / Origen", "Descripción", "Línea", "Columna"
    };

    public ModeloTablaErrores() {
        super(ENCABEZADOS, 0);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public void actualizarErrores(List<ErrorCompilacion> errores) {
        setRowCount(0);
        if (errores == null) return;
        int i = 1;
        for (ErrorCompilacion e : errores) {
            addRow(new Object[]{
                i++,
                e.getTipo() != null ? e.getTipo().getEtiqueta() : "—",
                e.getOrigen() != null ? e.getOrigen() : "—",
                e.getMensaje(),
                e.getLinea(),
                e.getColumna()
            });
        }
    }
}
