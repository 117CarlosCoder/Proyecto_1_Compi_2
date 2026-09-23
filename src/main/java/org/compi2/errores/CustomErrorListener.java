package org.compi2.errores;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

public class CustomErrorListener extends BaseErrorListener {
    private final TipoError tipoFijo;
    private final String origen;
    private final List<ErrorCompilacion> errores;

    public CustomErrorListener(String origen) {
        this(null, origen, new ArrayList<>());
    }

    public CustomErrorListener(String origen, List<ErrorCompilacion> errores) {
        this(null, origen, errores);
    }

    public CustomErrorListener(TipoError tipoFijo, String origen, List<ErrorCompilacion> errores) {
        this.tipoFijo = tipoFijo;
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

        TipoError tipo;
        if (tipoFijo != null) {
            tipo = tipoFijo;
        } else if (recognizer instanceof Lexer || offendingSymbol == null) {
            tipo = TipoError.LEXICO;
        } else {
            tipo = TipoError.SINTACTICO;
        }

        String mensajeLegible = traducirMensaje(tipo, msg, offendingSymbol);
        errores.add(new ErrorCompilacion(tipo, mensajeLegible, origen, line, charPositionInLine));
    }

    private String traducirMensaje(TipoError tipo, String msg, Object offendingSymbol) {
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

    public List<ErrorCompilacion> getErrores() {
        return errores;
    }

    public boolean tieneErrores() {
        return !errores.isEmpty();
    }

    public static void asociar(Lexer lexer, Parser parser, String origen, List<ErrorCompilacion> listaDestino) {
        CustomErrorListener listener = new CustomErrorListener(origen, listaDestino);
        if (lexer != null) {
            lexer.removeErrorListeners();
            lexer.addErrorListener(listener);
        }
        if (parser != null) {
            parser.removeErrorListeners();
            parser.addErrorListener(listener);
        }
    }
}
