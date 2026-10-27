package org.compi2.c3d;

import lombok.Getter;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.ast.expresiones.NodoAccesoArreglo;
import org.compi2.ast.expresiones.NodoAccesoMiembro;
import org.compi2.ast.expresiones.NodoExpresionLectura;
import org.compi2.ast.expresiones.NodoIdentificador;
import org.compi2.ast.expresiones.NodoLlamadaFuncion;
import org.compi2.ast.expresiones.NodoLlamadaMetodo;
import org.compi2.ast.expresiones.NodoOperacionUnaria;
import org.compi2.ast.sentencias.NodoAsignacion;
import org.compi2.ast.sentencias.NodoBreak;
import org.compi2.ast.sentencias.NodoContinue;
import org.compi2.ast.sentencias.NodoDeclaracion;
import org.compi2.ast.sentencias.NodoDoWhile;
import org.compi2.ast.sentencias.NodoFor;
import org.compi2.ast.sentencias.NodoFuncion;
import org.compi2.ast.sentencias.NodoIf;
import org.compi2.ast.sentencias.NodoImprimir;
import org.compi2.ast.sentencias.NodoLectura;
import org.compi2.ast.sentencias.NodoLlamadaSuper;
import org.compi2.ast.sentencias.NodoRetorno;
import org.compi2.ast.sentencias.NodoSwitch;
import org.compi2.ast.sentencias.NodoWhile;
import org.compi2.ast.sentencias.Parametro;
import org.compi2.c3d.generadores.ContextoC3D;
import org.compi2.c3d.generadores.GeneradorExpresiones;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.ArrayList;
import java.util.List;

public class GeneradorC3D {
    @Getter
    private final ContextoC3D contexto;
    private final GeneradorExpresiones generadorExpresiones;
    private Tipo tipoRetornoActual;

    public GeneradorC3D(TablaSimbolos tablaSimbolos) {
        this(new ContenedorC3D(), tablaSimbolos);
    }

    public GeneradorC3D(ContenedorC3D c3d, TablaSimbolos tablaSimbolos) {
        this.contexto = new ContextoC3D(c3d, tablaSimbolos);
        this.generadorExpresiones = new GeneradorExpresiones(contexto);
    }

    public ContenedorC3D getC3d() {
        return contexto.getC3d();
    }

    public String compilarAMain(List<NodoAST> instrucciones) {
        contexto.limpiar();
        prepararVariablesMain(instrucciones);
        generarSentencias(instrucciones);
        return generarCodigoCompleto();
    }

    public String compilarConFunciones(List<NodoFuncion> funciones, List<NodoAST> instruccionesMain) {
        contexto.limpiar();

        if (funciones == null || funciones.isEmpty()) {
            return compilarAMain(instruccionesMain);
        }

        ContenedorC3D c3d = getC3d();
        c3d.emitirGoto("L_main");

        for (NodoFuncion funcion : funciones) {
            generarFuncion(funcion);
        }

        c3d.emitirEtiqueta("L_main");
        contexto.limpiarLocales();
        prepararVariablesMain(instruccionesMain);
        generarSentencias(instruccionesMain);
        c3d.emitirGoto("L_fin_programa");

        contexto.emitirDespachadoresVirtuales();

        for (NodoFuncion funcion : funciones) {
            contexto.emitirDespachadorRetorno(resolverNombreFuncionC3D(funcion));
        }

        c3d.emitirEtiqueta("L_fin_programa");

        return generarCodigoCompleto();
    }

    public String resolverNombreFuncionC3D(NodoFuncion fn) {
        List<Tipo> tiposParams = new ArrayList<>();
        if (fn.getParametros() != null) {
            for (Parametro p : fn.getParametros()) {
                tiposParams.add(p.getTipo());
            }
        }
        if (fn.isEsMetodo()) {
            if (fn.getNombre().equals(fn.getClaseContenedora() + "_constructor")) {
                return contexto.resolverEtiquetaConstructor(fn.getClaseContenedora(), tiposParams);
            }
            return contexto.resolverEtiquetaMetodo(fn.getClaseContenedora(), fn.getNombre(), tiposParams);
        }
        return fn.getNombre();
    }

    private void prepararVariablesMain(List<NodoAST> instrucciones) {
        int siguienteOffset = contexto.asignarOffsetsLocales(instrucciones, 2);
        contexto.setOffsetMaximo(siguienteOffset);
    }

    private String generarCodigoCompleto() {
        ContenedorC3D c3d = getC3d();
        return PlantillaC.generarCodigoCompleto(c3d, c3d.generarCodigoC("    "));
    }

