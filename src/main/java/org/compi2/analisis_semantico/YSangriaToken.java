package org.compi2.analisis_semantico;

import gramaticas.y.yLexer;
import gramaticas.y.yParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenFactory;
import org.antlr.v4.runtime.TokenSource;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

public class YSangriaToken implements TokenSource {

    private final yLexer lexer;
    private final Stack<Integer> pilaSangrias = new Stack<>();
    private final Queue<Token> colaTokens = new LinkedList<>();

    public YSangriaToken(yLexer lexer) {
        this.lexer = lexer;
        this.pilaSangrias.push(0);
    }

    @Override
    public Token nextToken() {
        if (!colaTokens.isEmpty()) {
            return colaTokens.poll();
        }

        Token t = lexer.nextToken();

        if (t.getType() == yLexer.NUEVA_LINEA) {
            colaTokens.add(t);

            String texto = t.getText();
            int ultimoSalto = Math.max(texto.lastIndexOf('\n'), texto.lastIndexOf('\r'));
            int indent = 0;
            if (ultimoSalto >= 0) {
                for (int i = ultimoSalto + 1; i < texto.length(); i++) {
                    indent += (texto.charAt(i) == '\t') ? 4 : 1;
                }
            }

            int nivelActual = pilaSangrias.peek();
            if (indent > nivelActual) {
                pilaSangrias.push(indent);
                colaTokens.add(crearToken(yParser.SANGRIA, t));
            } else if (indent < nivelActual) {
                while (!pilaSangrias.isEmpty() && pilaSangrias.peek() > indent) {
                    pilaSangrias.pop();
                    colaTokens.add(crearToken(yParser.FIN_SANGRIA, t));
                }
            }
            return colaTokens.poll();
        }

        if (t.getType() == yLexer.EOF) {
            while (pilaSangrias.size() > 1) {
                pilaSangrias.pop();
                colaTokens.add(crearToken(yParser.FIN_SANGRIA, t));
            }
            colaTokens.add(t);
            return colaTokens.poll();
        }

        return t;
    }

    private Token crearToken(int tipo, Token base) {
        CommonToken token = new CommonToken(tipo);
        token.setLine(base.getLine());
        token.setCharPositionInLine(base.getCharPositionInLine());
        token.setStartIndex(base.getStopIndex());
        token.setStopIndex(base.getStopIndex());
        token.setText(tipo == yParser.SANGRIA ? "<SANGRIA>" : "<FIN_SANGRIA>");
        return token;
    }

    @Override public int getLine() { return lexer.getLine(); }
    @Override public int getCharPositionInLine() { return lexer.getCharPositionInLine(); }
    @Override public CharStream getInputStream() { return lexer.getInputStream(); }
    @Override public String getSourceName() { return lexer.getSourceName(); }
    @Override public void setTokenFactory(TokenFactory<?> factory) { lexer.setTokenFactory(factory); }
    @Override public TokenFactory<?> getTokenFactory() { return lexer.getTokenFactory(); }
}