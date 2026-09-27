package org.compi2.c3d.generadores;

import lombok.Setter;
import org.compi2.ast.NodoAST;
import org.compi2.ast.sentencias.*;
import org.compi2.c3d.ContenedorC3D;
import org.compi2.c3d.TipoOperadorC3D;

import java.util.List;

public class GeneradorControlFlujo {
    private final ContextoC3D contexto;
    @Setter
    private GeneradorExpresiones generadorExpresiones;
    @Setter
    private GeneradorSentencias generadorSentencias;

    public GeneradorControlFlujo(ContextoC3D contexto) {
        this.contexto = contexto;
    }

    public void generarIf(NodoIf nodoIf) {
        ContenedorC3D c3d = contexto.getC3d();
        String cond = generadorExpresiones.generarExpresion(nodoIf.getCondicion());
        String lThen = c3d.nuevaEtiqueta();
        String lElse = c3d.nuevaEtiqueta();
        String lFin = c3d.nuevaEtiqueta();

        c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, cond, "0", lThen);
        c3d.emitirGoto(lElse);

        c3d.emitirEtiqueta(lThen);
        generarBloque(nodoIf.getInstruccionesSi());
        c3d.emitirGoto(lFin);

        c3d.emitirEtiqueta(lElse);
        generarBloque(nodoIf.getInstruccionesAliter());
        c3d.emitirEtiqueta(lFin);
    }

    public void generarWhile(NodoWhile nodoWhile) {
        ContenedorC3D c3d = contexto.getC3d();
        String lInicio = c3d.nuevaEtiqueta();
        String lCuerpo = c3d.nuevaEtiqueta();
        String lFin = c3d.nuevaEtiqueta();

        ejecutarConBreakContinue(lFin, lInicio, () -> {
            c3d.emitirEtiqueta(lInicio);
            String cond = generadorExpresiones.generarExpresion(nodoWhile.getCondicion());
            c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, cond, "0", lCuerpo);
            c3d.emitirGoto(lFin);

            c3d.emitirEtiqueta(lCuerpo);
            generarBloque(nodoWhile.getInstrucciones());
            c3d.emitirGoto(lInicio);

            c3d.emitirEtiqueta(lFin);
        });
    }

    public void generarDoWhile(NodoDoWhile nodoDoWhile) {
        ContenedorC3D c3d = contexto.getC3d();
        String lInicio = c3d.nuevaEtiqueta();
        String lCond = c3d.nuevaEtiqueta();
        String lFin = c3d.nuevaEtiqueta();

        ejecutarConBreakContinue(lFin, lCond, () -> {
            c3d.emitirEtiqueta(lInicio);
            generarBloque(nodoDoWhile.getCuerpo());

            c3d.emitirEtiqueta(lCond);
            String cond = generadorExpresiones.generarExpresion(nodoDoWhile.getCondicion());
            c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, cond, "0", lInicio);
            c3d.emitirEtiqueta(lFin);
        });
    }

    public void generarFor(NodoFor nodoFor) {
        ContenedorC3D c3d = contexto.getC3d();
        if (nodoFor.getInicializacion() != null) {
            generadorSentencias.generarSentencia(nodoFor.getInicializacion());
        }

        String lInicio = c3d.nuevaEtiqueta();
        String lCuerpo = c3d.nuevaEtiqueta();
        String lPaso = c3d.nuevaEtiqueta();
        String lFin = c3d.nuevaEtiqueta();

        ejecutarConBreakContinue(lFin, lPaso, () -> {
            c3d.emitirEtiqueta(lInicio);
            emitirCondicionFor(nodoFor.getCondicion(), lCuerpo, lFin);

            c3d.emitirEtiqueta(lCuerpo);
            generarBloque(nodoFor.getCuerpo());

            c3d.emitirEtiqueta(lPaso);
            if (nodoFor.getPaso() != null) {
                generadorSentencias.generarSentencia(nodoFor.getPaso());
            }
            c3d.emitirGoto(lInicio);

            c3d.emitirEtiqueta(lFin);
        });
    }

    private void emitirCondicionFor(NodoAST condicion, String lCuerpo, String lFin) {
        ContenedorC3D c3d = contexto.getC3d();
        if (condicion != null) {
            String cond = generadorExpresiones.generarExpresion(condicion);
            c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, cond, "0", lCuerpo);
            c3d.emitirGoto(lFin);
        } else {
            c3d.emitirGoto(lCuerpo);
        }
    }

    public void generarBreak() {
        if (contexto.tieneBreak()) {
            contexto.getC3d().emitirGoto(contexto.peekBreak());
        }
    }

    public void generarContinue() {
        if (contexto.tieneContinue()) {
            contexto.getC3d().emitirGoto(contexto.peekContinue());
        }
    }

    public void generarRetorno(NodoRetorno nodoRet) {
        ContenedorC3D c3d = contexto.getC3d();
        if (nodoRet.getExpresion() != null) {
            String val = generadorExpresiones.generarExpresion(nodoRet.getExpresion());
            c3d.emitirAsignacionStack("P", val);
        }
        c3d.emitirReturn();
    }

    private void generarBloque(List<NodoAST> instrucciones) {
        if (instrucciones == null) return;
        for (NodoAST inst : instrucciones) {
            generadorSentencias.generarSentencia(inst);
        }
    }

    private void ejecutarConBreakContinue(String etiquetaBreak, String etiquetaContinue, Runnable cuerpo) {
        contexto.pushBreak(etiquetaBreak);
        contexto.pushContinue(etiquetaContinue);
        cuerpo.run();
        contexto.popBreak();
        contexto.popContinue();
    }
}