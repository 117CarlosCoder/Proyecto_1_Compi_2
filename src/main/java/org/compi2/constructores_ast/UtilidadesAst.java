package org.compi2.constructores_ast;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.compi2.ast.NodoAST;
import org.compi2.ast.expresiones.NodoLiteral;
import org.compi2.ast.expresiones.NodoOperacionBinaria;

final class UtilidadesAst {

    private UtilidadesAst() {
    }

    static NodoOperacionBinaria binaria(NodoAST izq, Token op, NodoAST der) {
        return new NodoOperacionBinaria(izq, op.getText(), der, op.getLine(), op.getCharPositionInLine());
    }

    static NodoOperacionBinaria binaria(NodoAST izq, String opTexto, Token tokenUbicacion, NodoAST der) {
        return new NodoOperacionBinaria(izq, opTexto, der, tokenUbicacion.getLine(), tokenUbicacion.getCharPositionInLine());
    }

    static NodoLiteral literalEntero(TerminalNode terminal) {
        Token token = terminal.getSymbol();
        int val = Integer.parseInt(token.getText());
        return new NodoLiteral(val, token.getLine(), token.getCharPositionInLine());
    }

    static NodoLiteral literalDecimal(TerminalNode terminal) {
        Token token = terminal.getSymbol();
        double val = Double.parseDouble(token.getText());
        return new NodoLiteral(val, token.getLine(), token.getCharPositionInLine());
    }

    static NodoLiteral literalCadena(TerminalNode terminal) {
        Token token = terminal.getSymbol();
        String raw = token.getText();
        String str = raw.substring(1, raw.length() - 1);
        return new NodoLiteral(str, token.getLine(), token.getCharPositionInLine());
    }

    static NodoLiteral literalCaracter(TerminalNode terminal) {
        Token token = terminal.getSymbol();
        char ch = token.getText().charAt(1);
        return new NodoLiteral(ch, token.getLine(), token.getCharPositionInLine());
    }
}
