package org.compi2.c3d.generadores;

import org.compi2.ast.NodoAST;
import org.compi2.ast.expresiones.NodoAccesoArreglo;
import org.compi2.ast.expresiones.NodoAccesoMiembro;
import org.compi2.ast.expresiones.NodoIdentificador;
import org.compi2.ast.expresiones.NodoLlamadaFuncion;
import org.compi2.ast.expresiones.NodoLlamadaMetodo;
import org.compi2.ast.expresiones.NodoOperacionUnaria;
import org.compi2.ast.sentencias.*;
import org.compi2.c3d.ContenedorC3D;
import org.compi2.c3d.Cuarteta;
import org.compi2.c3d.TipoOperadorC3D;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.ArrayList;

public class GeneradorSentencias {
    private final ContextoC3D contexto;
    private final GeneradorExpresiones generadorExpresiones;
    private final GeneradorControlFlujo generadorControlFlujo;
    private final GeneradorSubrutinas generadorSubrutinas;

    public GeneradorSentencias(ContextoC3D contexto,
                               GeneradorExpresiones generadorExpresiones,
                               GeneradorControlFlujo generadorControlFlujo,
                               GeneradorSubrutinas generadorSubrutinas) {
        this.contexto = contexto;
        this.generadorExpresiones = generadorExpresiones;
        this.generadorControlFlujo = generadorControlFlujo;
        this.generadorSubrutinas = generadorSubrutinas;
    }

    public void generarSentencia(NodoAST nodo) {
        if (nodo == null) return;

        if (nodo instanceof NodoDeclaracion decl) {
            generarDeclaracion(decl);
        } else if (nodo instanceof NodoAsignacion asig) {
            generarAsignacion(asig);
        } else if (nodo instanceof NodoLectura lectura) {
            generarLectura(lectura);
        } else if (nodo instanceof NodoIf nodoIf) {
            generadorControlFlujo.generarIf(nodoIf);
        } else if (nodo instanceof NodoWhile nodoWhile) {
            generadorControlFlujo.generarWhile(nodoWhile);
        } else if (nodo instanceof NodoDoWhile nodoDoWhile) {
            generadorControlFlujo.generarDoWhile(nodoDoWhile);
        } else if (nodo instanceof NodoFor nodoFor) {
            generadorControlFlujo.generarFor(nodoFor);
        } else if (nodo instanceof NodoBreak) {
            generadorControlFlujo.generarBreak();
        } else if (nodo instanceof NodoContinue) {
            generadorControlFlujo.generarContinue();
        } else if (nodo instanceof NodoRetorno nodoRet) {
            generadorControlFlujo.generarRetorno(nodoRet);
        } else if (nodo instanceof NodoImprimir nodoImp) {
            generarImprimir(nodoImp);
        } else if (nodo instanceof NodoOperacionUnaria unaria) {
            generadorExpresiones.generarExpresion(unaria);
        } else if (nodo instanceof NodoLlamadaFuncion llamada) {
            generadorExpresiones.generarExpresion(llamada);
        } else if (nodo instanceof NodoLlamadaMetodo metodo) {
            generadorExpresiones.generarExpresion(metodo);
        }
    }

    public void generarLectura(NodoLectura lectura) {
        ContenedorC3D c3d = contexto.getC3d();
        if (lectura.getDestino() == null) {
            c3d.emitir(new Cuarteta(TipoOperadorC3D.READ_NUM, null, null, null));
            return;
        }

        Tipo tipo = lectura.getDestino().comprobar(contexto.getTablaSimbolos(), new ArrayList<>());
        boolean esCadena = (tipo != null && tipo.getBase() == TipoBase.CADENA);
        TipoOperadorC3D opLectura = esCadena ? TipoOperadorC3D.READ_STR : TipoOperadorC3D.READ_NUM;

        String tVal = c3d.nuevoTemporal();
        c3d.emitir(new Cuarteta(opLectura, null, null, tVal));

        if (lectura.getDestino() instanceof NodoIdentificador id) {
            escribirEnVariable(id.getNombre(), tVal);
        } else if (lectura.getDestino() instanceof NodoAccesoArreglo accArr) {
            String tArr = generadorExpresiones.generarExpresion(accArr.getEstructura());
            String tIdx = generadorExpresiones.generarExpresion(accArr.getIndice());
            String tPos = c3d.nuevoTemporal();
            c3d.emitirOperacion(TipoOperadorC3D.SUMA, tPos, tArr, tIdx);
            c3d.emitirAsignacionHeap(tPos, tVal);
        } else if (lectura.getDestino() instanceof NodoAccesoMiembro accM) {
            String tObj = generadorExpresiones.generarExpresion(accM.getEstructura());
            int offset = contexto.obtenerOffsetCampo(accM.getEstructura(), accM.getNombreCampo());
            String tPos = c3d.nuevoTemporal();
            c3d.emitirOperacion(TipoOperadorC3D.SUMA, tPos, tObj, String.valueOf(offset));
            c3d.emitirAsignacionHeap(tPos, tVal);
        }
    }

