package org.compi2.UI.tablas;

import org.compi2.UI.util.FuentesUi;
import org.compi2.analisis_semantico.Simbolo;
import org.compi2.errores.ErrorCompilacion;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.util.List;

public class DialogoTabla extends JDialog {

    public DialogoTabla(Frame propietario, String titulo, String[] columnas, Object[][] datos) {
        super(propietario, titulo, true);
        setSize(900, 500);
        setLocationRelativeTo(propietario);

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        DefaultTableModel modelo = new DefaultTableModel(datos, columnas) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(26);
        tabla.setFont(FuentesUi.ui(Font.PLAIN, 13));
        tabla.getTableHeader().setFont(FuentesUi.ui(Font.BOLD, 13));
        tabla.setAutoCreateRowSorter(true);

        DefaultTableCellRenderer centro = new DefaultTableCellRenderer();
        centro.setHorizontalAlignment(SwingConstants.CENTER);

        for (int i = 0; i < tabla.getColumnCount(); i++) {
            String col = columnas[i].toLowerCase();
            if (col.contains("línea") || col.contains("columna") || col.contains("n°") || col.contains("offset")) {
                tabla.getColumnModel().getColumn(i).setCellRenderer(centro);
            }
        }

        panel.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JPanel pie = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JLabel lblTotal = new JLabel("Total de registros: " + datos.length + "   ");
        lblTotal.setFont(FuentesUi.ui(Font.BOLD, 12));
        JButton btnCerrar = new JButton("Cerrar");
        btnCerrar.addActionListener(e -> dispose());
        pie.add(lblTotal);
        pie.add(btnCerrar);
        panel.add(pie, BorderLayout.SOUTH);

        setContentPane(panel);
    }

    public static void mostrarSimbolos(Frame propietario, List<Simbolo> simbolos) {
        String[] cols = {"N°", "Nombre", "Tipo", "Categoría", "Ámbito", "Lenguaje", "Línea", "Columna", "Offset"};
        Object[][] datos = new Object[simbolos != null ? simbolos.size() : 0][cols.length];
        if (simbolos != null) {
            for (int i = 0; i < simbolos.size(); i++) {
                Simbolo s = simbolos.get(i);
                datos[i] = new Object[]{
                    s.getId(),
                    s.getNombre(),
                    s.getTipo() != null ? s.getTipo().getNombreTipo() : "—",
                    s.getCategoria(),
                    s.getAmbito(),
                    s.getLenguaje(),
                    s.getLinea(),
                    s.getColumna(),
                    s.getDesplazamiento()
                };
            }
        }
        new DialogoTabla(propietario, "Tabla de Símbolos", cols, datos).setVisible(true);
    }

    public static void mostrarErrores(Frame propietario, List<ErrorCompilacion> errores) {
        String[] cols = {"N°", "Tipo Error", "Archivo / Origen", "Línea", "Columna", "Descripción del Error"};
        Object[][] datos = new Object[errores != null ? errores.size() : 0][cols.length];
        if (errores != null) {
            for (int i = 0; i < errores.size(); i++) {
                ErrorCompilacion e = errores.get(i);
                datos[i] = new Object[]{
                    i + 1,
                    e.getTipo() != null ? e.getTipo().getEtiqueta() : "—",
                    e.getOrigen() != null ? e.getOrigen() : "—",
                    e.getLinea(),
                    e.getColumna(),
                    e.getMensaje()
                };
            }
        }
        new DialogoTabla(propietario, "Reporte de Errores", cols, datos).setVisible(true);
    }

    public static void mostrarTipos(Frame propietario) {
        String[] cols = {"N°", "Tipo", "Ecosistema / Sintaxis", "Categoría", "Descripción"};
        Object[][] datos = {
            {1, "numerus", "Pig Latin (.pig)", "Primitivo", "Número entero estándar"},
            {2, "decimalis", "Pig Latin (.pig)", "Primitivo", "Número de punto flotante de doble precisión"},
            {3, "textum", "Pig Latin (.pig)", "Primitivo", "Cadena de texto"},
            {4, "littera", "Pig Latin (.pig)", "Primitivo", "Carácter individual"},
            {5, "booleanus", "Pig Latin (.pig)", "Primitivo", "Valor booleano (verum / falsus)"},
            {6, "series T[n]", "Pig Latin (.pig)", "Compuesto", "Arreglo homogéneo de una o más dimensiones"},
            {7, "entero / flotante / caracter / bool / cadena", "Y? (.y)", "Primitivo", "Tipos básicos de Y?"},
            {8, "estructura", "Y? (.y)", "Compuesto", "Registro de campos heterogéneos"},
            {9, "int / double / char / boolean / String", "Zetariano (.z)", "Primitivo / Clase", "Tipos básicos estilo Java"},
            {10, "class", "Zetariano (.z)", "Compuesto", "Clase orientada a objetos con campos, métodos y constructores"}
        };
        new DialogoTabla(propietario, "Catálogo de Tipos del Lenguaje", cols, datos).setVisible(true);
    }
}