    public void generarFuncion(NodoFuncion funcion) {
        this.tipoRetornoActual = funcion.getTipoRetorno();
        String nombreC3D = resolverNombreFuncionC3D(funcion);
        contexto.limpiarLocales();
        contexto.setFuncionActual(nombreC3D);
        int offsetFinal = contexto.registrarParametros(funcion);
        offsetFinal = contexto.asignarOffsetsLocales(funcion.getCuerpo(), offsetFinal);
        contexto.setOffsetMaximo(offsetFinal);

        ContenedorC3D c3d = getC3d();
        c3d.emitirEtiqueta("fn_" + nombreC3D);
        generarSentencias(funcion.getCuerpo());
        c3d.emitirGoto("exit_" + nombreC3D);

        contexto.setFuncionActual(null);
        contexto.limpiarLocales();
        this.tipoRetornoActual = null;
    }

    public void generarSentencias(List<NodoAST> instrucciones) {
        if (instrucciones == null) return;
        for (NodoAST instruccion : instrucciones) {
            generarSentencia(instruccion);
        }
    }

    public void generarSentencia(NodoAST nodo) {
        if (nodo == null) return;

        switch (nodo) {
            case NodoDeclaracion decl -> generarDeclaracion(decl);
            case NodoAsignacion asig -> generarAsignacion(asig);
            case NodoLectura lect -> generarLectura(lect);
            case NodoIf nIf -> generarIf(nIf);
            case NodoSwitch nSwitch -> generarSwitch(nSwitch);
            case NodoWhile nWhile -> generarWhile(nWhile);
            case NodoDoWhile nDoWhile -> generarDoWhile(nDoWhile);
            case NodoFor nFor -> generarFor(nFor);
            case NodoBreak b -> contexto.saltarBreak();
            case NodoContinue c -> contexto.saltarContinue();
            case NodoRetorno ret -> generarRetorno(ret);
            case NodoImprimir imp -> generarImprimir(imp);
            case NodoOperacionUnaria un -> generarExpresion(un);
            case NodoLlamadaFuncion fn -> generarExpresion(fn);
            case NodoLlamadaMetodo met -> generarExpresion(met);
            case NodoLlamadaSuper llamadaSuper -> generarLlamadaSuper(llamadaSuper);
            default -> {}
        }
    }

    public String generarExpresion(NodoAST nodo) {
        return generadorExpresiones.generarExpresion(nodo);
    }

    // --- Generación Individual de Sentencias ---

    private void generarDeclaracion(NodoDeclaracion declaracion) {
        if (declaracion.getValorInicial() == null) {
            if (declaracion.getTipoDeclarado() != null
                    && declaracion.getTipoDeclarado().getBase() == TipoBase.OBJETO_CLASE) {
                contexto.escribirVariable(declaracion.getNombre(), "-1", false);
            }
            return;
        }
        if (declaracion.getValorInicial() instanceof NodoExpresionLectura lec && declaracion.getTipoDeclarado() != null) {
            lec.aplicarTipoEsperado(declaracion.getTipoDeclarado());
        }
        String valor = generarExpresion(declaracion.getValorInicial());
        Tipo tipoOrigen = contexto.resolverTipoEstructura(declaracion.getValorInicial());
        boolean escalar = contexto.requiereEscalarEnteroADecimal(declaracion.getTipoDeclarado(), tipoOrigen);
        contexto.escribirVariable(declaracion.getNombre(), valor, escalar);
    }

    private void generarAsignacion(NodoAsignacion asignacion) {
        if (asignacion.getValor() instanceof NodoExpresionLectura lec) {
            Tipo tipoDest = switch (asignacion.getDestino()) {
                case NodoIdentificador id -> contexto.obtenerTipoVariable(id.getNombre());
                case NodoAccesoArreglo arr -> contexto.resolverTipoEstructura(arr);
                case NodoAccesoMiembro mb -> contexto.resolverTipoEstructura(mb);
                case null, default -> null;
            };
            if (tipoDest != null) {
                lec.aplicarTipoEsperado(tipoDest);
            }
        }
        switch (asignacion.getDestino()) {
            case NodoIdentificador identificador -> {
                String valor = generarExpresion(asignacion.getValor());
                Tipo tipoDest = contexto.obtenerTipoVariable(identificador.getNombre());
                Tipo tipoOrig = contexto.resolverTipoEstructura(asignacion.getValor());
                boolean escalar = contexto.requiereEscalarEnteroADecimal(tipoDest, tipoOrig);
                contexto.escribirVariable(identificador.getNombre(), valor, escalar);
            }
            case NodoAccesoArreglo acceso -> generarAsignacionArreglo(acceso, asignacion.getValor());
            case NodoAccesoMiembro acceso -> generarAsignacionCampo(acceso, asignacion.getValor());
            case null, default -> {}
        }
    }

