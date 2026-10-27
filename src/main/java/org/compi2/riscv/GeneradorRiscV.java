package org.compi2.riscv;

import org.compi2.c3d.ContenedorC3D;
import org.compi2.c3d.Cuarteta;
import org.compi2.c3d.TipoOperadorC3D;

import java.util.List;


public class GeneradorRiscV {

    public static final int TAMANO_STACK_BYTES = 65536;
    public static final int TAMANO_HEAP_BYTES = 65536;
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
        sb.append("heap:\n");
        sb.append("    .space ").append(TAMANO_HEAP_BYTES).append("\n");
        sb.append("buffer_str:\n");
        sb.append("    .space ").append(TAMANO_BUFFER_STR_BYTES).append("\n");
        sb.append("temps:\n");
        sb.append("    .space ").append(tamanoTempsBytes).append("\n\n");

        sb.append(".text\n");
        sb.append(".globl main\n\n");
        sb.append("main:\n");
        sb.append("    # Inicializacion de registros fijos de memoria y punteros\n");
        sb.append("    li s0, 0           \n");
        sb.append("    li s1, 0          )\n");
        sb.append("    la s2, stack       \n");
        sb.append("    la s3, heap        \n");
        sb.append("    la s4, temps       \n");
        sb.append("    la s5, buffer_str  \n\n");

        if (cuartetas == null || cuartetas.isEmpty()) {
            sb.append("L_fin_programa:\n");
            sb.append("    li a0, 10\n");
            sb.append("    ecall\n");
            sb.append("\n");
            sb.append(generarSubrutinaPrintDecimal());
            return sb.toString();
        }

        boolean yaEmitioFinPrograma = false;

        for (int i = 0; i < cuartetas.size(); i++) {
            ContenedorC3D.RangoRutinaRiscV rango = contenedor.obtenerRutinaRiscVEnInicio(i);
            if (rango != null) {
                if (rango.tipo() == ContenedorC3D.TipoRutinaRiscV.READ_DECIMAL) {
                    sb.append("    # [RUTINA RISC-V] READ_DECIMAL -> ").append(rango.resultado()).append("\n");
                    sb.append("    jal ra, __venus_read_decimal\n");
                    guardarResultado(sb, rango.resultado(), "a0");
                    i = rango.finExclusive() - 1;
                    continue;
                } else if (rango.tipo() == ContenedorC3D.TipoRutinaRiscV.DECIMAL_TO_HEAP) {
                    sb.append("    # [RUTINA RISC-V] DECIMAL_TO_HEAP ").append(rango.arg()).append(" -> ").append(rango.resultado()).append("\n");
                    cargarOperando(sb, rango.arg(), "a1");
                    sb.append("    jal ra, __venus_decimal_to_heap\n");
                    guardarResultado(sb, rango.resultado(), "a0");
                    i = rango.finExclusive() - 1;
                    continue;
                }
            }

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
                        sb.append("    jal ra, __venus_mul_decimal\n");
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
                        sb.append("    jal ra, __venus_div_decimal\n");
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
                    cargarOperando(sb, a1, "a1");
                    sb.append("    jal ra, __venus_print_decimal\n");
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

        sb.append("\n");
        sb.append(generarSubrutinaPrintDecimal());

        return sb.toString();
    }

