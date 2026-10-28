package org.compi2.riscv;

import org.compi2.c3d.ContenedorC3D;
import org.compi2.c3d.Cuarteta;
import org.compi2.c3d.TipoOperadorC3D;

import java.util.List;


public class GeneradorRiscV {

    public static final int TAMANO_STACK_BYTES = 65536;
    public static final int TAMANO_HEAP_BYTES = 262144;
    public static final int TAMANO_BUFFER_STR_BYTES = 1024;
    public static final double ESCALA_PUNTO_FIJO = 10000.0;

    public static String generarCodigo(ContenedorC3D contenedor) {
        return new GeneradorRiscV().generar(contenedor);
    }

    public String generar(ContenedorC3D contenedor) {
        if (contenedor == null) {
            return "";
        }

        List<Cuarteta> cuartetas = contenedor.getCuartetas();
        int maxTemp = calcularMaximoIndiceTemporal(contenedor, cuartetas);
        int tamanoTempsBytes = Math.max(65536, (maxTemp + 32) * 4);

        StringBuilder sb = new StringBuilder();

        sb.append("# =========================================================================\n");
        sb.append("# Assembler RISC-V (RV32I / RV32M) - Estandar Simulador Venus\n");
        sb.append("# Generado por Compilador Compi 2\n");
        sb.append("# =========================================================================\n\n");

        sb.append(".data\n");
        sb.append(".align 2\n");
        sb.append("stack:\n");
        sb.append("    .space ").append(TAMANO_STACK_BYTES).append("\n");
        sb.append("buffer_str:\n");
        sb.append("    .space ").append(TAMANO_BUFFER_STR_BYTES).append("\n");
        sb.append("temps:\n");
        sb.append("    .space ").append(tamanoTempsBytes).append("\n");
        sb.append("heap:\n");
        sb.append("    .space ").append(TAMANO_HEAP_BYTES).append("\n\n");

        sb.append(".text\n");
        sb.append(".globl main\n\n");
        sb.append("main:\n");
        sb.append("    li s0, 0           \n");
        sb.append("    li s1, 0           \n");
        sb.append("    la s2, stack       \n");
        sb.append("    la s3, heap        \n");
        sb.append("    la s4, temps       \n");
        sb.append("    la s5, buffer_str  \n\n");

        if (cuartetas == null || cuartetas.isEmpty()) {
            sb.append("L_fin_programa:\n");
            sb.append("    li a0, 10\n");
            sb.append("    ecall\n");
            return sb.toString();
        }

        boolean yaEmitioFinPrograma = false;

        for (int i = 0; i < cuartetas.size(); i++) {
            Cuarteta c = cuartetas.get(i);
            TipoOperadorC3D op = c.getOperador();
            String a1 = c.getArg1();
            String a2 = c.getArg2();
            String res = c.getResultado();

            if (op == TipoOperadorC3D.ETIQUETA) {
                sb.append("\n").append(res).append(":\n");
                if ("L_fin_programa".equals(res)) {
                    yaEmitioFinPrograma = true;
                    sb.append("    li a0, 10\n");
                    sb.append("    ecall\n");
                }
                continue;
            }

            java.util.Set<ContenedorC3D.AccionCuartetaRiscV> acciones = contenedor.obtenerAccionesRiscV(i);
            sb.append("    # ").append(c.toFormatoCuarteta()).append("\n");

            switch (op) {
                case GOTO -> sb.append("    j ").append(res).append("\n");

                case IF_IGUAL -> {
                    cargarOperando(sb, a1, "t0");
                    cargarOperando(sb, a2, "t1");
                    aplicarEscalamientoOperandos(sb, acciones);
                    sb.append("    beq t0, t1, ").append(res).append("\n");
                }
                case IF_DIFERENTE -> {
                    cargarOperando(sb, a1, "t0");
                    cargarOperando(sb, a2, "t1");
                    aplicarEscalamientoOperandos(sb, acciones);
                    sb.append("    bne t0, t1, ").append(res).append("\n");
                }
                case IF_MENOR -> {
                    cargarOperando(sb, a1, "t0");
                    cargarOperando(sb, a2, "t1");
                    aplicarEscalamientoOperandos(sb, acciones);
                    sb.append("    blt t0, t1, ").append(res).append("\n");
                }
                case IF_MENOR_IGUAL -> {
                    cargarOperando(sb, a1, "t0");
                    cargarOperando(sb, a2, "t1");
                    aplicarEscalamientoOperandos(sb, acciones);
                    sb.append("    bge t1, t0, ").append(res).append("\n");
                }
                case IF_MAYOR -> {
                    cargarOperando(sb, a1, "t0");
                    cargarOperando(sb, a2, "t1");
                    aplicarEscalamientoOperandos(sb, acciones);
                    sb.append("    blt t1, t0, ").append(res).append("\n");
                }
                case IF_MAYOR_IGUAL -> {
                    cargarOperando(sb, a1, "t0");
                    cargarOperando(sb, a2, "t1");
                    aplicarEscalamientoOperandos(sb, acciones);
                    sb.append("    bge t0, t1, ").append(res).append("\n");
                }

                case ASIGNACION -> {
                    cargarOperando(sb, a1, "t0");
                    if (acciones.contains(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG1_X10000)) {
                        sb.append("    li t3, 10000\n");
                        sb.append("    mul t0, t0, t3\n");
                    }
                    guardarResultado(sb, res, "t0");
                }

                case SUMA -> {
                    cargarOperando(sb, a1, "t0");
                    cargarOperando(sb, a2, "t1");
                    aplicarEscalamientoOperandos(sb, acciones);
                    sb.append("    add t2, t0, t1\n");
                    guardarResultado(sb, res, "t2");
                }
                case RESTA -> {
                    cargarOperando(sb, a1, "t0");
                    cargarOperando(sb, a2, "t1");
                    aplicarEscalamientoOperandos(sb, acciones);
                    sb.append("    sub t2, t0, t1\n");
                    guardarResultado(sb, res, "t2");
                }
                case MULTIPLICACION -> {
                    cargarOperando(sb, a1, "t0");
                    cargarOperando(sb, a2, "t1");
                    aplicarEscalamientoOperandos(sb, acciones);
                    if (acciones.contains(ContenedorC3D.AccionCuartetaRiscV.MUL_DECIMAL)) {
                        sb.append("    li t6, 10000\n");
                        sb.append("    div t3, t0, t6\n");
                        sb.append("    rem t4, t0, t6\n");
                        sb.append("    div t5, t1, t6\n");
                        sb.append("    rem a2, t1, t6\n");
                        sb.append("    mul t2, t3, t5\n");
                        sb.append("    mul t2, t2, t6\n");
                        sb.append("    mul a3, t3, a2\n");
                        sb.append("    add t2, t2, a3\n");
                        sb.append("    mul a3, t5, t4\n");
                        sb.append("    add t2, t2, a3\n");
                        sb.append("    mul a3, t4, a2\n");
                        sb.append("    div a3, a3, t6\n");
                        sb.append("    add t2, t2, a3\n");
                    } else {
                        sb.append("    mul t2, t0, t1\n");
                    }
                    guardarResultado(sb, res, "t2");
                }
                case DIVISION, DIVISION_ENTERA -> {
                    cargarOperando(sb, a1, "t0");
                    cargarOperando(sb, a2, "t1");
                    aplicarEscalamientoOperandos(sb, acciones);
                    if (acciones.contains(ContenedorC3D.AccionCuartetaRiscV.DIV_DECIMAL)) {
                        sb.append("    div t3, t0, t1\n");
                        sb.append("    rem t4, t0, t1\n");
                        sb.append("    li t5, 100\n");
                        sb.append("    mul t4, t4, t5\n");
                        sb.append("    div a2, t4, t1\n");
                        sb.append("    rem t4, t4, t1\n");
                        sb.append("    mul t4, t4, t5\n");
                        sb.append("    div a3, t4, t1\n");
                        sb.append("    li t6, 10000\n");
                        sb.append("    mul t2, t3, t6\n");
                        sb.append("    mul a2, a2, t5\n");
                        sb.append("    add t2, t2, a2\n");
                        sb.append("    add t2, t2, a3\n");
                    } else {
                        sb.append("    div t2, t0, t1\n");
                    }
                    guardarResultado(sb, res, "t2");
                }
                case MODULO -> {
                    cargarOperando(sb, a1, "t0");
                    cargarOperando(sb, a2, "t1");
                    aplicarEscalamientoOperandos(sb, acciones);
                    sb.append("    rem t2, t0, t1\n");
                    guardarResultado(sb, res, "t2");
                }
                case MENOS_UNARIO -> {
                    cargarOperando(sb, a1, "t0");
                    sb.append("    sub t2, zero, t0\n");
                    guardarResultado(sb, res, "t2");
                }

                case ACCESO_STACK -> {
                    cargarOperando(sb, a1, "t0");
                    sb.append("    slli t0, t0, 2\n");
                    sb.append("    add t0, s2, t0\n");
                    sb.append("    lw t1, 0(t0)\n");
                    guardarResultado(sb, res, "t1");
                }
                case ASIGNACION_STACK -> {
                    cargarOperando(sb, res, "t0");
                    sb.append("    slli t0, t0, 2\n");
                    sb.append("    add t0, s2, t0\n");
                    cargarOperando(sb, a1, "t1");
                    if (acciones.contains(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG1_X10000)) {
                        sb.append("    li t3, 10000\n");
                        sb.append("    mul t1, t1, t3\n");
                    }
                    sb.append("    sw t1, 0(t0)\n");
                }

                case ACCESO_HEAP -> {
                    cargarOperando(sb, a1, "t0");
                    sb.append("    slli t0, t0, 2\n");
                    sb.append("    add t0, s3, t0\n");
                    sb.append("    lw t1, 0(t0)\n");
                    guardarResultado(sb, res, "t1");
                }
                case ASIGNACION_HEAP -> {
                    cargarOperando(sb, res, "t0");
                    sb.append("    slli t0, t0, 2\n");
                    sb.append("    add t0, s3, t0\n");
                    cargarOperando(sb, a1, "t1");
                    if (acciones.contains(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG1_X10000)) {
                        sb.append("    li t3, 10000\n");
                        sb.append("    mul t1, t1, t3\n");
                    }
                    sb.append("    sw t1, 0(t0)\n");
                }

                case ACCESO_BUFFER -> {
                    cargarOperando(sb, a1, "t0");
                    sb.append("    add t0, s5, t0\n");
                    sb.append("    lbu t1, 0(t0)\n");
                    guardarResultado(sb, res, "t1");
                }
                case ASIGNACION_BUFFER -> {
                    cargarOperando(sb, res, "t0");
                    sb.append("    add t0, s5, t0\n");
                    cargarOperando(sb, a1, "t1");
                    sb.append("    sb t1, 0(t0)\n");
                }

                case PRINT_INT -> {
                    sb.append("    li a0, 1\n");
                    cargarOperando(sb, a1, "a1");
                    sb.append("    ecall\n");
                }
                case PRINT_CHAR -> {
                    sb.append("    li a0, 11\n");
                    cargarOperando(sb, a1, "a1");
                    sb.append("    ecall\n");
                }
                case PRINT_DOUBLE -> {
                    String lblPos = "L_pdec_pos_" + i;
                    cargarOperando(sb, a1, "t0");
                    sb.append("    bgez t0, ").append(lblPos).append("\n");
                    sb.append("    li a0, 11\n");
                    sb.append("    li a1, 45\n");
                    sb.append("    ecall\n");
                    sb.append("    sub t0, zero, t0\n");
                    sb.append(lblPos).append(":\n");
                    sb.append("    li t1, 10000\n");
                    sb.append("    div t2, t0, t1\n");
                    sb.append("    rem t3, t0, t1\n");
                    sb.append("    li a0, 1\n");
                    sb.append("    mv a1, t2\n");
                    sb.append("    ecall\n");
                    sb.append("    li a0, 11\n");
                    sb.append("    li a1, 46\n");
                    sb.append("    ecall\n");
                    sb.append("    li t1, 1000\n");
                    sb.append("    div a1, t3, t1\n");
                    sb.append("    rem t3, t3, t1\n");
                    sb.append("    addi a1, a1, 48\n");
                    sb.append("    ecall\n");
                    sb.append("    li t1, 100\n");
                    sb.append("    div a1, t3, t1\n");
                    sb.append("    rem t3, t3, t1\n");
                    sb.append("    addi a1, a1, 48\n");
                    sb.append("    ecall\n");
                    sb.append("    li t1, 10\n");
                    sb.append("    div a1, t3, t1\n");
                    sb.append("    rem t3, t3, t1\n");
                    sb.append("    addi a1, a1, 48\n");
                    sb.append("    ecall\n");
                    sb.append("    addi a1, t3, 48\n");
                    sb.append("    ecall\n");
                }
                case READ_STR -> {
                    sb.append("    li a0, 8\n");
                    sb.append("    mv a1, s5\n");
                    sb.append("    li a2, ").append(TAMANO_BUFFER_STR_BYTES - 1).append("\n");
                    sb.append("    ecall\n");
                    if (res != null && !res.isBlank()) {
                        sb.append("    li t0, 1\n");
                        guardarResultado(sb, res, "t0");
                    }
                }
                case ETIQUETA -> {
                }
            }
        }

        if (!yaEmitioFinPrograma) {
            sb.append("\nL_fin_programa:\n");
            sb.append("    li a0, 10\n");
            sb.append("    ecall\n");
        }

        return sb.toString();
    }

