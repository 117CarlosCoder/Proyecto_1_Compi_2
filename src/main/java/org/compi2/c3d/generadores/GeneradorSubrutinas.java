package org.compi2.c3d.generadores;

import lombok.Setter;
import org.compi2.ast.NodoAST;
import org.compi2.ast.expresiones.NodoInstanciacionObjeto;
import org.compi2.ast.expresiones.NodoLlamadaFuncion;
import org.compi2.ast.expresiones.NodoLlamadaMetodo;
import org.compi2.ast.sentencias.NodoFuncion;
import org.compi2.ast.sentencias.Parametro;
import org.compi2.c3d.ContenedorC3D;
import org.compi2.c3d.Cuarteta;
import org.compi2.c3d.TipoOperadorC3D;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.List;

public class GeneradorSubrutinas {

    private static final int MARCO_TRANSICION = 20;
    private static final int TAMANO_OBJETO_DEFECTO = 10;

    private final ContextoC3D contexto;
    @Setter
    private GeneradorExpresiones generadorExpresiones;
    @Setter
    private GeneradorSentencias generadorSentencias;

    public GeneradorSubrutinas(ContextoC3D contexto) {
        this.contexto = contexto;
    }

    public void generarFuncion(NodoFuncion fn) {
        contexto.limpiarLocales();
        int offsetFinal = registrarParametrosLocales(fn);
        contexto.asignarOffsetsLocales(fn.getCuerpo(), offsetFinal);

        emitirInicioFuncion(fn);
        generarCuerpoFuncion(fn);
        emitirFinFuncion(fn);

        contexto.limpiarLocales();
    }

    private int registrarParametrosLocales(NodoFuncion fn) {
        int offsetActual = 1;
        if (fn.isEsMetodo() || esConstructor(fn)) {
            contexto.registrarOffsetLocal("this", offsetActual++);
            String claseCont = obtenerClaseContenedora(fn);
            if (claseCont != null) {
                contexto.registrarTipoLocal("this", new Tipo(TipoBase.OBJETO_CLASE, claseCont));
            }
        }
        if (fn.getParametros() != null) {
            for (Parametro p : fn.getParametros()) {
                contexto.registrarOffsetLocal(p.getNombre(), offsetActual++);
                if (p.getTipo() != null) {
                    contexto.registrarTipoLocal(p.getNombre(), p.getTipo());
                }
            }
        }
        return offsetActual;
    }

    private boolean esConstructor(NodoFuncion fn) {
        return fn.getNombre().endsWith("_constructor");
    }

    private String obtenerClaseContenedora(NodoFuncion fn) {
        if (fn.getClaseContenedora() != null) {
            return fn.getClaseContenedora();
        }
        return esConstructor(fn) ? fn.getNombre().substring(0, fn.getNombre().indexOf("_constructor")) : null;
    }

    private void emitirInicioFuncion(NodoFuncion fn) {
        contexto.getC3d().emitir(new Cuarteta(TipoOperadorC3D.INICIO_FUNCION, null, null, fn.getNombre()));
    }

    private void emitirFinFuncion(NodoFuncion fn) {
        contexto.getC3d().emitir(new Cuarteta(TipoOperadorC3D.FIN_FUNCION, null, null, fn.getNombre()));
    }

    private void generarCuerpoFuncion(NodoFuncion fn) {
        if (fn.getCuerpo() == null || generadorSentencias == null) return;
        for (NodoAST inst : fn.getCuerpo()) {
            generadorSentencias.generarSentencia(inst);
        }
    }

    public String generarLlamadaFuncion(NodoLlamadaFuncion llamada) {
        if (contexto.esMetodoDeThis(llamada.getNombreFuncion())) {
            ContenedorC3D c3d = contexto.getC3d();
            int offThis = contexto.obtenerOffset("this");
            String tThisPos = c3d.nuevoTemporal();
            c3d.emitirOperacion(TipoOperadorC3D.SUMA, tThisPos, "P", String.valueOf(offThis));
            String tThis = c3d.nuevoTemporal();
            c3d.emitirAccesoStack(tThis, tThisPos);

            pasarValorAPosicion(tThis, MARCO_TRANSICION + 1);
            pasarArgumentos(llamada.getArgumentos(), MARCO_TRANSICION, 2);
            return realizarLlamadaYObtenerRetorno(llamada.getNombreFuncion(), MARCO_TRANSICION);
        }
        pasarArgumentos(llamada.getArgumentos(), MARCO_TRANSICION, 1);
        return realizarLlamadaYObtenerRetorno(llamada.getNombreFuncion(), MARCO_TRANSICION);
    }

