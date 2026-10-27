package org.compi2.constructores_ast;

import gramaticas.piglatin.piglatinBaseVisitor;
import gramaticas.piglatin.piglatinParser;
import org.compi2.ast.NodoAST;
import org.compi2.ast.expresiones.*;
import org.compi2.ast.sentencias.*;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.ArrayList;
import java.util.List;

public class ConstructorAstPigLatin extends piglatinBaseVisitor<NodoAST> {

    @Override
    public NodoAST visitPrograma(piglatinParser.ProgramaContext ctx) {
        NodoPrograma prog = new NodoPrograma();

        if (ctx.importacion() != null) {
            for (piglatinParser.ImportacionContext imp : ctx.importacion()) {
                prog.agregarImport(
                    imp.ruta().getText(),
                    imp.getStart().getLine(),
                    imp.getStart().getCharPositionInLine()
                );
            }
        }

        if (ctx.variables() != null) {
            for (var hijo : ctx.variables().children) {
                if (hijo instanceof piglatinParser.DeclaracionVariableContext declVar) {
                    NodoAST decl = visit(declVar);
                    if (decl != null) prog.agregarDeclaracionGlobal(decl);
                } else if (hijo instanceof piglatinParser.DeclaracionArregloContext declArr) {
                    NodoAST decl = visit(declArr);
                    if (decl != null) prog.agregarDeclaracionGlobal(decl);
                }
            }
        }

        if (ctx.principal() != null && ctx.principal().instruccion() != null) {
            for (piglatinParser.InstruccionContext instCtx : ctx.principal().instruccion()) {
                NodoAST inst = visit(instCtx);
                if (inst != null) {
                    prog.agregarInstruccionMaior(inst);
                }
            }
        }

        return prog;
    }

    // --- Declaraciones ---

