package org.compi2.c3d.generadores;

import org.compi2.analisis_semantico.Simbolo;
import org.compi2.ast.NodoAST;
import org.compi2.ast.expresiones.NodoAccesoArreglo;
import org.compi2.ast.expresiones.NodoAccesoMiembro;
import org.compi2.ast.expresiones.NodoExpresionLectura;
import org.compi2.ast.expresiones.NodoExpresionTernaria;
import org.compi2.ast.expresiones.NodoIdentificador;
import org.compi2.ast.expresiones.NodoInstanciacionArreglo;
import org.compi2.ast.expresiones.NodoInstanciacionObjeto;
import org.compi2.ast.expresiones.NodoListaValores;
import org.compi2.ast.expresiones.NodoLiteral;
import org.compi2.ast.expresiones.NodoLlamadaFuncion;
import org.compi2.ast.expresiones.NodoLlamadaMetodo;
import org.compi2.ast.expresiones.NodoOperacionBinaria;
import org.compi2.ast.expresiones.NodoOperacionUnaria;
import org.compi2.c3d.ContenedorC3D;
import org.compi2.c3d.TipoOperadorC3D;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.ArrayList;
import java.util.List;

public class GeneradorExpresiones {
    private final ContextoC3D contexto;

    public GeneradorExpresiones(ContextoC3D contexto) {
        this.contexto = contexto;
    }

    public String generarExpresion(NodoAST nodo) {
        if (nodo == null) return "0";

        return switch (nodo) {
            case NodoLiteral lit -> generarLiteral(lit);
            case NodoIdentificador id -> generarIdentificador(id);
            case NodoOperacionUnaria unaria -> generarOperacionUnaria(unaria);
            case NodoOperacionBinaria bin -> generarOperacionBinaria(bin);
            case NodoAccesoArreglo accArr -> generarAccesoArreglo(accArr);
            case NodoAccesoMiembro accM -> generarAccesoMiembro(accM);
            case NodoListaValores lista -> generarListaValores(lista);
            case NodoInstanciacionArreglo instArr -> generarInstanciacionArreglo(instArr);
            case NodoInstanciacionObjeto instObj -> generarInstanciacionObjeto(instObj);
            case NodoExpresionTernaria ternaria -> generarTernario(ternaria);
            case NodoExpresionLectura lectura -> generarExpresionLectura(lectura);
            case NodoLlamadaFuncion llamada -> generarLlamadaFuncion(llamada);
            case NodoLlamadaMetodo metodo -> generarLlamadaMetodo(metodo);
            default -> "0";
        };
    }

    public String generarLiteral(NodoLiteral literal) {
        if (literal.getValor() == null) return "-1";
        TipoBase base = (literal.getTipo() != null) ? literal.getTipo().getBase() : null;
        if (base == TipoBase.NULO) {
            return "-1";
        }

        return switch (base) {
            case CADENA -> generarLiteralCadena((String) literal.getValor());
            case BOOLEANO -> Boolean.TRUE.equals(literal.getValor()) ? "1" : "0";
            case CARACTER -> String.valueOf((int) (Character) literal.getValor());
            case null, default -> String.valueOf(literal.getValor());
        };
    }

    private String generarExpresionLectura(NodoExpresionLectura lectura) {
        Tipo tipo = lectura.getTipoResuelto();
        if (tipo == null || tipo.getBase() == TipoBase.CADENA) {
            return contexto.emitirReadStr();
        }
        boolean esDecimal = tipo.getBase() == TipoBase.DECIMAL;
        String valor = contexto.getC3d().nuevoTemporal();
        contexto.emitirReadNum(valor, esDecimal);
        return valor;
    }

    private String generarLiteralCadena(String texto) {
        ContenedorC3D c3d = contexto.getC3d();
        String inicioCadena = c3d.nuevoTemporal();
        c3d.emitirAsignacion(inicioCadena, "H");
        for (int i = 0; i < texto.length(); i++) {
            c3d.emitirAsignacionHeap("H", String.valueOf((int) texto.charAt(i)));
            c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", "1");
        }
        c3d.emitirAsignacionHeap("H", "-1");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", "1");
        return inicioCadena;
    }

