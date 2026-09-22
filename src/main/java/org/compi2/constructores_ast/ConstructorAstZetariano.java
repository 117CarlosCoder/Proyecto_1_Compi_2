package org.compi2.constructores_ast;

import gramaticas.zetariano.zetarianoBaseVisitor;
import gramaticas.zetariano.zetarianoParser;
import org.compi2.ast.NodoAST;
import org.compi2.ast.expresiones.*;
import org.compi2.tipos.Tipo;

public class ConstructorAstZetariano extends zetarianoBaseVisitor<NodoAST> {

    @Override
    public NodoAST visitExprMultiplicativa(zetarianoParser.ExprMultiplicativaContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, ctx.op.getText(), der, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprAditiva(zetarianoParser.ExprAditivaContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, ctx.op.getText(), der, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprRelacional(zetarianoParser.ExprRelacionalContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, ctx.op.getText(), der, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprIgualdad(zetarianoParser.ExprIgualdadContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, ctx.op.getText(), der, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprConjuncion(zetarianoParser.ExprConjuncionContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, "&&", der, ctx.Y_LOGICO().getSymbol().getLine(), ctx.Y_LOGICO().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprDisyuncion(zetarianoParser.ExprDisyuncionContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, "||", der, ctx.O_LOGICO().getSymbol().getLine(), ctx.O_LOGICO().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprUnaria(zetarianoParser.ExprUnariaContext ctx) {
        NodoAST exp = visit(ctx.expresion());
        return new NodoOperacionUnaria(ctx.op.getText(), exp, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprPrefijo(zetarianoParser.ExprPrefijoContext ctx) {
        NodoAST exp = visit(ctx.expresion());
        return new NodoOperacionUnaria(ctx.op.getText(), exp, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprPostfijo(zetarianoParser.ExprPostfijoContext ctx) {
        NodoAST exp = visit(ctx.expresion());
        return new NodoOperacionUnaria(ctx.op.getText(), exp, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprParentesis(zetarianoParser.ExprParentesisContext ctx) {
        return visit(ctx.expresion());
    }

    @Override
    public NodoAST visitExprEncadenado(zetarianoParser.ExprEncadenadoContext ctx) {
        return visit(ctx.encadenado());
    }

    @Override
    public NodoAST visitEncadenado(zetarianoParser.EncadenadoContext ctx) {
        String baseId = ctx.ID().getText();
        NodoAST actual = new NodoIdentificador(baseId, ctx.ID().getSymbol().getLine(), ctx.ID().getSymbol().getCharPositionInLine());

        for (zetarianoParser.SufijoContext suf : ctx.sufijo()) {
            if (suf instanceof zetarianoParser.SufijoIndiceContext indCtx) {
                NodoAST indice = visit(indCtx.expresion());
                actual = new NodoAccesoArreglo(
                    actual,
                    indice,
                    indCtx.CORCHETE_IZQ().getSymbol().getLine(),
                    indCtx.CORCHETE_IZQ().getSymbol().getCharPositionInLine()
                );
            }
        }
        return actual;
    }

    @Override
    public NodoAST visitExprLiteral(zetarianoParser.ExprLiteralContext ctx) {
        return visit(ctx.literal());
    }

    @Override
    public NodoAST visitLitEntero(zetarianoParser.LitEnteroContext ctx) {
        int val = Integer.parseInt(ctx.LIT_ENTERO().getText());
        return new NodoLiteral(val, ctx.LIT_ENTERO().getSymbol().getLine(), ctx.LIT_ENTERO().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitDecimal(zetarianoParser.LitDecimalContext ctx) {
        double val = Double.parseDouble(ctx.LIT_DECIMAL().getText());
        return new NodoLiteral(val, ctx.LIT_DECIMAL().getSymbol().getLine(), ctx.LIT_DECIMAL().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitCadena(zetarianoParser.LitCadenaContext ctx) {
        String raw = ctx.LIT_CADENA().getText();
        String str = raw.substring(1, raw.length() - 1);
        return new NodoLiteral(str, ctx.LIT_CADENA().getSymbol().getLine(), ctx.LIT_CADENA().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitCaracter(zetarianoParser.LitCaracterContext ctx) {
        char ch = ctx.LIT_CARACTER().getText().charAt(1);
        return new NodoLiteral(ch, ctx.LIT_CARACTER().getSymbol().getLine(), ctx.LIT_CARACTER().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitBooleano(zetarianoParser.LitBooleanoContext ctx) {
        boolean val = Boolean.parseBoolean(ctx.LIT_BOOLEANO().getText());
        return new NodoLiteral(val, ctx.LIT_BOOLEANO().getSymbol().getLine(), ctx.LIT_BOOLEANO().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitNulo(zetarianoParser.LitNuloContext ctx) {
        return new NodoLiteral(null, Tipo.VOID, ctx.NULO().getSymbol().getLine(), ctx.NULO().getSymbol().getCharPositionInLine());
    }
}