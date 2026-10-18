package org.compi2.c3d;

public class PlantillaC {

    public static String generarCodigoCompleto(ContenedorC3D contenedor, String codigoMain) {
        StringBuilder codigo = new StringBuilder();

        codigo.append("#include <stdio.h>\n\n");
        codigo.append("double stack[100000];\n");
        codigo.append("double heap[100000];\n");
        codigo.append("char buffer_str[1024];\n");
        codigo.append("double P = 0;\n");
        codigo.append("double H = 0;\n\n");

        String temporales = contenedor.getGestorTemporales().generarDeclaracionC();
        if (temporales != null && !temporales.isBlank()) {
            codigo.append(temporales).append("\n\n");
        }

        codigo.append("int main() {\n");
        if (codigoMain != null && !codigoMain.isBlank()) {
            codigo.append(codigoMain);
        }
        codigo.append("    return 0;\n");
        codigo.append("}\n");

        return codigo.toString();
    }
}
