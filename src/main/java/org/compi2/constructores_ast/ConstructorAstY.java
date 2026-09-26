package org.compi2.constructores_ast;

import gramaticas.y.yBaseVisitor;
import gramaticas.y.yParser;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.compi2.ast.NodoAST;
import org.compi2.ast.expresiones.*;
import org.compi2.ast.sentencias.*;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConstructorAstY extends yBaseVisitor<NodoAST> {

    @Override
    public NodoAST visitPrograma(yParser.ProgramaContext ctx) {
        NodoProgramaY prog = new NodoProgramaY();

        if (ctx.seccionEstructuras() != null && ctx.seccionEstructuras().definicionEstructura() != null) {
            for (yParser.DefinicionEstructuraContext defEst : ctx.seccionEstructuras().definicionEstructura()) {
                NodoAST est = visit(defEst);
                if (est instanceof NodoEstructura ne) {
                    prog.agregarEstructura(ne);
                }
            }
        }

        if (ctx.seccionFunciones() != null && ctx.seccionFunciones().definicionFuncion() != null) {
            for (yParser.DefinicionFuncionContext defFn : ctx.seccionFunciones().definicionFuncion()) {
                NodoAST fn = visit(defFn);
                if (fn instanceof NodoFuncion nf) {
                    prog.agregarFuncion(nf);
                }
            }
        }

        return prog;
    }

    @Override
    public NodoAST visitDefinicionEstructura(yParser.DefinicionEstructuraContext ctx) {
        String nombre = ctx.ID().getText();
        Map<String, Tipo> campos = new LinkedHashMap<>();

        if (ctx.bloqueCampos() != null && ctx.bloqueCampos().campoEstructura() != null) {
            for (yParser.CampoEstructuraContext campo : ctx.bloqueCampos().campoEstructura()) {
                Tipo tipoCampo = obtenerTipo(campo.tipo());
                int dimensiones = campo.CORCHETE_IZQ().size();
                if (dimensiones > 0) {
                    tipoCampo = new Tipo(tipoCampo, dimensiones);
                }
                campos.put(campo.ID().getText(), tipoCampo);
            }
        }

        return new NodoEstructura(
            nombre,
            campos,
            ctx.ESTRUCTURA().getSymbol().getLine(),
            ctx.ESTRUCTURA().getSymbol().getCharPositionInLine()
        );
    }

    @Override
    public NodoAST visitDefinicionFuncion(yParser.DefinicionFuncionContext ctx) {
        String nombre = ctx.ID().getText();
        List<Parametro> params = new ArrayList<>();

        if (ctx.parametros() != null) {
            for (yParser.ParametroContext p : ctx.parametros().parametro()) {
                Tipo t = obtenerTipo(p.tipo());
                if (p.CORCHETE_IZQ() != null && !p.CORCHETE_IZQ().isEmpty()) {
                    t = new Tipo(t, p.CORCHETE_IZQ().size());
                }
                params.add(new Parametro(
                    p.ID().getText(),
                    t,
                    p.ID().getSymbol().getLine(),
                    p.ID().getSymbol().getCharPositionInLine()
                ));
            }
        }

        Tipo tipoRetorno = ctx.tipo() != null ? obtenerTipo(ctx.tipo()) : Tipo.VOID;
        List<NodoAST> cuerpo = obtenerInstruccionesBloque(ctx.bloque());

        return new NodoFuncion(
            nombre,
            params,
            tipoRetorno,
            cuerpo,
            ctx.ID().getSymbol().getLine(),
            ctx.ID().getSymbol().getCharPositionInLine()
        );
    }

    private List<NodoAST> obtenerInstruccionesBloque(yParser.BloqueContext ctx) {
        List<NodoAST> lista = new ArrayList<>();
        if (ctx != null && ctx.instruccion() != null) {
            for (yParser.InstruccionContext inst : ctx.instruccion()) {
                NodoAST n = visit(inst);
                if (n != null) lista.add(n);
            }
        }
        return lista;
    }

    private Tipo obtenerTipo(yParser.TipoContext ctx) {
        if (ctx instanceof yParser.TipoEnteroContext) return Tipo.ENTERO;
        if (ctx instanceof yParser.TipoFlotanteContext) return Tipo.DECIMAL;
        if (ctx instanceof yParser.TipoCadenaContext) return Tipo.CADENA;
        if (ctx instanceof yParser.TipoCaracterContext) return Tipo.CARACTER;
        if (ctx instanceof yParser.TipoBoolContext) return Tipo.BOOLEANO;
        if (ctx instanceof yParser.TipoEstructuraContext estCtx) {
            return new Tipo(TipoBase.ESTRUCTURA, estCtx.ID().getText());
        }
        return Tipo.ERROR;
    }

    @Override
    public NodoAST visitInstDefinicionEstructura(yParser.InstDefinicionEstructuraContext ctx) {
        return visit(ctx.definicionEstructura());
    }

    @Override
    public NodoAST visitInstDeclaracion(yParser.InstDeclaracionContext ctx) {
        return visit(ctx.declaracionVariable());
    }

    @Override
    public NodoAST visitDeclaracionVariable(yParser.DeclaracionVariableContext ctx) {
        String id = ctx.ID().getText();
        Tipo tipoBase = obtenerTipo(ctx.tipo());
        int dimensiones = ctx.CORCHETE_IZQ().size();
        Tipo tipoFinal = dimensiones > 0 ? new Tipo(tipoBase, dimensiones) : tipoBase;

        List<NodoAST> tamanios = new ArrayList<>();
        if (dimensiones > 0 && ctx.expresion() != null) {
            for (yParser.ExpresionContext exp : ctx.expresion()) {
                tamanios.add(visit(exp));
            }
        }

        NodoAST init = null;
        if (ctx.valorInicial() != null) {
            init = visit(ctx.valorInicial());
        } else if (dimensiones > 0) {
            init = new NodoInstanciacionArreglo(tipoBase, tamanios, ctx.ID().getSymbol().getLine(), ctx.ID().getSymbol().getCharPositionInLine());
        }

        return new NodoDeclaracion(id, tipoFinal, init, tamanios, ctx.ID().getSymbol().getLine(), ctx.ID().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstAsignacion(yParser.InstAsignacionContext ctx) {
        return visit(ctx.asignacion());
    }

    @Override
    public NodoAST visitAsignacion(yParser.AsignacionContext ctx) {
        NodoAST destino = visit(ctx.acceso());
        NodoAST valor = visit(ctx.valorInicial());
        return new NodoAsignacion(destino, valor, ctx.ASIGNACION().getSymbol().getLine(), ctx.ASIGNACION().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitValorExpresion(yParser.ValorExpresionContext ctx) {
        return visit(ctx.expresion());
    }

    @Override
    public NodoAST visitValorLista(yParser.ValorListaContext ctx) {
        return visit(ctx.listaLiteral());
    }

    @Override
    public NodoAST visitListaLiteral(yParser.ListaLiteralContext ctx) {
        List<NodoAST> valores = new ArrayList<>();
        for (yParser.ElementoListaContext elem : ctx.elementoLista()) {
            valores.add(visit(elem));
        }
        return new NodoListaValores(valores, ctx.LLAVE_IZQ().getSymbol().getLine(), ctx.LLAVE_IZQ().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitElementoExpresion(yParser.ElementoExpresionContext ctx) {
        return visit(ctx.expresion());
    }

    @Override
    public NodoAST visitElementoAnidado(yParser.ElementoAnidadoContext ctx) {
        return visit(ctx.listaLiteral());
    }

    @Override
    public NodoAST visitInstIncDec(yParser.InstIncDecContext ctx) {
        return visit(ctx.incrementoDecremento());
    }

    @Override
    public NodoAST visitIncrementoDecremento(yParser.IncrementoDecrementoContext ctx) {
        NodoAST destino = visit(ctx.acceso());
        String op = ctx.INCREMENTO() != null ? "++" : "--";
        int l = ctx.INCREMENTO() != null ? ctx.INCREMENTO().getSymbol().getLine() : ctx.DECREMENTO().getSymbol().getLine();
        int c = ctx.INCREMENTO() != null ? ctx.INCREMENTO().getSymbol().getCharPositionInLine() : ctx.DECREMENTO().getSymbol().getCharPositionInLine();
        return new NodoOperacionUnaria(op, destino, l, c, false);
    }

    @Override
    public NodoAST visitInstLlamada(yParser.InstLlamadaContext ctx) {
        return visit(ctx.llamadaFuncion());
    }

    @Override
    public NodoAST visitLlamadaFuncion(yParser.LlamadaFuncionContext ctx) {
        String nombre = ctx.ID().getText();
        List<NodoAST> args = new ArrayList<>();
        if (ctx.argumentos() != null) {
            for (yParser.ExpresionContext exp : ctx.argumentos().expresion()) {
                args.add(visit(exp));
            }
        }
        return new NodoLlamadaFuncion(nombre, args, ctx.ID().getSymbol().getLine(), ctx.ID().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstImprimir(yParser.InstImprimirContext ctx) {
        List<NodoAST> exprs = new ArrayList<>();
        if (ctx.expresion() != null) {
            for (yParser.ExpresionContext exp : ctx.expresion()) {
                exprs.add(visit(exp));
            }
        }
        return new NodoImprimir(exprs, true, ctx.IMPRIMIR().getSymbol().getLine(), ctx.IMPRIMIR().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstRetornar(yParser.InstRetornarContext ctx) {
        NodoAST exp = ctx.expresion() != null ? visit(ctx.expresion()) : null;
        return new NodoRetorno(exp, ctx.RETORNAR().getSymbol().getLine(), ctx.RETORNAR().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstRomper(yParser.InstRomperContext ctx) {
        return new NodoBreak(ctx.ROMPER().getSymbol().getLine(), ctx.ROMPER().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstContinuar(yParser.InstContinuarContext ctx) {
        return new NodoContinue(ctx.CONTINUAR().getSymbol().getLine(), ctx.CONTINUAR().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstSi(yParser.InstSiContext ctx) {
        return visit(ctx.condicionalSi());
    }

    @Override
    public NodoAST visitCondicionalSi(yParser.CondicionalSiContext ctx) {
        List<yParser.BloqueContext> bloques = ctx.bloque();
        List<yParser.ExpresionContext> expresiones = ctx.expresion();

        List<NodoAST> cuerpoContrario = null;
        if (ctx.CONTRARIO() != null) {
            cuerpoContrario = obtenerInstruccionesBloque(bloques.get(bloques.size() - 1));
        }

        NodoAST nodoActual = null;
        if (cuerpoContrario != null) {
            nodoActual = new NodoIf(
                new NodoLiteral(true, ctx.CONTRARIO().getSymbol().getLine(), ctx.CONTRARIO().getSymbol().getCharPositionInLine()),
                cuerpoContrario,
                null,
                ctx.CONTRARIO().getSymbol().getLine(),
                ctx.CONTRARIO().getSymbol().getCharPositionInLine()
            );
        }

        int totalIfSino = expresiones.size();
        for (int i = totalIfSino - 1; i >= 0; i--) {
            NodoAST cond = visit(expresiones.get(i));
            List<NodoAST> cuerpo = obtenerInstruccionesBloque(bloques.get(i));
            List<NodoAST> aliter = new ArrayList<>();
            if (nodoActual != null) {
                aliter.add(nodoActual);
            }
            nodoActual = new NodoIf(cond, cuerpo, aliter, expresiones.get(i).getStart().getLine(), expresiones.get(i).getStart().getCharPositionInLine());
        }

        return nodoActual;
    }

    @Override
    public NodoAST visitInstElegir(yParser.InstElegirContext ctx) {
        return visit(ctx.seleccionElegir());
    }

    @Override
    public NodoAST visitSeleccionElegir(yParser.SeleccionElegirContext ctx) {
        NodoAST control = visit(ctx.expresion());
        List<NodoSwitch.CasoSwitch> casos = new ArrayList<>();

        if (ctx.seccionCaso() != null) {
            for (yParser.SeccionCasoContext sc : ctx.seccionCaso()) {
                NodoAST valorCaso = visit(sc.literal());
                List<NodoAST> insts = new ArrayList<>();
                if (sc.instruccionSuelta() != null) {
                    for (yParser.InstruccionSueltaContext is : sc.instruccionSuelta()) {
                        NodoAST n = visit(is);
                        if (n != null) insts.add(n);
                    }
                }
                casos.add(new NodoSwitch.CasoSwitch(valorCaso, insts, sc.CASO().getSymbol().getLine(), sc.CASO().getSymbol().getCharPositionInLine()));
            }
        }

        List<NodoAST> casoDefecto = null;
        if (ctx.seccionSiempre() != null) {
            casoDefecto = new ArrayList<>();
            if (ctx.seccionSiempre().instruccionSuelta() != null) {
                for (yParser.InstruccionSueltaContext is : ctx.seccionSiempre().instruccionSuelta()) {
                    NodoAST n = visit(is);
                    if (n != null) casoDefecto.add(n);
                }
            }
        }

        return new NodoSwitch(control, casos, casoDefecto, ctx.ELEGIR().getSymbol().getLine(), ctx.ELEGIR().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstruccionSuelta(yParser.InstruccionSueltaContext ctx) {
        if (ctx.declaracionVariable() != null) return visit(ctx.declaracionVariable());
        if (ctx.asignacion() != null) return visit(ctx.asignacion());
        if (ctx.incrementoDecremento() != null) return visit(ctx.incrementoDecremento());
        if (ctx.llamadaFuncion() != null) return visit(ctx.llamadaFuncion());
        if (ctx.IMPRIMIR() != null) {
            List<NodoAST> exprs = new ArrayList<>();
            if (ctx.expresion() != null) {
                for (yParser.ExpresionContext exp : ctx.expresion()) exprs.add(visit(exp));
            }
            return new NodoImprimir(exprs, true, ctx.IMPRIMIR().getSymbol().getLine(), ctx.IMPRIMIR().getSymbol().getCharPositionInLine());
        }
        if (ctx.RETORNAR() != null) {
            NodoAST exp = ctx.expresion(0) != null ? visit(ctx.expresion(0)) : null;
            return new NodoRetorno(exp, ctx.RETORNAR().getSymbol().getLine(), ctx.RETORNAR().getSymbol().getCharPositionInLine());
        }
        if (ctx.ROMPER() != null) {
            return new NodoBreak(ctx.ROMPER().getSymbol().getLine(), ctx.ROMPER().getSymbol().getCharPositionInLine());
        }
        if (ctx.CONTINUAR() != null) {
            return new NodoContinue(ctx.CONTINUAR().getSymbol().getLine(), ctx.CONTINUAR().getSymbol().getCharPositionInLine());
        }
        return null;
    }

    @Override
    public NodoAST visitInstPara(yParser.InstParaContext ctx) {
        return visit(ctx.cicloPara());
    }

    @Override
    public NodoAST visitCicloPara(yParser.CicloParaContext ctx) {
        NodoAST init = ctx.paraInicio() != null ? visit(ctx.paraInicio()) : null;
        NodoAST cond = ctx.expresion() != null ? visit(ctx.expresion()) : null;
        NodoAST paso = ctx.paraPaso() != null ? visit(ctx.paraPaso()) : null;
        List<NodoAST> cuerpo = obtenerInstruccionesBloque(ctx.bloque());
        return new NodoFor(init, cond, paso, cuerpo, ctx.PARA().getSymbol().getLine(), ctx.PARA().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitParaInicioDeclaracion(yParser.ParaInicioDeclaracionContext ctx) {
        return visit(ctx.declaracionVariable());
    }

    @Override
    public NodoAST visitParaInicioAsignacion(yParser.ParaInicioAsignacionContext ctx) {
        return visit(ctx.asignacion());
    }

    @Override
    public NodoAST visitParaPaso(yParser.ParaPasoContext ctx) {
        if (ctx.asignacion() != null) return visit(ctx.asignacion());
        return visit(ctx.incrementoDecremento());
    }

    @Override
    public NodoAST visitInstMientras(yParser.InstMientrasContext ctx) {
        return visit(ctx.cicloMientras());
    }

    @Override
    public NodoAST visitCicloMientras(yParser.CicloMientrasContext ctx) {
        NodoAST cond = visit(ctx.expresion());
        List<NodoAST> cuerpo = obtenerInstruccionesBloque(ctx.bloque());
        return new NodoWhile(cond, cuerpo, ctx.MIENTRAS().getSymbol().getLine(), ctx.MIENTRAS().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstHacer(yParser.InstHacerContext ctx) {
        return visit(ctx.cicloHacer());
    }

    @Override
    public NodoAST visitCicloHacer(yParser.CicloHacerContext ctx) {
        List<NodoAST> cuerpo = obtenerInstruccionesBloque(ctx.bloque());
        NodoAST cond = visit(ctx.expresion());
        return new NodoDoWhile(cond, cuerpo, ctx.HACER().getSymbol().getLine(), ctx.HACER().getSymbol().getCharPositionInLine());
    }

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
        return new NodoExpresionLectura(ctx.LEER().getSymbol().getLine(), ctx.LEER().getSymbol().getCharPositionInLine());
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
            if (hijo instanceof TerminalNode t) {
                if (t.getSymbol().getType() == yParser.PUNTO) {
                    i++;
                    TerminalNode campo = (TerminalNode) ctx.getChild(i);
                    actual = new NodoAccesoMiembro(actual, campo.getText(), t.getSymbol().getLine(), t.getSymbol().getCharPositionInLine());
                } else if (t.getSymbol().getType() == yParser.CORCHETE_IZQ) {
                    i++;
                    ParseTree expHijo = ctx.getChild(i);
                    if (expHijo instanceof yParser.ExpresionContext expCtx) {
                        NodoAST indice = visit(expCtx);
                        actual = new NodoAccesoArreglo(actual, indice, t.getSymbol().getLine(), t.getSymbol().getCharPositionInLine());
                    }
                    i++;
                } else if (t.getSymbol().getType() == yParser.PAR_IZQ) {
                    i++;
                    ParseTree argsHijo = ctx.getChild(i);
                    List<NodoAST> args = new ArrayList<>();
                    if (argsHijo instanceof yParser.ArgumentosContext argsCtx) {
                        for (yParser.ExpresionContext expCtx : argsCtx.expresion()) {
                            args.add(visit(expCtx));
                        }
                        i++;
                    }
                    if (actual instanceof NodoAccesoMiembro m) {
                        actual = new NodoLlamadaMetodo(m.getEstructura(), m.getNombreCampo(), args, t.getSymbol().getLine(), t.getSymbol().getCharPositionInLine());
                    } else if (actual instanceof NodoIdentificador id) {
                        actual = new NodoLlamadaFuncion(id.getNombre(), args, t.getSymbol().getLine(), t.getSymbol().getCharPositionInLine());
                    }
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