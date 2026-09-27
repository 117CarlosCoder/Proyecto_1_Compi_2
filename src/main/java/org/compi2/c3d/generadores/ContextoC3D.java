package org.compi2.c3d.generadores;

import lombok.Getter;
import org.compi2.analisis_semantico.Simbolo;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.ast.expresiones.NodoIdentificador;
import org.compi2.ast.sentencias.*;
import org.compi2.c3d.ContenedorC3D;
import org.compi2.tipos.Tipo;

import java.util.*;

public class ContextoC3D {
    @Getter
    private final ContenedorC3D c3d;
    @Getter
    private final TablaSimbolos tablaSimbolos;

    private final Deque<String> etiquetasBreak = new ArrayDeque<>();
    private final Deque<String> etiquetasContinue = new ArrayDeque<>();
    private final Map<String, Integer> offsetsLocales = new HashMap<>();
    private final Map<String, Tipo> tiposLocales = new HashMap<>();

    public ContextoC3D(TablaSimbolos tablaSimbolos) {
        this.c3d = new ContenedorC3D();
        this.tablaSimbolos = (tablaSimbolos != null) ? tablaSimbolos : new TablaSimbolos();
    }

    public ContextoC3D(ContenedorC3D c3d, TablaSimbolos tablaSimbolos) {
        this.c3d = (c3d != null) ? c3d : new ContenedorC3D();
        this.tablaSimbolos = (tablaSimbolos != null) ? tablaSimbolos : new TablaSimbolos();
    }

    public int obtenerOffset(String nombre) {
        if (offsetsLocales.containsKey(nombre)) {
            return offsetsLocales.get(nombre);
        }
        Simbolo s = tablaSimbolos.buscarSimbolo(nombre);
        return (s != null) ? s.getDesplazamiento() : 0;
    }

    public Tipo resolverTipoEstructura(NodoAST estructura) {
        if (estructura instanceof NodoIdentificador id && tiposLocales.containsKey(id.getNombre())) {
            return tiposLocales.get(id.getNombre());
        }
        return estructura.comprobar(tablaSimbolos, new ArrayList<>());
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
        if (!offsetsLocales.containsKey("this"))
            return false;
        if (offsetsLocales.containsKey(nombre))
            return false;
        Tipo tipoThis = tiposLocales.get("this");
        if (tipoThis == null)
            return false;
        Tipo tipoClase = tablaSimbolos.buscarTipoCompuesto(tipoThis.getNombreTipo());
        if (tipoClase == null)
            tipoClase = tipoThis;
        return tipoClase.obtenerOffsetCampo(nombre) >= 0;
    }

    public int obtenerOffsetCampoThis(String nombre) {
        Tipo tipoThis = tiposLocales.get("this");
        if (tipoThis == null)
            return 0;
        Tipo tipoClase = tablaSimbolos.buscarTipoCompuesto(tipoThis.getNombreTipo());
        if (tipoClase == null)
            tipoClase = tipoThis;
        return Math.max(0, tipoClase.obtenerOffsetCampo(nombre));
    }

    public boolean esMetodoDeThis(String nombreMetodo) {
        if (!offsetsLocales.containsKey("this"))
            return false;
        Tipo tipoThis = tiposLocales.get("this");
        if (tipoThis == null)
            return false;
        Tipo tipoClase = tablaSimbolos.buscarTipoCompuesto(tipoThis.getNombreTipo());
        if (tipoClase == null)
            tipoClase = tipoThis;
        return tipoClase.buscarMetodo(nombreMetodo) != null;
    }

    public int asignarOffsetsLocales(List<NodoAST> instrucciones, int offsetBase) {
        if (instrucciones == null)
            return offsetBase;
        int off = offsetBase;
        for (NodoAST inst : instrucciones) {
            off = procesarInstruccionParaOffsets(inst, off);
        }
        return off;
    }

    private int procesarInstruccionParaOffsets(NodoAST inst, int off) {
        if (inst instanceof NodoDeclaracion decl) {
            return registrarDeclaracionSiNueva(decl, off);
        } else if (inst instanceof NodoIf nIf) {
            off = asignarOffsetsLocales(nIf.getInstruccionesSi(), off);
            return asignarOffsetsLocales(nIf.getInstruccionesAliter(), off);
        } else if (inst instanceof NodoWhile nW) {
            return asignarOffsetsLocales(nW.getInstrucciones(), off);
        } else if (inst instanceof NodoDoWhile nDw) {
            return asignarOffsetsLocales(nDw.getCuerpo(), off);
        } else if (inst instanceof NodoFor nF) {
            if (nF.getInicializacion() instanceof NodoDeclaracion declIni) {
                off = registrarDeclaracionSiNueva(declIni, off);
            }
            return asignarOffsetsLocales(nF.getCuerpo(), off);
        } else if (inst instanceof NodoSwitch nSw) {
            if (nSw.getCasos() != null) {
                for (NodoSwitch.CasoSwitch c : nSw.getCasos()) {
                    off = asignarOffsetsLocales(c.getInstrucciones(), off);
                }
            }
            return asignarOffsetsLocales(nSw.getCasoDefecto(), off);
        }
        return off;
    }

    private int registrarDeclaracionSiNueva(NodoDeclaracion decl, int offsetActual) {
        if (offsetsLocales.containsKey(decl.getNombre())) {
            return offsetActual;
        }
        offsetsLocales.put(decl.getNombre(), offsetActual);
        if (decl.getTipoDeclarado() != null) {
            tiposLocales.put(decl.getNombre(), decl.getTipoDeclarado());
        }
        return offsetActual + 1;
    }

    public void pushBreak(String etiqueta) {
        etiquetasBreak.push(etiqueta);
    }

    public String popBreak() {
        return etiquetasBreak.pop();
    }

    public String peekBreak() {
        return etiquetasBreak.peek();
    }

    public boolean tieneBreak() {
        return !etiquetasBreak.isEmpty();
    }

    public void pushContinue(String etiqueta) {
        etiquetasContinue.push(etiqueta);
    }

    public String popContinue() {
        return etiquetasContinue.pop();
    }

    public String peekContinue() {
        return etiquetasContinue.peek();
    }

    public boolean tieneContinue() {
        return !etiquetasContinue.isEmpty();
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
    }
}