package org.compi2.c3d.generadores;

import lombok.Getter;
import lombok.Setter;
import org.compi2.analisis_semantico.Simbolo;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.ast.expresiones.NodoAccesoArreglo;
import org.compi2.ast.expresiones.NodoAccesoMiembro;
import org.compi2.ast.expresiones.NodoIdentificador;
import org.compi2.ast.expresiones.NodoLlamadaFuncion;
import org.compi2.ast.expresiones.NodoLlamadaMetodo;
import org.compi2.ast.sentencias.NodoDeclaracion;
import org.compi2.ast.sentencias.NodoDoWhile;
import org.compi2.ast.sentencias.NodoFor;
import org.compi2.ast.sentencias.NodoFuncion;
import org.compi2.ast.sentencias.NodoIf;
import org.compi2.ast.sentencias.NodoSwitch;
import org.compi2.ast.sentencias.NodoWhile;
import org.compi2.ast.sentencias.Parametro;
import org.compi2.c3d.ContenedorC3D;
import org.compi2.c3d.TipoOperadorC3D;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ContextoC3D {

    public record SitioLlamada(int id, String etiquetaRetorno) {}

    @Getter
    private final ContenedorC3D c3d;
    @Getter
    private final TablaSimbolos tablaSimbolos;
    private final RutinasC3D rutinas;

    private final Deque<String> etiquetasBreak = new ArrayDeque<>();
    private final Deque<String> etiquetasContinue = new ArrayDeque<>();
    private final Map<String, Integer> offsetsLocales = new HashMap<>();
    private final Map<String, Tipo> tiposLocales = new HashMap<>();

    @Setter
    @Getter
    private String funcionActual;
    @Setter
    @Getter
    private int offsetMaximo = 2;

    private int contadorLlamadas = 0;
    private final Map<String, List<SitioLlamada>> sitiosLlamadaPorFuncion = new HashMap<>();
    private final Set<String> temporalesActivos = new LinkedHashSet<>();

    public ContextoC3D(TablaSimbolos tablaSimbolos) {
        this(new ContenedorC3D(), tablaSimbolos);
    }

    public ContextoC3D(ContenedorC3D c3d, TablaSimbolos tablaSimbolos) {
        this.c3d = (c3d != null) ? c3d : new ContenedorC3D();
        this.tablaSimbolos = (tablaSimbolos != null) ? tablaSimbolos : new TablaSimbolos();
        this.rutinas = new RutinasC3D(this.c3d);
    }

    private String calcularPosicionStack(int offset) {
        String tempPosicion = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempPosicion, "P", String.valueOf(offset));
        return tempPosicion;
    }

    private String calcularPosicionCampoThis(String nombreCampo) {
        int offsetCampo = obtenerOffsetCampoThis(nombreCampo);
        int offsetThis = obtenerOffset("this");

        String tempPosicionThis = calcularPosicionStack(offsetThis);

        String tempDireccionThis = c3d.nuevoTemporal();
        c3d.emitirAccesoStack(tempDireccionThis, tempPosicionThis);

        String tempPosicionCampo = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempPosicionCampo, tempDireccionThis, String.valueOf(offsetCampo));

        return tempPosicionCampo;
    }

    private Tipo obtenerTipoClaseThis() {
        Tipo tipoThis = tiposLocales.get("this");
        if (tipoThis == null) {
            return null;
        }
        Tipo tipoClase = tablaSimbolos.buscarTipoCompuesto(tipoThis.getNombreTipo());
        if (tipoClase != null) {
            return tipoClase;
        }
        return tipoThis;
    }

    public String leerVariable(String nombre) {
        if (esCampoDeThis(nombre)) {
            String tempPosicionCampo = calcularPosicionCampoThis(nombre);
            String tempValor = c3d.nuevoTemporal();
            c3d.emitirAccesoHeap(tempValor, tempPosicionCampo);
            return tempValor;
        }

        int offset = obtenerOffset(nombre);
        String tempPosicion = calcularPosicionStack(offset);
        String tempValor = c3d.nuevoTemporal();
        c3d.emitirAccesoStack(tempValor, tempPosicion);
        return tempValor;
    }

    public void escribirVariable(String nombre, String valor) {
        if (esCampoDeThis(nombre)) {
            String tempPosicionCampo = calcularPosicionCampoThis(nombre);
            c3d.emitirAsignacionHeap(tempPosicionCampo, valor);
            return;
        }

        int offset = obtenerOffset(nombre);
        String tempPosicion = calcularPosicionStack(offset);
        c3d.emitirAsignacionStack(tempPosicion, valor);
    }

    public String aplicarIncremento(String nombre, boolean esIncremento, boolean esPrefijo) {
        TipoOperadorC3D operador = esIncremento ? TipoOperadorC3D.SUMA : TipoOperadorC3D.RESTA;
        String tempNuevoValor = c3d.nuevoTemporal();

        if (esCampoDeThis(nombre)) {
            String tempPosicionCampo = calcularPosicionCampoThis(nombre);
            String tempValorActual = c3d.nuevoTemporal();
            c3d.emitirAccesoHeap(tempValorActual, tempPosicionCampo);
            c3d.emitirOperacion(operador, tempNuevoValor, tempValorActual, "1");
            c3d.emitirAsignacionHeap(tempPosicionCampo, tempNuevoValor);
            return esPrefijo ? tempNuevoValor : tempValorActual;
        }

        int offset = obtenerOffset(nombre);
        String tempPosicion = calcularPosicionStack(offset);
        String tempValorActual = c3d.nuevoTemporal();
        c3d.emitirAccesoStack(tempValorActual, tempPosicion);
        c3d.emitirOperacion(operador, tempNuevoValor, tempValorActual, "1");
        c3d.emitirAsignacionStack(tempPosicion, tempNuevoValor);
        return esPrefijo ? tempNuevoValor : tempValorActual;
    }

    public String leerHeap(String base, String offset) {
        String tempPosicion = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempPosicion, base, offset);
        String tempValor = c3d.nuevoTemporal();
        c3d.emitirAccesoHeap(tempValor, tempPosicion);
        return tempValor;
    }

    public void escribirHeap(String base, String offset, String valor) {
        String tempPosicion = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempPosicion, base, offset);
        c3d.emitirAsignacionHeap(tempPosicion, valor);
    }

    public void pasarParametro(int offsetStack, String valor) {
        String tempPosicion = calcularPosicionStack(offsetStack);
        c3d.emitirAsignacionStack(tempPosicion, valor);
    }

    public void registrarSitioLlamada(String nombreFuncion, int id, String etiquetaRetorno) {
        List<SitioLlamada> sitios = sitiosLlamadaPorFuncion.get(nombreFuncion);
        if (sitios == null) {
            sitios = new ArrayList<>();
            sitiosLlamadaPorFuncion.put(nombreFuncion, sitios);
        }
        sitios.add(new SitioLlamada(id, etiquetaRetorno));
    }

    public void emitirDespachadorRetorno(String nombreFuncion) {
        c3d.emitirEtiqueta("exit_" + nombreFuncion);
        List<SitioLlamada> sitios = sitiosLlamadaPorFuncion.get(nombreFuncion);
        if (sitios != null && !sitios.isEmpty()) {
            String tempPosicionRetorno = c3d.nuevoTemporal();
            c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempPosicionRetorno, "P", "1");
            String tempIdRetorno = c3d.nuevoTemporal();
            c3d.emitirAccesoStack(tempIdRetorno, tempPosicionRetorno);
            for (SitioLlamada sitio : sitios) {
                c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, tempIdRetorno, String.valueOf(sitio.id()), sitio.etiquetaRetorno());
            }
        }
        c3d.emitirGoto("L_fin_programa");
    }

    public int calcularDesplazamientoTransicion() {
        return Math.max(offsetMaximo + temporalesActivos.size() + 2, 4);
    }

    public String realizarLlamadaSubrutina(String nombreFuncion, boolean conRetorno) {
        return realizarLlamadaSubrutina(nombreFuncion, calcularDesplazamientoTransicion(), conRetorno);
    }

    public String realizarLlamadaSubrutina(String nombreFuncion, int desplazamiento, boolean conRetorno) {
        int idLlamada = ++contadorLlamadas;
        String etiquetaRetorno = "L_ret_" + idLlamada;
        registrarSitioLlamada(nombreFuncion, idLlamada, etiquetaRetorno);

        List<String> temporalesParaSalvar = new ArrayList<>(temporalesActivos);
        int baseSalvar = offsetMaximo;
        salvarTemporalesActivos(temporalesParaSalvar, baseSalvar);

        String tempPosicionRetorno = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempPosicionRetorno, "P", String.valueOf(desplazamiento + 1));
        c3d.emitirAsignacionStack(tempPosicionRetorno, String.valueOf(idLlamada));

        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "P", "P", String.valueOf(desplazamiento));

        c3d.emitirGoto("fn_" + nombreFuncion);

        c3d.emitirEtiqueta(etiquetaRetorno);

        String tempRetorno = null;
        if (conRetorno) {
            tempRetorno = c3d.nuevoTemporal();
            c3d.emitirAccesoStack(tempRetorno, "P");
        }

        c3d.emitirOperacion(TipoOperadorC3D.RESTA, "P", "P", String.valueOf(desplazamiento));

        restaurarTemporalesActivos(temporalesParaSalvar, baseSalvar);

        return tempRetorno;
    }

    private void salvarTemporalesActivos(List<String> temporales, int baseSalvar) {
        for (int i = 0; i < temporales.size(); i++) {
            String tempPosicionGuardado = calcularPosicionStack(baseSalvar + i);
            c3d.emitirAsignacionStack(tempPosicionGuardado, temporales.get(i));
        }
    }

    private void restaurarTemporalesActivos(List<String> temporales, int baseSalvar) {
        for (int i = 0; i < temporales.size(); i++) {
            String tempPosicionRestaurar = calcularPosicionStack(baseSalvar + i);
            c3d.emitirAccesoStack(temporales.get(i), tempPosicionRestaurar);
        }
    }

    public void registrarTemporalActivo(String temporal) {
        if (temporal != null && temporal.startsWith("t")) {
            temporalesActivos.add(temporal);
        }
    }

    public void desregistrarTemporalActivo(String temporal) {
        if (temporal != null) {
            temporalesActivos.remove(temporal);
        }
    }

    public void emitirPrintStr(String punteroHeap) {
        rutinas.emitirPrintStr(punteroHeap);
    }

    public void emitirImpresion(Tipo tipo, String valor) {
        TipoBase base = (tipo != null) ? tipo.getBase() : null;
        switch (base) {
            case CADENA -> emitirPrintStr(valor);
            case DECIMAL -> c3d.emitirPrintDouble(valor);
            case CARACTER -> c3d.emitirPrintChar(valor);
            case null, default -> c3d.emitirPrintInt(valor);
        }
    }

    public void emitirReadNum(String destino) {
        rutinas.emitirReadNum(destino);
    }

    public String emitirReadStr() {
        return rutinas.emitirReadStr();
    }

    public String emitirConcatStrStr(String p1, String p2) {
        return rutinas.emitirConcatStrStr(p1, p2);
    }

    public String emitirConcatStrNum(String p1, String num) {
        return rutinas.emitirConcatStrNum(p1, num);
    }

    public String emitirConcatNumStr(String num, String p2) {
        return rutinas.emitirConcatNumStr(num, p2);
    }

    public String emitirNumeroAHeap(String num) {
        return rutinas.emitirNumeroAHeap(num);
    }

    public void entrarCiclo(String etiquetaBreak, String etiquetaContinue) {
        etiquetasBreak.push(etiquetaBreak);
        etiquetasContinue.push(etiquetaContinue);
    }

    public void salirCiclo() {
        etiquetasBreak.pop();
        etiquetasContinue.pop();
    }

    public void entrarSwitch(String etiquetaBreak) {
        etiquetasBreak.push(etiquetaBreak);
    }

    public void salirSwitch() {
        etiquetasBreak.pop();
    }

    public void saltarBreak() {
        if (!etiquetasBreak.isEmpty()) {
            c3d.emitirGoto(etiquetasBreak.peek());
        }
    }

    public void saltarContinue() {
        if (!etiquetasContinue.isEmpty()) {
            c3d.emitirGoto(etiquetasContinue.peek());
        }
    }

    public int obtenerOffset(String nombre) {
        if (offsetsLocales.containsKey(nombre)) {
            return offsetsLocales.get(nombre);
        }
        Simbolo simbolo = tablaSimbolos.buscarSimbolo(nombre);
        if (simbolo != null) {
            return simbolo.getDesplazamiento();
        }
        return 0;
    }

    public String resolverEtiquetaConstructor(String nombreClase, List<Tipo> tiposParametros) {
        Tipo tipoClase = tablaSimbolos.buscarTipoCompuesto(nombreClase);
        if (tipoClase != null && tipoClase.getConstructores().size() > 1) {
            return nombreClase + "_constructor__" + Tipo.unirTiposParametros(tiposParametros);
        }
        return nombreClase + "_constructor";
    }

    public String resolverEtiquetaMetodo(String nombreClase, String nombreMetodo, List<Tipo> tiposParametros) {
        if (requiereCalificarMetodo(nombreClase, nombreMetodo)) {
            return nombreClase + "__" + nombreMetodo + "__" + Tipo.unirTiposParametros(tiposParametros);
        }
        return nombreMetodo;
    }

    private boolean requiereCalificarMetodo(String nombreClase, String nombreMetodo) {
        Tipo tipoClase = tablaSimbolos.buscarTipoCompuesto(nombreClase);
        if (tipoClase != null && tipoClase.obtenerSobrecargasMetodo(nombreMetodo).size() > 1) {
            return true;
        }
        if (tablaSimbolos.buscarFuncionGlobal(nombreMetodo) != null) {
            return true;
        }
        long conteoMetodos = tablaSimbolos.obtenerTodosLosSimbolos().stream()
                .filter(s -> "metodo".equals(s.getCategoria()) && nombreMetodo.equals(s.getNombre()))
                .count();
        return conteoMetodos > 1;
    }

    public String obtenerNombreClaseThis() {
        Tipo tipoThis = tiposLocales.get("this");
        return (tipoThis != null) ? tipoThis.getNombreTipo() : null;
    }

    public Tipo resolverTipoEstructura(NodoAST estructura) {
        if (estructura == null) {
            return null;
        }
        switch (estructura) {
            case NodoIdentificador id -> {
                if (tiposLocales.containsKey(id.getNombre())) {
                    return tiposLocales.get(id.getNombre());
                }
                if (esCampoDeThis(id.getNombre())) {
                    Tipo tipoClaseThis = obtenerTipoClaseThis();
                    if (tipoClaseThis != null && tipoClaseThis.buscarCampo(id.getNombre()) != null) {
                        return tipoClaseThis.buscarCampo(id.getNombre());
                    }
                }
                Simbolo s = tablaSimbolos.buscarSimbolo(id.getNombre());
                if (s != null && s.getTipo() != null) {
                    return s.getTipo();
                }
            }
            case NodoAccesoMiembro miembro -> {
                Tipo tipoBase = resolverTipoEstructura(miembro.getEstructura());
                if (tipoBase != null) {
                    Tipo tipoComp = tablaSimbolos.buscarTipoCompuesto(tipoBase.getNombreTipo());
                    Tipo tipoEfectivo = (tipoComp != null) ? tipoComp : tipoBase;
                    Tipo tipoCampo = tipoEfectivo.buscarCampo(miembro.getNombreCampo());
                    if (tipoCampo != null) {
                        return tipoCampo;
                    }
                }
            }
            case NodoAccesoArreglo accesoArr -> {
                Tipo tipoArr = resolverTipoEstructura(accesoArr.getEstructura());
                if (tipoArr != null && tipoArr.getBase() == TipoBase.ARREGLO) {
                    return tipoArr.getTipoComponente();
                }
            }
            case NodoLlamadaMetodo llamadaMetodo -> {
                if (llamadaMetodo.getMetodoResuelto() != null) {
                    return llamadaMetodo.getMetodoResuelto().getTipoRetorno();
                }
            }
            case NodoLlamadaFuncion llamadaFuncion -> {
                if (llamadaFuncion.getFuncionResuelta() != null) {
                    return llamadaFuncion.getFuncionResuelta().getTipoRetorno();
                }
            }
            default -> {}
        }
        try {
            return estructura.comprobar(tablaSimbolos, new ArrayList<>());
        } catch (Exception e) {
            return null;
        }
    }

    public int obtenerOffsetCampo(NodoAST estructura, String nombreCampo) {
        Tipo tipoEstructura = resolverTipoEstructura(estructura);
        if (tipoEstructura == null) {
            return 0;
        }
        Tipo tipoCompuesto = tablaSimbolos.buscarTipoCompuesto(tipoEstructura.getNombreTipo());
        Tipo tipoEfectivo = (tipoCompuesto != null) ? tipoCompuesto : tipoEstructura;
        return Math.max(0, tipoEfectivo.obtenerOffsetCampo(nombreCampo));
    }

    public boolean esCampoDeThis(String nombre) {
        if (!offsetsLocales.containsKey("this") || offsetsLocales.containsKey(nombre)) {
            return false;
        }
        Tipo tipoClase = obtenerTipoClaseThis();
        if (tipoClase == null) {
            return false;
        }
        return tipoClase.obtenerOffsetCampo(nombre) >= 0;
    }

    public int obtenerOffsetCampoThis(String nombre) {
        Tipo tipoClase = obtenerTipoClaseThis();
        if (tipoClase == null) {
            return 0;
        }
        return Math.max(0, tipoClase.obtenerOffsetCampo(nombre));
    }

    public boolean esMetodoDeThis(String nombreMetodo) {
        if (!offsetsLocales.containsKey("this")) {
            return false;
        }
        Tipo tipoClase = obtenerTipoClaseThis();
        if (tipoClase == null) {
            return false;
        }
        return tipoClase.buscarMetodo(nombreMetodo) != null;
    }

    public int registrarParametros(NodoFuncion funcion) {
        int offsetActual = 2;
        if (funcion.isEsMetodo() || esConstructor(funcion)) {
            registrarOffsetLocal("this", offsetActual++);
            String claseContenedora = obtenerClaseContenedora(funcion);
            if (claseContenedora != null) {
                registrarTipoLocal("this", new Tipo(TipoBase.OBJETO_CLASE, claseContenedora));
            }
        }
        if (funcion.getParametros() != null) {
            for (Parametro parametro : funcion.getParametros()) {
                registrarOffsetLocal(parametro.getNombre(), offsetActual++);
                if (parametro.getTipo() != null) {
                    registrarTipoLocal(parametro.getNombre(), parametro.getTipo());
                }
            }
        }
        return offsetActual;
    }

    private boolean esConstructor(NodoFuncion funcion) {
        return funcion.getNombre().endsWith("_constructor");
    }

    private String obtenerClaseContenedora(NodoFuncion funcion) {
        if (funcion.getClaseContenedora() != null) {
            return funcion.getClaseContenedora();
        }
        if (esConstructor(funcion)) {
            int indiceConstructor = funcion.getNombre().indexOf("_constructor");
            return funcion.getNombre().substring(0, indiceConstructor);
        }
        return null;
    }

    public int asignarOffsetsLocales(List<NodoAST> instrucciones, int offsetBase) {
        if (instrucciones == null) {
            return offsetBase;
        }
        int offsetActual = offsetBase;
        for (NodoAST instruccion : instrucciones) {
            offsetActual = procesarInstruccionParaOffsets(instruccion, offsetActual);
        }
        return offsetActual;
    }

    private int procesarInstruccionParaOffsets(NodoAST instruccion, int offsetActual) {
        switch (instruccion) {
            case NodoDeclaracion declaracion -> {
                return registrarDeclaracionSiNueva(declaracion, offsetActual);
            }
            case NodoIf nodoIf -> {
                offsetActual = asignarOffsetsLocales(nodoIf.getInstruccionesSi(), offsetActual);
                return asignarOffsetsLocales(nodoIf.getInstruccionesAliter(), offsetActual);
            }
            case NodoWhile nodoWhile -> {
                return asignarOffsetsLocales(nodoWhile.getInstrucciones(), offsetActual);
            }
            case NodoDoWhile nodoDoWhile -> {
                return asignarOffsetsLocales(nodoDoWhile.getCuerpo(), offsetActual);
            }
            case NodoFor nodoFor -> {
                if (nodoFor.getInicializacion() instanceof NodoDeclaracion declaracionInicial) {
                    offsetActual = registrarDeclaracionSiNueva(declaracionInicial, offsetActual);
                }
                return asignarOffsetsLocales(nodoFor.getCuerpo(), offsetActual);
            }
            case NodoSwitch nodoSwitch -> {
                if (nodoSwitch.getCasos() != null) {
                    for (NodoSwitch.CasoSwitch caso : nodoSwitch.getCasos()) {
                        offsetActual = asignarOffsetsLocales(caso.getInstrucciones(), offsetActual);
                    }
                }
                return asignarOffsetsLocales(nodoSwitch.getCasoDefecto(), offsetActual);
            }
            case null, default -> {
                return offsetActual;
            }
        }
    }

    private int registrarDeclaracionSiNueva(NodoDeclaracion declaracion, int offsetActual) {
        if (offsetsLocales.containsKey(declaracion.getNombre())) {
            return offsetActual;
        }
        offsetsLocales.put(declaracion.getNombre(), offsetActual);
        if (declaracion.getTipoDeclarado() != null) {
            tiposLocales.put(declaracion.getNombre(), declaracion.getTipoDeclarado());
        }
        return offsetActual + 1;
    }

    public void registrarOffsetLocal(String nombre, int offset) {
        offsetsLocales.put(nombre, offset);
    }

    public void registrarTipoLocal(String nombre, Tipo tipo) {
        tiposLocales.put(nombre, tipo);
    }

    public void limpiarLocales() {
        offsetsLocales.clear();
        tiposLocales.clear();
        temporalesActivos.clear();
    }

    public void limpiar() {
        c3d.limpiar();
        etiquetasBreak.clear();
        etiquetasContinue.clear();
        limpiarLocales();
        funcionActual = null;
        contadorLlamadas = 0;
        sitiosLlamadaPorFuncion.clear();
        offsetMaximo = 2;
    }
}
