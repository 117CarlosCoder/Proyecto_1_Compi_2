package org.compi2.analisis_semantico;

import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

public class ComprobadorTipos {

    public static boolean esCompatibleAsignacion(Tipo destino, Tipo origen) {
        if (destino.getBase() == TipoBase.ERROR || origen.getBase() == TipoBase.ERROR) return false;
        if (destino.getBase() == TipoBase.VOID || origen.getBase() == TipoBase.VOID) return false;

        if (destino.esMismoTipo(origen)) return true;

        if (destino.getBase().esNumerico() && origen.getBase().esNumerico()) {
            return destino.getBase().getJerarquia() >= origen.getBase().getJerarquia();
        }
        return false;
    }

    public static Tipo resolverSuma(Tipo t1, Tipo t2) {
        if (t1.getBase() == TipoBase.ERROR || t2.getBase() == TipoBase.ERROR) return Tipo.ERROR;
        if (t1.getBase() == TipoBase.VOID || t2.getBase() == TipoBase.VOID) return Tipo.ERROR;
        if (t1.getBase() == TipoBase.CADENA || t2.getBase() == TipoBase.CADENA) return Tipo.CADENA;

        if (t1.getBase().esNumerico() && t2.getBase().esNumerico()) {
            return (t1.getBase().getJerarquia() >= t2.getBase().getJerarquia()) ? t1 : t2;
        }
        return Tipo.ERROR;
    }

    public static Tipo resolverAritmetica(Tipo t1, Tipo t2) {
        if (t1.getBase() == TipoBase.ERROR || t2.getBase() == TipoBase.ERROR) return Tipo.ERROR;
        if (t1.getBase() == TipoBase.VOID || t2.getBase() == TipoBase.VOID) return Tipo.ERROR;
        if (t1.getBase() == TipoBase.CADENA || t2.getBase() == TipoBase.CADENA) return Tipo.ERROR;

        if (t1.getBase().esNumerico() && t2.getBase().esNumerico()) {
            return (t1.getBase().getJerarquia() >= t2.getBase().getJerarquia()) ? t1 : t2;
        }
        return Tipo.ERROR;
    }

    public static Tipo resolverRelacional(Tipo t1, Tipo t2) {
        if (t1.getBase() == TipoBase.ERROR || t2.getBase() == TipoBase.ERROR) return Tipo.ERROR;
        if (t1.getBase().esNumerico() && t2.getBase().esNumerico()) {
            return Tipo.BOOLEANO;
        }
        return Tipo.ERROR;
    }

    public static Tipo resolverIgualdad(Tipo t1, Tipo t2) {
        if (t1.getBase() == TipoBase.ERROR || t2.getBase() == TipoBase.ERROR) return Tipo.ERROR;
        if (t1.esMismoTipo(t2)) return Tipo.BOOLEANO;
        if (t1.getBase().esNumerico() && t2.getBase().esNumerico()) return Tipo.BOOLEANO;
        return Tipo.ERROR;
    }

    public static Tipo resolverLogico(Tipo t1, Tipo t2) {
        if (t1.getBase() == TipoBase.BOOLEANO && t2.getBase() == TipoBase.BOOLEANO) {
            return Tipo.BOOLEANO;
        }
        return Tipo.ERROR;
    }

    public static Tipo mapearTipoPrimitivo(String lexema) {
        if (lexema == null) return Tipo.ERROR;
        return switch (lexema.toLowerCase().trim()) {
            case "numerus", "entero", "int" -> Tipo.ENTERO;
            case "decimalis", "flotante", "decimal", "double", "float" -> Tipo.DECIMAL;
            case "littera", "caracter", "char" -> Tipo.CARACTER;
            case "booleanus", "bool", "booleano", "boolean" -> Tipo.BOOLEANO;
            case "textum", "cadena", "string" -> Tipo.CADENA;
            case "void" -> Tipo.VOID;
            default -> new Tipo(TipoBase.OBJETO_CLASE, lexema.trim());
        };
    }
}