    private void aplicarEscalamientoOperandos(StringBuilder sb, java.util.Set<ContenedorC3D.AccionCuartetaRiscV> acciones) {
        if (acciones.contains(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG1_X10000)) {
            sb.append("    li t3, 10000\n");
            sb.append("    mul t0, t0, t3\n");
        }
        if (acciones.contains(ContenedorC3D.AccionCuartetaRiscV.DESESCALAR_ARG1_DIV_10000)) {
            sb.append("    li t3, 10000\n");
            sb.append("    div t0, t0, t3\n");
        }
        if (acciones.contains(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG2_X10000)) {
            sb.append("    li t3, 10000\n");
            sb.append("    mul t1, t1, t3\n");
        }
    }

    private void cargarOperando(StringBuilder sb, String op, String regDestino) {
        if (op == null || op.isBlank()) {
            sb.append("    li ").append(regDestino).append(", 0\n");
            return;
        }

        op = op.trim();
        if ("P".equals(op)) {
            sb.append("    mv ").append(regDestino).append(", s0\n");
        } else if ("H".equals(op)) {
            sb.append("    mv ").append(regDestino).append(", s1\n");
        } else if (esTemporal(op)) {
            int index = extraerIndiceTemporal(op);
            int offset = index * 4;
            if (offset >= -2048 && offset <= 2047) {
                sb.append("    lw ").append(regDestino).append(", ").append(offset).append("(s4)\n");
            } else {
                sb.append("    li t6, ").append(offset).append("\n");
                sb.append("    add t6, s4, t6\n");
                sb.append("    lw ").append(regDestino).append(", 0(t6)\n");
            }
        } else {
            if (op.contains(".")) {
                try {
                    double val = Double.parseDouble(op);
                    int valEscalado = (int) Math.round(val * ESCALA_PUNTO_FIJO);
                    sb.append("    li ").append(regDestino).append(", ").append(valEscalado).append("\n");
                } catch (NumberFormatException e) {
                    sb.append("    li ").append(regDestino).append(", 0\n");
                }
            } else {
                try {
                    long val = Long.parseLong(op);
                    sb.append("    li ").append(regDestino).append(", ").append(val).append("\n");
                } catch (NumberFormatException e) {
                    sb.append("    li ").append(regDestino).append(", 0\n");
                }
            }
        }
    }

