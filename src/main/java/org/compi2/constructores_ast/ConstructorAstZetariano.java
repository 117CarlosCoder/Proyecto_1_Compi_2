package org.compi2.constructores_ast;

import gramaticas.zetariano.zetarianoBaseVisitor;
import gramaticas.zetariano.zetarianoParser;
import org.compi2.ast.NodoAST;
import org.compi2.ast.expresiones.*;
import org.compi2.ast.sentencias.*;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;

import java.util.ArrayList;
import java.util.List;

public class ConstructorAstZetariano extends zetarianoBaseVisitor<NodoAST> {

    @Override
    public NodoAST visitUnidadCompilacion(zetarianoParser.UnidadCompilacionContext ctx) {
        NodoUnidadCompilacionZ raiz = new NodoUnidadCompilacionZ();
        if (ctx.definicionClase() != null) {
            for (zetarianoParser.DefinicionClaseContext defClase : ctx.definicionClase()) {
                NodoAST cl = visit(defClase);
                if (cl instanceof NodoClase nc) {
                    raiz.agregarClase(nc);
                }
            }
        }
        return raiz;
    }

    @Override
    public NodoAST visitDefinicionClase(zetarianoParser.DefinicionClaseContext ctx) {
        String nombreClase = ctx.ID().getText();
        List<NodoDeclaracion> campos = new ArrayList<>();
        List<NodoConstructor> constructores = new ArrayList<>();
        List<NodoFuncion> metodos = new ArrayList<>();

        if (ctx.miembro() != null) {
            for (zetarianoParser.MiembroContext m : ctx.miembro()) {
                if (m instanceof zetarianoParser.MiembroCampoContext mc) {
                    NodoAST decl = visit(mc.declaracionCampo());
                    if (decl instanceof NodoDeclaracion nd)
                        campos.add(nd);
                } else if (m instanceof zetarianoParser.MiembroConstructorContext mconst) {
                    NodoAST c = visit(mconst.constructor());
                    if (c instanceof NodoConstructor nc)
                        constructores.add(nc);
                } else if (m instanceof zetarianoParser.MiembroMetodoContext mm) {
                    NodoAST met = visit(mm.metodo());
                    if (met instanceof NodoFuncion nf)
                        metodos.add(nf);
                }
            }
        }

        return new NodoClase(
                nombreClase,
                campos,
                constructores,
                metodos,
                ctx.CLASE().getSymbol().getLine(),
                ctx.CLASE().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitDeclaracionCampo(zetarianoParser.DeclaracionCampoContext ctx) {
        String id = ctx.ID().getText();
        Tipo t = obtenerTipo(ctx.tipo());
        NodoAST init = ctx.inicializador() != null ? visit(ctx.inicializador()) : null;
        return new NodoDeclaracion(id, t, init, ctx.ID().getSymbol().getLine(),
                ctx.ID().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitConstructor(zetarianoParser.ConstructorContext ctx) {
        String nombre = ctx.ID().getText();
        List<Parametro> params = obtenerParametros(ctx.parametrosFormales());
        List<NodoAST> cuerpo = obtenerInstruccionesBloque(ctx.bloque());
        return new NodoConstructor(nombre, params, cuerpo, ctx.ID().getSymbol().getLine(),
                ctx.ID().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitMetodo(zetarianoParser.MetodoContext ctx) {
        String nombre = ctx.ID().getText();
        Tipo retTipo = obtenerTipoRetorno(ctx.tipoRetorno());
        List<Parametro> params = obtenerParametros(ctx.parametrosFormales());
        List<NodoAST> cuerpo = obtenerInstruccionesBloque(ctx.bloque());
        return new NodoFuncion(nombre, params, retTipo, cuerpo, ctx.ID().getSymbol().getLine(),
                ctx.ID().getSymbol().getCharPositionInLine());
    }

    private Tipo obtenerTipo(zetarianoParser.TipoContext ctx) {
        Tipo base = obtenerTipoBase(ctx.tipoBase());
        int dims = ctx.CORCHETE_IZQ().size();
        return dims > 0 ? new Tipo(base, dims) : base;
    }

    private Tipo obtenerTipoBase(zetarianoParser.TipoBaseContext ctx) {
        if (ctx instanceof zetarianoParser.BaseEnteroContext)
            return Tipo.ENTERO;
        if (ctx instanceof zetarianoParser.BaseDecimalContext)
            return Tipo.DECIMAL;
        if (ctx instanceof zetarianoParser.BaseCaracterContext)
            return Tipo.CARACTER;
        if (ctx instanceof zetarianoParser.BaseBooleanoContext)
            return Tipo.BOOLEANO;
        if (ctx instanceof zetarianoParser.BaseCadenaContext)
            return Tipo.CADENA;
        if (ctx instanceof zetarianoParser.BaseClaseContext bClase) {
            return new Tipo(TipoBase.OBJETO_CLASE, bClase.ID().getText());
        }
        return Tipo.ERROR;
    }

    private Tipo obtenerTipoRetorno(zetarianoParser.TipoRetornoContext ctx) {
        if (ctx instanceof zetarianoParser.RetornoVacioContext)
            return Tipo.VOID;
        if (ctx instanceof zetarianoParser.RetornoTipoContext rt)
            return obtenerTipo(rt.tipo());
        return Tipo.VOID;
    }

    private List<Parametro> obtenerParametros(zetarianoParser.ParametrosFormalesContext ctx) {
        List<Parametro> lista = new ArrayList<>();
        if (ctx != null && ctx.parametroFormal() != null) {
            for (zetarianoParser.ParametroFormalContext p : ctx.parametroFormal()) {
                Tipo t = obtenerTipo(p.tipo());
                lista.add(new Parametro(p.ID().getText(), t, p.ID().getSymbol().getLine(),
                        p.ID().getSymbol().getCharPositionInLine()));
            }
        }
        return lista;
    }

    private List<NodoAST> obtenerInstruccionesBloque(zetarianoParser.BloqueContext ctx) {
        List<NodoAST> lista = new ArrayList<>();
        if (ctx != null && ctx.instruccion() != null) {
            for (zetarianoParser.InstruccionContext inst : ctx.instruccion()) {
                lista.addAll(obtenerInstruccionesDeInst(inst));
            }
        }
        return lista;
    }

    private List<NodoAST> obtenerInstruccionesDeInst(zetarianoParser.InstruccionContext ctx) {
        List<NodoAST> lista = new ArrayList<>();
        if (ctx == null)
            return lista;

        if (ctx instanceof zetarianoParser.InstBloqueContext ib) {
            lista.addAll(obtenerInstruccionesBloque(ib.bloque()));
        } else {
            NodoAST n = visit(ctx);
            if (n != null)
                lista.add(n);
        }
        return lista;
    }

    @Override
    public NodoAST visitInstDeclaracionLocal(zetarianoParser.InstDeclaracionLocalContext ctx) {
        return visit(ctx.declaracionLocal());
    }

    @Override
    public NodoAST visitDeclaracionLocal(zetarianoParser.DeclaracionLocalContext ctx) {
        return visit(ctx.declaracionLocalBase());
    }

    @Override
    public NodoAST visitDeclaracionLocalBase(zetarianoParser.DeclaracionLocalBaseContext ctx) {
        String id = ctx.ID().getText();
        Tipo t = obtenerTipo(ctx.tipo());
        NodoAST init = ctx.inicializador() != null ? visit(ctx.inicializador()) : null;
        return new NodoDeclaracion(id, t, init, ctx.ID().getSymbol().getLine(),
                ctx.ID().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInicExpresion(zetarianoParser.InicExpresionContext ctx) {
        return visit(ctx.expresion());
    }

    @Override
    public NodoAST visitInicLista(zetarianoParser.InicListaContext ctx) {
        return visit(ctx.listaInicializador());
    }

    @Override
    public NodoAST visitListaInicializador(zetarianoParser.ListaInicializadorContext ctx) {
        List<NodoAST> valores = new ArrayList<>();
        for (zetarianoParser.InicializadorContext inic : ctx.inicializador()) {
            valores.add(visit(inic));
        }
        return new NodoListaValores(valores, ctx.LLAVE_IZQ().getSymbol().getLine(),
                ctx.LLAVE_IZQ().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstAsignacion(zetarianoParser.InstAsignacionContext ctx) {
        return visit(ctx.asignacion());
    }

    @Override
    public NodoAST visitAsignacion(zetarianoParser.AsignacionContext ctx) {
        NodoAST destino = visit(ctx.encadenado());
        NodoAST valor = visit(ctx.expresion());
        int l = ctx.operadorAsignacion().getStart().getLine();
        int c = ctx.operadorAsignacion().getStart().getCharPositionInLine();

        if (ctx.operadorAsignacion() instanceof zetarianoParser.OpAsignaContext) {
            return new NodoAsignacion(destino, valor, l, c);
        }

        String opBin = null;
        if (ctx.operadorAsignacion() instanceof zetarianoParser.OpAsignaSumaContext)
            opBin = "+";
        else if (ctx.operadorAsignacion() instanceof zetarianoParser.OpAsignaRestaContext)
            opBin = "-";
        else if (ctx.operadorAsignacion() instanceof zetarianoParser.OpAsignaProductoContext)
            opBin = "*";
        else if (ctx.operadorAsignacion() instanceof zetarianoParser.OpAsignaDivisionContext)
            opBin = "/";
        else if (ctx.operadorAsignacion() instanceof zetarianoParser.OpAsignaModuloContext)
            opBin = "%";

        NodoAST valorFinal = new NodoOperacionBinaria(destino, opBin, valor, l, c);
        return new NodoAsignacion(destino, valorFinal, l, c);
    }

    @Override
    public NodoAST visitInstIncDec(zetarianoParser.InstIncDecContext ctx) {
        return visit(ctx.incrementoDecremento());
    }

    @Override
    public NodoAST visitIncDecPostfijo(zetarianoParser.IncDecPostfijoContext ctx) {
        NodoAST dest = visit(ctx.encadenado());
        String op = ctx.INCREMENTO() != null ? "++" : "--";
        int l = ctx.getStart().getLine();
        int c = ctx.getStart().getCharPositionInLine();
        return new NodoOperacionUnaria(op, dest, l, c);
    }

    @Override
    public NodoAST visitIncDecPrefijo(zetarianoParser.IncDecPrefijoContext ctx) {
        NodoAST dest = visit(ctx.encadenado());
        String op = ctx.INCREMENTO() != null ? "++" : "--";
        int l = ctx.getStart().getLine();
        int c = ctx.getStart().getCharPositionInLine();
        return new NodoOperacionUnaria(op, dest, l, c);
    }

    @Override
    public NodoAST visitInstSi(zetarianoParser.InstSiContext ctx) {
        return visit(ctx.condicionalSi());
    }

    @Override
    public NodoAST visitCondicionalSi(zetarianoParser.CondicionalSiContext ctx) {
        NodoAST cond = visit(ctx.expresion());
        List<NodoAST> cuerpoSi = obtenerInstruccionesDeInst(ctx.instruccion(0));
        List<NodoAST> cuerpoSino = (ctx.instruccion().size() > 1) ? obtenerInstruccionesDeInst(ctx.instruccion(1))
                : null;
        return new NodoIf(cond, cuerpoSi, cuerpoSino, ctx.SI().getSymbol().getLine(),
                ctx.SI().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstSwitch(zetarianoParser.InstSwitchContext ctx) {
        return visit(ctx.seleccionSwitch());
    }

    @Override
    public NodoAST visitSeleccionSwitch(zetarianoParser.SeleccionSwitchContext ctx) {
        NodoAST control = visit(ctx.expresion());
        List<NodoSwitch.CasoSwitch> casos = new ArrayList<>();

        if (ctx.seccionCaso() != null) {
            for (zetarianoParser.SeccionCasoContext sc : ctx.seccionCaso()) {
                NodoAST valorCaso = visit(sc.expresion());
                List<NodoAST> insts = new ArrayList<>();
                if (sc.instruccion() != null) {
                    for (zetarianoParser.InstruccionContext i : sc.instruccion()) {
                        insts.addAll(obtenerInstruccionesDeInst(i));
                    }
                }
                casos.add(new NodoSwitch.CasoSwitch(valorCaso, insts, sc.CASO().getSymbol().getLine(),
                        sc.CASO().getSymbol().getCharPositionInLine()));
            }
        }

        List<NodoAST> defInsts = null;
        if (ctx.seccionDefecto() != null) {
            defInsts = new ArrayList<>();
            if (ctx.seccionDefecto().instruccion() != null) {
                for (zetarianoParser.InstruccionContext i : ctx.seccionDefecto().instruccion()) {
                    defInsts.addAll(obtenerInstruccionesDeInst(i));
                }
            }
        }

        return new NodoSwitch(control, casos, defInsts, ctx.SWITCH().getSymbol().getLine(),
                ctx.SWITCH().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstFor(zetarianoParser.InstForContext ctx) {
        return visit(ctx.cicloFor());
    }

    @Override
    public NodoAST visitCicloFor(zetarianoParser.CicloForContext ctx) {
        NodoAST init = ctx.inicioFor() != null ? visit(ctx.inicioFor()) : null;
        NodoAST cond = ctx.expresion() != null ? visit(ctx.expresion()) : null;
        NodoAST paso = ctx.pasoFor() != null ? visit(ctx.pasoFor()) : null;
        List<NodoAST> cuerpo = obtenerInstruccionesDeInst(ctx.instruccion());
        return new NodoFor(init, cond, paso, cuerpo, ctx.PARA().getSymbol().getLine(),
                ctx.PARA().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitForInicioDeclaracion(zetarianoParser.ForInicioDeclaracionContext ctx) {
        return visit(ctx.declaracionLocalBase());
    }

    @Override
    public NodoAST visitForInicioAsignacion(zetarianoParser.ForInicioAsignacionContext ctx) {
        return visit(ctx.asignacion());
    }

    @Override
    public NodoAST visitPasoFor(zetarianoParser.PasoForContext ctx) {
        if (ctx.asignacion() != null)
            return visit(ctx.asignacion());
        return visit(ctx.incrementoDecremento());
    }

    @Override
    public NodoAST visitInstWhile(zetarianoParser.InstWhileContext ctx) {
        return visit(ctx.cicloWhile());
    }

    @Override
    public NodoAST visitCicloWhile(zetarianoParser.CicloWhileContext ctx) {
        NodoAST cond = visit(ctx.expresion());
        List<NodoAST> cuerpo = obtenerInstruccionesDeInst(ctx.instruccion());
        return new NodoWhile(cond, cuerpo, ctx.MIENTRAS().getSymbol().getLine(),
                ctx.MIENTRAS().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstDoWhile(zetarianoParser.InstDoWhileContext ctx) {
        return visit(ctx.cicloDoWhile());
    }

    @Override
    public NodoAST visitCicloDoWhile(zetarianoParser.CicloDoWhileContext ctx) {
        List<NodoAST> cuerpo = obtenerInstruccionesDeInst(ctx.instruccion());
        NodoAST cond = visit(ctx.expresion());
        return new NodoDoWhile(cond, cuerpo, ctx.HACER().getSymbol().getLine(),
                ctx.HACER().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstRomper(zetarianoParser.InstRomperContext ctx) {
        return new NodoBreak(ctx.ROMPER().getSymbol().getLine(), ctx.ROMPER().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstContinuar(zetarianoParser.InstContinuarContext ctx) {
        return new NodoContinue(ctx.CONTINUAR().getSymbol().getLine(),
                ctx.CONTINUAR().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstRetornar(zetarianoParser.InstRetornarContext ctx) {
        NodoAST exp = ctx.expresion() != null ? visit(ctx.expresion()) : null;
        return new NodoRetorno(exp, ctx.RETORNAR().getSymbol().getLine(),
                ctx.RETORNAR().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstImpresion(zetarianoParser.InstImpresionContext ctx) {
        return visit(ctx.impresion());
    }

    @Override
    public NodoAST visitImpresion(zetarianoParser.ImpresionContext ctx) {
        List<NodoAST> exprs = new ArrayList<>();
        if (ctx.expresion() != null) {
            exprs.add(visit(ctx.expresion()));
        }
        boolean conSalto = ctx.IMPRIMIR_LINEA() != null;
        int l = conSalto ? ctx.IMPRIMIR_LINEA().getSymbol().getLine() : ctx.IMPRIMIR().getSymbol().getLine();
        int c = conSalto ? ctx.IMPRIMIR_LINEA().getSymbol().getCharPositionInLine()
                : ctx.IMPRIMIR().getSymbol().getCharPositionInLine();
        return new NodoImprimir(exprs, conSalto, l, c);
    }

    @Override
    public NodoAST visitInstLeerLinea(zetarianoParser.InstLeerLineaContext ctx) {
        return new NodoLectura(null, ctx.LEER_LINEA().getSymbol().getLine(),
                ctx.LEER_LINEA().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitInstExpresion(zetarianoParser.InstExpresionContext ctx) {
        return visit(ctx.encadenado());
    }

    @Override
    public NodoAST visitInstVacia(zetarianoParser.InstVaciaContext ctx) {
        return null;
    }

    @Override
    public NodoAST visitExprNuevoObjeto(zetarianoParser.ExprNuevoObjetoContext ctx) {
        String clase = ctx.tipoBase().getText();
        List<NodoAST> args = new ArrayList<>();
        if (ctx.argumentos() != null) {
            for (zetarianoParser.ExpresionContext exp : ctx.argumentos().expresion()) {
                args.add(visit(exp));
            }
        }
        return new NodoInstanciacionObjeto(clase, args, ctx.NUEVO().getSymbol().getLine(),
                ctx.NUEVO().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprNuevoArreglo(zetarianoParser.ExprNuevoArregloContext ctx) {
        Tipo tipoBase = obtenerTipoBase(ctx.tipoBase());
        List<NodoAST> tamanios = new ArrayList<>();
        for (zetarianoParser.ExpresionContext exp : ctx.expresion()) {
            tamanios.add(visit(exp));
        }
        return new NodoInstanciacionArreglo(tipoBase, tamanios, ctx.NUEVO().getSymbol().getLine(),
                ctx.NUEVO().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprTernaria(zetarianoParser.ExprTernariaContext ctx) {
        NodoAST cond = visit(ctx.expresion(0));
        NodoAST vVerdadero = visit(ctx.expresion(1));
        NodoAST vFalso = visit(ctx.expresion(2));
        return new NodoExpresionTernaria(cond, vVerdadero, vFalso, ctx.INTERROGACION().getSymbol().getLine(),
                ctx.INTERROGACION().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprLeerLinea(zetarianoParser.ExprLeerLineaContext ctx) {
        return new NodoExpresionLectura(ctx.LEER_LINEA().getSymbol().getLine(),
                ctx.LEER_LINEA().getSymbol().getCharPositionInLine());
    }

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
        return new NodoOperacionBinaria(izq, "&&", der, ctx.Y_LOGICO().getSymbol().getLine(),
                ctx.Y_LOGICO().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitExprDisyuncion(zetarianoParser.ExprDisyuncionContext ctx) {
        NodoAST izq = visit(ctx.expresion(0));
        NodoAST der = visit(ctx.expresion(1));
        return new NodoOperacionBinaria(izq, "||", der, ctx.O_LOGICO().getSymbol().getLine(),
                ctx.O_LOGICO().getSymbol().getCharPositionInLine());
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
        NodoAST actual = new NodoIdentificador(baseId, ctx.ID().getSymbol().getLine(),
                ctx.ID().getSymbol().getCharPositionInLine());

        for (zetarianoParser.SufijoContext suf : ctx.sufijo()) {
            if (suf instanceof zetarianoParser.SufijoIndiceContext indCtx) {
                NodoAST indice = visit(indCtx.expresion());
                actual = new NodoAccesoArreglo(
                        actual,
                        indice,
                        indCtx.CORCHETE_IZQ().getSymbol().getLine(),
                        indCtx.CORCHETE_IZQ().getSymbol().getCharPositionInLine());
            } else if (suf instanceof zetarianoParser.SufijoMiembroContext mCtx) {
                actual = new NodoAccesoMiembro(
                        actual,
                        mCtx.ID().getText(),
                        mCtx.PUNTO().getSymbol().getLine(),
                        mCtx.PUNTO().getSymbol().getCharPositionInLine());
            } else if (suf instanceof zetarianoParser.SufijoLlamadaContext callCtx) {
                List<NodoAST> args = new ArrayList<>();
                if (callCtx.argumentos() != null) {
                    for (zetarianoParser.ExpresionContext expCtx : callCtx.argumentos().expresion()) {
                        args.add(visit(expCtx));
                    }
                }
                if (actual instanceof NodoAccesoMiembro miembro) {
                    actual = new NodoLlamadaMetodo(
                            miembro.getEstructura(),
                            miembro.getNombreCampo(),
                            args,
                            callCtx.PAR_IZQ().getSymbol().getLine(),
                            callCtx.PAR_IZQ().getSymbol().getCharPositionInLine());
                } else if (actual instanceof NodoIdentificador id) {
                    actual = new NodoLlamadaFuncion(
                            id.getNombre(),
                            args,
                            callCtx.PAR_IZQ().getSymbol().getLine(),
                            callCtx.PAR_IZQ().getSymbol().getCharPositionInLine());
                }
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
        return new NodoLiteral(val, ctx.LIT_ENTERO().getSymbol().getLine(),
                ctx.LIT_ENTERO().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitDecimal(zetarianoParser.LitDecimalContext ctx) {
        double val = Double.parseDouble(ctx.LIT_DECIMAL().getText());
        return new NodoLiteral(val, ctx.LIT_DECIMAL().getSymbol().getLine(),
                ctx.LIT_DECIMAL().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitCadena(zetarianoParser.LitCadenaContext ctx) {
        String raw = ctx.LIT_CADENA().getText();
        String str = raw.substring(1, raw.length() - 1);
        return new NodoLiteral(str, ctx.LIT_CADENA().getSymbol().getLine(),
                ctx.LIT_CADENA().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitCaracter(zetarianoParser.LitCaracterContext ctx) {
        char ch = ctx.LIT_CARACTER().getText().charAt(1);
        return new NodoLiteral(ch, ctx.LIT_CARACTER().getSymbol().getLine(),
                ctx.LIT_CARACTER().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitBooleano(zetarianoParser.LitBooleanoContext ctx) {
        boolean val = Boolean.parseBoolean(ctx.LIT_BOOLEANO().getText());
        return new NodoLiteral(val, ctx.LIT_BOOLEANO().getSymbol().getLine(),
                ctx.LIT_BOOLEANO().getSymbol().getCharPositionInLine());
    }

    @Override
    public NodoAST visitLitNulo(zetarianoParser.LitNuloContext ctx) {
        return new NodoLiteral(null, Tipo.VOID, ctx.NULO().getSymbol().getLine(),
                ctx.NULO().getSymbol().getCharPositionInLine());
    }
}