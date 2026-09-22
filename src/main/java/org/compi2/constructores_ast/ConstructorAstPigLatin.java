package org.compi2.constructores_ast;

import gramaticas.piglatin.piglatinBaseVisitor;
import gramaticas.piglatin.piglatinParser;
import org.compi2.ast.NodoAST;
import org.compi2.ast.expresiones.*;

public class ConstructorAstPigLatin extends piglatinBaseVisitor<NodoAST> {

    @Override
    public NodoAST visitExprMultiplicativa(piglatinParser.ExprMultiplicativaContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, ctx.op.getText(), der, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprAditiva(piglatinParser.ExprAditivaContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, ctx.op.getText(), der, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprRelacional(piglatinParser.ExprRelacionalContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, ctx.op.getText(), der, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprIgualdad(piglatinParser.ExprIgualdadContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, ctx.op.getText(), der, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprConjuncion(piglatinParser.ExprConjuncionContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, "&&", der, ctx.Y_LOGICO().getSymbol().getLine(), ctx.Y_LOGICO().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprDisyuncion(piglatinParser.ExprDisyuncionContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, "||", der, ctx.O_LOGICO().getSymbol().getLine(), ctx.O_LOGICO().getSymbol().getCharPositionInLine());
    }


    @Override
    public NodoAST visitExprUnaria(piglatinParser.ExprUnariaContext ctx) {
        NodoAST exp = visit(ctx.expresion());
        return new NodoOperacionUnaria(ctx.op.getText(), exp, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprParentesis(piglatinParser.ExprParentesisContext ctx) {
        return visit(ctx.expresion());
    }

    @Override
    public NodoAST visitExprEncadenado(piglatinParser.ExprEncadenadoContext ctx) {
        return visit(ctx.encadenado());
    }

    @Override
    public NodoAST visitEncadenado(piglatinParser.EncadenadoContext ctx) {
        String baseId = ctx.ID().getText();
        NodoAST actual = new NodoIdentificador(baseId, ctx.ID().getSymbol().getLine(), ctx.ID().getSymbol().getCharPositionInLine());

        for (piglatinParser.SufijoContext suf : ctx.sufijo()) {
            if (suf instanceof piglatinParser.SufijoIndiceContext indCtx) {
                NodoAST ind = visit(indCtx.expresion());
                actual = new NodoAccesoArreglo(
                    actual,
                    ind,
                    indCtx.CORCHETE_IZQ().getSymbol().getLine(),
                    indCtx.CORCHETE_IZQ().getSymbol().getCharPositionInLine()
                );
            }
        }
        return actual;
    }

    @Override
    public NodoAST visitExprLiteral(piglatinParser.ExprLiteralContext ctx) {
        return visit(ctx.literal());
    }

    @Override
    public NodoAST visitLitEntero(piglatinParser.LitEnteroContext ctx) {
        int val = Integer.parseInt(ctx.LIT_ENTERO().getText());
        return new NodoLiteral(val, ctx.LIT_ENTERO().getSymbol().getLine(), ctx.LIT_ENTERO().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitDecimal(piglatinParser.LitDecimalContext ctx) {
        double val = Double.parseDouble(ctx.LIT_DECIMAL().getText());
        return new NodoLiteral(val, ctx.LIT_DECIMAL().getSymbol().getLine(), ctx.LIT_DECIMAL().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitCadena(piglatinParser.LitCadenaContext ctx) {
        String raw = ctx.LIT_CADENA().getText();
        String str = raw.substring(1, raw.length() - 1);
        return new NodoLiteral(str, ctx.LIT_CADENA().getSymbol().getLine(), ctx.LIT_CADENA().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitCaracter(piglatinParser.LitCaracterContext ctx) {
        char ch = ctx.LIT_CARACTER().getText().charAt(1);
        return new NodoLiteral(ch, ctx.LIT_CARACTER().getSymbol().getLine(), ctx.LIT_CARACTER().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitBooleano(piglatinParser.LitBooleanoContext ctx) {
        return visit(ctx.literalBooleano());
    }

    @Override
    public NodoAST visitLiteralBooleano(piglatinParser.LiteralBooleanoContext ctx) {
        boolean val = ctx.getText().equals("verum");
        return new NodoLiteral(val, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
    }
}