    @Override
    public NodoAST visitDeclaracionTipada(piglatinParser.DeclaracionTipadaContext ctx) {
        String id = ctx.ID().getText();
        Tipo tipo = obtenerTipo(ctx.tipoDeclarable());
        NodoAST init = ctx.valorInicial() != null ? visit(ctx.valorInicial()) : null;
        return new NodoDeclaracion(id, tipo, init, ctx.ID().getSymbol().getLine(), ctx.ID().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitDeclaracionObjeto(piglatinParser.DeclaracionObjetoContext ctx) {
        String varId = ctx.ID(0).getText();
        String claseId = ctx.ID(1).getText();
        Tipo tipo = (ctx.tipoDeclarable() != null)
            ? obtenerTipo(ctx.tipoDeclarable())
            : new Tipo(TipoBase.OBJETO_CLASE, claseId);

        List<NodoAST> args = new ArrayList<>();
        if (ctx.argumentos() != null) {
            for (piglatinParser.ExpresionContext exp : ctx.argumentos().expresion()) {
                args.add(visit(exp));
            }
        }
        NodoAST init = new NodoInstanciacionObjeto(
            claseId, args, ctx.NOVUS().getSymbol().getLine(), ctx.NOVUS().getSymbol().getCharPositionInLine()
        );
        return new NodoDeclaracion(varId, tipo, init, ctx.ID(0).getSymbol().getLine(), ctx.ID(0).getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitDeclaracionObjetoNulo(piglatinParser.DeclaracionObjetoNuloContext ctx) {
        String varId = ctx.ID().getText();
        Tipo tipo = obtenerTipo(ctx.tipoDeclarable());
        NodoAST init = new NodoLiteral(
            null, Tipo.NULO, ctx.NULO().getSymbol().getLine(), ctx.NULO().getSymbol().getCharPositionInLine()
        );
        return new NodoDeclaracion(varId, tipo, init, ctx.ID().getSymbol().getLine(), ctx.ID().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitDeclaracionBooleana(piglatinParser.DeclaracionBooleanaContext ctx) {
        String id = ctx.ID().getText();
        NodoAST init = visit(ctx.literalBooleano());
        return new NodoDeclaracion(id, Tipo.BOOLEANO, init, ctx.ID().getSymbol().getLine(), ctx.ID().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitDeclaracionArreglo(piglatinParser.DeclaracionArregloContext ctx) {
        String id = ctx.ID().getText();
        Tipo tipoComponente = obtenerTipo(ctx.tipoDeclarable());
        int dimensiones = ctx.CORCHETE_IZQ().size();
        Tipo tipoArreglo = new Tipo(tipoComponente, dimensiones);

        List<NodoAST> tamanios = new ArrayList<>();
        if (ctx.expresion() != null) {
            for (piglatinParser.ExpresionContext exp : ctx.expresion()) {
                tamanios.add(visit(exp));
            }
        }

        NodoAST init = null;
        if (ctx.listaValores() != null) {
            init = visit(ctx.listaValores());
        } else {
            init = new NodoInstanciacionArreglo(tipoComponente, tamanios, ctx.SERIES().getSymbol().getLine(), ctx.SERIES().getSymbol().getCharPositionInLine());
        }

        return new NodoDeclaracion(id, tipoArreglo, init, tamanios, ctx.ID().getSymbol().getLine(), ctx.ID().getSymbol().getCharPositionInLine());
    }

    private Tipo obtenerTipo(piglatinParser.TipoDeclarableContext ctx) {
        if (ctx instanceof piglatinParser.TipoDeclPrimitivoContext primCtx) {
            return obtenerTipoPrimitivo(primCtx.tipoPrimitivo());
        } else if (ctx instanceof piglatinParser.TipoDeclNombradoContext nomCtx) {
            return new Tipo(TipoBase.OBJETO_CLASE, nomCtx.ID().getText());
        }
        return Tipo.ERROR;
    }

    private Tipo obtenerTipoPrimitivo(piglatinParser.TipoPrimitivoContext ctx) {
        if (ctx instanceof piglatinParser.PrimNumerusContext) return Tipo.ENTERO;
        if (ctx instanceof piglatinParser.PrimDecimalisContext) return Tipo.DECIMAL;
        if (ctx instanceof piglatinParser.PrimTextumContext) return Tipo.CADENA;
        if (ctx instanceof piglatinParser.PrimLitteraContext) return Tipo.CARACTER;
        if (ctx instanceof piglatinParser.PrimBooleanusContext) return Tipo.BOOLEANO;
        return Tipo.ERROR;
    }



    @Override
    public NodoAST visitInstAsignacion(piglatinParser.InstAsignacionContext ctx) {
        return visit(ctx.asignacion());
    }

    @Override
    public NodoAST visitAsignacion(piglatinParser.AsignacionContext ctx) {
        NodoAST destino = visit(ctx.encadenado());
        NodoAST valor = visit(ctx.valorInicial());
        return new NodoAsignacion(destino, valor, ctx.ASIGNACION().getSymbol().getLine(), ctx.ASIGNACION().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitListaValores(piglatinParser.ListaValoresContext ctx) {
        List<NodoAST> valores = new ArrayList<>();
        for (piglatinParser.ElementoListaContext elem : ctx.elementoLista()) {
            valores.add(visit(elem));
        }
        return new NodoListaValores(valores, ctx.LLAVE_IZQ().getSymbol().getLine(), ctx.LLAVE_IZQ().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitImpresion(piglatinParser.ImpresionContext ctx) {
        List<NodoAST> exprs = new ArrayList<>();
        for (piglatinParser.ExpresionContext exp : ctx.expresion()) {
            exprs.add(visit(exp));
        }
        return new NodoImprimir(exprs, true, ctx.IMPRIME(0).getSymbol().getLine(), ctx.IMPRIME(0).getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLectura(piglatinParser.LecturaContext ctx) {
        NodoAST destino = ctx.ID() != null ? new NodoIdentificador(
            ctx.ID().getText(), ctx.ID().getSymbol().getLine(), ctx.ID().getSymbol().getCharPositionInLine()
        ) : null;
        return new NodoLectura(destino, ctx.LEE().getSymbol().getLine(), ctx.LEE().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitCondicionalSi(piglatinParser.CondicionalSiContext ctx) {
        List<piglatinParser.BloqueContext> bloques = ctx.bloque();
        List<piglatinParser.ExpresionContext> expresiones = ctx.expresion();

        NodoAST nodoFinal = null;
        int expIdx = expresiones.size() - 1;

        for (int b = bloques.size() - 1; b >= 0; b--) {
            List<NodoAST> cuerpoBloque = obtenerInstruccionesBloque(bloques.get(b));
            if (b == 0) {
                // El bloque principal si(...)
                NodoAST cond = visit(expresiones.get(0));
                List<NodoAST> aliterList = new ArrayList<>();
                if (nodoFinal != null) {
                    aliterList.add(nodoFinal);
                }
                return new NodoIf(cond, cuerpoBloque, aliterList, ctx.SI().getSymbol().getLine(), ctx.SI().getSymbol().getCharPositionInLine());
            } else {

                if (b < bloques.size() - 1 || expresiones.size() == bloques.size()) {
                    NodoAST cond = visit(expresiones.get(expIdx--));
                    List<NodoAST> aliterList = new ArrayList<>();
                    if (nodoFinal != null) {
                        aliterList.add(nodoFinal);
                    }
                    nodoFinal = new NodoIf(cond, cuerpoBloque, aliterList, bloques.get(b).getStart().getLine(), bloques.get(b).getStart().getCharPositionInLine());
                } else {
                    nodoFinal = new NodoIf(
                        new NodoLiteral(true, bloques.get(b).getStart().getLine(), bloques.get(b).getStart().getCharPositionInLine()),
                        cuerpoBloque,
                        null,
                        bloques.get(b).getStart().getLine(),
                        bloques.get(b).getStart().getCharPositionInLine()
                    );
                }
            }
        }

        return nodoFinal;
    }

    private List<NodoAST> obtenerInstruccionesBloque(piglatinParser.BloqueContext ctx) {
        List<NodoAST> lista = new ArrayList<>();
        if (ctx != null && ctx.instruccion() != null) {
            for (piglatinParser.InstruccionContext inst : ctx.instruccion()) {
                NodoAST n = visit(inst);
                if (n != null) lista.add(n);
            }
        }
        return lista;
    }

    @Override
    public NodoAST visitCicloDum(piglatinParser.CicloDumContext ctx) {
        NodoAST cond = visit(ctx.expresion());
        List<NodoAST> cuerpo = obtenerInstruccionesBloque(ctx.bloque());
        return new NodoWhile(cond, cuerpo, ctx.DUM().getSymbol().getLine(), ctx.DUM().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitCicloFacere(piglatinParser.CicloFacereContext ctx) {
        List<NodoAST> cuerpo = obtenerInstruccionesBloque(ctx.bloque());
        NodoAST cond = visit(ctx.expresion());
        return new NodoDoWhile(cond, cuerpo, ctx.FACERE().getSymbol().getLine(), ctx.FACERE().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitCicloPer(piglatinParser.CicloPerContext ctx) {
        NodoAST init = visit(ctx.perInicio());
        NodoAST cond = visit(ctx.expresion());
        NodoAST paso = visit(ctx.perPaso());
        List<NodoAST> cuerpo = obtenerInstruccionesBloque(ctx.bloque());
        return new NodoFor(init, cond, paso, cuerpo, ctx.PER().getSymbol().getLine(), ctx.PER().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitPerInicioDeclaracion(piglatinParser.PerInicioDeclaracionContext ctx) {
        String id = ctx.ID().getText();
        Tipo tipo = obtenerTipoPrimitivo(ctx.tipoPrimitivo());
        NodoAST init = visit(ctx.expresion());
        return new NodoDeclaracion(id, tipo, init, ctx.ID().getSymbol().getLine(), ctx.ID().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitPerInicioAsignacion(piglatinParser.PerInicioAsignacionContext ctx) {
        return visit(ctx.asignacion());
    }

    @Override
    public NodoAST visitPerPaso(piglatinParser.PerPasoContext ctx) {
        if (ctx.asignacion() != null) return visit(ctx.asignacion());
        return visit(ctx.incrementoDecremento());
    }

    @Override
    public NodoAST visitInstControlCiclo(piglatinParser.InstControlCicloContext ctx) {
        return visit(ctx.controlCiclo());
    }

    @Override
    public NodoAST visitControlCiclo(piglatinParser.ControlCicloContext ctx) {
        if (ctx.INTERRUMPIR() != null) {
            return new NodoBreak(ctx.INTERRUMPIR().getSymbol().getLine(), ctx.INTERRUMPIR().getSymbol().getCharPositionInLine());
        }
        return new NodoContinue(ctx.PERGE().getSymbol().getLine(), ctx.PERGE().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstExpresion(piglatinParser.InstExpresionContext ctx) {
        return visit(ctx.encadenado());
    }

    @Override
    public NodoAST visitInstIncrementoDecremento(piglatinParser.InstIncrementoDecrementoContext ctx) {
        return visit(ctx.incrementoDecremento());
    }

    @Override
    public NodoAST visitIncrementoDecremento(piglatinParser.IncrementoDecrementoContext ctx) {
        NodoAST exp = visit(ctx.encadenado());
        String op = ctx.INCREMENTO() != null ? "++" : "--";
        int l = ctx.INCREMENTO() != null ? ctx.INCREMENTO().getSymbol().getLine() : ctx.DECREMENTO().getSymbol().getLine();
        int c = ctx.INCREMENTO() != null ? ctx.INCREMENTO().getSymbol().getCharPositionInLine() : ctx.DECREMENTO().getSymbol().getCharPositionInLine();
        return new NodoOperacionUnaria(op, exp, l, c);
    }
    @Override
    public NodoAST visitExprNovus(piglatinParser.ExprNovusContext ctx) {
        String clase = ctx.ID().getText();
        List<NodoAST> args = new ArrayList<>();
        if (ctx.argumentos() != null) {
            for (piglatinParser.ExpresionContext exp : ctx.argumentos().expresion()) {
                args.add(visit(exp));
            }
        }
        return new NodoInstanciacionObjeto(clase, args, ctx.NOVUS().getSymbol().getLine(), ctx.NOVUS().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprMultiplicativa(piglatinParser.ExprMultiplicativaContext ctx) {
        return UtilidadesAst.binaria(visit(ctx.expresion(0)), ctx.op, visit(ctx.expresion(1)));
    }

    @Override
    public NodoAST visitExprAditiva(piglatinParser.ExprAditivaContext ctx) {
        return UtilidadesAst.binaria(visit(ctx.expresion(0)), ctx.op, visit(ctx.expresion(1)));
    }

    @Override
    public NodoAST visitExprRelacional(piglatinParser.ExprRelacionalContext ctx) {
        return UtilidadesAst.binaria(visit(ctx.expresion(0)), ctx.op, visit(ctx.expresion(1)));
    }

    @Override
    public NodoAST visitExprIgualdad(piglatinParser.ExprIgualdadContext ctx) {
        return UtilidadesAst.binaria(visit(ctx.expresion(0)), ctx.op, visit(ctx.expresion(1)));
    }

    @Override
    public NodoAST visitExprConjuncion(piglatinParser.ExprConjuncionContext ctx) {
        return UtilidadesAst.binaria(visit(ctx.expresion(0)), "&&", ctx.Y_LOGICO().getSymbol(), visit(ctx.expresion(1)));
    }

    @Override
    public NodoAST visitExprDisyuncion(piglatinParser.ExprDisyuncionContext ctx) {
        return UtilidadesAst.binaria(visit(ctx.expresion(0)), "||", ctx.O_LOGICO().getSymbol(), visit(ctx.expresion(1)));
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
            } else if (suf instanceof piglatinParser.SufijoMiembroContext mCtx) {
                actual = new NodoAccesoMiembro(
                    actual,
                    mCtx.ID().getText(),
                    mCtx.PUNTO().getSymbol().getLine(),
                    mCtx.PUNTO().getSymbol().getCharPositionInLine()
                );
            } else if (suf instanceof piglatinParser.SufijoLlamadaContext callCtx) {
                List<NodoAST> args = new ArrayList<>();
                if (callCtx.argumentos() != null) {
                    for (piglatinParser.ExpresionContext expCtx : callCtx.argumentos().expresion()) {
                        args.add(visit(expCtx));
                    }
                }
                if (actual instanceof NodoAccesoMiembro miembro) {
                    actual = new NodoLlamadaMetodo(
                        miembro.getEstructura(),
                        miembro.getNombreCampo(),
                        args,
                        callCtx.PAR_IZQ().getSymbol().getLine(),
                        callCtx.PAR_IZQ().getSymbol().getCharPositionInLine()
                    );
                } else if (actual instanceof NodoIdentificador id) {
                    actual = new NodoLlamadaFuncion(
                        id.getNombre(),
                        args,
                        callCtx.PAR_IZQ().getSymbol().getLine(),
                        callCtx.PAR_IZQ().getSymbol().getCharPositionInLine()
                    );
                }
            }
        }
        return actual;
    }

    @Override
    public NodoAST visitLitEntero(piglatinParser.LitEnteroContext ctx) {
        return UtilidadesAst.literalEntero(ctx.LIT_ENTERO());
    }

    @Override
    public NodoAST visitLitDecimal(piglatinParser.LitDecimalContext ctx) {
        return UtilidadesAst.literalDecimal(ctx.LIT_DECIMAL());
    }

    @Override
    public NodoAST visitLitCadena(piglatinParser.LitCadenaContext ctx) {
        return UtilidadesAst.literalCadena(ctx.LIT_CADENA());
    }

    @Override
    public NodoAST visitLitCaracter(piglatinParser.LitCaracterContext ctx) {
        return UtilidadesAst.literalCaracter(ctx.LIT_CARACTER());
    }

    @Override
    public NodoAST visitLiteralBooleano(piglatinParser.LiteralBooleanoContext ctx) {
        boolean val = ctx.getText().equals("verum");
        return new NodoLiteral(val, ctx.getStart().getLine(), ctx.getStart().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitNulo(piglatinParser.LitNuloContext ctx) {
        return new NodoLiteral(null, Tipo.NULO, ctx.NULO().getSymbol().getLine(), ctx.NULO().getSymbol().getCharPositionInLine());
    }
}