    private void guardarResultado(StringBuilder sb, String res, String regOrigen) {
        if (res == null || res.isBlank()) {
            return;
        }

        res = res.trim();
        if ("P".equals(res)) {
            sb.append("    mv s0, ").append(regOrigen).append("\n");
        } else if ("H".equals(res)) {
            sb.append("    mv s1, ").append(regOrigen).append("\n");
        } else if (esTemporal(res)) {
            int index = extraerIndiceTemporal(res);
            int offset = index * 4;
            if (offset >= -2048 && offset <= 2047) {
                sb.append("    sw ").append(regOrigen).append(", ").append(offset).append("(s4)\n");
            } else {
                sb.append("    li t6, ").append(offset).append("\n");
                sb.append("    add t6, s4, t6\n");
                sb.append("    sw ").append(regOrigen).append(", 0(t6)\n");
            }
        }
    }

    private boolean esTemporal(String op) {
        if (op == null || op.length() < 2 || op.charAt(0) != 't') {
            return false;
        }
        for (int i = 1; i < op.length(); i++) {
            if (!Character.isDigit(op.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private int extraerIndiceTemporal(String op) {
        return Integer.parseInt(op.substring(1));
    }

    private int calcularMaximoIndiceTemporal(ContenedorC3D contenedor, List<Cuarteta> cuartetas) {
        int maxTemp = 0;
        if (contenedor.getGestorTemporales() != null) {
            maxTemp = contenedor.getGestorTemporales().getContador();
        }
        if (cuartetas != null) {
            for (Cuarteta c : cuartetas) {
                maxTemp = Math.max(maxTemp, obtenerIndiceSiEsTemporal(c.getArg1()));
                maxTemp = Math.max(maxTemp, obtenerIndiceSiEsTemporal(c.getArg2()));
                maxTemp = Math.max(maxTemp, obtenerIndiceSiEsTemporal(c.getResultado()));
            }
        }
        return maxTemp;
    }

    private int obtenerIndiceSiEsTemporal(String op) {
        if (esTemporal(op)) {
            return extraerIndiceTemporal(op);
        }
        return 0;
    }
}
