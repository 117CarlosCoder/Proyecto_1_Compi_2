package org.compi2.riscv;

import lombok.Getter;
import lombok.Setter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;


@Getter
@Setter
public class SimuladorRiscV {

    public static final int LIMITE_INSTRUCCIONES_DEFAULT = 50_000_000;

    private final int[] registros = new int[32];

    private final byte[][] paginasMemoria = new byte[65536][];

    private final Map<String, Integer> etiquetasDatos = new HashMap<>();
    private final Map<String, Integer> etiquetasTexto = new HashMap<>();

    private final List<Instruccion> instrucciones = new ArrayList<>();

    private int pc = 0;
    private long pasosEjecutados = 0;
    private int limiteInstrucciones = LIMITE_INSTRUCCIONES_DEFAULT;
    private volatile boolean detenido = false;
    private boolean finalizado = false;
    private Process procesoExterno;

    private Consumer<String> consumidorSalida = s -> {};
    private Supplier<String> proveedorEntrada = () -> "";

    private static final Map<String, Integer> REG_MAP = new HashMap<>();

    static {
        for (int i = 0; i <= 31; i++) {
            REG_MAP.put("x" + i, i);
        }
        REG_MAP.put("zero", 0);
        REG_MAP.put("ra", 1);
        REG_MAP.put("sp", 2);
        REG_MAP.put("gp", 3);
        REG_MAP.put("tp", 4);
        REG_MAP.put("t0", 5);
        REG_MAP.put("t1", 6);
        REG_MAP.put("t2", 7);
        REG_MAP.put("s0", 8);
        REG_MAP.put("fp", 8);
        REG_MAP.put("s1", 9);
        REG_MAP.put("a0", 10);
        REG_MAP.put("a1", 11);
        REG_MAP.put("a2", 12);
        REG_MAP.put("a3", 13);
        REG_MAP.put("a4", 14);
        REG_MAP.put("a5", 15);
        REG_MAP.put("a6", 16);
        REG_MAP.put("a7", 17);
        REG_MAP.put("s2", 18);
        REG_MAP.put("s3", 19);
        REG_MAP.put("s4", 20);
        REG_MAP.put("s5", 21);
        REG_MAP.put("s6", 22);
        REG_MAP.put("s7", 23);
        REG_MAP.put("s8", 24);
        REG_MAP.put("s9", 25);
        REG_MAP.put("s10", 26);
        REG_MAP.put("s11", 27);
        REG_MAP.put("t3", 28);
        REG_MAP.put("t4", 29);
        REG_MAP.put("t5", 30);
        REG_MAP.put("t6", 31);
    }

    public record Instruccion(String mnem, String[] args, String lineaOriginal) {}

    private record MemOp(int reg, int offset) {}

    public SimuladorRiscV() {}

    public static String ejecutarCodigo(String codigoRiscV) {
        StringBuilder sb = new StringBuilder();
        SimuladorRiscV sim = new SimuladorRiscV();
        sim.ejecutar(codigoRiscV, sb::append);
        return sb.toString();
    }

    public static String ejecutarCodigo(String codigoRiscV, Supplier<String> proveedorEntrada) {
        StringBuilder sb = new StringBuilder();
        SimuladorRiscV sim = new SimuladorRiscV();
        sim.setProveedorEntrada(proveedorEntrada);
        sim.ejecutar(codigoRiscV, sb::append);
        return sb.toString();
    }

