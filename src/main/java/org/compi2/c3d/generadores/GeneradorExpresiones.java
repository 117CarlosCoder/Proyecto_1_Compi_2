package org.compi2.c3d.generadores;

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
        if (literal.getValor() == null) return "0";
        TipoBase base = (literal.getTipo() != null) ? literal.getTipo().getBase() : null;

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
        String valor = contexto.getC3d().nuevoTemporal();
        contexto.emitirReadNum(valor);
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

        return switch (operacion.getOperador()) {
            case "+" -> generarSuma(operacion, izquierda, derecha, resultado);
            case "-" -> emitirOperacionAritmetica(TipoOperadorC3D.RESTA, resultado, izquierda, derecha);
            case "*" -> emitirOperacionAritmetica(TipoOperadorC3D.MULTIPLICACION, resultado, izquierda, derecha);
            case "/" -> generarDivision(operacion, izquierda, derecha, resultado);
            case "%" -> emitirOperacionAritmetica(TipoOperadorC3D.MODULO, resultado, izquierda, derecha);
            case "==" -> emitirRelacional(TipoOperadorC3D.IF_IGUAL, izquierda, derecha);
            case "!=" -> emitirRelacional(TipoOperadorC3D.IF_DIFERENTE, izquierda, derecha);
            case "<" -> emitirRelacional(TipoOperadorC3D.IF_MENOR, izquierda, derecha);
            case "<=" -> emitirRelacional(TipoOperadorC3D.IF_MENOR_IGUAL, izquierda, derecha);
            case ">" -> emitirRelacional(TipoOperadorC3D.IF_MAYOR, izquierda, derecha);
            case ">=" -> emitirRelacional(TipoOperadorC3D.IF_MAYOR_IGUAL, izquierda, derecha);
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
            return contexto.emitirConcatStrNum(izquierda, derecha);
        }
        if (derechaEsCadena) {
            return contexto.emitirConcatNumStr(izquierda, derecha);
        }
        return emitirOperacionAritmetica(TipoOperadorC3D.SUMA, resultado, izquierda, derecha);
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

        return emitirOperacionAritmetica(operador, resultado, izquierda, derecha);
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
        ContenedorC3D c3d = contexto.getC3d();
        String resultado = c3d.nuevoTemporal();
        String verdadero = c3d.nuevaEtiqueta();
        String falso = c3d.nuevaEtiqueta();
        String fin = c3d.nuevaEtiqueta();

        c3d.emitirIf(operador, izquierda, derecha, verdadero);
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

        String base = emitirReservaDimension(dimensiones, 0);

        for (String dim : dimensiones) {
            contexto.desregistrarTemporalActivo(dim);
        }

        return base;
    }

    private String emitirReservaDimension(List<String> dimensiones, int nivel) {
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
            c3d.emitirAsignacionHeap(pos, "0");
        } else {
            String subArreglo = emitirReservaDimension(dimensiones, nivel + 1);
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
        c3d.emitirAsignacion(inicioObjeto, "H");

        int tamano = 10;
        Tipo tipoClase = contexto.getTablaSimbolos().buscarTipoCompuesto(objeto.getNombreClase());
        if (tipoClase != null && tipoClase.getCampos() != null) {
            tamano = Math.max(1, tipoClase.getCampos().size());
        }
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", String.valueOf(tamano));

        boolean tieneArgumentos = objeto.getArgumentos() != null && !objeto.getArgumentos().isEmpty();
        boolean tieneConstructor = tieneArgumentos || contexto.getTablaSimbolos().tieneConstructor(objeto.getNombreClase());
        if (tieneConstructor) {
            List<Tipo> tiposParams = objeto.getConstructorResuelto() != null
                    ? objeto.getConstructorResuelto().getParametros()
                    : List.of();
            String nombreCtor = contexto.resolverEtiquetaConstructor(objeto.getNombreClase(), tiposParams);
            generarLlamada(nombreCtor, objeto.getArgumentos(), inicioObjeto, false);
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

        c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, condicion, "0", verdadero);
        c3d.emitirGoto(falso);

        c3d.emitirEtiqueta(verdadero);
        c3d.emitirAsignacion(resultado, generarExpresion(ternaria.getExpresionTrue()));
        c3d.emitirGoto(fin);

        c3d.emitirEtiqueta(falso);
        c3d.emitirAsignacion(resultado, generarExpresion(ternaria.getExpresionFalse()));
        c3d.emitirEtiqueta(fin);

        return resultado;
    }

    public String generarLlamadaFuncion(NodoLlamadaFuncion llamada) {
        boolean esMetodoPropio = llamada.isEsMetodoDeClaseActual()
                || (llamada.getFuncionResuelta() == null && contexto.esMetodoDeThis(llamada.getNombreFuncion()));
        if (esMetodoPropio) {
            String receptor = contexto.leerVariable("this");
            String claseObj = llamada.getClaseObjetivoResuelta() != null
                    ? llamada.getClaseObjetivoResuelta()
                    : contexto.obtenerNombreClaseThis();
            List<Tipo> tiposParams = llamada.getFuncionResuelta() != null
                    ? llamada.getFuncionResuelta().getParametros()
                    : List.of();
            String nombreDestino = (claseObj != null)
                    ? contexto.resolverEtiquetaMetodo(claseObj, llamada.getNombreFuncion(), tiposParams)
                    : llamada.getNombreFuncion();
            return generarLlamada(nombreDestino, llamada.getArgumentos(), receptor, true);
        }
        return generarLlamada(llamada.getNombreFuncion(), llamada.getArgumentos(), null, true);
    }

    public String generarLlamadaMetodo(NodoLlamadaMetodo metodo) {
        String receptor = generarExpresion(metodo.getEstructura());
        String claseReceptora = metodo.getClaseObjetivoResuelta();
        if (claseReceptora == null) {
            Tipo tipoReceptor = contexto.resolverTipoEstructura(metodo.getEstructura());
            if (tipoReceptor != null) {
                claseReceptora = tipoReceptor.getNombreTipo();
            }
        }
        List<Tipo> tiposParams = metodo.getMetodoResuelto() != null
                ? metodo.getMetodoResuelto().getParametros()
                : List.of();
        String nombreDestino = (claseReceptora != null)
                ? contexto.resolverEtiquetaMetodo(claseReceptora, metodo.getNombreMetodo(), tiposParams)
                : metodo.getNombreMetodo();
        return generarLlamada(nombreDestino, metodo.getArgumentos(), receptor, true);
    }

    private String generarLlamada(String nombre, List<NodoAST> argumentos, String receptor, boolean conRetorno) {
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
        pasarArgumentos(valores, desplazamiento, primerParametro);
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

    private void pasarArgumentos(List<String> argumentos, int desplazamiento, int offsetInicial) {
        if (argumentos == null) return;
        for (int i = 0; i < argumentos.size(); i++) {
            String valor = argumentos.get(i);
            contexto.pasarParametro(desplazamiento + offsetInicial + i, valor);
            contexto.desregistrarTemporalActivo(valor);
        }
    }
}
