package org.compi2.tipos;

import lombok.Getter;
import org.compi2.analisis_semantico.ComprobadorTipos;

import java.util.*;
import java.util.function.Function;

@Getter
public class Tipo {

    public static final Tipo ENTERO = new Tipo(TipoBase.ENTERO);
    public static final Tipo DECIMAL = new Tipo(TipoBase.DECIMAL);
    public static final Tipo CARACTER = new Tipo(TipoBase.CARACTER);
    public static final Tipo BOOLEANO = new Tipo(TipoBase.BOOLEANO);
    public static final Tipo CADENA = new Tipo(TipoBase.CADENA);
    public static final Tipo VOID = new Tipo(TipoBase.VOID);
    public static final Tipo ERROR = new Tipo(TipoBase.ERROR);

    private final TipoBase base;
    private final String nombreTipo;
    private final Tipo tipoComponente;
    private final int dimensiones;
    private final List<Tipo> parametros;
    private final Tipo tipoRetorno;

    private final Map<String, Tipo> campos = new LinkedHashMap<>();
    private final Map<String, List<Tipo>> metodos = new HashMap<>();
    private final List<List<Tipo>> constructores = new ArrayList<>();

    public Tipo(TipoBase base) {
        this(base, base.name().toLowerCase());
    }

    public Tipo(TipoBase base, String nombreTipo) {
        this(base, nombreTipo, null, 0, null, null);
    }

    public Tipo(Tipo tipoComponente, int dimensiones) {
        this(TipoBase.ARREGLO,
            tipoComponente.getNombreTipo() + "[]".repeat(dimensiones),
            tipoComponente, dimensiones, null, null);
    }

    public Tipo(String nombreTipo, List<Tipo> parametros, Tipo tipoRetorno) {
        this(TipoBase.FUNCION, nombreTipo, null, 0,
            parametros != null ? parametros : Collections.emptyList(), tipoRetorno);
    }

    public Tipo(TipoBase base, String nombreTipo, Tipo tipoComponente, int dimensiones,
                List<Tipo> parametros, Tipo tipoRetorno) {
        this.base = base;
        this.nombreTipo = nombreTipo;
        this.tipoComponente = tipoComponente;
        this.dimensiones = dimensiones;
        this.parametros = parametros;
        this.tipoRetorno = tipoRetorno;
    }

    public void agregarCampo(String nombre, Tipo tipo) {
        campos.put(nombre, tipo);
    }

    public Tipo buscarCampo(String nombre) {
        return campos.get(nombre);
    }

    public Map<String, Tipo> getCampos() {
        return Collections.unmodifiableMap(campos);
    }

    public List<String> obtenerNombresCampos() {
        return new ArrayList<>(campos.keySet());
    }

    public String obtenerNombreCampoPorIndice(int index) {
        if (index < 0 || index >= campos.size()) return null;
        return obtenerNombresCampos().get(index);
    }

    public Tipo obtenerTipoCampoPorIndice(int index) {
        String nombre = obtenerNombreCampoPorIndice(index);
        return nombre != null ? campos.get(nombre) : null;
    }

    public int obtenerOffsetCampo(String nombreCampo) {
        int idx = 0;
        for (String c : campos.keySet()) {
            if (c.equals(nombreCampo)) return idx;
            idx++;
        }
        return -1;
    }

    public int calcularTamanoEnHeap() {
        boolean esCompuesto = base == TipoBase.OBJETO_CLASE || base == TipoBase.ESTRUCTURA;
        return esCompuesto ? campos.size() : 1;
    }

    public static int calcularTamanoEnHeapArreglo(List<Integer> dims) {
        if (dims == null || dims.isEmpty()) return 1;
        int totalElementos = dims.stream().reduce(1, (a, b) -> a * b);
        return 1 + dims.size() + totalElementos;
    }

    public static int calcularOffsetAplanado(List<Integer> indices, List<Integer> dims) {
        if (indices == null || dims == null || indices.isEmpty()) return 0;
        int offset = indices.get(0);
        for (int k = 1; k < indices.size(); k++) {
            int dimActual = k < dims.size() ? dims.get(k) : 1;
            offset = offset * dimActual + indices.get(k);
        }
        return offset;
    }