    public String generarIdentificador(NodoIdentificador id) {
        if ("super".equals(id.getNombre())) {
            return contexto.leerVariable("this");
        }
        return contexto.leerVariable(id.getNombre());
    }

    public String generarOperacionUnaria(NodoOperacionUnaria unaria) {
        if ("++".equals(unaria.getOperador()) || "--".equals(unaria.getOperador())) {
            return generarIncrementoDecremento(unaria);
        }

        ContenedorC3D c3d = contexto.getC3d();
        String valor = generarExpresion(unaria.getExpresion());
        String resultado = c3d.nuevoTemporal();

        return switch (unaria.getOperador()) {
            case "-" -> {
                c3d.emitirOperacion(TipoOperadorC3D.MENOS_UNARIO, resultado, valor, null);
                yield resultado;
            }
            case "!" -> generarNegacionLogica(valor, resultado);
            default -> valor;
        };
    }

    private String generarIncrementoDecremento(NodoOperacionUnaria unaria) {
        boolean esIncremento = "++".equals(unaria.getOperador());
        boolean esPrefijo = unaria.isEsPrefijo();
        return switch (unaria.getExpresion()) {
            case NodoIdentificador id -> contexto.aplicarIncremento(id.getNombre(), esIncremento, esPrefijo);
            case NodoAccesoMiembro acceso -> {
                String objeto = generarExpresion(acceso.getEstructura());
                int offset = contexto.obtenerOffsetCampo(acceso.getEstructura(), acceso.getNombreCampo());
                yield aplicarIncrementoHeap(objeto, String.valueOf(offset), esIncremento, esPrefijo);
            }
            case NodoAccesoArreglo acceso -> {
                String arreglo = generarExpresion(acceso.getEstructura());
                contexto.registrarTemporalActivo(arreglo);
                String indice = generarExpresion(acceso.getIndice());
                contexto.desregistrarTemporalActivo(arreglo);
                yield aplicarIncrementoHeap(arreglo, indice, esIncremento, esPrefijo);
            }
            case null, default -> "0";
        };
    }