    private void aplicarEscalamientoOperandos(StringBuilder sb, java.util.Set<ContenedorC3D.AccionCuartetaRiscV> acciones) {
        if (acciones.contains(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG1_X10000)) {
            sb.append("    li t3, 10000\n");
            sb.append("    mul t0, t0, t3\n");
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

    private String generarSubrutinaPrintDecimal() {
        return """
            # =========================================================================
            # Subrutinas Auxiliares RISC-V RV32IM (Simulador Venus)
            # =========================================================================

    
            __venus_print_decimal:
                bgez a1, __vpd_pos
                mv t0, a1
                li a0, 11
                li a1, 45          
                ecall
                sub a1, zero, t0   
            __vpd_pos:
                li t1, 10000
                div t2, a1, t1      
                rem t3, a1, t1      

                
                li a0, 1
                mv a1, t2
                ecall

               
                li a0, 11
                li a1, 46          
                ecall

                
                li t4, 1000
                bge t3, t4, __vpd_print_dec
                li a0, 11
                li a1, 48        
                ecall

                li t4, 100
                bge t3, t4, __vpd_print_dec
                li a0, 11
                li a1, 48    
                ecall

                li t4, 10
                bge t3, t4, __vpd_print_dec
                li a0, 11
                li a1, 48   
                ecall

            __vpd_print_dec:
                li a0, 1
                mv a1, t3
                ecall
                ret


            __venus_read_decimal:
                li a0, 8
                mv a1, s5
                li a2, 1023
                ecall

                mv t0, s5          
                li t1, 1          
                li t2, 0          
                li t3, 0           
                li t4, 1000        

            __vrd_skip_ws:
                lbu t5, 0(t0)
                beqz t5, __vrd_fin_calc
                li t6, 32          
                beq t5, t6, __vrd_avanzar_ws
                li t6, 9           
                beq t5, t6, __vrd_avanzar_ws
                li t6, 13         
                beq t5, t6, __vrd_avanzar_ws
                li t6, 10         
                beq t5, t6, __vrd_avanzar_ws
                j __vrd_check_sign
            __vrd_avanzar_ws:
                addi t0, t0, 1
                j __vrd_skip_ws

            __vrd_check_sign:
                li t6, 43          
                beq t5, t6, __vrd_sign_pos
                li t6, 45        
                beq t5, t6, __vrd_sign_neg
                j __vrd_parse_int
            __vrd_sign_pos:
                addi t0, t0, 1
                j __vrd_parse_int
            __vrd_sign_neg:
                li t1, -1
                addi t0, t0, 1

            __vrd_parse_int:
                lbu t5, 0(t0)
                beqz t5, __vrd_fin_calc
                li t6, 46        
                beq t5, t6, __vrd_dot_found
                li t6, 48     
                blt t5, t6, __vrd_fin_calc
                li t6, 57     
                bgt t5, t6, __vrd_fin_calc

                addi t5, t5, -48
                li t6, 10
                mul t2, t2, t6
                add t2, t2, t5
                addi t0, t0, 1
                j __vrd_parse_int

            __vrd_dot_found:
                addi t0, t0, 1

            __vrd_parse_frac:
                lbu t5, 0(t0)
                beqz t5, __vrd_fin_calc
                li t6, 48         
                blt t5, t6, __vrd_fin_calc
                li t6, 57         
                bgt t5, t6, __vrd_fin_calc

                addi t5, t5, -48
                beqz t4, __vrd_frac_extra
                mul t6, t5, t4
                add t3, t3, t6
                li t6, 10
                div t4, t4, t6
            __vrd_frac_extra:
                addi t0, t0, 1
                j __vrd_parse_frac

            __vrd_fin_calc:
                li t6, 10000
                mul a0, t2, t6
                add a0, a0, t3
                bgez t1, __vrd_ret
                sub a0, zero, a0
            __vrd_ret:
                ret

            __venus_mul_decimal:
                li t3, 1
                bgez t0, __vmd_chk_t1
                sub t0, zero, t0
                li t3, -1
            __vmd_chk_t1:
                bgez t1, __vmd_calc
                sub t1, zero, t1
                sub t3, zero, t3

            __vmd_calc:
                li t6, 10000
                div t4, t0, t6     # q0
                rem t5, t0, t6     # r0
                div a2, t1, t6     # q1
                rem a3, t1, t6     # r1

                mul a4, t4, a2     # q0 * q1
                mul a4, a4, t6     # (q0 * q1) * 10000

                mul a5, t4, a3     # q0 * r1
                mul t0, a2, t5     # q1 * r0
                add a5, a5, t0     # q0 * r1 + q1 * r0

                mul t0, t5, a3     # r0 * r1
                div t0, t0, t6     # (r0 * r1) / 10000

                add t2, a4, a5
                add t2, t2, t0

                bgez t3, __vmd_ret
                sub t2, zero, t2
            __vmd_ret:
                ret

            __venus_div_decimal:
                beqz t1, __vdd_zero
                li t3, 1
                bgez t0, __vdd_chk_t1
                sub t0, zero, t0
                li t3, -1
            __vdd_chk_t1:
                bgez t1, __vdd_calc
                sub t1, zero, t1
                sub t3, zero, t3

            __vdd_calc:
                div t4, t0, t1     # q = t0 / t1
                rem t5, t0, t1     # r = t0 % t1

                li t6, 100
                mul t5, t5, t6     # r1 = r * 100
                div a2, t5, t1     # q1 = r1 / t1
                rem t5, t5, t1     # rem1 = r1 % t1

                mul t5, t5, t6     # r2 = rem1 * 100
                div a3, t5, t1     # q2 = r2 / t1

                li t6, 10000
                mul t4, t4, t6     # q * 10000
                li t6, 100
                mul a2, a2, t6     # q1 * 100
                add t2, t4, a2
                add t2, t2, a3     # t2 = q * 10000 + q1 * 100 + q2

                bgez t3, __vdd_ret
                sub t2, zero, t2
                j __vdd_ret

            __vdd_zero:
                li t2, 0
            __vdd_ret:
                ret

            __venus_decimal_to_heap:
                mv a0, s1          # a0 = inicio heap

                bgez a1, __vdh_extraer_partes
                add t0, s1, s1
                add t0, t0, t0
                add t0, s3, t0
                li t1, 45          # '-'
                sw t1, 0(t0)
                addi s1, s1, 1
                sub a1, zero, a1

            __vdh_extraer_partes:
                li t6, 10000
                div t2, a1, t6     # parte entera
                rem t3, a1, t6     # parte decimal

                bnez t2, __vdh_parte_entera_no_cero
                add t0, s1, s1
                add t0, t0, t0
                add t0, s3, t0
                li t1, 48          # '0'
                sw t1, 0(t0)
                addi s1, s1, 1
                j __vdh_escribir_punto

            __vdh_parte_entera_no_cero:
                li t4, 0
            __vdh_ciclo_div_int:
                beqz t2, __vdh_copiar_digitos_heap
                li t6, 10
                rem t5, t2, t6
                div t2, t2, t6
                addi t5, t5, 48
                add t0, s5, t4
                sb t5, 0(t0)
                addi t4, t4, 1
                j __vdh_ciclo_div_int

            __vdh_copiar_digitos_heap:
            __vdh_ciclo_copia:
                beqz t4, __vdh_escribir_punto
                addi t4, t4, -1
                add t0, s5, t4
                lbu t1, 0(t0)
                add t0, s1, s1
                add t0, t0, t0
                add t0, s3, t0
                sw t1, 0(t0)
                addi s1, s1, 1
                j __vdh_ciclo_copia

            __vdh_escribir_punto:
                add t0, s1, s1
                add t0, t0, t0
                add t0, s3, t0
                li t1, 46          # '.'
                sw t1, 0(t0)
                addi s1, s1, 1

                li t6, 1000
                div t1, t3, t6
                rem t3, t3, t6
                addi t1, t1, 48
                add t0, s1, s1
                add t0, t0, t0
                add t0, s3, t0
                sw t1, 0(t0)
                addi s1, s1, 1

                li t6, 100
                div t1, t3, t6
                rem t3, t3, t6
                addi t1, t1, 48
                add t0, s1, s1
                add t0, t0, t0
                add t0, s3, t0
                sw t1, 0(t0)
                addi s1, s1, 1

                li t6, 10
                div t1, t3, t6
                rem t3, t3, t6
                addi t1, t1, 48
                add t0, s1, s1
                add t0, t0, t0
                add t0, s3, t0
                sw t1, 0(t0)
                addi s1, s1, 1

                addi t1, t3, 48
                add t0, s1, s1
                add t0, t0, t0
                add t0, s3, t0
                sw t1, 0(t0)
                addi s1, s1, 1

                add t0, s1, s1
                add t0, t0, t0
                add t0, s3, t0
                li t1, -1        
                sw t1, 0(t0)
                addi s1, s1, 1

                ret
            """;
    }
}