    private void generarAsignacionArreglo(NodoAccesoArreglo acceso, NodoAST expresion) {
        String arreglo = generarExpresion(acceso.getEstructura());
        contexto.registrarTemporalActivo(arreglo);
        String indice = generarExpresion(acceso.getIndice());
        contexto.registrarTemporalActivo(indice);
        String valor = generarExpresion(expresion);
        contexto.desregistrarTemporalActivo(indice);
        contexto.desregistrarTemporalActivo(arreglo);
        Tipo tipoDest = contexto.resolverTipoEstructura(acceso);
        Tipo tipoOrig = contexto.resolverTipoEstructura(expresion);
        boolean escalar = contexto.requiereEscalarEnteroADecimal(tipoDest, tipoOrig);
        contexto.escribirHeap(arreglo, indice, valor, escalar);
    }

    private void generarAsignacionCampo(NodoAccesoMiembro acceso, NodoAST expresion) {
        String objeto = generarExpresion(acceso.getEstructura());
        contexto.registrarTemporalActivo(objeto);
        int offset = contexto.obtenerOffsetCampo(acceso.getEstructura(), acceso.getNombreCampo());
        String valor = generarExpresion(expresion);
        contexto.desregistrarTemporalActivo(objeto);
        Tipo tipoDest = contexto.resolverTipoEstructura(acceso);
        Tipo tipoOrig = contexto.resolverTipoEstructura(expresion);
        boolean escalar = contexto.requiereEscalarEnteroADecimal(tipoDest, tipoOrig);
        contexto.escribirHeap(objeto, String.valueOf(offset), valor, escalar);
    }

    private void generarLectura(NodoLectura lectura) {
        if (lectura.getDestino() == null) {
            String valorIgnorado = contexto.getC3d().nuevoTemporal();
            contexto.emitirReadNum(valorIgnorado, false);
            return;
        }

        Tipo tipo = lectura.getTipoDestino();
        boolean esCadena = (tipo != null && tipo.getBase() == TipoBase.CADENA);

        Tipo tipoDest = (tipo != null) ? tipo : contexto.resolverTipoEstructura(lectura.getDestino());
        boolean esDecimal = (tipoDest != null && tipoDest.getBase() == TipoBase.DECIMAL);

        String valor;
        if (esCadena) {
            valor = contexto.emitirReadStr();
        } else {
            valor = contexto.getC3d().nuevoTemporal();
            contexto.emitirReadNum(valor, esDecimal);
        }

        switch (lectura.getDestino()) {
            case NodoIdentificador identificador -> contexto.escribirVariable(identificador.getNombre(), valor);
            case NodoAccesoArreglo acceso -> {
                String arreglo = generarExpresion(acceso.getEstructura());
                contexto.registrarTemporalActivo(arreglo);
                String indice = generarExpresion(acceso.getIndice());
                contexto.desregistrarTemporalActivo(arreglo);
                contexto.escribirHeap(arreglo, indice, valor);
            }
            case NodoAccesoMiembro acceso -> {
                String objeto = generarExpresion(acceso.getEstructura());
                int offset = contexto.obtenerOffsetCampo(acceso.getEstructura(), acceso.getNombreCampo());
                contexto.escribirHeap(objeto, String.valueOf(offset), valor);
            }
            case null, default -> {}
        }
    }

    private void generarImprimir(NodoImprimir impresion) {
        List<NodoAST> expresiones = impresion.getExpresiones();
        if (expresiones != null) {
            List<Tipo> tipos = impresion.getTiposExpresiones();
            for (int i = 0; i < expresiones.size(); i++) {
                NodoAST expresion = expresiones.get(i);
                Tipo tipo = (tipos != null && i < tipos.size() && tipos.get(i) != null)
                        ? tipos.get(i)
                        : expresion.comprobar(contexto.getTablaSimbolos(), new ArrayList<>());
                String valor = generarExpresion(expresion);
                contexto.emitirImpresion(tipo, valor);
            }
        }
        if (impresion.isConSaltoLinea()) {
            getC3d().emitirPrintChar("10");
        }
    }

    // --- Control de Flujo ---

