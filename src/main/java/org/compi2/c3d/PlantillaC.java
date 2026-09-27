package org.compi2.c3d;

public class PlantillaC {

    public static String generarCodigoCompleto(ContenedorC3D contenedor, String codigoFunciones, String codigoMain) {
        StringBuilder sb = new StringBuilder();

        sb.append("#include <stdio.h>\n");
        sb.append("#include <math.h>\n\n");
        sb.append("#define null 0\n\n");

        sb.append("double stack[100000];\n");
        sb.append("double heap[100000];\n");
        sb.append("double P = 0;\n");
        sb.append("double H = 0;\n\n");

        String declTemps = contenedor.getGestorTemporales().generarDeclaracionC();
        if (declTemps != null && !declTemps.isBlank()) {
            sb.append(declTemps).append("\n\n");
        }

        sb.append("void print_str(int ptr) {\n");
        sb.append("    while ((int)heap[ptr] != -1 && (int)heap[ptr] != 0) {\n");
        sb.append("        printf(\"%c\", (char)heap[ptr]);\n");
        sb.append("        ptr++;\n");
        sb.append("    }\n");
        sb.append("}\n\n");

        sb.append("double read_num() {\n");
        sb.append("    double val = 0;\n");
        sb.append("    char buffer[256];\n");
        sb.append("    if (scanf(\"%255s\", buffer) == 1) {\n");
        sb.append("        if (sscanf(buffer, \"%lf\", &val) != 1) {\n");
        sb.append("            val = (double)buffer[0];\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("    return val;\n");
        sb.append("}\n\n");

        sb.append("int read_str() {\n");
        sb.append("    char buf[1024];\n");
        sb.append("    if (scanf(\"%1023s\", buf) != 1) {\n");
        sb.append("        int ptr = (int)H;\n");
        sb.append("        heap[(int)H] = -1;\n");
        sb.append("        H = H + 1;\n");
        sb.append("        return ptr;\n");
        sb.append("    }\n");
        sb.append("    int ptr = (int)H;\n");
        sb.append("    for (int i = 0; buf[i] != '\\0'; i++) {\n");
        sb.append("        heap[(int)H] = (double)buf[i];\n");
        sb.append("        H = H + 1;\n");
        sb.append("    }\n");
        sb.append("    heap[(int)H] = -1;\n");
        sb.append("    H = H + 1;\n");
        sb.append("    return ptr;\n");
        sb.append("}\n\n");

        sb.append("int concat_str_str(int p1, int p2) {\n");
        sb.append("    int res = (int)H;\n");
        sb.append("    while ((int)heap[p1] != -1 && (int)heap[p1] != 0) {\n");
        sb.append("        heap[(int)H] = heap[p1];\n");
        sb.append("        H = H + 1;\n");
        sb.append("        p1++;\n");
        sb.append("    }\n");
        sb.append("    while ((int)heap[p2] != -1 && (int)heap[p2] != 0) {\n");
        sb.append("        heap[(int)H] = heap[p2];\n");
        sb.append("        H = H + 1;\n");
        sb.append("        p2++;\n");
        sb.append("    }\n");
        sb.append("    heap[(int)H] = -1;\n");
        sb.append("    H = H + 1;\n");
        sb.append("    return res;\n");
        sb.append("}\n\n");

        sb.append("int concat_str_num(int p1, double num) {\n");
        sb.append("    int res = (int)H;\n");
        sb.append("    while ((int)heap[p1] != -1 && (int)heap[p1] != 0) {\n");
        sb.append("        heap[(int)H] = heap[p1];\n");
        sb.append("        H = H + 1;\n");
        sb.append("        p1++;\n");
        sb.append("    }\n");
        sb.append("    char buf[64];\n");
        sb.append("    if (floor(num) == num) {\n");
        sb.append("        sprintf(buf, \"%ld\", (long)num);\n");
        sb.append("    } else {\n");
        sb.append("        sprintf(buf, \"%g\", num);\n");
        sb.append("    }\n");
        sb.append("    for (int i = 0; buf[i] != '\\0'; i++) {\n");
        sb.append("        heap[(int)H] = (double)buf[i];\n");
        sb.append("        H = H + 1;\n");
        sb.append("    }\n");
        sb.append("    heap[(int)H] = -1;\n");
        sb.append("    H = H + 1;\n");
        sb.append("    return res;\n");
        sb.append("}\n\n");

        sb.append("int concat_num_str(double num, int p2) {\n");
        sb.append("    int res = (int)H;\n");
        sb.append("    char buf[64];\n");
        sb.append("    if (floor(num) == num) {\n");
        sb.append("        sprintf(buf, \"%ld\", (long)num);\n");
        sb.append("    } else {\n");
        sb.append("        sprintf(buf, \"%g\", num);\n");
        sb.append("    }\n");
        sb.append("    for (int i = 0; buf[i] != '\\0'; i++) {\n");
        sb.append("        heap[(int)H] = (double)buf[i];\n");
        sb.append("        H = H + 1;\n");
        sb.append("    }\n");
        sb.append("    while ((int)heap[p2] != -1 && (int)heap[p2] != 0) {\n");
        sb.append("        heap[(int)H] = heap[p2];\n");
        sb.append("        H = H + 1;\n");
        sb.append("        p2++;\n");
        sb.append("    }\n");
        sb.append("    heap[(int)H] = -1;\n");
        sb.append("    H = H + 1;\n");
        sb.append("    return res;\n");
        sb.append("}\n\n");

        if (codigoFunciones != null && !codigoFunciones.isBlank()) {
            sb.append(codigoFunciones).append("\n");
        }

        sb.append("int main() {\n");
        if (codigoMain != null && !codigoMain.isBlank()) {
            sb.append(codigoMain);
        }
        sb.append("    return 0;\n");
        sb.append("}\n");

        return sb.toString();
    }
}