    public void agregarMetodo(String nombre, Tipo tipoFuncion) {
        metodos.computeIfAbsent(nombre, k -> new ArrayList<>()).add(tipoFuncion);
    }

    public List<Tipo> obtenerSobrecargasMetodo(String nombre) {
        return metodos.getOrDefault(nombre, Collections.emptyList());
    }

    public Tipo buscarMetodo(String nombre) {
        List<Tipo> candidatos = metodos.get(nombre);
        return (candidatos == null || candidatos.isEmpty()) ? null : candidatos.get(0);
    }

    public Tipo buscarMetodo(String nombre, List<Tipo> args) {
        List<Tipo> candidatos = metodos.get(nombre);
        if (candidatos == null) return null;
        return buscarCoincidencia(candidatos, args, Tipo::getParametros);
    }

    public boolean existeMetodo(String nombre) {
        return metodos.containsKey(nombre) && !metodos.get(nombre).isEmpty();
    }

    public void agregarConstructor(List<Tipo> params) {
        constructores.add(params != null ? new ArrayList<>(params) : Collections.emptyList());
    }

    public boolean existeConstructor(List<Tipo> args) {
        List<Tipo> listaArgs = args != null ? args : Collections.emptyList();
        if (constructores.isEmpty() && listaArgs.isEmpty()) return true;
        return buscarCoincidencia(constructores, listaArgs, Function.identity()) != null;
    }


    private static <T> T buscarCoincidencia(List<T> candidatos, List<Tipo> args,
                                            Function<T, List<Tipo>> obtenerParametros) {

        List<Tipo> listaArgs = args != null ? args : Collections.emptyList();

        T exacto = buscarPor(candidatos, obtenerParametros, params -> coincideFirmaExacta(params, listaArgs));
        if (exacto != null) return exacto;

        return buscarPor(candidatos, obtenerParametros, params -> coincideFirma(params, listaArgs));
    }

    private static <T> T buscarPor(List<T> candidatos, Function<T, List<Tipo>> obtenerParametros,
                                   java.util.function.Predicate<List<Tipo>> coincide) {
        for (T candidato : candidatos) {
            List<Tipo> params = obtenerParametros.apply(candidato);
            if (coincide.test(params != null ? params : Collections.emptyList())) {
                return candidato;
            }
        }
        return null;
    }

    public static boolean coincideFirmaExacta(List<Tipo> esperados, List<Tipo> recibidos) {
        if (esperados.size() != recibidos.size()) return false;
        for (int i = 0; i < esperados.size(); i++) {
            if (!esperados.get(i).esMismoTipo(recibidos.get(i))) return false;
        }
        return true;
    }

    public static boolean coincideFirma(List<Tipo> esperados, List<Tipo> recibidos) {
        if (esperados.size() != recibidos.size()) return false;
        for (int i = 0; i < esperados.size(); i++) {
            Tipo esperado = esperados.get(i);
            Tipo recibido = recibidos.get(i);
            boolean compatible = esperado.esMismoTipo(recibido)
                || ComprobadorTipos.esCompatibleAsignacion(esperado, recibido);
            if (!compatible) return false;
        }
        return true;
    }


    public boolean esMismoTipo(Tipo otro) {
        if (otro == null || this.base != otro.base) return false;

        return switch (this.base) {
            case ARREGLO -> dimensiones == otro.dimensiones
                && tipoComponente != null
                && tipoComponente.esMismoTipo(otro.tipoComponente);
            case OBJETO_CLASE, ESTRUCTURA -> nombreTipo.equals(otro.nombreTipo);
            case FUNCION -> esMismaFuncion(otro);
            default -> true;
        };
    }

    private boolean esMismaFuncion(Tipo otro) {
        if (tipoRetorno != null && !tipoRetorno.esMismoTipo(otro.tipoRetorno)) return false;
        if (parametros == null || otro.parametros == null) return parametros == otro.parametros;
        if (parametros.size() != otro.parametros.size()) return false;
        for (int i = 0; i < parametros.size(); i++) {
            if (!parametros.get(i).esMismoTipo(otro.parametros.get(i))) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return nombreTipo;
    }
}