package org.compi2.c3d.generadores;

import org.compi2.c3d.ContenedorC3D;
import org.compi2.c3d.Cuarteta;
import org.compi2.c3d.TipoOperadorC3D;

final class RutinasC3D {

    private final ContenedorC3D c3d;

    RutinasC3D(ContenedorC3D c3d) {
        this.c3d = c3d;
    }

    void emitirPrintStr(String punteroHeap) {
        String tempPuntero = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tempPuntero, punteroHeap);
        String etiquetaCiclo = c3d.nuevaEtiqueta();
        String etiquetaFin = c3d.nuevaEtiqueta();

        c3d.emitirEtiqueta(etiquetaCiclo);
        String tempCaracter = c3d.nuevoTemporal();
        c3d.emitirAccesoHeap(tempCaracter, tempPuntero);
        c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, tempCaracter, "-1", etiquetaFin);
        c3d.emitirPrintChar(tempCaracter);
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempPuntero, tempPuntero, "1");
        c3d.emitirGoto(etiquetaCiclo);

        c3d.emitirEtiqueta(etiquetaFin);
    }

    void emitirReadNum(String destino) {
        c3d.emitirAsignacionBuffer("0", "0");
        String tempEstadoLectura = c3d.nuevoTemporal();
        c3d.emitir(new Cuarteta(TipoOperadorC3D.READ_STR, null, null, tempEstadoLectura));

        String etiquetaFin = c3d.nuevaEtiqueta();
        String etiquetaFinDeArchivo = c3d.nuevaEtiqueta();

        c3d.emitirIf(TipoOperadorC3D.IF_MENOR_IGUAL, tempEstadoLectura, "0", etiquetaFinDeArchivo);

        String tempPrimerCaracter = c3d.nuevoTemporal();
        c3d.emitirAccesoBuffer(tempPrimerCaracter, "0");
        c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, tempPrimerCaracter, "0", etiquetaFinDeArchivo);

        String tempIndice = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tempIndice, "0");
        String tempSigno = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tempSigno, "1");
        String tempTieneDigitos = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tempTieneDigitos, "0");

        parsearSignoMantisaEnBuffer(tempIndice, tempSigno);

        c3d.emitirAsignacion(destino, "0");

        String tempCaracterActual = c3d.nuevoTemporal();
        String etiquetaVerificarPunto = parsearParteEnteraEnBuffer(
                destino, tempIndice, tempTieneDigitos, tempCaracterActual);

        c3d.emitirEtiqueta(etiquetaVerificarPunto);
        String etiquetaVerificarExponente = c3d.nuevaEtiqueta();
        parsearParteDecimalEnBuffer(destino, tempIndice, tempTieneDigitos, tempCaracterActual, etiquetaVerificarExponente);

        c3d.emitirEtiqueta(etiquetaVerificarExponente);
        String etiquetaFallbackCaracter = c3d.nuevaEtiqueta();
        c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, tempTieneDigitos, "0", etiquetaFallbackCaracter);

        String etiquetaFinMantisa = parsearExponenteEnBuffer(destino, tempIndice);

        c3d.emitirEtiqueta(etiquetaFinMantisa);
        c3d.emitirOperacion(TipoOperadorC3D.MULTIPLICACION, destino, destino, tempSigno);
        c3d.emitirGoto(etiquetaFin);

        c3d.emitirEtiqueta(etiquetaFallbackCaracter);
        c3d.emitirAsignacion(destino, tempPrimerCaracter);
        c3d.emitirGoto(etiquetaFin);

        c3d.emitirEtiqueta(etiquetaFinDeArchivo);
        c3d.emitirAsignacion(destino, "0");

        c3d.emitirEtiqueta(etiquetaFin);
    }

    private void parsearSignoMantisaEnBuffer(String tempIndice, String tempSigno) {
        String tempCaracter = c3d.nuevoTemporal();
        c3d.emitirAccesoBuffer(tempCaracter, tempIndice);

        String etiquetaVerificarNegativo = c3d.nuevaEtiqueta();
        String etiquetaInicioDigitos = c3d.nuevaEtiqueta();

        c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, tempCaracter, "43", etiquetaVerificarNegativo); // no es '+'
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempIndice, tempIndice, "1"); // avanzar tras '+'
        c3d.emitirGoto(etiquetaInicioDigitos);

        c3d.emitirEtiqueta(etiquetaVerificarNegativo);
        c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, tempCaracter, "45", etiquetaInicioDigitos); // no es '-'
        c3d.emitirAsignacion(tempSigno, "-1");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempIndice, tempIndice, "1"); // avanzar tras '-'

        c3d.emitirEtiqueta(etiquetaInicioDigitos);
    }

    private String parsearParteEnteraEnBuffer(String destino, String tempIndice, String tempTieneDigitos,
                                              String tempCaracterActual) {
        String etiquetaCicloEntero = c3d.nuevaEtiqueta();
        c3d.emitirEtiqueta(etiquetaCicloEntero);

        c3d.emitirAccesoBuffer(tempCaracterActual, tempIndice);
        String etiquetaVerificarPunto = c3d.nuevaEtiqueta();
        c3d.emitirIf(TipoOperadorC3D.IF_MENOR, tempCaracterActual, "48", etiquetaVerificarPunto);
        c3d.emitirIf(TipoOperadorC3D.IF_MAYOR, tempCaracterActual, "57", etiquetaVerificarPunto);

        c3d.emitirAsignacion(tempTieneDigitos, "1");
        c3d.emitirOperacion(TipoOperadorC3D.MULTIPLICACION, destino, destino, "10");

        String tempDigito = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.RESTA, tempDigito, tempCaracterActual, "48");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, destino, destino, tempDigito);

        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempIndice, tempIndice, "1");
        c3d.emitirGoto(etiquetaCicloEntero);
        return etiquetaVerificarPunto;
    }

    private void parsearParteDecimalEnBuffer(String destino, String tempIndice, String tempTieneDigitos,
                                             String tempCaracterActual, String etiquetaVerificarExponente) {
        c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, tempCaracterActual, "46", etiquetaVerificarExponente);
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempIndice, tempIndice, "1"); // avanzar tras '.'

        String tempFactor = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tempFactor, "0.1");

        String etiquetaCicloFraccionario = c3d.nuevaEtiqueta();
        c3d.emitirEtiqueta(etiquetaCicloFraccionario);

        String tempCaracterFraccionario = c3d.nuevoTemporal();
        c3d.emitirAccesoBuffer(tempCaracterFraccionario, tempIndice);
        c3d.emitirIf(TipoOperadorC3D.IF_MENOR, tempCaracterFraccionario, "48", etiquetaVerificarExponente);
        c3d.emitirIf(TipoOperadorC3D.IF_MAYOR, tempCaracterFraccionario, "57", etiquetaVerificarExponente);

        c3d.emitirAsignacion(tempTieneDigitos, "1");

        String tempDigitoFraccionario = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.RESTA, tempDigitoFraccionario, tempCaracterFraccionario, "48");

        String tempValorFraccionario = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.MULTIPLICACION, tempValorFraccionario, tempDigitoFraccionario, tempFactor);
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, destino, destino, tempValorFraccionario);

        c3d.emitirOperacion(TipoOperadorC3D.MULTIPLICACION, tempFactor, tempFactor, "0.1");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempIndice, tempIndice, "1");
        c3d.emitirGoto(etiquetaCicloFraccionario);
    }

    private String parsearExponenteEnBuffer(String destino, String tempIndice) {
        String tempCaracterActual = c3d.nuevoTemporal();
        c3d.emitirAccesoBuffer(tempCaracterActual, tempIndice);

        String etiquetaParsearExp = c3d.nuevaEtiqueta();
        String etiquetaFinMantisa = c3d.nuevaEtiqueta();
        c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, tempCaracterActual, "101", etiquetaParsearExp); // 'e'
        c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, tempCaracterActual, "69", etiquetaParsearExp);  // 'E'
        c3d.emitirGoto(etiquetaFinMantisa);

        c3d.emitirEtiqueta(etiquetaParsearExp);
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempIndice, tempIndice, "1"); // avanzar tras 'e'/'E'

        String tempExpSigno = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tempExpSigno, "1");

        String tempCaracterExp = c3d.nuevoTemporal();
        c3d.emitirAccesoBuffer(tempCaracterExp, tempIndice);

        String etiquetaVerificarExpNegativo = c3d.nuevaEtiqueta();
        String etiquetaDespuesSignoExp = c3d.nuevaEtiqueta();
        c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, tempCaracterExp, "43", etiquetaVerificarExpNegativo); // no es '+'
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempIndice, tempIndice, "1"); // avanzar tras '+'
        c3d.emitirGoto(etiquetaDespuesSignoExp);

        c3d.emitirEtiqueta(etiquetaVerificarExpNegativo);
        c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, tempCaracterExp, "45", etiquetaDespuesSignoExp); // no es '-'
        c3d.emitirAsignacion(tempExpSigno, "-1");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempIndice, tempIndice, "1"); // avanzar tras '-'

        c3d.emitirEtiqueta(etiquetaDespuesSignoExp);
        String tempValorExponente = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tempValorExponente, "0");

        String etiquetaCicloDigitosExp = c3d.nuevaEtiqueta();
        String etiquetaAplicarExponente = c3d.nuevaEtiqueta();
        c3d.emitirEtiqueta(etiquetaCicloDigitosExp);

        String tempCaracterDigitoExp = c3d.nuevoTemporal();
        c3d.emitirAccesoBuffer(tempCaracterDigitoExp, tempIndice);
        c3d.emitirIf(TipoOperadorC3D.IF_MENOR, tempCaracterDigitoExp, "48", etiquetaAplicarExponente);
        c3d.emitirIf(TipoOperadorC3D.IF_MAYOR, tempCaracterDigitoExp, "57", etiquetaAplicarExponente);

        c3d.emitirOperacion(TipoOperadorC3D.MULTIPLICACION, tempValorExponente, tempValorExponente, "10");
        String tempDigitoExp = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.RESTA, tempDigitoExp, tempCaracterDigitoExp, "48");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempValorExponente, tempValorExponente, tempDigitoExp);
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempIndice, tempIndice, "1");
        c3d.emitirGoto(etiquetaCicloDigitosExp);

        c3d.emitirEtiqueta(etiquetaAplicarExponente);
        aplicarMultiplicacionExponente(destino, tempValorExponente, tempExpSigno, etiquetaFinMantisa);
        return etiquetaFinMantisa;
    }

    private void aplicarMultiplicacionExponente(String destino, String tempValorExponente, String tempExpSigno, String etiquetaFinMantisa) {
        String etiquetaCicloExpNegativo = c3d.nuevaEtiqueta();
        String etiquetaCicloExpPositivo = c3d.nuevaEtiqueta();
        c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, tempExpSigno, "-1", etiquetaCicloExpNegativo);

        // Exponente positivo: multiplicar destino por 10 mientras tempValorExponente > 0
        c3d.emitirEtiqueta(etiquetaCicloExpPositivo);
        c3d.emitirIf(TipoOperadorC3D.IF_MENOR_IGUAL, tempValorExponente, "0", etiquetaFinMantisa);
        c3d.emitirOperacion(TipoOperadorC3D.MULTIPLICACION, destino, destino, "10");
        c3d.emitirOperacion(TipoOperadorC3D.RESTA, tempValorExponente, tempValorExponente, "1");
        c3d.emitirGoto(etiquetaCicloExpPositivo);

        // Exponente negativo: dividir destino entre 10 mientras tempValorExponente > 0
        c3d.emitirEtiqueta(etiquetaCicloExpNegativo);
        c3d.emitirIf(TipoOperadorC3D.IF_MENOR_IGUAL, tempValorExponente, "0", etiquetaFinMantisa);
        c3d.emitirOperacion(TipoOperadorC3D.DIVISION, destino, destino, "10");
        c3d.emitirOperacion(TipoOperadorC3D.RESTA, tempValorExponente, tempValorExponente, "1");
        c3d.emitirGoto(etiquetaCicloExpNegativo);
    }

    String emitirReadStr() {
        c3d.emitirAsignacionBuffer("0", "0");
        String tempEstadoLectura = c3d.nuevoTemporal();
        c3d.emitir(new Cuarteta(TipoOperadorC3D.READ_STR, null, null, tempEstadoLectura));

        String tempBaseHeap = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tempBaseHeap, "H");

        String etiquetaCiclo = c3d.nuevaEtiqueta();
        String etiquetaFin = c3d.nuevaEtiqueta();

        c3d.emitirIf(TipoOperadorC3D.IF_MENOR_IGUAL, tempEstadoLectura, "0", etiquetaFin);

        String tempIndice = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tempIndice, "0");

        c3d.emitirEtiqueta(etiquetaCiclo);
        String tempCaracter = c3d.nuevoTemporal();
        c3d.emitirAccesoBuffer(tempCaracter, tempIndice);
        c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, tempCaracter, "0", etiquetaFin);
        c3d.emitirAsignacionHeap("H", tempCaracter);
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", "1");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempIndice, tempIndice, "1");
        c3d.emitirGoto(etiquetaCiclo);

        c3d.emitirEtiqueta(etiquetaFin);
        cerrarCadenaEnHeap();
        return tempBaseHeap;
    }

    // =========================================================================
    // Concatenación y Operaciones de Cadenas en Heap
    // =========================================================================

    private void copiarCadenaEnHeap(String punteroCadena) {
        String tempPuntero = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tempPuntero, punteroCadena);

        String etiquetaCiclo = c3d.nuevaEtiqueta();
        String etiquetaFin = c3d.nuevaEtiqueta();

        c3d.emitirEtiqueta(etiquetaCiclo);
        String tempCaracter = c3d.nuevoTemporal();
        c3d.emitirAccesoHeap(tempCaracter, tempPuntero);
        c3d.emitirIf(TipoOperadorC3D.IF_IGUAL, tempCaracter, "-1", etiquetaFin);

        c3d.emitirAsignacionHeap("H", tempCaracter);
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", "1");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempPuntero, tempPuntero, "1");
        c3d.emitirGoto(etiquetaCiclo);

        c3d.emitirEtiqueta(etiquetaFin);
    }

    private void cerrarCadenaEnHeap() {
        c3d.emitirAsignacionHeap("H", "-1");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", "1");
    }

    String emitirConcatStrStr(String p1, String p2) {
        String tempResultado = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tempResultado, "H");

        copiarCadenaEnHeap(p1);
        copiarCadenaEnHeap(p2);

        cerrarCadenaEnHeap();
        return tempResultado;
    }

    String emitirConcatStrNum(String p1, String num) {
        return emitirConcatStrNum(p1, num, false);
    }

    String emitirConcatStrNum(String p1, String num, boolean esDecimal) {
        String tempNumeroCadena = emitirNumeroAHeap(num, esDecimal);
        return emitirConcatStrStr(p1, tempNumeroCadena);
    }

    String emitirConcatNumStr(String num, String p2) {
        return emitirConcatNumStr(num, p2, false);
    }

    String emitirConcatNumStr(String num, String p2, boolean esDecimal) {
        String tempNumeroCadena = emitirNumeroAHeap(num, esDecimal);
        return emitirConcatStrStr(tempNumeroCadena, p2);
    }

    String emitirNumeroAHeap(String num) {
        return emitirNumeroAHeap(num, false);
    }

    String emitirNumeroAHeap(String num, boolean esDecimal) {
        int inicio = c3d.size();
        String tempBaseHeap = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tempBaseHeap, "H");

        String tempValor = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tempValor, num);

        // 1. Signo negativo si aplica
        emitirSignoNegativoNumeroAHeap(tempValor);

        // 2. Extraer y escribir parte entera
        emitirParteEnteraNumeroAHeap(tempValor);

        // 3. Extraer y escribir parte decimal si existe
        emitirParteDecimalNumeroAHeap(tempValor);

        // 4. Centinela de fin de cadena (-1)
        cerrarCadenaEnHeap();

        int fin = c3d.size();
        if (esDecimal) {
            c3d.registrarRutinaRiscV(new ContenedorC3D.RangoRutinaRiscV(
                    ContenedorC3D.TipoRutinaRiscV.DECIMAL_TO_HEAP,
                    inicio,
                    fin,
                    num,
                    tempBaseHeap
            ));
        }

        return tempBaseHeap;
    }

    private void emitirSignoNegativoNumeroAHeap(String tempValor) {
        String etiquetaPositivo = c3d.nuevaEtiqueta();
        c3d.emitirIf(TipoOperadorC3D.IF_MAYOR_IGUAL, tempValor, "0", etiquetaPositivo);
        c3d.emitirAsignacionHeap("H", "45"); // '-'
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", "1");
        c3d.emitirOperacion(TipoOperadorC3D.MENOS_UNARIO, tempValor, tempValor, null);
        c3d.emitirEtiqueta(etiquetaPositivo);
    }

    private void emitirParteEnteraNumeroAHeap(String tempValor) {
        // Extraer parte entera: tempParteEntera = (int)tempValor / 1
        String tempParteEntera = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.DIVISION_ENTERA, tempParteEntera, tempValor, "1");

        String etiquetaNoCero = c3d.nuevaEtiqueta();
        String etiquetaFinEntero = c3d.nuevaEtiqueta();
        c3d.emitirIf(TipoOperadorC3D.IF_DIFERENTE, tempParteEntera, "0", etiquetaNoCero);

        // Si es 0, escribir '0' directamente en heap
        c3d.emitirAsignacionHeap("H", "48");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", "1");
        c3d.emitirGoto(etiquetaFinEntero);

        // Si tempParteEntera > 0, extraer dígitos al buffer auxiliar
        c3d.emitirEtiqueta(etiquetaNoCero);
        String tempIndice = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tempIndice, "0");
        String etiquetaCicloDigitos = c3d.nuevaEtiqueta();

        c3d.emitirEtiqueta(etiquetaCicloDigitos);
        String tempDigito = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.MODULO, tempDigito, tempParteEntera, "10");
        String tempCaracterDigito = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempCaracterDigito, tempDigito, "48");
        c3d.emitirAsignacionBuffer(tempIndice, tempCaracterDigito);
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempIndice, tempIndice, "1");
        c3d.emitirOperacion(TipoOperadorC3D.DIVISION_ENTERA, tempParteEntera, tempParteEntera, "10");
        c3d.emitirIf(TipoOperadorC3D.IF_MAYOR, tempParteEntera, "0", etiquetaCicloDigitos);

        // Escribir dígitos invertidos desde buffer_str hacia el heap
        String etiquetaCicloReverso = c3d.nuevaEtiqueta();
        c3d.emitirEtiqueta(etiquetaCicloReverso);
        c3d.emitirIf(TipoOperadorC3D.IF_MENOR_IGUAL, tempIndice, "0", etiquetaFinEntero);
        c3d.emitirOperacion(TipoOperadorC3D.RESTA, tempIndice, tempIndice, "1");
        String tempCaracter = c3d.nuevoTemporal();
        c3d.emitirAccesoBuffer(tempCaracter, tempIndice);
        c3d.emitirAsignacionHeap("H", tempCaracter);
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", "1");
        c3d.emitirGoto(etiquetaCicloReverso);

        c3d.emitirEtiqueta(etiquetaFinEntero);
    }

    private void emitirParteDecimalNumeroAHeap(String tempValor) {
        // Parte decimal: tempFraccion = tempValor - ((int)tempValor / 1)
        String tempParteEntera = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.DIVISION_ENTERA, tempParteEntera, tempValor, "1");
        String tempFraccion = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.RESTA, tempFraccion, tempValor, tempParteEntera);

        String etiquetaFinDecimal = c3d.nuevaEtiqueta();
        c3d.emitirIf(TipoOperadorC3D.IF_MENOR_IGUAL, tempFraccion, "0.00001", etiquetaFinDecimal);

        // Escribir punto '.' (ASCII 46)
        c3d.emitirAsignacionHeap("H", "46");
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", "1");

        // Extraer hasta 6 dígitos decimales
        String tempContadorDecimales = c3d.nuevoTemporal();
        c3d.emitirAsignacion(tempContadorDecimales, "0");
        String etiquetaCicloFraccion = c3d.nuevaEtiqueta();

        c3d.emitirEtiqueta(etiquetaCicloFraccion);
        c3d.emitirIf(TipoOperadorC3D.IF_MAYOR_IGUAL, tempContadorDecimales, "6", etiquetaFinDecimal);
        c3d.emitirIf(TipoOperadorC3D.IF_MENOR_IGUAL, tempFraccion, "0.00001", etiquetaFinDecimal);

        c3d.emitirOperacion(TipoOperadorC3D.MULTIPLICACION, tempFraccion, tempFraccion, "10");
        String tempDigitoDecimal = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.DIVISION_ENTERA, tempDigitoDecimal, tempFraccion, "1");
        String tempCaracterDecimal = c3d.nuevoTemporal();
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempCaracterDecimal, tempDigitoDecimal, "48");
        c3d.emitirAsignacionHeap("H", tempCaracterDecimal);
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, "H", "H", "1");

        c3d.emitirOperacion(TipoOperadorC3D.RESTA, tempFraccion, tempFraccion, tempDigitoDecimal);
        c3d.emitirOperacion(TipoOperadorC3D.SUMA, tempContadorDecimales, tempContadorDecimales, "1");
        c3d.emitirGoto(etiquetaCicloFraccion);

        c3d.emitirEtiqueta(etiquetaFinDecimal);
    }
}
