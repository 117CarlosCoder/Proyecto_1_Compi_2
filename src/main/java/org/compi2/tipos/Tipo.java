package org.compi2.tipos;

import lombok.Getter;
import lombok.Setter;
import org.compi2.analisis_semantico.ComprobadorTipos;
import org.compi2.analisis_semantico.TablaSimbolos;

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
    public static final Tipo NULO = new Tipo(TipoBase.NULO, "null");
    public static final Tipo ERROR = new Tipo(TipoBase.ERROR);

    private final TipoBase base;
    private final String nombreTipo;
    private final Tipo tipoComponente;
    private final int dimensiones;
    private final List<Tipo> parametros;
    @Setter
    private Tipo tipoRetorno;

    private final Map<String, Tipo> campos = new LinkedHashMap<>();
    private final Map<String, Visibilidad> visibilidadCampos = new HashMap<>();
    private final Map<String, String> claseDeclaranteCampos = new HashMap<>();
    private final Map<String, List<Tipo>> metodos = new HashMap<>();
    private final List<List<Tipo>> constructores = new ArrayList<>();
    private final List<Visibilidad> visibilidadConstructores = new ArrayList<>();

    @Setter
    private String clasePadre;
    @Setter
    private Tipo tipoPadre;
    @Setter
    private Visibilidad visibilidad = Visibilidad.PUBLIC;
    @Setter
    private String claseDeclarante;

    private boolean padreEnlazado = false;

    public Tipo(TipoBase base, String nombreTipo, Tipo tipoComponente, int dimensiones, List<Tipo> parametros, Tipo tipoRetorno) {
        this.base = base;
        this.nombreTipo = nombreTipo;
        this.tipoComponente = tipoComponente;
        this.dimensiones = dimensiones;
        this.parametros = parametros;
        this.tipoRetorno = tipoRetorno;
    }

    public Tipo(TipoBase base, String nombreTipo, Tipo tipoComponente, int dimensiones, List<Tipo> parametros) {
        this(base, nombreTipo, tipoComponente, dimensiones, parametros, null);
    }

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

    public void agregarCampo(String nombre, Tipo tipo) {
        agregarCampo(nombre, tipo, Visibilidad.DEFAULT, this.nombreTipo);
    }

    public void agregarCampo(String nombre, Tipo tipo, Visibilidad vis, String claseDeclarante) {
        campos.put(nombre, tipo);
        visibilidadCampos.put(nombre, vis != null ? vis : Visibilidad.DEFAULT);
        claseDeclaranteCampos.put(nombre, claseDeclarante != null ? claseDeclarante : this.nombreTipo);
    }

    public Tipo buscarCampo(String nombre) {
        Tipo c = campos.get(nombre);
        if (c != null) return c;
        if (tipoPadre != null) return tipoPadre.buscarCampo(nombre);
        return null;
    }

    public Visibilidad obtenerVisibilidadCampo(String nombre) {
        Visibilidad v = visibilidadCampos.get(nombre);
        if (v != null) return v;
        if (tipoPadre != null) return tipoPadre.obtenerVisibilidadCampo(nombre);
        return Visibilidad.PUBLIC;
    }

    public String obtenerClaseDeclaranteCampo(String nombre) {
        String d = claseDeclaranteCampos.get(nombre);
        if (d != null) return d;
        if (tipoPadre != null) return tipoPadre.obtenerClaseDeclaranteCampo(nombre);
        return this.nombreTipo;
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

    public void agregarMetodo(String nombre, Tipo tipoFuncion) {
        metodos.computeIfAbsent(nombre, k -> new ArrayList<>()).add(tipoFuncion);
    }

    public List<Tipo> obtenerSobrecargasMetodo(String nombre) {
        List<Tipo> propias = metodos.getOrDefault(nombre, Collections.emptyList());
        if (tipoPadre == null) {
            return propias;
        }
        List<Tipo> todas = new ArrayList<>(propias);
        for (Tipo mPadre : tipoPadre.obtenerSobrecargasMetodo(nombre)) {
            if (mPadre.getVisibilidad() == Visibilidad.PRIVATE) {
                continue;
            }
            boolean sobreescrito = propias.stream().anyMatch(mPropio ->
                coincideFirmaExacta(mPropio.getParametros(), mPadre.getParametros()));
            if (!sobreescrito) {
                todas.add(mPadre);
            }
        }
        return todas;
    }

    public Tipo buscarMetodo(String nombre) {
        List<Tipo> sobrecargas = obtenerSobrecargasMetodo(nombre);
        return (sobrecargas.isEmpty()) ? null : sobrecargas.get(0);
    }

    public Tipo buscarMetodo(String nombre, List<Tipo> args) {
        List<Tipo> candidatos = obtenerSobrecargasMetodo(nombre);
        if (candidatos.isEmpty()) return null;
        return buscarCoincidencia(candidatos, args, Tipo::getParametros);
    }

    public Tipo buscarMetodoEnAncestros(String nombre, List<Tipo> params) {
        Tipo actual = this;
        Set<String> visitados = new HashSet<>();
        while (actual != null && actual.getNombreTipo() != null && visitados.add(actual.getNombreTipo())) {
            List<Tipo> sobrecargas = actual.metodos.get(nombre);
            if (sobrecargas != null) {
                for (Tipo cand : sobrecargas) {
                    if (cand.getVisibilidad() != Visibilidad.PRIVATE && coincideFirmaExacta(cand.getParametros(), params)) {
                        return cand;
                    }
                }
            }
            actual = actual.getTipoPadre();
        }
        return null;
    }

    public Tipo buscarMetodoAncestrosCualquiera(String nombre, List<Tipo> args) {
        Tipo actual = this.tipoPadre;
        Set<String> visitados = new HashSet<>();
        while (actual != null && actual.getNombreTipo() != null && visitados.add(actual.getNombreTipo())) {
            List<Tipo> propias = actual.metodos.get(nombre);
            if (propias != null) {
                Tipo cand = buscarCoincidencia(propias, args != null ? args : Collections.emptyList(), Tipo::getParametros);
                if (cand != null) {
                    return cand;
                }
            }
            actual = actual.getTipoPadre();
        }
        return null;
    }

    public String obtenerClaseDeclaranteMetodo(String nombre, List<Tipo> params) {
        List<Tipo> propias = metodos.get(nombre);
        if (propias != null) {
            for (Tipo m : propias) {
                if (params == null || params.isEmpty()) {
                    if (m.getParametros() == null || m.getParametros().isEmpty()) {
                        return this.nombreTipo;
                    }
                }
                if (m.getParametros() != null && params != null
                        && (coincideFirmaExacta(m.getParametros(), params) || coincideFirma(m.getParametros(), params))) {
                    return this.nombreTipo;
                }
            }
            if (propias.size() == 1 && (params == null || params.isEmpty())) {
                return this.nombreTipo;
            }
        }
        if (tipoPadre != null) {
            return tipoPadre.obtenerClaseDeclaranteMetodo(nombre, params);
        }
        return this.nombreTipo;
    }

    public void actualizarTipoCampo(String nombre, Tipo nuevoTipo) {
        if (nombre != null && nuevoTipo != null) {
            campos.put(nombre, nuevoTipo);
        }
    }

    public void resolverTiposConstructores(TablaSimbolos tablaSimbolos) {
        if (tablaSimbolos == null) return;
        for (List<Tipo> params : constructores) {
            for (int i = 0; i < params.size(); i++) {
                params.set(i, tablaSimbolos.resolverTipo(params.get(i)));
            }
        }
    }

    public void resolverTiposMetodos(TablaSimbolos tablaSimbolos) {
        if (tablaSimbolos == null) return;
        for (List<Tipo> lista : metodos.values()) {
            for (Tipo m : lista) {
                if (m.getTipoRetorno() != null) {
                    m.setTipoRetorno(tablaSimbolos.resolverTipo(m.getTipoRetorno()));
                }
                if (m.getParametros() != null) {
                    for (int i = 0; i < m.getParametros().size(); i++) {
                        m.getParametros().set(i, tablaSimbolos.resolverTipo(m.getParametros().get(i)));
                    }
                }
            }
        }
    }

    public boolean existeMetodo(String nombre) {
        if (metodos.containsKey(nombre) && !metodos.get(nombre).isEmpty()) {
            return true;
        }
        return tipoPadre != null && tipoPadre.existeMetodo(nombre);
    }

    public void agregarConstructor(List<Tipo> params) {
        agregarConstructor(params, Visibilidad.PUBLIC);
    }

    public void agregarConstructor(List<Tipo> params, Visibilidad vis) {
        constructores.add(params != null ? new ArrayList<>(params) : Collections.emptyList());
        visibilidadConstructores.add(vis != null ? vis : Visibilidad.PUBLIC);
    }

    public List<Tipo> buscarConstructor(List<Tipo> args) {
        List<Tipo> listaArgs = args != null ? args : Collections.emptyList();
        if (constructores.isEmpty() && listaArgs.isEmpty()) return Collections.emptyList();
        return buscarCoincidencia(constructores, listaArgs, Function.identity());
    }

    public Visibilidad obtenerVisibilidadConstructor(List<Tipo> paramsCtor) {
        if (constructores.isEmpty() && (paramsCtor == null || paramsCtor.isEmpty())) {
            return Visibilidad.PUBLIC;
        }
        if (paramsCtor == null) paramsCtor = Collections.emptyList();
        for (int i = 0; i < constructores.size(); i++) {
            if (coincideFirmaExacta(constructores.get(i), paramsCtor)) {
                return (i < visibilidadConstructores.size()) ? visibilidadConstructores.get(i) : Visibilidad.PUBLIC;
            }
        }
        return Visibilidad.PUBLIC;
    }

    public boolean esSubclaseDe(Tipo posibleAncestro) {
        if (posibleAncestro == null) return false;
        return esSubclaseDe(posibleAncestro.getNombreTipo());
    }

    public boolean esSubclaseDe(String posibleAncestro) {
        if (posibleAncestro == null) return false;
        if (posibleAncestro.equals(this.clasePadre)) return true;
        Set<String> visitados = new HashSet<>();
        Tipo actual = this.tipoPadre;
        while (actual != null && actual.getNombreTipo() != null && visitados.add(actual.getNombreTipo())) {
            if (posibleAncestro.equals(actual.getNombreTipo())) {
                return true;
            }
            actual = actual.getTipoPadre();
        }
        return false;
    }

    public boolean esSubclaseDe(String posibleAncestro, TablaSimbolos tabla) {
        if (posibleAncestro == null) return false;
        if (esSubclaseDe(posibleAncestro)) return true;
        if (tabla != null) {
            Set<String> visitados = new HashSet<>();
            String actual = this.clasePadre;
            while (actual != null && visitados.add(actual)) {
                if (posibleAncestro.equals(actual)) {
                    return true;
                }
                Tipo t = tabla.buscarTipoCompuesto(actual);
                if (t == null) break;
                actual = t.getClasePadre();
            }
        }
        return false;
    }

    public void enlazarPadre(Tipo padre) {
        this.tipoPadre = padre;
        if (padre == null || padreEnlazado) return;
        padreEnlazado = true;

        if (padre.getTipoPadre() != null) {
            padre.enlazarPadre(padre.getTipoPadre());
        }

        Map<String, Tipo> propios = new LinkedHashMap<>(this.campos);
        Map<String, Visibilidad> visPropios = new HashMap<>(this.visibilidadCampos);
        Map<String, String> decPropios = new HashMap<>(this.claseDeclaranteCampos);

        this.campos.clear();
        this.visibilidadCampos.clear();
        this.claseDeclaranteCampos.clear();

        // 1. Campos del padre (offsets iniciales 0..N-1)
        for (Map.Entry<String, Tipo> entry : padre.getCampos().entrySet()) {
            this.campos.put(entry.getKey(), entry.getValue());
            this.visibilidadCampos.put(entry.getKey(), padre.obtenerVisibilidadCampo(entry.getKey()));
            this.claseDeclaranteCampos.put(entry.getKey(), padre.obtenerClaseDeclaranteCampo(entry.getKey()));
        }

        // 2. Campos propios (offsets N..N+M-1)
        for (Map.Entry<String, Tipo> entry : propios.entrySet()) {
            this.campos.put(entry.getKey(), entry.getValue());
            this.visibilidadCampos.put(entry.getKey(), visPropios.getOrDefault(entry.getKey(), Visibilidad.DEFAULT));
            this.claseDeclaranteCampos.put(entry.getKey(), decPropios.getOrDefault(entry.getKey(), this.nombreTipo));
        }
    }

    public String nombreParaEtiqueta() {
        if (base == null) return "void";
        return switch (base) {
            case BOOLEANO, CARACTER, ENTERO, DECIMAL, CADENA -> base.name().toLowerCase();
            case VOID -> "void";
            case NULO -> "null";
            case ERROR -> "err";
            case ARREGLO -> "arr" + dimensiones + "_" + (tipoComponente != null ? tipoComponente.nombreParaEtiqueta() : "void");
            case OBJETO_CLASE -> "cls_" + (nombreTipo != null ? nombreTipo.replaceAll("[^a-zA-Z0-9_]", "_") : "anon");
            case ESTRUCTURA -> "ref_" + (nombreTipo != null ? nombreTipo.replaceAll("[^a-zA-Z0-9_]", "_") : "anon");
            case FUNCION -> "fn";
        };
    }

    public static String unirTiposParametros(List<Tipo> parametros) {
        if (parametros == null || parametros.isEmpty()) {
            return "void";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parametros.size(); i++) {
            if (i > 0) sb.append("_");
            sb.append(parametros.get(i).nombreParaEtiqueta());
        }
        return sb.toString();
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