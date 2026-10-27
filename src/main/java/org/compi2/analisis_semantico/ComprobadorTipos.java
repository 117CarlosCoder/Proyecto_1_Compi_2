package org.compi2.analisis_semantico;

import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

public class ComprobadorTipos {

    public static boolean esCompatibleAsignacion(Tipo destino, Tipo origen) {
        if (destino == null || origen == null) return false;
        if (destino.getBase() == TipoBase.ERROR || origen.getBase() == TipoBase.ERROR) return false;

        if (origen.getBase() == TipoBase.VOID) return false;
        if (origen.getBase() == TipoBase.NULO) {
            return destino.getBase() == TipoBase.OBJETO_CLASE ||
                destino.getBase() == TipoBase.ESTRUCTURA ||
                destino.getBase() == TipoBase.ARREGLO;

        }

        if (destino.getBase() == TipoBase.VOID || destino.getBase() == TipoBase.NULO) return false;

        if (destino.esMismoTipo(origen)) return true;

        if ((destino.getBase() == TipoBase.ESTRUCTURA || destino.getBase() == TipoBase.OBJETO_CLASE)
                && (origen.getBase() == TipoBase.ESTRUCTURA || origen.getBase() == TipoBase.OBJETO_CLASE)) {
            if (destino.getNombreTipo() != null && destino.getNombreTipo().equals(origen.getNombreTipo())) {
                return true;
            }
            if (origen.esSubclaseDe(destino)) {
                return true;
            }
        }

        if (destino.getBase().esNumerico() && origen.getBase().esNumerico()) {
            return destino.getBase().getJerarquia() >= origen.getBase().getJerarquia();
        }

        return false;
    }

    public static Tipo resolverSuma(Tipo t1, Tipo t2) {
        if (t1 == null || t2 == null) return Tipo.ERROR;
        if (t1.getBase() == TipoBase.ERROR || t2.getBase() == TipoBase.ERROR) return Tipo.ERROR;
        if (t1.getBase() == TipoBase.VOID || t2.getBase() == TipoBase.VOID) return Tipo.ERROR;
        if (t1.getBase() == TipoBase.NULO || t2.getBase() == TipoBase.NULO) return Tipo.ERROR;

        if (t1.getBase() == TipoBase.CADENA || t2.getBase() == TipoBase.CADENA) {
            return Tipo.CADENA;
        }

        if (t1.getBase().esNumerico() && t2.getBase().esNumerico()) {
            if (t1.getBase() == TipoBase.CARACTER && t2.getBase() == TipoBase.CARACTER) {
                return Tipo.ENTERO;
            }
            return (t1.getBase().getJerarquia() >= t2.getBase().getJerarquia()) ? t1 : t2;
        }

        return Tipo.ERROR;
    }

    public static Tipo resolverAritmetica(Tipo t1, Tipo t2) {
        if (t1 == null || t2 == null) return Tipo.ERROR;
        if (t1.getBase() == TipoBase.ERROR || t2.getBase() == TipoBase.ERROR) return Tipo.ERROR;
        if (t1.getBase() == TipoBase.VOID || t2.getBase() == TipoBase.VOID) return Tipo.ERROR;
        if (t1.getBase() == TipoBase.NULO || t2.getBase() == TipoBase.NULO) return Tipo.ERROR;
        if (t1.getBase() == TipoBase.CADENA || t2.getBase() == TipoBase.CADENA) return Tipo.ERROR;

        if (t1.getBase().esNumerico() && t2.getBase().esNumerico()) {
            if (t1.getBase() == TipoBase.CARACTER && t2.getBase() == TipoBase.CARACTER) {
                return Tipo.ENTERO;
            }
            return (t1.getBase().getJerarquia() >= t2.getBase().getJerarquia()) ? t1 : t2;
        }

        return Tipo.ERROR;
    }