    private void emitirSaltoCondicional(String condicion, String siVerdadero, String siFalso) {
        ContenedorC3D c3d = getC3d();
        c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, condicion, "0", siVerdadero);
        c3d.emitirGoto(siFalso);
    }

    private void generarIf(NodoIf nodoIf) {
        ContenedorC3D c3d = getC3d();
        String condicion = generarExpresion(nodoIf.getCondicion());
        String ramaSi = c3d.nuevaEtiqueta();
        String ramaNo = c3d.nuevaEtiqueta();
        String fin = c3d.nuevaEtiqueta();

        emitirSaltoCondicional(condicion, ramaSi, ramaNo);

        c3d.emitirEtiqueta(ramaSi);
        generarSentencias(nodoIf.getInstruccionesSi());
        c3d.emitirGoto(fin);

        c3d.emitirEtiqueta(ramaNo);
        generarSentencias(nodoIf.getInstruccionesAliter());
        c3d.emitirEtiqueta(fin);
    }

    private void generarSwitch(NodoSwitch nodoSwitch) {
        ContenedorC3D c3d = getC3d();
        String valorControl = (nodoSwitch.getExpresionControl() != null)
                ? generarExpresion(nodoSwitch.getExpresionControl())
                : "0";

        List<NodoSwitch.CasoSwitch> casos = (nodoSwitch.getCasos() != null)
                ? nodoSwitch.getCasos()
                : List.of();
        List<NodoAST> casoDefecto = nodoSwitch.getCasoDefecto();
        boolean tieneDefecto = (casoDefecto != null && !casoDefecto.isEmpty());

        String etiquetaFin = c3d.nuevaEtiqueta();
        String etiquetaDefecto = tieneDefecto ? c3d.nuevaEtiqueta() : etiquetaFin;

        List<String> etiquetasCasos = new ArrayList<>(casos.size());
        for (int i = 0; i < casos.size(); i++) {
            etiquetasCasos.add(c3d.nuevaEtiqueta());
        }

        Tipo tipoControl = nodoSwitch.getTipoControl();
        boolean esCadena = (tipoControl != null && tipoControl.getBase() == TipoBase.CADENA);

        contexto.registrarTemporalActivo(valorControl);
        try {
            for (int i = 0; i < casos.size(); i++) {
                NodoSwitch.CasoSwitch caso = casos.get(i);
                if (caso != null && caso.getValorCaso() != null) {
                    String valorCaso = generarExpresion(caso.getValorCaso());
                    if (esCadena) {
                        emitirComparacionCadenaSwitch(valorControl, valorCaso, etiquetasCasos.get(i));
                    } else {
                        c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, valorControl, valorCaso, etiquetasCasos.get(i));
                    }
                }
            }
        } finally {
            contexto.desregistrarTemporalActivo(valorControl);
        }

        c3d.emitirGoto(etiquetaDefecto);

        contexto.entrarSwitch(etiquetaFin);
        try {
            for (int i = 0; i < casos.size(); i++) {
                c3d.emitirEtiqueta(etiquetasCasos.get(i));
                NodoSwitch.CasoSwitch caso = casos.get(i);
                if (caso != null) {
                    generarSentencias(caso.getInstrucciones());
                }
            }
            if (tieneDefecto) {
                c3d.emitirEtiqueta(etiquetaDefecto);
                generarSentencias(casoDefecto);
            }
        } finally {
            contexto.salirSwitch();
        }

        c3d.emitirEtiqueta(etiquetaFin);
    }

    private void emitirComparacionCadenaSwitch(String punteroControl, String punteroCaso, String etiquetaCaso) {
        ContenedorC3D c3d = getC3d();
        String cursorControl = c3d.nuevoTemporal();
        String cursorCaso = c3d.nuevoTemporal();
        c3d.emitirAsignacion(cursorControl, punteroControl);
        c3d.emitirAsignacion(cursorCaso, punteroCaso);

        String etiquetaCiclo = c3d.nuevaEtiqueta();
        String etiquetaSiguienteCaso = c3d.nuevaEtiqueta();

        c3d.emitirEtiqueta(etiquetaCiclo);
        String charControl = c3d.nuevoTemporal();
        String charCaso = c3d.nuevoTemporal();
        c3d.emitirAccesoHeap(charControl, cursorControl);
        c3d.emitirAccesoHeap(charCaso, cursorCaso);
        c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, charControl, charCaso, etiquetaSiguienteCaso);
        c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, charControl, "-1", etiquetaCaso);
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, cursorControl, cursorControl, "1");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, cursorCaso, cursorCaso, "1");
        c3d.emitirGoto(etiquetaCiclo);

        c3d.emitirEtiqueta(etiquetaSiguienteCaso);
    }

    private void generarWhile(NodoWhile nodoWhile) {
        ContenedorC3D c3d = getC3d();
        String inicio = c3d.nuevaEtiqueta();
        String cuerpo = c3d.nuevaEtiqueta();
        String fin = c3d.nuevaEtiqueta();

        contexto.entrarCiclo(fin, inicio);
        c3d.emitirEtiqueta(inicio);
        String condicion = generarExpresion(nodoWhile.getCondicion());
        emitirSaltoCondicional(condicion, cuerpo, fin);

        c3d.emitirEtiqueta(cuerpo);
        generarSentencias(nodoWhile.getInstrucciones());
        c3d.emitirGoto(inicio);

        c3d.emitirEtiqueta(fin);
        contexto.salirCiclo();
    }

    private void generarDoWhile(NodoDoWhile nodoDoWhile) {
        ContenedorC3D c3d = getC3d();
        String inicio = c3d.nuevaEtiqueta();
        String comprobarCondicion = c3d.nuevaEtiqueta();
        String fin = c3d.nuevaEtiqueta();

        contexto.entrarCiclo(fin, comprobarCondicion);
        c3d.emitirEtiqueta(inicio);
        generarSentencias(nodoDoWhile.getCuerpo());

        c3d.emitirEtiqueta(comprobarCondicion);
        String condicion = generarExpresion(nodoDoWhile.getCondicion());
        c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, condicion, "0", inicio);
        c3d.emitirEtiqueta(fin);
        contexto.salirCiclo();
    }

    private void generarFor(NodoFor nodoFor) {
        ContenedorC3D c3d = getC3d();
        if (nodoFor.getInicializacion() != null) {
            generarSentencia(nodoFor.getInicializacion());
        }

        String inicio = c3d.nuevaEtiqueta();
        String cuerpo = c3d.nuevaEtiqueta();
        String paso = c3d.nuevaEtiqueta();
        String fin = c3d.nuevaEtiqueta();

        contexto.entrarCiclo(fin, paso);
        c3d.emitirEtiqueta(inicio);
        if (nodoFor.getCondicion() != null) {
            String condicion = generarExpresion(nodoFor.getCondicion());
            emitirSaltoCondicional(condicion, cuerpo, fin);
        } else {
            c3d.emitirGoto(cuerpo);
        }

        c3d.emitirEtiqueta(cuerpo);
        generarSentencias(nodoFor.getCuerpo());

        c3d.emitirEtiqueta(paso);
        if (nodoFor.getPaso() != null) {
            generarSentencia(nodoFor.getPaso());
        }
        c3d.emitirGoto(inicio);

        c3d.emitirEtiqueta(fin);
        contexto.salirCiclo();
    }

    private void generarRetorno(NodoRetorno retorno) {
        ContenedorC3D c3d = getC3d();
        if (contexto.getFuncionActual() != null) {
            if (retorno.getExpresion() != null) {
                String valor = generarExpresion(retorno.getExpresion());
                c3d.emitirAsignacionStack("P", valor);
                Tipo tipoOrig = contexto.resolverTipoEstructura(retorno.getExpresion());
                if (contexto.requiereEscalarEnteroADecimal(tipoRetornoActual, tipoOrig)) {
                    contexto.marcarAccionUltimaCuartetaRiscV(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG1_X10000);
                }
            }
            c3d.emitirGoto("exit_" + contexto.getFuncionActual());
        } else {
            c3d.emitirGoto("L_fin_programa");
        }
    }

    private void generarLlamadaSuper(NodoLlamadaSuper llamadaSuper) {
        String receptorThis = contexto.leerVariable("this");
        String claseActual = contexto.obtenerNombreClaseThis();
        Tipo tipoClase = (claseActual != null) ? contexto.getTablaSimbolos().buscarTipoCompuesto(claseActual) : null;
        String clasePadre = llamadaSuper.getClasePadreResuelta();
        if (clasePadre == null && tipoClase != null) {
            clasePadre = tipoClase.getClasePadre();
        }
        if (clasePadre == null || !contexto.getTablaSimbolos().tieneConstructor(clasePadre)) {
            return;
        }

        List<Tipo> tiposArgs = new ArrayList<>();
        if (llamadaSuper.getConstructorResuelto() != null) {
            tiposArgs = llamadaSuper.getConstructorResuelto().getParametros();
        } else if (llamadaSuper.getArgumentos() != null) {
            for (NodoAST arg : llamadaSuper.getArgumentos()) {
                tiposArgs.add(contexto.resolverTipoEstructura(arg));
            }
        }

        String nombreCtorPadre = contexto.resolverEtiquetaConstructor(clasePadre, tiposArgs);
        generadorExpresiones.generarLlamada(nombreCtorPadre, llamadaSuper.getArgumentos(), receptorThis, false, tiposArgs);
    }
}
