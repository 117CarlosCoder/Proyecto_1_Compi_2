package org.compi2.c3d;

import lombok.Getter;

@Getter
public enum TipoOperadorC3D {
    SUMA("+"),
    RESTA("-"),
    MULTIPLICACION("*"),
    DIVISION("/"),
    MODULO("%"),
    MENOS_UNARIO("-"),

    ASIGNACION("="),

    ETIQUETA("label"),
    GOTO("goto"),
    IF_IGUAL("=="),
    IF_DIFERENTE("!="),
    IF_MENOR("<"),
    IF_MENOR_IGUAL("<="),
    IF_MAYOR(">"),
    IF_MAYOR_IGUAL(">="),

    ACCESO_STACK("stack_get"),
    ASIGNACION_STACK("stack_set"),
    ACCESO_HEAP("heap_get"),
    ASIGNACION_HEAP("heap_set"),

    INICIO_FUNCION("func_begin"),
    FIN_FUNCION("func_end"),
    CALL("call"),
    RETURN("return"),

    PRINT_INT("print_int"),
    PRINT_DOUBLE("print_double"),
    PRINT_CHAR("print_char"),
    PRINT_STR("print_str"),

    READ_NUM("read_num"),
    READ_STR("read_str"),

    CONCAT_STR_STR("concat_str_str"),
    CONCAT_STR_NUM("concat_str_num"),
    CONCAT_NUM_STR("concat_num_str");

    private final String simbolo;

    TipoOperadorC3D(String simbolo) {
        this.simbolo = simbolo;
    }
}