    public static Tipo resolverRelacional(Tipo t1, Tipo t2) {
        if (t1 == null || t2 == null) return Tipo.ERROR;
        if (t1.getBase() == TipoBase.ERROR || t2.getBase() == TipoBase.ERROR) return Tipo.ERROR;

        if (t1.getBase().esNumerico() && t2.getBase().esNumerico()) {
            return Tipo.BOOLEANO;
        }
        return Tipo.ERROR;
    }

    public static Tipo resolverIgualdad(Tipo t1, Tipo t2) {
        if (t1 == null || t2 == null) return Tipo.ERROR;
        if (t1.getBase() == TipoBase.ERROR || t2.getBase() == TipoBase.ERROR) return Tipo.ERROR;

        if (t1.getBase() == TipoBase.VOID || t1.getBase() == TipoBase.NULO) {
            return (esTipoReferencia(t2) || t2.getBase() == TipoBase.NULO || t2.getBase() == TipoBase.VOID) ? Tipo.BOOLEANO : Tipo.ERROR;
        }
        if (t2.getBase() == TipoBase.VOID || t2.getBase() == TipoBase.NULO) {
            return (esTipoReferencia(t1) || t1.getBase() == TipoBase.NULO || t1.getBase() == TipoBase.VOID) ? Tipo.BOOLEANO : Tipo.ERROR;
        }

        if (t1.esMismoTipo(t2)) return Tipo.BOOLEANO;
        if (t1.getBase().esNumerico() && t2.getBase().esNumerico()) return Tipo.BOOLEANO;

        if ((t1.getBase() == TipoBase.OBJETO_CLASE || t1.getBase() == TipoBase.ESTRUCTURA)
                && (t2.getBase() == TipoBase.OBJETO_CLASE || t2.getBase() == TipoBase.ESTRUCTURA)) {
            if (t1.esSubclaseDe(t2) || t2.esSubclaseDe(t1)) {
                return Tipo.BOOLEANO;
            }
        }

        return Tipo.ERROR;
    }

    public static Tipo resolverLogico(Tipo t1, Tipo t2) {
        if (t1 == null || t2 == null) return Tipo.ERROR;
        if (t1.getBase() == TipoBase.BOOLEANO && t2.getBase() == TipoBase.BOOLEANO) {
            return Tipo.BOOLEANO;
        }
        return Tipo.ERROR;
    }

    private static boolean esTipoReferencia(Tipo t) {
        return t.getBase() == TipoBase.OBJETO_CLASE ||
            t.getBase() == TipoBase.ESTRUCTURA ||
            t.getBase() == TipoBase.ARREGLO;
    }

    public static Tipo resolverTernario(Tipo tTrue, Tipo tFalse) {
        if (tTrue == null || tFalse == null) return Tipo.ERROR;
        if (tTrue.getBase() == TipoBase.ERROR || tFalse.getBase() == TipoBase.ERROR) return Tipo.ERROR;

        if (tTrue.esMismoTipo(tFalse)) return tTrue;

        if (tTrue.getBase().esNumerico() && tFalse.getBase().esNumerico()) {
            return (tTrue.getBase().getJerarquia() >= tFalse.getBase().getJerarquia()) ? tTrue : tFalse;
        }

        if ((tTrue.getBase() == TipoBase.VOID || tTrue.getBase() == TipoBase.NULO) && esTipoReferencia(tFalse)) return tFalse;
        if ((tFalse.getBase() == TipoBase.VOID || tFalse.getBase() == TipoBase.NULO) && esTipoReferencia(tTrue)) return tTrue;

        if ((tTrue.getBase() == TipoBase.OBJETO_CLASE || tTrue.getBase() == TipoBase.ESTRUCTURA)
                && (tFalse.getBase() == TipoBase.OBJETO_CLASE || tFalse.getBase() == TipoBase.ESTRUCTURA)) {
            if (tTrue.esSubclaseDe(tFalse)) return tFalse;
            if (tFalse.esSubclaseDe(tTrue)) return tTrue;
        }

        return Tipo.ERROR;
    }
}