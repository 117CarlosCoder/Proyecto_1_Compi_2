package org.compi2.c3d.generadores;

import lombok.Setter;
import org.compi2.ast.NodoAST;
import org.compi2.ast.expresiones.*;
import org.compi2.c3d.ContenedorC3D;
import org.compi2.c3d.Cuarteta;
import org.compi2.c3d.TipoOperadorC3D;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.function.BiConsumer;

public class GeneradorExpresiones {
    private final ContextoC3D contexto;
    @Setter
    private GeneradorSubrutinas generadorSubrutinas;

    public GeneradorExpresiones(ContextoC3D contexto) {
        this.contexto = contexto;
    }

    public GeneradorExpresiones(ContextoC3D contexto, GeneradorSubrutinas generadorSubrutinas) {
        this.contexto = contexto;
        this.generadorSubrutinas = generadorSubrutinas;
    }

    public String generarExpresion(NodoAST nodo) {
        if (nodo == null) return "0";

        if (nodo instanceof NodoLiteral lit) {
            return generarLiteral(lit);
        } else if (nodo instanceof NodoIdentificador id) {
            return generarIdentificador(id);
        } else if (nodo instanceof NodoOperacionUnaria unaria) {
            return generarOperacionUnaria(unaria);
        } else if (nodo instanceof NodoOperacionBinaria bin) {
            return generarOperacionBinaria(bin);
        } else if (nodo instanceof NodoAccesoArreglo accArr) {
            return generarAccesoArreglo(accArr);
        } else if (nodo instanceof NodoAccesoMiembro accM) {
            return generarAccesoMiembro(accM);
        } else if (nodo instanceof NodoListaValores lista) {
            return generarListaValores(lista);
        } else if (nodo instanceof NodoInstanciacionArreglo instArr) {
            return generarInstanciacionArreglo(instArr);
        } else if (nodo instanceof NodoExpresionTernaria ternaria) {
            return generarTernario(ternaria);
        } else if (nodo instanceof NodoExpresionLectura) {
            ContenedorC3D c3d = contexto.getC3d();
            String tVal = c3d.nuevoTemporal();
            c3d.emitir(new Cuarteta(TipoOperadorC3D.READ_STR, null, null, tVal));
            return tVal;
        } else if (nodo instanceof NodoInstanciacionObjeto instObj) {
            return (generadorSubrutinas != null) ? generadorSubrutinas.generarInstanciacionObjeto(instObj) : "0";
        } else if (nodo instanceof NodoLlamadaFuncion llamada) {
            return (generadorSubrutinas != null) ? generadorSubrutinas.generarLlamadaFuncion(llamada) : "0";
        } else if (nodo instanceof NodoLlamadaMetodo metodo) {
            return (generadorSubrutinas != null) ? generadorSubrutinas.generarLlamadaMetodo(metodo) : "0";
        }

        return "0";
    }

    public String generarLiteral(NodoLiteral lit) {
        ContenedorC3D c3d = contexto.getC3d();
        if (lit.getValor() == null) {
            return "0";
        }
        if (lit.getTipo().getBase() == TipoBase.CADENA) {
            return generarLiteralCadena((String) lit.getValor(), c3d);
        } else if (lit.getTipo().getBase() == TipoBase.BOOLEANO) {
            return Boolean.TRUE.equals(lit.getValor()) ? "1" : "0";
        } else if (lit.getTipo().getBase() == TipoBase.CARACTER) {
            return String.valueOf((int) (Character) lit.getValor());
        }
        return String.valueOf(lit.getValor());
    }