    private String aplicarIncrementoHeap(String base, String offset, boolean esIncremento, boolean esPrefijo) {
        ContenedorC3D c3d = contexto.getC3d();
        TipoOperadorC3D operador = esIncremento ? TipoOperadorC3D.SUMA : TipoOperadorC3D.RESTA;
        String tempPosicion = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempPosicion, base, offset);
        String tempValorActual = c3d.nuevoTemporal();
        c3d.emitirAccesoHeap(tempValorActual, tempPosicion);
        String tempNuevoValor = c3d.nuevoTemporal();
        c3d.emitirOperacion(operador, tempNuevoValor, tempValorActual, "1");
        c3d.emitirAsignacionHeap(tempPosicion, tempNuevoValor);
        return esPrefijo ? tempNuevoValor : tempValorActual;
    }

    public String generarOperacionBinaria(NodoOperacionBinaria operacion) {
        ContenedorC3D c3d = contexto.getC3d();
        String izquierda = generarExpresion(operacion.getIzq());
        // Una llamada a la derecha puede reutilizar temporales: proteger la izquierda.
        contexto.registrarTemporalActivo(izquierda);
        String derecha = generarExpresion(operacion.getDer());
        contexto.desregistrarTemporalActivo(izquierda);
        String resultado = c3d.nuevoTemporal();

        Tipo tipoIzquierda = operacion.getTipoIzq() != null
                ? operacion.getTipoIzq()
                : contexto.resolverTipoEstructura(operacion.getIzq());
        Tipo tipoDerecha = operacion.getTipoDer() != null
                ? operacion.getTipoDer()
                : contexto.resolverTipoEstructura(operacion.getDer());

        return switch (operacion.getOperador()) {
            case "+" -> generarSuma(operacion, izquierda, derecha, resultado);
            case "-" -> {
                String res = emitirOperacionAritmetica(TipoOperadorC3D.RESTA, resultado, izquierda, derecha);
                if (contexto.requiereEscalarEnteroADecimal(tipoDerecha, tipoIzquierda)) {
                    contexto.marcarAccionUltimaCuartetaRiscV(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG1_X10000);
                } else if (contexto.requiereEscalarEnteroADecimal(tipoIzquierda, tipoDerecha)) {
                    contexto.marcarAccionUltimaCuartetaRiscV(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG2_X10000);
                }
                yield res;
            }
            case "*" -> {
                String res = emitirOperacionAritmetica(TipoOperadorC3D.MULTIPLICACION, resultado, izquierda, derecha);
                boolean izqDec = tipoIzquierda != null && tipoIzquierda.getBase() == TipoBase.DECIMAL;
                boolean derDec = tipoDerecha != null && tipoDerecha.getBase() == TipoBase.DECIMAL;
                if (izqDec && derDec) {
                    contexto.marcarAccionUltimaCuartetaRiscV(ContenedorC3D.AccionCuartetaRiscV.MUL_DECIMAL);
                }
                yield res;
            }
            case "/" -> generarDivision(operacion, izquierda, derecha, resultado);
            case "%" -> emitirOperacionAritmetica(TipoOperadorC3D.MODULO, resultado, izquierda, derecha);
            case "==" -> emitirRelacional(TipoOperadorC3D.IF_IGUAL, izquierda, derecha, tipoIzquierda, tipoDerecha);
            case "!=" -> emitirRelacional(TipoOperadorC3D.IF_DIFERENTE, izquierda, derecha, tipoIzquierda, tipoDerecha);
            case "<" -> emitirRelacional(TipoOperadorC3D.IF_MENOR, izquierda, derecha, tipoIzquierda, tipoDerecha);
            case "<=" -> emitirRelacional(TipoOperadorC3D.IF_MENOR_IGUAL, izquierda, derecha, tipoIzquierda, tipoDerecha);
            case ">" -> emitirRelacional(TipoOperadorC3D.IF_MAYOR, izquierda, derecha, tipoIzquierda, tipoDerecha);
            case ">=" -> emitirRelacional(TipoOperadorC3D.IF_MAYOR_IGUAL, izquierda, derecha, tipoIzquierda, tipoDerecha);
            case "&&" -> generarAndLogico(izquierda, derecha, resultado);
            case "||" -> generarOrLogico(izquierda, derecha, resultado);
            default -> "0";
        };
    }

    private String generarSuma(NodoOperacionBinaria operacion, String izquierda, String derecha, String resultado) {
        Tipo tipoIzquierda = operacion.getTipoIzq() != null
                ? operacion.getTipoIzq()
                : contexto.resolverTipoEstructura(operacion.getIzq());
        Tipo tipoDerecha = operacion.getTipoDer() != null
                ? operacion.getTipoDer()
                : contexto.resolverTipoEstructura(operacion.getDer());
        boolean izquierdaEsCadena = tipoIzquierda != null && tipoIzquierda.getBase() == TipoBase.CADENA;
        boolean derechaEsCadena = tipoDerecha != null && tipoDerecha.getBase() == TipoBase.CADENA;
        boolean izquierdaEsCaracter = tipoIzquierda != null && tipoIzquierda.getBase() == TipoBase.CARACTER;
        boolean derechaEsCaracter = tipoDerecha != null && tipoDerecha.getBase() == TipoBase.CARACTER;

        if (izquierdaEsCadena && derechaEsCadena) {
            return contexto.emitirConcatStrStr(izquierda, derecha);
        }
        if (izquierdaEsCadena && derechaEsCaracter) {
            return contexto.emitirConcatStrStr(izquierda, convertirCaracterCadena(derecha));
        }
        if (izquierdaEsCaracter && derechaEsCadena) {
            return contexto.emitirConcatStrStr(convertirCaracterCadena(izquierda), derecha);
        }
        if (izquierdaEsCadena) {
            boolean esDec = tipoDerecha != null && tipoDerecha.getBase() == TipoBase.DECIMAL;
            return contexto.emitirConcatStrNum(izquierda, derecha, esDec);
        }
        if (derechaEsCadena) {
            boolean esDec = tipoIzquierda != null && tipoIzquierda.getBase() == TipoBase.DECIMAL;
            return contexto.emitirConcatNumStr(izquierda, derecha, esDec);
        }
        String res = emitirOperacionAritmetica(TipoOperadorC3D.SUMA, resultado, izquierda, derecha);
        if (contexto.requiereEscalarEnteroADecimal(tipoDerecha, tipoIzquierda)) {
            contexto.marcarAccionUltimaCuartetaRiscV(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG1_X10000);
        } else if (contexto.requiereEscalarEnteroADecimal(tipoIzquierda, tipoDerecha)) {
            contexto.marcarAccionUltimaCuartetaRiscV(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG2_X10000);
        }
        return res;
    }

    private String generarDivision(NodoOperacionBinaria operacion, String izquierda, String derecha, String resultado) {
        Tipo tipoIzquierda = operacion.getTipoIzq() != null
                ? operacion.getTipoIzq()
                : contexto.resolverTipoEstructura(operacion.getIzq());
        Tipo tipoDerecha = operacion.getTipoDer() != null
                ? operacion.getTipoDer()
                : contexto.resolverTipoEstructura(operacion.getDer());

        boolean izqEnteroOCaracter = tipoIzquierda != null
                && (tipoIzquierda.getBase() == TipoBase.ENTERO || tipoIzquierda.getBase() == TipoBase.CARACTER);
        boolean derEnteroOCaracter = tipoDerecha != null
                && (tipoDerecha.getBase() == TipoBase.ENTERO || tipoDerecha.getBase() == TipoBase.CARACTER);

        TipoOperadorC3D operador = (izqEnteroOCaracter && derEnteroOCaracter)
                ? TipoOperadorC3D.DIVISION_ENTERA
                : TipoOperadorC3D.DIVISION;

        String res = emitirOperacionAritmetica(operador, resultado, izquierda, derecha);
        boolean izqDec = tipoIzquierda != null && tipoIzquierda.getBase() == TipoBase.DECIMAL;
        boolean derDec = tipoDerecha != null && tipoDerecha.getBase() == TipoBase.DECIMAL;
        if (izqDec || derDec) {
            contexto.marcarAccionUltimaCuartetaRiscV(ContenedorC3D.AccionCuartetaRiscV.DIV_DECIMAL);
            if (contexto.requiereEscalarEnteroADecimal(tipoDerecha, tipoIzquierda)) {
                contexto.marcarAccionUltimaCuartetaRiscV(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG1_X10000);
            }
            if (contexto.requiereEscalarEnteroADecimal(tipoIzquierda, tipoDerecha)) {
                contexto.marcarAccionUltimaCuartetaRiscV(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG2_X10000);
            }
        }
        return res;
    }

    private String convertirCaracterCadena(String valorCaracter) {
        ContenedorC3D c3d = contexto.getC3d();
        String inicioCadena = c3d.nuevoTemporal();
        c3d.emitirAsignacion(inicioCadena, "H");
        c3d.emitirAsignacionHeap("H", valorCaracter);
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", "1");
        c3d.emitirAsignacionHeap("H", "-1");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", "1");
        return inicioCadena;
    }

    private String emitirOperacionAritmetica(TipoOperadorC3D operador, String resultado, String izquierda, String derecha) {
        contexto.getC3d().emitirOperacion(operador, resultado, izquierda, derecha);
        return resultado;
    }

    public String emitirRelacional(TipoOperadorC3D operador, String izquierda, String derecha) {
        return emitirRelacional(operador, izquierda, derecha, null, null);
    }

    public String emitirRelacional(TipoOperadorC3D operador, String izquierda, String derecha, Tipo tipoIzquierda, Tipo tipoDerecha) {
        ContenedorC3D c3d = contexto.getC3d();
        String resultado = c3d.nuevoTemporal();
        String verdadero = c3d.nuevaEtiqueta();
        String falso = c3d.nuevaEtiqueta();
        String fin = c3d.nuevaEtiqueta();

        c3d.emitirIf(operador, izquierda, derecha, verdadero);
        if (contexto.requiereEscalarEnteroADecimal(tipoDerecha, tipoIzquierda)) {
            contexto.marcarAccionUltimaCuartetaRiscV(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG1_X10000);
        } else if (contexto.requiereEscalarEnteroADecimal(tipoIzquierda, tipoDerecha)) {
            contexto.marcarAccionUltimaCuartetaRiscV(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG2_X10000);
        }
        c3d.emitirGoto(falso);

        return materializarBooleano(resultado, verdadero, falso, fin);
    }

    private String generarNegacionLogica(String valor, String resultado) {
        ContenedorC3D c3d = contexto.getC3d();
        String verdadero = c3d.nuevaEtiqueta();
        String falso = c3d.nuevaEtiqueta();
        String fin = c3d.nuevaEtiqueta();

        c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, valor, "0", verdadero);
        c3d.emitirGoto(falso);

        return materializarBooleano(resultado, verdadero, falso, fin);
    }

    private String generarAndLogico(String izquierda, String derecha, String resultado) {
        ContenedorC3D c3d = contexto.getC3d();
        String verdadero = c3d.nuevaEtiqueta();
        String falso = c3d.nuevaEtiqueta();
        String fin = c3d.nuevaEtiqueta();

        c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, izquierda, "0", falso);
        c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, derecha, "0", falso);
        c3d.emitirGoto(verdadero);

        return materializarBooleano(resultado, verdadero, falso, fin);
    }

    private String generarOrLogico(String izquierda, String derecha, String resultado) {
        ContenedorC3D c3d = contexto.getC3d();
        String verdadero = c3d.nuevaEtiqueta();
        String falso = c3d.nuevaEtiqueta();
        String fin = c3d.nuevaEtiqueta();

        c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, izquierda, "0", verdadero);
        c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, derecha, "0", verdadero);
        c3d.emitirGoto(falso);

        return materializarBooleano(resultado, verdadero, falso, fin);
    }

    // Las expresiones booleanas también necesitan un valor para asignarse o imprimirse.
    private String materializarBooleano(String resultado, String verdadero, String falso, String fin) {
        ContenedorC3D c3d = contexto.getC3d();
        c3d.emitirEtiqueta(verdadero);
        c3d.emitirAsignacion(resultado, "1");
        c3d.emitirGoto(fin);
        c3d.emitirEtiqueta(falso);
        c3d.emitirAsignacion(resultado, "0");
        c3d.emitirEtiqueta(fin);
        return resultado;
    }

    public String generarAccesoArreglo(NodoAccesoArreglo acceso) {
        String arreglo = generarExpresion(acceso.getEstructura());
        contexto.registrarTemporalActivo(arreglo);
        String indice = generarExpresion(acceso.getIndice());
        contexto.desregistrarTemporalActivo(arreglo);
        return contexto.leerHeap(arreglo, indice);
    }

    public String generarAccesoMiembro(NodoAccesoMiembro acceso) {
        String objeto = generarExpresion(acceso.getEstructura());
        int offset = contexto.obtenerOffsetCampo(acceso.getEstructura(), acceso.getNombreCampo());
        return contexto.leerHeap(objeto, String.valueOf(offset));
    }

    public String generarListaValores(NodoListaValores lista) {
        ContenedorC3D c3d = contexto.getC3d();
        List<NodoAST> elementos = (lista.getElementos() != null) ? lista.getElementos() : List.of();
        String inicioLista = c3d.nuevoTemporal();
        c3d.emitirAsignacion(inicioLista, "H");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", String.valueOf(elementos.size()));
        contexto.registrarTemporalActivo(inicioLista);
        for (int i = 0; i < elementos.size(); i++) {
            String valor = generarExpresion(elementos.get(i));
            contexto.escribirHeap(inicioLista, String.valueOf(i), valor);
        }
        contexto.desregistrarTemporalActivo(inicioLista);
        return inicioLista;
    }

    public String generarInstanciacionArreglo(NodoInstanciacionArreglo arreglo) {
        ContenedorC3D c3d = contexto.getC3d();
        List<NodoAST> exprDims = (arreglo.getTamanosDimensiones() != null) ? arreglo.getTamanosDimensiones() : List.of();
        List<String> dimensiones = new ArrayList<>();
        if (exprDims.isEmpty()) {
            dimensiones.add("1");
        } else {
            for (NodoAST exprDim : exprDims) {
                String valorDim = generarExpresion(exprDim);
                contexto.registrarTemporalActivo(valorDim);
                dimensiones.add(valorDim);
            }
        }

        String valorDefectoHoja = (arreglo.getTipoComponente() != null
                && arreglo.getTipoComponente().getBase() == TipoBase.OBJETO_CLASE) ? "-1" : "0";
        String base = emitirReservaDimension(dimensiones, 0, valorDefectoHoja);

        for (String dim : dimensiones) {
            contexto.desregistrarTemporalActivo(dim);
        }

        return base;
    }

    private String emitirReservaDimension(List<String> dimensiones, int nivel, String valorDefectoHoja) {
        ContenedorC3D c3d = contexto.getC3d();
        String tamano = dimensiones.get(nivel);
        String base = c3d.nuevoTemporal();
        c3d.emitirAsignacion(base, "H");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", tamano);

        String idx = c3d.nuevoTemporal();
        c3d.emitirAsignacion(idx, "0");
        String lblInicio = c3d.nuevaEtiqueta();
        String lblFin = c3d.nuevaEtiqueta();
        c3d.emitirEtiqueta(lblInicio);
        c3d.emitirIf(TipoOperadorC3D.IF_MAYOR_IGUAL, idx, tamano, lblFin);
        String pos = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, pos, base, idx);
        if (nivel == dimensiones.size() - 1) {
            c3d.emitirAsignacionHeap(pos, valorDefectoHoja);
        } else {
            String subArreglo = emitirReservaDimension(dimensiones, nivel + 1, valorDefectoHoja);
            c3d.emitirAsignacionHeap(pos, subArreglo);
        }
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, idx, idx, "1");
        c3d.emitirGoto(lblInicio);
        c3d.emitirEtiqueta(lblFin);
        return base;
    }

    public String generarInstanciacionObjeto(NodoInstanciacionObjeto objeto) {
        ContenedorC3D c3d = contexto.getC3d();
        String inicioObjeto = c3d.nuevoTemporal();

        int tamano = 10;
        Tipo tipoClase = contexto.getTablaSimbolos().buscarTipoCompuesto(objeto.getNombreClase());
        if (tipoClase != null && tipoClase.getCampos() != null) {
            tamano = Math.max(1, tipoClase.getCampos().size());
        }

        boolean participaEnHerencia = contexto.participaEnHerencia(objeto.getNombreClase());
        if (participaEnHerencia) {
            int idClase = contexto.obtenerIdClase(objeto.getNombreClase());
            c3d.emitirAsignacionHeap("H", String.valueOf(idClase));
            c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", "1");
            c3d.emitirAsignacion(inicioObjeto, "H");
            c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", String.valueOf(tamano));
        } else {
            c3d.emitirAsignacion(inicioObjeto, "H");
            c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", String.valueOf(tamano));
        }

        if (tipoClase != null && tipoClase.getCampos() != null) {
            int offsetCampo = 0;
            for (Tipo tCampo : tipoClase.getCampos().values()) {
                if (tCampo != null && tCampo.getBase() == TipoBase.OBJETO_CLASE) {
                    contexto.escribirHeap(inicioObjeto, String.valueOf(offsetCampo), "-1");
                }
                offsetCampo++;
            }
        }

        boolean tieneArgumentos = objeto.getArgumentos() != null && !objeto.getArgumentos().isEmpty();
        boolean tieneConstructor = tieneArgumentos || contexto.getTablaSimbolos().tieneConstructor(objeto.getNombreClase());
        if (tieneConstructor) {
            List<Tipo> tiposParams = objeto.getConstructorResuelto() != null
                    ? objeto.getConstructorResuelto().getParametros()
                    : List.of();
            String nombreCtor = contexto.resolverEtiquetaConstructor(objeto.getNombreClase(), tiposParams);
            generarLlamada(nombreCtor, objeto.getArgumentos(), inicioObjeto, false, tiposParams);
        }
        return inicioObjeto;
    }

    public String generarTernario(NodoExpresionTernaria ternaria) {
        ContenedorC3D c3d = contexto.getC3d();
        String condicion = generarExpresion(ternaria.getCondicion());
        String resultado = c3d.nuevoTemporal();
        String verdadero = c3d.nuevaEtiqueta();
        String falso = c3d.nuevaEtiqueta();
        String fin = c3d.nuevaEtiqueta();

        Tipo tipoRes = ternaria.getTipoResultado() != null
                ? ternaria.getTipoResultado()
                : contexto.resolverTipoEstructura(ternaria);
        Tipo tipoTrue = ternaria.getTipoTrue() != null
                ? ternaria.getTipoTrue()
                : contexto.resolverTipoEstructura(ternaria.getExpresionTrue());
        Tipo tipoFalse = ternaria.getTipoFalse() != null
                ? ternaria.getTipoFalse()
                : contexto.resolverTipoEstructura(ternaria.getExpresionFalse());

        c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, condicion, "0", verdadero);
        c3d.emitirGoto(falso);

        c3d.emitirEtiqueta(verdadero);
        c3d.emitirAsignacion(resultado, generarExpresion(ternaria.getExpresionTrue()));
        if (contexto.requiereEscalarEnteroADecimal(tipoRes, tipoTrue)) {
            contexto.marcarAccionUltimaCuartetaRiscV(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG1_X10000);
        }
        c3d.emitirGoto(fin);

        c3d.emitirEtiqueta(falso);
        c3d.emitirAsignacion(resultado, generarExpresion(ternaria.getExpresionFalse()));
        if (contexto.requiereEscalarEnteroADecimal(tipoRes, tipoFalse)) {
            contexto.marcarAccionUltimaCuartetaRiscV(ContenedorC3D.AccionCuartetaRiscV.ESCALAR_ARG1_X10000);
        }
        c3d.emitirEtiqueta(fin);

        return resultado;
    }

    public String generarLlamadaFuncion(NodoLlamadaFuncion llamada) {
        List<Tipo> tiposParams = llamada.getFuncionResuelta() != null
                ? llamada.getFuncionResuelta().getParametros()
                : null;
        if (tiposParams == null) {
            Simbolo fGlobal = contexto.getTablaSimbolos().buscarFuncionGlobal(llamada.getNombreFuncion());
            if (fGlobal != null && fGlobal.getTipo() != null) {
                tiposParams = fGlobal.getTipo().getParametros();
            }
        }
        boolean esMetodoPropio = llamada.isEsMetodoDeClaseActual()
                || (llamada.getFuncionResuelta() == null && contexto.esMetodoDeThis(llamada.getNombreFuncion()));
        if (esMetodoPropio) {
            String receptor = contexto.leerVariable("this");
            String claseObj = llamada.getClaseObjetivoResuelta() != null
                    ? llamada.getClaseObjetivoResuelta()
                    : contexto.obtenerNombreClaseThis();
            if (tiposParams == null) {
                tiposParams = List.of();
            }
            if (claseObj != null && contexto.tieneSobrescriturasEnSubclase(claseObj, llamada.getNombreFuncion(), tiposParams)) {
                return generarLlamadaVirtual(claseObj, llamada.getNombreFuncion(), tiposParams, llamada.getArgumentos(), receptor, true);
            }
            String nombreDestino = (claseObj != null)
                    ? contexto.resolverEtiquetaMetodo(claseObj, llamada.getNombreFuncion(), tiposParams)
                    : llamada.getNombreFuncion();
            return generarLlamada(nombreDestino, llamada.getArgumentos(), receptor, true, tiposParams);
        }
        return generarLlamada(llamada.getNombreFuncion(), llamada.getArgumentos(), null, true, tiposParams);
    }

    public String generarLlamadaMetodo(NodoLlamadaMetodo metodo) {
        List<Tipo> tiposParams = metodo.getMetodoResuelto() != null
                ? metodo.getMetodoResuelto().getParametros()
                : List.of();

        boolean esSuper = metodo.getEstructura() instanceof NodoIdentificador id && "super".equals(id.getNombre());
        if (esSuper) {
            String receptor = contexto.leerVariable("this");
            String claseActual = contexto.obtenerNombreClaseThis();
            Tipo tipoClase = (claseActual != null) ? contexto.getTablaSimbolos().buscarTipoCompuesto(claseActual) : null;
            String clasePadre = (tipoClase != null) ? tipoClase.getClasePadre() : null;
            String nombreDestino = (clasePadre != null)
                    ? contexto.resolverEtiquetaMetodo(clasePadre, metodo.getNombreMetodo(), tiposParams)
                    : metodo.getNombreMetodo();
            return generarLlamada(nombreDestino, metodo.getArgumentos(), receptor, true, tiposParams);
        }

        String receptor = generarExpresion(metodo.getEstructura());
        String claseReceptora = metodo.getClaseObjetivoResuelta();
        if (claseReceptora == null) {
            Tipo tipoReceptor = contexto.resolverTipoEstructura(metodo.getEstructura());
            if (tipoReceptor != null) {
                claseReceptora = tipoReceptor.getNombreTipo();
            }
        }

        if (claseReceptora != null && contexto.tieneSobrescriturasEnSubclase(claseReceptora, metodo.getNombreMetodo(), tiposParams)) {
            return generarLlamadaVirtual(claseReceptora, metodo.getNombreMetodo(), tiposParams, metodo.getArgumentos(), receptor, true);
        }

        String nombreDestino = (claseReceptora != null)
                ? contexto.resolverEtiquetaMetodo(claseReceptora, metodo.getNombreMetodo(), tiposParams)
                : metodo.getNombreMetodo();
        return generarLlamada(nombreDestino, metodo.getArgumentos(), receptor, true, tiposParams);
    }

    public String generarLlamadaVirtual(String claseBase, String nombreMetodo, List<Tipo> tiposParams,
                                        List<NodoAST> argumentos, String receptor, boolean conRetorno) {
        if (receptor != null) {
            contexto.registrarTemporalActivo(receptor);
        }
        List<String> valores = evaluarArgumentos(argumentos);
        int desplazamiento = contexto.calcularDesplazamientoTransicion();
        int primerParametro = 2;
        if (receptor != null) {
            contexto.pasarParametro(desplazamiento + 2, receptor);
            primerParametro = 3;
        }
        pasarArgumentos(argumentos, valores, desplazamiento, primerParametro, tiposParams);
        String retorno = contexto.realizarLlamadaVirtual(claseBase, nombreMetodo, tiposParams, desplazamiento, conRetorno);
        if (receptor != null) {
            contexto.desregistrarTemporalActivo(receptor);
        }
        return retorno;
    }

    public String generarLlamada(String nombre, List<NodoAST> argumentos, String receptor, boolean conRetorno) {
        return generarLlamada(nombre, argumentos, receptor, conRetorno, null);
    }

    public String generarLlamada(String nombre, List<NodoAST> argumentos, String receptor, boolean conRetorno, List<Tipo> tiposParametros) {
        if (receptor != null) {
            contexto.registrarTemporalActivo(receptor);
        }
        List<String> valores = evaluarArgumentos(argumentos);
        int desplazamiento = contexto.calcularDesplazamientoTransicion();
        int primerParametro = 2;
        if (receptor != null) {
            contexto.pasarParametro(desplazamiento + 2, receptor);
            primerParametro = 3;
        }
        pasarArgumentos(argumentos, valores, desplazamiento, primerParametro, tiposParametros);
        String retorno = contexto.realizarLlamadaSubrutina(nombre, desplazamiento, conRetorno);
        if (receptor != null) {
            contexto.desregistrarTemporalActivo(receptor);
        }
        return retorno;
    }

    private List<String> evaluarArgumentos(List<NodoAST> argumentos) {
        List<String> evaluados = new ArrayList<>();
        if (argumentos == null) return evaluados;
        for (NodoAST argumento : argumentos) {
            String valor = generarExpresion(argumento);
            contexto.registrarTemporalActivo(valor);
            evaluados.add(valor);
        }
        return evaluados;
    }

    private void pasarArgumentos(List<NodoAST> nodosArgs, List<String> argumentos, int desplazamiento, int offsetInicial, List<Tipo> tiposParams) {
        if (argumentos == null) return;
        for (int i = 0; i < argumentos.size(); i++) {
            String valor = argumentos.get(i);
            Tipo tipoParam = (tiposParams != null && i < tiposParams.size()) ? tiposParams.get(i) : null;
            Tipo tipoArg = (nodosArgs != null && i < nodosArgs.size()) ? contexto.resolverTipoEstructura(nodosArgs.get(i)) : null;
            boolean escalar = contexto.requiereEscalarEnteroADecimal(tipoParam, tipoArg);
            contexto.pasarParametro(desplazamiento + offsetInicial + i, valor, escalar);
            contexto.desregistrarTemporalActivo(valor);
        }
    }
}
