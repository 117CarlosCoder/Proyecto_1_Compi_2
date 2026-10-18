package org.compi2.c3d;

import lombok.Getter;

@Getter
public class Cuarteta {
    private final TipoOperadorC3D operador;
    private final String arg1;
    private final String arg2;
    private final String resultado;

    public Cuarteta(TipoOperadorC3D operador, String arg1, String arg2, String resultado) {
        this.operador = operador;
        this.arg1 = (arg1 != null) ? arg1.trim() : null;
        this.arg2 = (arg2 != null) ? arg2.trim() : null;
        this.resultado = (resultado != null) ? resultado.trim() : null;
    }

    public String toCodigoC() {
        return switch (operador) {
            case ETIQUETA -> resultado + ":";
            case GOTO -> "goto " + resultado + ";";
            case IF_IGUAL, IF_DIFERENTE, IF_MENOR, IF_MENOR_IGUAL, IF_MAYOR, IF_MAYOR_IGUAL ->
                "if (" + arg1 + " " + operador.getSimbolo() + " " + arg2 + ") goto " + resultado + ";";
            case ASIGNACION -> resultado + " = " + arg1 + ";";
            case SUMA, RESTA, MULTIPLICACION, DIVISION ->
                resultado + " = " + arg1 + " " + operador.getSimbolo() + " " + arg2 + ";";
            case DIVISION_ENTERA -> resultado + " = (int)" + arg1 + " / (int)" + arg2 + ";";
            case MODULO -> resultado + " = (int)" + arg1 + " % (int)" + arg2 + ";";
            case MENOS_UNARIO -> resultado + " = -" + arg1 + ";";
            case ACCESO_STACK -> resultado + " = stack[(int)" + arg1 + "];";
            case ASIGNACION_STACK -> "stack[(int)" + resultado + "] = " + arg1 + ";";
            case ACCESO_HEAP -> resultado + " = heap[(int)" + arg1 + "];";
            case ASIGNACION_HEAP -> "heap[(int)" + resultado + "] = " + arg1 + ";";
            case ACCESO_BUFFER -> resultado + " = (double)buffer_str[(int)" + arg1 + "];";
            case ASIGNACION_BUFFER -> "buffer_str[(int)" + resultado + "] = (char)" + arg1 + ";";
            case PRINT_INT -> "printf(\"%d\", (int)" + arg1 + ");";
            case PRINT_DOUBLE -> "printf(\"%f\", " + arg1 + ");";
            case PRINT_CHAR -> "printf(\"%c\", (char)" + arg1 + ");";
            case READ_STR -> {
                String lectura = "scanf(\"%1023s\", buffer_str);";
                if (resultado != null) {
                    yield resultado + " = (double)" + lectura;
                }
                yield lectura;
            }
        };
    }

    public String toFormatoCuarteta() {
        String op = (operador != null) ? operador.getSimbolo() : "-";
        String a1 = (arg1 != null) ? arg1 : "-";
        String a2 = (arg2 != null) ? arg2 : "-";
        String res = (resultado != null) ? resultado : "-";
        return String.format("(%-6s, %-10s, %-10s, %-10s)", op, a1, a2, res);
    }

    @Override
    public String toString() {
        return toFormatoCuarteta();
    }
}