    private String generarLiteralCadena(String str, ContenedorC3D c3d) {
        String tBase = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tBase, "H");
        for (int i = 0; i < str.length(); i++) {
            c3d.emitirAsignacionHeap("H", String.valueOf((int) str.charAt(i)));
            c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", "1");
        }
        c3d.emitirAsignacionHeap("H", "-1");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", "1");
        return tBase;
    }

    public String generarIdentificador(NodoIdentificador id) {
        ContenedorC3D c3d = contexto.getC3d();
        if (contexto.esCampoDeThis(id.getNombre())) {
            int offCampo = contexto.obtenerOffsetCampoThis(id.getNombre());
            int offThis = contexto.obtenerOffset("this");
            String tThisPos = c3d.nuevoTemporal();
            c3d.emitirOperacion(TipoOperadorC3D.SUMA, tThisPos, "P", String.valueOf(offThis));
            String tThis = c3d.nuevoTemporal();
            c3d.emitirAccesoStack(tThis, tThisPos);
            String tPos = c3d.nuevoTemporal();
            c3d.emitirOperacion(TipoOperadorC3D.SUMA, tPos, tThis, String.valueOf(offCampo));
            String tVal = c3d.nuevoTemporal();
            c3d.emitirAccesoHeap(tVal, tPos);
            return tVal;
        }
        int offset = contexto.obtenerOffset(id.getNombre());
        String tPos = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tPos, "P", String.valueOf(offset));
        String tVal = c3d.nuevoTemporal();
        c3d.emitirAccesoStack(tVal, tPos);
        return tVal;
    }

    public String generarOperacionUnaria(NodoOperacionUnaria unaria) {
        ContenedorC3D c3d = contexto.getC3d();
        String opVal = generarExpresion(unaria.getExpresion());
        String tRes = c3d.nuevoTemporal();

        return switch (unaria.getOperador()) {
            case "-" -> {
                c3d.emitirOperacion(TipoOperadorC3D.MENOS_UNARIO, tRes, opVal, null);
                yield tRes;
            }
            case "!" -> generarCortoCircuito(tRes, (lTrue, lFalse) -> {
                c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, opVal, "0", lTrue);
                c3d.emitirGoto(lFalse);
            });
            case "++", "--" -> generarIncrementoDecremento(unaria);
            default -> opVal;
        };
    }

    private String generarIncrementoDecremento(NodoOperacionUnaria unaria) {
        if (!(unaria.getExpresion() instanceof NodoIdentificador id)) {
            return "0";
        }
        ContenedorC3D c3d = contexto.getC3d();
        TipoOperadorC3D op = "++".equals(unaria.getOperador()) ? TipoOperadorC3D.SUMA : TipoOperadorC3D.RESTA;
        String tNuevo = c3d.nuevoTemporal();

        if (contexto.esCampoDeThis(id.getNombre())) {
            int offCampo = contexto.obtenerOffsetCampoThis(id.getNombre());
            int offThis = contexto.obtenerOffset("this");
            String tThisPos = c3d.nuevoTemporal();
            c3d.emitirOperacion(TipoOperadorC3D.SUMA, tThisPos, "P", String.valueOf(offThis));
            String tThis = c3d.nuevoTemporal();
            c3d.emitirAccesoStack(tThis, tThisPos);
            String tPos = c3d.nuevoTemporal();
            c3d.emitirOperacion(TipoOperadorC3D.SUMA, tPos, tThis, String.valueOf(offCampo));
            String tVal = c3d.nuevoTemporal();
            c3d.emitirAccesoHeap(tVal, tPos);
            c3d.emitirOperacion(op, tNuevo, tVal, "1");
            c3d.emitirAsignacionHeap(tPos, tNuevo);
            return unaria.isEsPrefijo() ? tNuevo : tVal;
        }

        int offset = contexto.obtenerOffset(id.getNombre());
        String tPos = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tPos, "P", String.valueOf(offset));
        String tVal = c3d.nuevoTemporal();
        c3d.emitirAccesoStack(tVal, tPos);
        c3d.emitirOperacion(op, tNuevo, tVal, "1");
        c3d.emitirAsignacionStack(tPos, tNuevo);
        return unaria.isEsPrefijo() ? tNuevo : tVal;
    }

    public String generarOperacionBinaria(NodoOperacionBinaria bin) {
        ContenedorC3D c3d = contexto.getC3d();
        String izq = generarExpresion(bin.getIzq());
        String der = generarExpresion(bin.getDer());
        String tRes = c3d.nuevoTemporal();

        return switch (bin.getOperador()) {
            case "+" -> {
                Tipo tIzq = contexto.resolverTipoEstructura(bin.getIzq());
                Tipo tDer = contexto.resolverTipoEstructura(bin.getDer());
                boolean izqCad = (tIzq != null && tIzq.getBase() == TipoBase.CADENA);
                boolean derCad = (tDer != null && tDer.getBase() == TipoBase.CADENA);

                if (izqCad && derCad) {
                    yield emitirYRetornar(tRes, () -> c3d.emitir(new Cuarteta(TipoOperadorC3D.CONCAT_STR_STR, izq, der, tRes)));
                } else if (izqCad) {
                    yield emitirYRetornar(tRes, () -> c3d.emitir(new Cuarteta(TipoOperadorC3D.CONCAT_STR_NUM, izq, der, tRes)));
                } else if (derCad) {
                    yield emitirYRetornar(tRes, () -> c3d.emitir(new Cuarteta(TipoOperadorC3D.CONCAT_NUM_STR, izq, der, tRes)));
                }
                yield emitirYRetornar(tRes, () -> c3d.emitirOperacion(TipoOperadorC3D.SUMA, tRes, izq, der));
            }
            case "-" -> emitirYRetornar(tRes, () -> c3d.emitirOperacion(TipoOperadorC3D.RESTA, tRes, izq, der));
            case "*" -> emitirYRetornar(tRes, () -> c3d.emitirOperacion(TipoOperadorC3D.MULTIPLICACION, tRes, izq, der));
            case "/" -> emitirYRetornar(tRes, () -> c3d.emitirOperacion(TipoOperadorC3D.DIVISION, tRes, izq, der));
            case "%" -> emitirYRetornar(tRes, () -> c3d.emitirOperacion(TipoOperadorC3D.MODULO, tRes, izq, der));
            case "==" -> emitirRelacional(TipoOperadorC3D.IF_IGUAL, izq, der);
            case "!=" -> emitirRelacional(TipoOperadorC3D.IF_DIFERENTE, izq, der);
            case "<" -> emitirRelacional(TipoOperadorC3D.IF_MENOR, izq, der);
            case "<=" -> emitirRelacional(TipoOperadorC3D.IF_MENOR_IGUAL, izq, der);
            case ">" -> emitirRelacional(TipoOperadorC3D.IF_MAYOR, izq, der);
            case ">=" -> emitirRelacional(TipoOperadorC3D.IF_MAYOR_IGUAL, izq, der);
            case "&&" -> generarCortoCircuito(tRes, (lTrue, lFalse) -> {
                c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, izq, "0", lFalse);
                c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, der, "0", lFalse);
                c3d.emitirGoto(lTrue);
            });
            case "||" -> generarCortoCircuito(tRes, (lTrue, lFalse) -> {
                c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, izq, "0", lTrue);
                c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, der, "0", lTrue);
                c3d.emitirGoto(lFalse);
            });
            default -> "0";
        };
    }

    private String emitirYRetornar(String tRes, Runnable accion) {
        accion.run();
        return tRes;
    }

    public String emitirRelacional(TipoOperadorC3D opRel, String izq, String der) {
        ContenedorC3D c3d = contexto.getC3d();
        String tRes = c3d.nuevoTemporal();
        return generarCortoCircuito(tRes, (lTrue, lFalse) -> {
            c3d.emitirIf(opRel, izq, der, lTrue);
            c3d.emitirGoto(lFalse);
        });
    }


    private String generarCortoCircuito(String tRes, BiConsumer<String, String> emitirSaltos) {
        ContenedorC3D c3d = contexto.getC3d();
        String lTrue = c3d.nuevaEtiqueta();
        String lFalse = c3d.nuevaEtiqueta();
        String lFin = c3d.nuevaEtiqueta();

        emitirSaltos.accept(lTrue, lFalse);

        c3d.emitirEtiqueta(lTrue);
        c3d.emitirAsignacion(tRes, "1");
        c3d.emitirGoto(lFin);
        c3d.emitirEtiqueta(lFalse);
        c3d.emitirAsignacion(tRes, "0");
        c3d.emitirEtiqueta(lFin);
        return tRes;
    }

    public String generarAccesoArreglo(NodoAccesoArreglo accArr) {
        ContenedorC3D c3d = contexto.getC3d();
        String tArr = generarExpresion(accArr.getEstructura());
        String tIdx = generarExpresion(accArr.getIndice());
        String tPos = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tPos, tArr, tIdx);
        String tVal = c3d.nuevoTemporal();
        c3d.emitirAccesoHeap(tVal, tPos);
        return tVal;
    }

    public String generarAccesoMiembro(NodoAccesoMiembro accM) {
        ContenedorC3D c3d = contexto.getC3d();
        String tObj = generarExpresion(accM.getEstructura());
        int offset = contexto.obtenerOffsetCampo(accM.getEstructura(), accM.getNombreCampo());
        String tPos = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tPos, tObj, String.valueOf(offset));
        String tVal = c3d.nuevoTemporal();
        c3d.emitirAccesoHeap(tVal, tPos);
        return tVal;
    }

    public String generarListaValores(NodoListaValores lista) {
        ContenedorC3D c3d = contexto.getC3d();
        String tBase = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tBase, "H");
        for (NodoAST elem : lista.getElementos()) {
            String tElem = generarExpresion(elem);
            c3d.emitirAsignacionHeap("H", tElem);
            c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", "1");
        }
        return tBase;
    }

    public String generarInstanciacionArreglo(NodoInstanciacionArreglo instArr) {
        ContenedorC3D c3d = contexto.getC3d();
        String tTam = (!instArr.getTamanosDimensiones().isEmpty())
            ? generarExpresion(instArr.getTamanosDimensiones().get(0))
            : "1";
        String tBase = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tBase, "H");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", tTam);
        return tBase;
    }

    public String generarTernario(NodoExpresionTernaria ternaria) {
        ContenedorC3D c3d = contexto.getC3d();
        String cond = generarExpresion(ternaria.getCondicion());
        String tRes = c3d.nuevoTemporal();
        String lTrue = c3d.nuevaEtiqueta();
        String lFalse = c3d.nuevaEtiqueta();
        String lFin = c3d.nuevaEtiqueta();

        c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, cond, "0", lTrue);
        c3d.emitirGoto(lFalse);

        c3d.emitirEtiqueta(lTrue);
        String valTrue = generarExpresion(ternaria.getExpresionTrue());
        c3d.emitirAsignacion(tRes, valTrue);
        c3d.emitirGoto(lFin);

        c3d.emitirEtiqueta(lFalse);
        String valFalse = generarExpresion(ternaria.getExpresionFalse());
        c3d.emitirAsignacion(tRes, valFalse);
        c3d.emitirEtiqueta(lFin);

        return tRes;
    }
}