    public void generarDeclaracion(NodoDeclaracion decl) {
        if (decl.getValorInicial() == null) return;
        String val = generadorExpresiones.generarExpresion(decl.getValorInicial());
        escribirEnVariable(decl.getNombre(), val);
    }

    public void generarAsignacion(NodoAsignacion asig) {
        if (asig.getDestino() instanceof NodoIdentificador id) {
            String val = generadorExpresiones.generarExpresion(asig.getValor());
            escribirEnVariable(id.getNombre(), val);
        } else if (asig.getDestino() instanceof NodoAccesoArreglo accArr) {
            asignarEnArreglo(accArr, asig.getValor());
        } else if (asig.getDestino() instanceof NodoAccesoMiembro accM) {
            asignarEnMiembro(accM, asig.getValor());
        }
    }

    private void escribirEnVariable(String nombre, String valor) {
        ContenedorC3D c3d = contexto.getC3d();
        if (contexto.esCampoDeThis(nombre)) {
            int offCampo = contexto.obtenerOffsetCampoThis(nombre);
            int offThis = contexto.obtenerOffset("this");
            String tThisPos = c3d.nuevoTemporal();
            c3d.emitirOperacion(TipoOperadorC3D.SUMA, tThisPos, "P", String.valueOf(offThis));
            String tThis = c3d.nuevoTemporal();
            c3d.emitirAccesoStack(tThis, tThisPos);
            String tPos = c3d.nuevoTemporal();
            c3d.emitirOperacion(TipoOperadorC3D.SUMA, tPos, tThis, String.valueOf(offCampo));
            c3d.emitirAsignacionHeap(tPos, valor);
            return;
        }
        int offset = contexto.obtenerOffset(nombre);
        String tPos = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tPos, "P", String.valueOf(offset));
        c3d.emitirAsignacionStack(tPos, valor);
    }

    private void asignarEnArreglo(NodoAccesoArreglo accArr, NodoAST valorExpr) {
        ContenedorC3D c3d = contexto.getC3d();
        String tArr = generadorExpresiones.generarExpresion(accArr.getEstructura());
        String tIdx = generadorExpresiones.generarExpresion(accArr.getIndice());
        String val = generadorExpresiones.generarExpresion(valorExpr);
        String tPos = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tPos, tArr, tIdx);
        c3d.emitirAsignacionHeap(tPos, val);
    }

    private void asignarEnMiembro(NodoAccesoMiembro accM, NodoAST valorExpr) {
        ContenedorC3D c3d = contexto.getC3d();
        String tObj = generadorExpresiones.generarExpresion(accM.getEstructura());
        int offset = contexto.obtenerOffsetCampo(accM.getEstructura(), accM.getNombreCampo());
        String val = generadorExpresiones.generarExpresion(valorExpr);
        String tPos = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tPos, tObj, String.valueOf(offset));
        c3d.emitirAsignacionHeap(tPos, val);
    }

    public void generarImprimir(NodoImprimir nodoImp) {
        ContenedorC3D c3d = contexto.getC3d();
        if (nodoImp.getExpresiones() != null) {
            for (NodoAST expr : nodoImp.getExpresiones()) {
                imprimirExpresion(expr, c3d);
            }
        }
        if (nodoImp.isConSaltoLinea()) {
            c3d.emitirPrintChar("10");
        }
    }

    private void imprimirExpresion(NodoAST expr, ContenedorC3D c3d) {
        Tipo t = expr.comprobar(contexto.getTablaSimbolos(), new ArrayList<>());
        String val = generadorExpresiones.generarExpresion(expr);
        TipoBase base = (t != null) ? t.getBase() : null;

        if (base == TipoBase.CADENA) {
            c3d.emitirPrintStr(val);
        } else if (base == TipoBase.DECIMAL) {
            c3d.emitirPrintDouble(val);
        } else if (base == TipoBase.CARACTER) {
            c3d.emitirPrintChar(val);
        } else {
            c3d.emitirPrintInt(val);
        }
    }
}