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

    public Cuarteta(TipoOperadorC3D operador, String arg1, String arg2, String resultado, String comentarioIgnorado) {
        this(operador, arg1, arg2, resultado);
    }

    public String toCodigoC() {
        return switch (operador) {
            case ETIQUETA -> resultado + ":";
            case GOTO -> "goto " + resultado + ";";
            case IF_IGUAL, IF_DIFERENTE, IF_MENOR, IF_MENOR_IGUAL, IF_MAYOR, IF_MAYOR_IGUAL ->
                String.format("if (%s %s %s) goto %s;", arg1, operador.getSimbolo(), arg2, resultado);
            case ASIGNACION -> String.format("%s = %s;", resultado, arg1);
            case SUMA, RESTA, MULTIPLICACION, DIVISION ->
                String.format("%s = %s %s %s;", resultado, arg1, operador.getSimbolo(), arg2);
            case MODULO -> String.format("%s = (int)%s %% (int)%s;", resultado, arg1, arg2);
            case MENOS_UNARIO -> String.format("%s = -%s;", resultado, arg1);
            case ACCESO_STACK -> String.format("%s = stack[(int)%s];", resultado, arg1);
            case ASIGNACION_STACK -> String.format("stack[(int)%s] = %s;", resultado, arg1);
            case ACCESO_HEAP -> String.format("%s = heap[(int)%s];", resultado, arg1);
            case ASIGNACION_HEAP -> String.format("heap[(int)%s] = %s;", resultado, arg1);
            case INICIO_FUNCION -> "void " + resultado + "() {";
            case FIN_FUNCION -> "}";
            case CALL -> resultado + "();";
            case RETURN -> "return;";
            case PRINT_INT -> String.format("printf(\"%%d\", (int)%s);", arg1);
            case PRINT_DOUBLE -> String.format("printf(\"%%f\", %s);", arg1);
            case PRINT_CHAR -> String.format("printf(\"%%c\", (char)%s);", arg1);
            case PRINT_STR -> String.format("print_str((int)%s);", arg1);
            case READ_NUM -> (resultado != null) ? String.format("%s = read_num();", resultado) : "read_num();";
            case READ_STR -> (resultado != null) ? String.format("%s = read_str();", resultado) : "read_str();";
            case CONCAT_STR_STR -> String.format("%s = concat_str_str((int)%s, (int)%s);", resultado, arg1, arg2);
            case CONCAT_STR_NUM -> String.format("%s = concat_str_num((int)%s, %s);", resultado, arg1, arg2);
            case CONCAT_NUM_STR -> String.format("%s = concat_num_str(%s, (int)%s);", resultado, arg1, arg2);
            default -> String.format("/* Op no soportado: %s */", operador);
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