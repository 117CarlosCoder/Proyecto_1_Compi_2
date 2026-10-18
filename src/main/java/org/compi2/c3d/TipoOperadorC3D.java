package org.compi2.c3d;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TipoOperadorC3D {
    SUMA("+"),
    RESTA("-"),
    MULTIPLICACION("*"),
    DIVISION("/"),
    DIVISION_ENTERA("//"),
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

    ACCESO_BUFFER("buffer_get"),
    ASIGNACION_BUFFER("buffer_set"),

    PRINT_INT("print_int"),
    PRINT_DOUBLE("print_double"),
    PRINT_CHAR("print_char"),

    READ_STR("read_str");

    private final String simbolo;
}
