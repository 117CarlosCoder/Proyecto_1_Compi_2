package org.compi2.errores;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

public class CustomErrorListener extends BaseErrorListener {
    private final String origen;
    private final List<ErrorCompilacion> errores;

    public CustomErrorListener(String origen, List<ErrorCompilacion> errores) {
        this.origen = (origen != null && !origen.isBlank()) ? origen : "Desconocido";
        this.errores = (errores != null) ? errores : new ArrayList<>();
    }

    @Override
    public void syntaxError(
            Recognizer<?, ?> recognizer,
            Object offendingSymbol,
            int line,
            int charPositionInLine,
            String msg,
            RecognitionException e) {

        TipoError tipo = (recognizer instanceof Lexer || offendingSymbol == null)
                ? TipoError.LEXICO
                : TipoError.SINTACTICO;

        String mensajeLegible = traducirMensaje(tipo, msg);
        errores.add(new ErrorCompilacion(tipo, mensajeLegible, origen, line, charPositionInLine));
    }

    private String traducirMensaje(TipoError tipo, String msg) {
        if (msg == null) return "Error de análisis.";

        if (tipo == TipoError.LEXICO) {
            if (msg.startsWith("token recognition error at: ")) {
                String token = msg.substring("token recognition error at: ".length());
                return "Carácter no reconocido o inválido: " + token;
            }
            return "Error léxico: " + msg;
        }

        if (msg.startsWith("mismatched input")) {
            return msg.replace("mismatched input", "Símbolo inesperado")
                      .replace("expecting", "se esperaba");
        } else if (msg.startsWith("no viable alternative at input")) {
            return msg.replace("no viable alternative at input", "Estructura sintáctica inválida cerca de");
        } else if (msg.startsWith("extraneous input")) {
            return msg.replace("extraneous input", "Símbolo sobrante o inesperado")
                      .replace("expecting", "se esperaba");
        } else if (msg.startsWith("missing")) {
            return msg.replace("missing", "Falta el símbolo")
                      .replace("at", "en");
        }

        return msg;
    }
}