    public static void ejecutarCodigo(String codigoRiscV, Consumer<String> salidaConsola) {
        SimuladorRiscV sim = new SimuladorRiscV();
        sim.ejecutar(codigoRiscV, salidaConsola);
    }

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            System.err.println("Uso: java org.compi2.riscv.SimuladorRiscV <archivo.s>");
            return;
        }

        try {
            Path rutaArchivo = Path.of(args[0]);
            if (!Files.exists(rutaArchivo)) {
                System.err.println("Archivo no encontrado: " + args[0]);
                return;
            }

            String codigoRiscV = Files.readString(rutaArchivo, StandardCharsets.UTF_8);

            PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

            SimuladorRiscV sim = new SimuladorRiscV();
            sim.setConsumidorSalida(s -> {
                out.print(s);
                out.flush();
            });

            sim.setProveedorEntrada(() -> {
                try {
                    String linea = in.readLine();
                    return linea != null ? linea : "";
                } catch (IOException e) {
                    return "";
                }
            });

            sim.ejecutar(codigoRiscV, sim.getConsumidorSalida());
        } catch (Exception e) {
            System.err.println("Error durante la ejecucion de Assembler RISC-V: " + e.getMessage());
        }
    }

    public Process lanzarEnTerminalExterna(String codigoRiscV, String nombreArchivo) throws IOException {
        String baseNombre = (nombreArchivo != null && !nombreArchivo.isBlank())
                ? Path.of(nombreArchivo).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "programa";
        if (baseNombre.toLowerCase().endsWith(".s")) {
            baseNombre = baseNombre.substring(0, baseNombre.length() - 2);
        }

        Path tempDir = Files.createTempDirectory("riscv_run_");
        Path tempS = tempDir.resolve(baseNombre + ".s");
        Files.writeString(tempS, codigoRiscV != null ? codigoRiscV : "", StandardCharsets.UTF_8);

        Path tempBat = tempDir.resolve("ejecutar_" + baseNombre + ".bat");

        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        String classPath = System.getProperty("java.class.path");

        String titulo = "Assembler RISC-V (Venus RV32IM)" + (nombreArchivo != null && !nombreArchivo.isBlank() ? " - " + nombreArchivo : "");

        StringBuilder batContent = new StringBuilder();
        batContent.append("@echo off\r\n");
        batContent.append("chcp 65001 >nul\r\n");
        batContent.append("title ").append(titulo).append("\r\n");
        batContent.append("\"").append(javaBin).append("\" -Dfile.encoding=UTF-8 -cp \"")
                  .append(classPath).append("\" org.compi2.riscv.SimuladorRiscV \"")
                  .append(tempS.toAbsolutePath().toString()).append("\"\r\n");
        batContent.append("echo.\r\n");
        batContent.append("echo [Ejecucion de Assembler RISC-V finalizada]\r\n");
        batContent.append("pause\r\n");
        batContent.append("exit\r\n");

        Files.writeString(tempBat, batContent.toString(), StandardCharsets.UTF_8);

        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", titulo, tempBat.toAbsolutePath().toString());
        return pb.start();
    }

    public void detener() {
        this.detenido = true;
        synchronized (this) {
            if (procesoExterno != null && procesoExterno.isAlive()) {
                procesoExterno.destroyForcibly();
            }
        }
    }

    public void ejecutar(String codigoRiscV, Consumer<String> salidaConsola) {
        if (salidaConsola != null) {
            this.consumidorSalida = salidaConsola;
        }

        if (debeIntentarVenusOficial(codigoRiscV)) {
            boolean exitoVenus = ejecutarConVenusOficial(codigoRiscV);
            if (exitoVenus) {
                return;
            }
        }

        ejecutarMotorIntegrado(codigoRiscV);
    }

    public void ejecutarMotorIntegrado(String codigoRiscV) {
        cargarCodigo(codigoRiscV);
        ejecutarInstrucciones();
    }

    private boolean debeIntentarVenusOficial(String codigoRiscV) {
        if (codigoRiscV == null || codigoRiscV.isBlank()) {
            return false;
        }
        File venusJar = new File("bin/venus/venus.jar");
        if (!venusJar.exists() || !venusJar.isFile()) {
            return false;
        }
        if (contieneLecturaInteractiva(codigoRiscV)) {
            return false;
        }
        if (tieneDirectivasNoSoportadas(codigoRiscV)) {
            return false;
        }
        return true;
    }

    private boolean contieneLecturaInteractiva(String codigoRiscV) {
        if (codigoRiscV == null) {
            return false;
        }
        return codigoRiscV.matches("(?s).*\\bli\\s+a0\\s*,\\s*8\\b.*");
    }

    private boolean tieneDirectivasNoSoportadas(String codigoRiscV) {
        if (codigoRiscV == null) {
            return false;
        }
        if (codigoRiscV.matches("(?s).*\\bli\\s+a0\\s*,\\s*93\\b.*")) {
            return true;
        }
        String[] lineas = codigoRiscV.split("\\r?\\n");
        for (String lineaOriginal : lineas) {
            String l = lineaOriginal.trim();
            int idxComent = l.indexOf('#');
            if (idxComent >= 0) {
                l = l.substring(0, idxComent).trim();
            }
            if (l.startsWith(".")) {
                String dir = l.split("\\s+")[0].toLowerCase();
                switch (dir) {
                    case ".data", ".text", ".globl", ".global", ".space", ".align",
                         ".word", ".byte", ".asciiz", ".ascii", ".string" -> {}
                    default -> {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean ejecutarConVenusOficial(String codigoRiscV) {
        File venusJar = new File("bin/venus/venus.jar");
        if (!venusJar.exists()) {
            return false;
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("venus_run_", ".s");
            Files.writeString(tempFile, codigoRiscV, StandardCharsets.UTF_8);

            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            int limite = limiteInstrucciones > 0 ? limiteInstrucciones : -1;

            ProcessBuilder pb = new ProcessBuilder(
                    javaBin,
                    "-Dfile.encoding=UTF-8",
                    "-jar",
                    venusJar.getAbsolutePath(),
                    "-ms",
                    String.valueOf(limite),
                    tempFile.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);

            synchronized (this) {
                if (detenido) {
                    return true;
                }
                this.procesoExterno = pb.start();
            }

            char[] buffer = new char[1024];
            int leidos;
            StringBuilder salidaTotal = new StringBuilder();
            boolean errorAssembler = false;
            boolean primerChunk = true;

            try (InputStreamReader isr = new InputStreamReader(procesoExterno.getInputStream(), StandardCharsets.UTF_8)) {
                while ((leidos = isr.read(buffer)) != -1) {
                    if (detenido) {
                        break;
                    }
                    String chunk = new String(buffer, 0, leidos);
                    salidaTotal.append(chunk);

                    if (primerChunk) {
                        String actual = salidaTotal.toString();
                        if (actual.contains("Could not assemble program!")
                                || actual.contains("AssemblerError:")
                                || actual.contains("unknown assembler directive")
                                || actual.contains("Venus ran into a simulator error!")
                                || actual.contains("Invalid ecall")) {
                            errorAssembler = true;
                            break;
                        }
                        primerChunk = false;
                    }

                    if (consumidorSalida != null) {
                        consumidorSalida.accept(chunk);
                    }
                }
            }

            int exitCode = 0;
            try {
                exitCode = procesoExterno.waitFor();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                detener();
                return true;
            }

            if (detenido) {
                return true;
            }

            if (errorAssembler || exitCode != 0) {
                return false;
            }

            return true;

        } catch (Exception e) {
            return false;
        } finally {
            synchronized (this) {
                if (procesoExterno != null) {
                    if (procesoExterno.isAlive()) {
                        procesoExterno.destroyForcibly();
                    }
                    procesoExterno = null;
                }
            }
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {}
            }
        }
    }

    public void cargarCodigo(String codigoRiscV) {
        reiniciarEstado();
        if (codigoRiscV == null || codigoRiscV.isBlank()) {
            return;
        }

        String[] lineas = codigoRiscV.split("\\r?\\n");
        boolean enData = false;
        boolean enText = false;
        int dataPtr = 0;

        for (String lineaOriginal : lineas) {
            String linea = lineaOriginal;
            int idxComentario = encontrarIndiceFueraComillas(linea, '#');
            if (idxComentario >= 0) {
                linea = linea.substring(0, idxComentario);
            }
            linea = linea.trim();
            if (linea.isEmpty()) {
                continue;
            }

            if (linea.equalsIgnoreCase(".data")) {
                enData = true;
                enText = false;
                continue;
            }
            if (linea.equalsIgnoreCase(".text")) {
                enData = false;
                enText = true;
                continue;
            }
            if (linea.startsWith(".globl") || linea.startsWith(".global")) {
                continue;
            }

            // Procesar etiquetas posibles ignorando ':' dentro de comillas
            int idxDosPuntos;
            while ((idxDosPuntos = encontrarIndiceFueraComillas(linea, ':')) >= 0) {
                String etiqueta = linea.substring(0, idxDosPuntos).trim();
                if (!etiqueta.isEmpty()) {
                    if (enData) {
                        etiquetasDatos.put(etiqueta, dataPtr);
                    } else {
                        etiquetasTexto.put(etiqueta, instrucciones.size());
                    }
                }
                linea = linea.substring(idxDosPuntos + 1).trim();
            }
            if (linea.isEmpty()) {
                continue;
            }

            if (enData) {
                dataPtr = procesarDirectivaData(linea, dataPtr);
            } else if (enText) {
                procesarInstruccionText(linea, lineaOriginal);
            }
        }

        // Posicionar PC en la etiqueta main si existe
        if (etiquetasTexto.containsKey("main")) {
            this.pc = etiquetasTexto.get("main");
        } else {
            this.pc = 0;
        }
    }

    private int procesarDirectivaData(String linea, int dataPtr) {
        String[] partes = linea.split("\\s+", 2);
        String directiva = partes[0].toLowerCase();
        String resto = partes.length > 1 ? partes[1].trim() : "";

        switch (directiva) {
            case ".space" -> {
                int bytes = parseImm(resto);
                dataPtr += bytes;
            }
            case ".align" -> {
                int alignExp = parseImm(resto);
                int mask = (1 << alignExp) - 1;
                dataPtr = (dataPtr + mask) & ~mask;
            }
            case ".word" -> {
                String[] words = resto.split(",");
                for (String w : words) {
                    escribirWord(dataPtr, parseImm(w.trim()));
                    dataPtr += 4;
                }
            }
            case ".byte" -> {
                String[] bytes = resto.split(",");
                for (String b : bytes) {
                    escribirByte(dataPtr, (byte) parseImm(b.trim()));
                    dataPtr += 1;
                }
            }
            case ".asciiz", ".string" -> {
                String str = desescaparString(resto);
                byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
                for (byte b : bytes) {
                    escribirByte(dataPtr++, b);
                }
                escribirByte(dataPtr++, (byte) 0);
            }
            case ".ascii" -> {
                String str = desescaparString(resto);
                byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
                for (byte b : bytes) {
                    escribirByte(dataPtr++, b);
                }
            }
        }
        return dataPtr;
    }

    private void procesarInstruccionText(String linea, String lineaOriginal) {
        int idxEspacio = -1;
        for (int i = 0; i < linea.length(); i++) {
            char ch = linea.charAt(i);
            if (Character.isWhitespace(ch)) {
                idxEspacio = i;
                break;
            }
        }

        String mnem;
        String[] args;
        if (idxEspacio < 0) {
            mnem = linea.toLowerCase();
            args = new String[0];
        } else {
            mnem = linea.substring(0, idxEspacio).trim().toLowerCase();
            String resto = linea.substring(idxEspacio + 1).trim();
            if (resto.isEmpty()) {
                args = new String[0];
            } else {
                String[] partes = resto.split(",");
                args = new String[partes.length];
                for (int i = 0; i < partes.length; i++) {
                    args[i] = partes[i].trim();
                }
            }
        }
        instrucciones.add(new Instruccion(mnem, args, lineaOriginal));
    }

    private void ejecutarInstrucciones() {
        this.pasosEjecutados = 0;
        this.detenido = false;
        this.finalizado = false;

        while (pc >= 0 && pc < instrucciones.size() && !detenido && !finalizado) {
            if (++pasosEjecutados > limiteInstrucciones) {
                throw new RuntimeException("Limite de seguridad de instrucciones alcanzado (" 
                        + limiteInstrucciones + " pasos). Posible ciclo infinito.");
            }

            Instruccion inst = instrucciones.get(pc);
            int siguientePc = pc + 1;

            switch (inst.mnem()) {
                case "li" -> {
                    int rd = parseReg(inst.args()[0]);
                    int imm = parseImm(inst.args()[1]);
                    setRegistro(rd, imm);
                }
                case "la" -> {
                    int rd = parseReg(inst.args()[0]);
                    int addr = resolverSimbolo(inst.args()[1]);
                    setRegistro(rd, addr);
                }
                case "mv" -> {
                    int rd = parseReg(inst.args()[0]);
                    int rs = parseReg(inst.args()[1]);
                    setRegistro(rd, getRegistro(rs));
                }

                // Aritmética
                case "add" -> {
                    int rd = parseReg(inst.args()[0]);
                    int rs1 = parseReg(inst.args()[1]);
                    int rs2 = parseReg(inst.args()[2]);
                    setRegistro(rd, getRegistro(rs1) + getRegistro(rs2));
                }
                case "addi" -> {
                    int rd = parseReg(inst.args()[0]);
                    int rs1 = parseReg(inst.args()[1]);
                    int imm = parseImm(inst.args()[2]);
                    setRegistro(rd, getRegistro(rs1) + imm);
                }
                case "sub" -> {
                    int rd = parseReg(inst.args()[0]);
                    int rs1 = parseReg(inst.args()[1]);
                    int rs2 = parseReg(inst.args()[2]);
                    setRegistro(rd, getRegistro(rs1) - getRegistro(rs2));
                }
                case "neg" -> {
                    int rd = parseReg(inst.args()[0]);
                    int rs1 = parseReg(inst.args()[1]);
                    setRegistro(rd, -getRegistro(rs1));
                }
                case "mul" -> {
                    int rd = parseReg(inst.args()[0]);
                    int rs1 = parseReg(inst.args()[1]);
                    int rs2 = parseReg(inst.args()[2]);
                    setRegistro(rd, getRegistro(rs1) * getRegistro(rs2));
                }
                case "div" -> {
                    int rd = parseReg(inst.args()[0]);
                    int rs1 = parseReg(inst.args()[1]);
                    int rs2 = parseReg(inst.args()[2]);
                    int val1 = getRegistro(rs1);
                    int val2 = getRegistro(rs2);
                    if (val2 == 0) {
                        setRegistro(rd, -1);
                    } else if (val1 == Integer.MIN_VALUE && val2 == -1) {
                        setRegistro(rd, Integer.MIN_VALUE);
                    } else {
                        setRegistro(rd, val1 / val2);
                    }
                }
                case "rem" -> {
                    int rd = parseReg(inst.args()[0]);
                    int rs1 = parseReg(inst.args()[1]);
                    int rs2 = parseReg(inst.args()[2]);
                    int val1 = getRegistro(rs1);
                    int val2 = getRegistro(rs2);
                    if (val2 == 0) {
                        setRegistro(rd, val1);
                    } else if (val1 == Integer.MIN_VALUE && val2 == -1) {
                        setRegistro(rd, 0);
                    } else {
                        setRegistro(rd, val1 % val2);
                    }
                }

                case "slli" -> {
                    int rd = parseReg(inst.args()[0]);
                    int rs1 = parseReg(inst.args()[1]);
                    int shamt = parseImm(inst.args()[2]) & 0x1F;
                    setRegistro(rd, getRegistro(rs1) << shamt);
                }
                case "srli" -> {
                    int rd = parseReg(inst.args()[0]);
                    int rs1 = parseReg(inst.args()[1]);
                    int shamt = parseImm(inst.args()[2]) & 0x1F;
                    setRegistro(rd, getRegistro(rs1) >>> shamt);
                }
                case "srai" -> {
                    int rd = parseReg(inst.args()[0]);
                    int rs1 = parseReg(inst.args()[1]);
                    int shamt = parseImm(inst.args()[2]) & 0x1F;
                    setRegistro(rd, getRegistro(rs1) >> shamt);
                }

                case "lw" -> {
                    int rd = parseReg(inst.args()[0]);
                    MemOp mem = parseMemOp(inst.args()[1]);
                    int addr = getRegistro(mem.reg()) + mem.offset();
                    setRegistro(rd, leerWord(addr));
                }
                case "sw" -> {
                    int rs2 = parseReg(inst.args()[0]);
                    MemOp mem = parseMemOp(inst.args()[1]);
                    int addr = getRegistro(mem.reg()) + mem.offset();
                    escribirWord(addr, getRegistro(rs2));
                }
                case "lbu" -> {
                    int rd = parseReg(inst.args()[0]);
                    MemOp mem = parseMemOp(inst.args()[1]);
                    int addr = getRegistro(mem.reg()) + mem.offset();
                    setRegistro(rd, leerByteUnsigned(addr));
                }
                case "lb" -> {
                    int rd = parseReg(inst.args()[0]);
                    MemOp mem = parseMemOp(inst.args()[1]);
                    int addr = getRegistro(mem.reg()) + mem.offset();
                    setRegistro(rd, (int) leerByte(addr));
                }
                case "sb" -> {
                    int rs2 = parseReg(inst.args()[0]);
                    MemOp mem = parseMemOp(inst.args()[1]);
                    int addr = getRegistro(mem.reg()) + mem.offset();
                    escribirByte(addr, (byte) (getRegistro(rs2) & 0xFF));
                }

                case "beq" -> {
                    int rs1 = parseReg(inst.args()[0]);
                    int rs2 = parseReg(inst.args()[1]);
                    if (getRegistro(rs1) == getRegistro(rs2)) {
                        siguientePc = resolverEtiqueta(inst.args()[2]);
                    }
                }
                case "bne" -> {
                    int rs1 = parseReg(inst.args()[0]);
                    int rs2 = parseReg(inst.args()[1]);
                    if (getRegistro(rs1) != getRegistro(rs2)) {
                        siguientePc = resolverEtiqueta(inst.args()[2]);
                    }
                }
                case "blt" -> {
                    int rs1 = parseReg(inst.args()[0]);
                    int rs2 = parseReg(inst.args()[1]);
                    if (getRegistro(rs1) < getRegistro(rs2)) {
                        siguientePc = resolverEtiqueta(inst.args()[2]);
                    }
                }
                case "bge" -> {
                    int rs1 = parseReg(inst.args()[0]);
                    int rs2 = parseReg(inst.args()[1]);
                    if (getRegistro(rs1) >= getRegistro(rs2)) {
                        siguientePc = resolverEtiqueta(inst.args()[2]);
                    }
                }
                case "bltu" -> {
                    int rs1 = parseReg(inst.args()[0]);
                    int rs2 = parseReg(inst.args()[1]);
                    if (Integer.compareUnsigned(getRegistro(rs1), getRegistro(rs2)) < 0) {
                        siguientePc = resolverEtiqueta(inst.args()[2]);
                    }
                }
                case "bgeu" -> {
                    int rs1 = parseReg(inst.args()[0]);
                    int rs2 = parseReg(inst.args()[1]);
                    if (Integer.compareUnsigned(getRegistro(rs1), getRegistro(rs2)) >= 0) {
                        siguientePc = resolverEtiqueta(inst.args()[2]);
                    }
                }
                case "bgt" -> {
                    int rs1 = parseReg(inst.args()[0]);
                    int rs2 = parseReg(inst.args()[1]);
                    if (getRegistro(rs1) > getRegistro(rs2)) {
                        siguientePc = resolverEtiqueta(inst.args()[2]);
                    }
                }
                case "ble" -> {
                    int rs1 = parseReg(inst.args()[0]);
                    int rs2 = parseReg(inst.args()[1]);
                    if (getRegistro(rs1) <= getRegistro(rs2)) {
                        siguientePc = resolverEtiqueta(inst.args()[2]);
                    }
                }
                case "bgez" -> {
                    int rs1 = parseReg(inst.args()[0]);
                    if (getRegistro(rs1) >= 0) {
                        siguientePc = resolverEtiqueta(inst.args()[1]);
                    }
                }
                case "blez" -> {
                    int rs1 = parseReg(inst.args()[0]);
                    if (getRegistro(rs1) <= 0) {
                        siguientePc = resolverEtiqueta(inst.args()[1]);
                    }
                }
                case "bgtz" -> {
                    int rs1 = parseReg(inst.args()[0]);
                    if (getRegistro(rs1) > 0) {
                        siguientePc = resolverEtiqueta(inst.args()[1]);
                    }
                }
                case "bltz" -> {
                    int rs1 = parseReg(inst.args()[0]);
                    if (getRegistro(rs1) < 0) {
                        siguientePc = resolverEtiqueta(inst.args()[1]);
                    }
                }
                case "beqz" -> {
                    int rs1 = parseReg(inst.args()[0]);
                    if (getRegistro(rs1) == 0) {
                        siguientePc = resolverEtiqueta(inst.args()[1]);
                    }
                }
                case "bnez" -> {
                    int rs1 = parseReg(inst.args()[0]);
                    if (getRegistro(rs1) != 0) {
                        siguientePc = resolverEtiqueta(inst.args()[1]);
                    }
                }

                case "j" -> siguientePc = resolverEtiqueta(inst.args()[0]);
                case "jal" -> {
                    int rd = 1; 
                    String etiqueta = inst.args()[0];
                    if (inst.args().length == 2) {
                        rd = parseReg(inst.args()[0]);
                        etiqueta = inst.args()[1];
                    }
                    setRegistro(rd, pc + 1);
                    siguientePc = resolverEtiqueta(etiqueta);
                }
                case "jr" -> {
                    int rs1 = parseReg(inst.args()[0]);
                    siguientePc = getRegistro(rs1);
                }
                case "jalr" -> {
                    int rd = 1;
                    int rs1 = parseReg(inst.args()[0]);
                    int offset = 0;
                    if (inst.args().length >= 2) {
                        rd = parseReg(inst.args()[0]);
                        MemOp mem = parseMemOp(inst.args()[1]);
                        rs1 = mem.reg();
                        offset = mem.offset();
                    }
                    int destino = getRegistro(rs1) + offset;
                    setRegistro(rd, pc + 1);
                    siguientePc = destino;
                }
                case "ret" -> siguientePc = getRegistro(1); // ra

                case "ecall" -> procesarEcall();

                case "nop" -> {}
                default -> throw new UnsupportedOperationException("Instruccion RISC-V no soportada: " + inst.mnem());
            }

            this.pc = siguientePc;
        }
    }

    private void procesarEcall() {
        int a0 = getRegistro(10);
        switch (a0) {
            case 1 -> { 
                int val = getRegistro(11);
                consumidorSalida.accept(String.valueOf(val));
            }
            case 4 -> { 
                int addr = getRegistro(11);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                while (true) {
                    byte b = leerByte(addr++);
                    if (b == 0) {
                        break;
                    }
                    baos.write(b);
                }
                consumidorSalida.accept(baos.toString(StandardCharsets.UTF_8));
            }
            case 8 -> { 
                int addr = getRegistro(11);
                int maxLen = getRegistro(12) > 0 ? getRegistro(12) : 1023;
                String entrada = (proveedorEntrada != null) ? proveedorEntrada.get() : "";
                if (entrada == null) {
                    entrada = "";
                }
                byte[] bytes = entrada.getBytes(StandardCharsets.UTF_8);
                int len = Math.min(bytes.length, maxLen);
                for (int i = 0; i < len; i++) {
                    escribirByte(addr + i, bytes[i]);
                }
                escribirByte(addr + len, (byte) 0);
            }
            case 10, 93 -> { 
                this.finalizado = true;
            }
            case 11 -> { 
                int c = getRegistro(11) & 0xFF;
                consumidorSalida.accept(String.valueOf((char) c));
            }
            default -> {}
        }
    }

    public int getRegistro(int r) {
        if (r <= 0 || r >= 32) {
            return 0;
        }
        return registros[r];
    }

    public void setRegistro(int r, int valor) {
        if (r > 0 && r < 32) {
            registros[r] = valor;
        }
    }

    public byte leerByte(int direccion) {
        int pageIndex = (direccion >>> 16) & 0xFFFF;
        byte[] page = paginasMemoria[pageIndex];
        if (page == null) {
            return 0;
        }
        return page[direccion & 0xFFFF];
    }

    public int leerByteUnsigned(int direccion) {
        return leerByte(direccion) & 0xFF;
    }

    public void escribirByte(int direccion, byte valor) {
        int pageIndex = (direccion >>> 16) & 0xFFFF;
        byte[] page = paginasMemoria[pageIndex];
        if (page == null) {
            page = new byte[65536];
            paginasMemoria[pageIndex] = page;
        }
        page[direccion & 0xFFFF] = valor;
    }

    public int leerWord(int direccion) {
        int b0 = leerByte(direccion) & 0xFF;
        int b1 = leerByte(direccion + 1) & 0xFF;
        int b2 = leerByte(direccion + 2) & 0xFF;
        int b3 = leerByte(direccion + 3) & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    public void escribirWord(int direccion, int valor) {
        escribirByte(direccion, (byte) (valor & 0xFF));
        escribirByte(direccion + 1, (byte) ((valor >>> 8) & 0xFF));
        escribirByte(direccion + 2, (byte) ((valor >>> 16) & 0xFF));
        escribirByte(direccion + 3, (byte) ((valor >>> 24) & 0xFF));
    }

    public int parseReg(String s) {
        if (s == null) {
            return 0;
        }
        s = s.trim().toLowerCase();
        if (s.endsWith(",")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        Integer reg = REG_MAP.get(s);
        if (reg != null) {
            return reg;
        }
        if (s.startsWith("x")) {
            try {
                return Integer.parseInt(s.substring(1));
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    public int parseImm(String s) {
        if (s == null) {
            return 0;
        }
        s = s.trim();
        if (s.endsWith(",")) {
            s = s.substring(0, s.length() - 1).trim();
        }
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return (int) Long.parseLong(s.substring(2), 16);
        }
        if (s.startsWith("'") && s.endsWith("'") && s.length() == 3) {
            return s.charAt(1);
        }
        try {
            return (int) Long.parseLong(s);
        } catch (NumberFormatException e) {
            // Intentar como etiqueta de datos o texto
            return resolverSimbolo(s);
        }
    }

    private MemOp parseMemOp(String arg) {
        arg = arg.trim();
        int openParen = arg.indexOf('(');
        int closeParen = arg.indexOf(')');
        if (openParen >= 0 && closeParen > openParen) {
            String offsetStr = arg.substring(0, openParen).trim();
            String regStr = arg.substring(openParen + 1, closeParen).trim();
            int offset = offsetStr.isEmpty() ? 0 : parseImm(offsetStr);
            int reg = parseReg(regStr);
            return new MemOp(reg, offset);
        }
        int offset = parseImm(arg);
        return new MemOp(0, offset);
    }

    public int resolverSimbolo(String symbol) {
        if (symbol == null) {
            return 0;
        }
        symbol = symbol.trim();
        if (etiquetasDatos.containsKey(symbol)) {
            return etiquetasDatos.get(symbol);
        }
        if (etiquetasTexto.containsKey(symbol)) {
            return etiquetasTexto.get(symbol);
        }
        if (symbol.startsWith("0x") || symbol.startsWith("0X")) {
            try {
                return (int) Long.parseLong(symbol.substring(2), 16);
            } catch (NumberFormatException ignored) {}
        }
        try {
            return (int) Long.parseLong(symbol);
        } catch (NumberFormatException ignored) {}

        return 0;
    }

    private static int encontrarIndiceFueraComillas(String linea, char objetivo) {
        if (linea == null) {
            return -1;
        }
        boolean enDoble = false;
        boolean enSimple = false;
        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (c == '\\' && (enDoble || enSimple) && i + 1 < linea.length()) {
                i++; // saltar caracter escapado dentro de comillas
                continue;
            }
            if (c == '"' && !enSimple) {
                enDoble = !enDoble;
            } else if (c == '\'' && !enDoble) {
                enSimple = !enSimple;
            } else if (c == objetivo && !enDoble && !enSimple) {
                return i;
            }
        }
        return -1;
    }

    public int resolverEtiqueta(String label) {
        label = label.trim();
        Integer pos = etiquetasTexto.get(label);
        if (pos == null) {
            throw new RuntimeException("Etiqueta no encontrada en segmento .text: " + label);
        }
        return pos;
    }

    private String desescaparString(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        return s.replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r")
                .replace("\\0", "\0")
                .replace("\\\"", "\"");
    }

    public void reiniciarEstado() {
        for (int i = 0; i < 32; i++) {
            registros[i] = 0;
        }
        for (int i = 0; i < paginasMemoria.length; i++) {
            paginasMemoria[i] = null;
        }
        etiquetasDatos.clear();
        etiquetasTexto.clear();
        instrucciones.clear();
        pc = 0;
        pasosEjecutados = 0;
        detenido = false;
        finalizado = false;
    }
}
