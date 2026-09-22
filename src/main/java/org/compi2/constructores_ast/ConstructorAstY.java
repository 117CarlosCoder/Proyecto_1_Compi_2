package org.compi2.constructores_ast;

import gramaticas.y.yBaseVisitor;
import gramaticas.y.yParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.compi2.ast.NodoAST;
import org.compi2.ast.expresiones.*;
import org.compi2.tipos.Tipo;

public class ConstructorAstY extends yBaseVisitor<NodoAST> {

    @Override
    public NodoAST visitExprMultiplicativa(yParser.ExprMultiplicativaContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, ctx.op.getText(), der, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprAditiva(yParser.ExprAditivaContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, ctx.op.getText(), der, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprRelacional(yParser.ExprRelacionalContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, ctx.op.getText(), der, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprIgualdad(yParser.ExprIgualdadContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, ctx.op.getText(), der, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprConjuncion(yParser.ExprConjuncionContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, "&&", der, ctx.Y_LOGICO().getSymbol().getLine(), ctx.Y_LOGICO().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprDisyuncion(yParser.ExprDisyuncionContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, "||", der, ctx.O_LOGICO().getSymbol().getLine(), ctx.O_LOGICO().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprUnaria(yParser.ExprUnariaContext ctx) {
        NodoAST exp = visit(ctx.expresion());
        return new NodoOperacionUnaria(ctx.op.getText(), exp, ctx.op.getLine(), ctx.op.getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprParentesis(yParser.ExprParentesisContext ctx) {
        return visit(ctx.expresion());
    }

    @Override
    public NodoAST visitExprLeer(yParser.ExprLeerContext ctx) {
        // En Y?, leer() devuelve una entrada de texto por teclado (cadena)
        return new NodoLiteral("", Tipo.CADENA, ctx.LEER().getSymbol().getLine(), ctx.LEER().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprAcceso(yParser.ExprAccesoContext ctx) {
        return visit(ctx.acceso());
    }

    @Override
    public NodoAST visitAcceso(yParser.AccesoContext ctx) {
        String baseId = ctx.ID(0).getText();
        NodoAST actual = new NodoIdentificador(
            baseId,
            ctx.ID(0).getSymbol().getLine(),
            ctx.ID(0).getSymbol().getCharPositionInLine()
        );

        for (int i = 1; i < ctx.getChildCount(); i++) {
            ParseTree hijo = ctx.getChild(i);
            if (hijo instanceof TerminalNode t && t.getSymbol().getType() == yParser.CORCHETE_IZQ) {
                i++;
                ParseTree expHijo = ctx.getChild(i);
                if (expHijo instanceof yParser.ExpresionContext expCtx) {
                    NodoAST indice = visit(expCtx);
                    actual = new NodoAccesoArreglo(
                        actual,
                        indice,
                        t.getSymbol().getLine(),
                        t.getSymbol().getCharPositionInLine()
                    );
                }
            }
        }
        return actual;
    }

    @Override
    public NodoAST visitExprLiteral(yParser.ExprLiteralContext ctx) {
        return visit(ctx.literal());
    }

    @Override
    public NodoAST visitLitEntero(yParser.LitEnteroContext ctx) {
        int val = Integer.parseInt(ctx.LIT_ENTERO().getText());
        return new NodoLiteral(val, ctx.LIT_ENTERO().getSymbol().getLine(), ctx.LIT_ENTERO().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitDecimal(yParser.LitDecimalContext ctx) {
        double val = Double.parseDouble(ctx.LIT_DECIMAL().getText());
        return new NodoLiteral(val, ctx.LIT_DECIMAL().getSymbol().getLine(), ctx.LIT_DECIMAL().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitCadena(yParser.LitCadenaContext ctx) {
        String raw = ctx.LIT_CADENA().getText();
        String str = raw.substring(1, raw.length() - 1);
        return new NodoLiteral(str, ctx.LIT_CADENA().getSymbol().getLine(), ctx.LIT_CADENA().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitCaracter(yParser.LitCaracterContext ctx) {
        char ch = ctx.LIT_CARACTER().getText().charAt(1);
        return new NodoLiteral(ch, ctx.LIT_CARACTER().getSymbol().getLine(), ctx.LIT_CARACTER().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitVerdadero(yParser.LitVerdaderoContext ctx) {
        return new NodoLiteral(true, ctx.LIT_VERDADERO().getSymbol().getLine(), ctx.LIT_VERDADERO().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitFalso(yParser.LitFalsoContext ctx) {
        return new NodoLiteral(false, ctx.LIT_FALSO().getSymbol().getLine(), ctx.LIT_FALSO().getSymbol().getCharPositionInLine());
    }
}