    public String generarLlamadaMetodo(NodoLlamadaMetodo metodo) {
        String tThis = generadorExpresiones.generarExpresion(metodo.getEstructura());
        pasarValorAPosicion(tThis, MARCO_TRANSICION + 1);
        pasarArgumentos(metodo.getArgumentos(), MARCO_TRANSICION, 2);
        return realizarLlamadaYObtenerRetorno(metodo.getNombreMetodo(), MARCO_TRANSICION);
    }

    public String generarInstanciacionObjeto(NodoInstanciacionObjeto instObj) {
        String tBase = reservarEspacioObjeto(instObj);

        boolean tieneArgs = instObj.getArgumentos() != null && !instObj.getArgumentos().isEmpty();
        boolean tieneCtor = tieneArgs || contexto.getTablaSimbolos().tieneConstructor(instObj.getNombreClase());
        if (tieneCtor) {
            pasarValorAPosicion(tBase, MARCO_TRANSICION + 1);
            pasarArgumentos(instObj.getArgumentos(), MARCO_TRANSICION, 2);
            realizarLlamadaSinRetorno(instObj.getNombreClase() + "_constructor", MARCO_TRANSICION);
        }
        return tBase;
    }

    private String reservarEspacioObjeto(NodoInstanciacionObjeto instObj) {
        ContenedorC3D c3d = contexto.getC3d();
        String tBase = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tBase, "H");

        int tamano = TAMANO_OBJETO_DEFECTO;
        Tipo tComp = contexto.getTablaSimbolos().buscarTipoCompuesto(instObj.getNombreClase());
        if (tComp != null && tComp.getCampos() != null) {
            tamano = Math.max(1, tComp.getCampos().size());
        }
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", String.valueOf(tamano));
        return tBase;
    }

    private void pasarValorAPosicion(String valor, int offsetAbsoluto) {
        ContenedorC3D c3d = contexto.getC3d();
        String tPos = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tPos, "P", String.valueOf(offsetAbsoluto));
        c3d.emitirAsignacionStack(tPos, valor);
    }

    private void pasarArgumentos(List<NodoAST> args, int marcoTransicion, int offsetInicial) {
        if (args == null) return;
        for (int i = 0; i < args.size(); i++) {
            String tArg = generadorExpresiones.generarExpresion(args.get(i));
            pasarValorAPosicion(tArg, marcoTransicion + offsetInicial + i);
        }
    }

    private void emitirMoverP(int marcoTransicion) {
        contexto.getC3d().emitirOperacion(TipoOperadorC3D.SUMA, "P", "P", String.valueOf(marcoTransicion));
    }

    private void emitirRestaurarP(int marcoTransicion) {
        contexto.getC3d().emitirOperacion(TipoOperadorC3D.RESTA, "P", "P", String.valueOf(marcoTransicion));
    }

    private String realizarLlamadaYObtenerRetorno(String nombreFuncion, int marcoTransicion) {
        ContenedorC3D c3d = contexto.getC3d();
        emitirMoverP(marcoTransicion);
        c3d.emitirCall(nombreFuncion);
        String tRet = c3d.nuevoTemporal();
        c3d.emitirAccesoStack(tRet, "P");
        emitirRestaurarP(marcoTransicion);
        return tRet;
    }

    private void realizarLlamadaSinRetorno(String nombreFuncion, int marcoTransicion) {
        emitirMoverP(marcoTransicion);
        contexto.getC3d().emitirCall(nombreFuncion);
        emitirRestaurarP(marcoTransicion);
    }
}