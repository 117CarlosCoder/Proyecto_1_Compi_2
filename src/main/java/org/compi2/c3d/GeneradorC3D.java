package org.compi2.c3d;

import lombok.Getter;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.ast.sentencias.NodoFuncion;
import org.compi2.ast.sentencias.NodoPrograma;
import org.compi2.c3d.generadores.*;

import java.util.List;

public class GeneradorC3D {
    @Getter
    private final ContextoC3D contexto;
    @Getter
    private final GeneradorExpresiones generadorExpresiones;
    @Getter
    private final GeneradorSentencias generadorSentencias;
    @Getter
    private final GeneradorControlFlujo generadorControlFlujo;
    @Getter
    private final GeneradorSubrutinas generadorSubrutinas;

    public GeneradorC3D(TablaSimbolos tablaSimbolos) {
        this(new ContenedorC3D(), tablaSimbolos);
    }

    public GeneradorC3D(ContenedorC3D c3d, TablaSimbolos tablaSimbolos) {
        this.contexto = new ContextoC3D(c3d, tablaSimbolos);
        this.generadorControlFlujo = new GeneradorControlFlujo(contexto);
        this.generadorSubrutinas = new GeneradorSubrutinas(contexto);
        this.generadorExpresiones = new GeneradorExpresiones(contexto, generadorSubrutinas);
        this.generadorSentencias = new GeneradorSentencias(contexto, generadorExpresiones, generadorControlFlujo, generadorSubrutinas);

        this.generadorControlFlujo.setGeneradorExpresiones(generadorExpresiones);
        this.generadorControlFlujo.setGeneradorSentencias(generadorSentencias);
        this.generadorSubrutinas.setGeneradorExpresiones(generadorExpresiones);
        this.generadorSubrutinas.setGeneradorSentencias(generadorSentencias);
    }

    public ContenedorC3D getC3d() {
        return contexto.getC3d();
    }

    public TablaSimbolos getTablaSimbolos() {
        return contexto.getTablaSimbolos();
    }

    public String compilarAMain(List<NodoAST> instrucciones) {
        ContenedorC3D c3d = contexto.getC3d();
        c3d.limpiar();
        generarSentencias(instrucciones);
        return generarCodigoCompleto(c3d, null);
    }

    public String compilarConFunciones(List<NodoFuncion> funciones, List<NodoAST> instruccionesMain) {
        ContenedorC3D c3d = contexto.getC3d();
        c3d.limpiar();

        String codigoFunciones = generarCodigoFunciones(funciones, c3d);
        generarSentencias(instruccionesMain);

        return generarCodigoCompleto(c3d, codigoFunciones);
    }

    private String generarCodigoFunciones(List<NodoFuncion> funciones, ContenedorC3D c3d) {
        if (funciones == null || funciones.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();

        for (NodoFuncion fn : funciones) {
            sb.append("void ").append(fn.getNombre()).append("();\n");
        }
        sb.append("\n");

        for (NodoFuncion fn : funciones) {
            ContenedorC3D c3dFn = new ContenedorC3D(c3d.getGestorTemporales(), c3d.getGestorEtiquetas());
            GeneradorC3D genFn = new GeneradorC3D(c3dFn, contexto.getTablaSimbolos());
            genFn.generarFuncion(fn);
            sb.append(c3dFn.generarCodigoC("    ")).append("\n");
        }
        return sb.toString();
    }

    private void generarSentencias(List<NodoAST> instrucciones) {
        if (instrucciones == null) return;
        for (NodoAST inst : instrucciones) {
            generarSentencia(inst);
        }
    }

    private String generarCodigoCompleto(ContenedorC3D c3d, String codigoFunciones) {
        String codigoC = c3d.generarCodigoC("    ");
        return PlantillaC.generarCodigoCompleto(c3d, codigoFunciones, codigoC);
    }

    public void generarFuncion(NodoFuncion fn) {
        generadorSubrutinas.generarFuncion(fn);
    }

    public void generarSentencia(NodoAST nodo) {
        generadorSentencias.generarSentencia(nodo);
    }

    public String generarExpresion(NodoAST nodo) {
        return generadorExpresiones.generarExpresion(nodo);
    }
}