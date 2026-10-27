package org.compi2.ast.sentencias;

import lombok.Getter;
import lombok.Setter;
import org.compi2.analisis_semantico.ErrorSemantico;
import org.compi2.analisis_semantico.TablaSimbolos;
import org.compi2.ast.NodoAST;
import org.compi2.tipos.Tipo;
import org.compi2.tipos.TipoBase;
import org.compi2.tipos.Visibilidad;

import org.compi2.ast.expresiones.NodoAccesoMiembro;
import org.compi2.ast.expresiones.NodoIdentificador;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
public class NodoClase implements NodoAST {
    private final String nombre;
    private final String clasePadre;
    @Setter
    private Visibilidad visibilidad;
    private final List<NodoDeclaracion> campos;
    private final List<NodoConstructor> constructores;
    private final List<NodoFuncion> metodos;
    private final int linea;
    private final int columna;

    private boolean claseRegistrada = false;
    private Tipo tipoClaseCache;

    public NodoClase(String nombre, List<NodoDeclaracion> campos, List<NodoConstructor> constructores,
                     List<NodoFuncion> metodos, int linea, int columna) {
        this(nombre, null, Visibilidad.PUBLIC, campos, constructores, metodos, linea, columna);
    }

    public NodoClase(String nombre, String clasePadre, Visibilidad visibilidad,
                     List<NodoDeclaracion> campos, List<NodoConstructor> constructores,
                     List<NodoFuncion> metodos, int linea, int columna) {
        this.nombre = nombre;
        this.clasePadre = clasePadre;
        this.visibilidad = visibilidad != null ? visibilidad : Visibilidad.PUBLIC;
        this.campos = campos;
        this.constructores = constructores;
        this.metodos = metodos;
        this.linea = linea;
        this.columna = columna;
    }

    private boolean herenciaEnlazada = false;
    private boolean enlazandoHerencia = false;

    public Tipo registrarEnTablaSimbolos(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        if (claseRegistrada && tipoClaseCache != null && tablaSimbolos.existeTipoCompuesto(nombre)) {
            return tipoClaseCache;
        }

        tipoClaseCache = new Tipo(TipoBase.OBJETO_CLASE, nombre);
        tipoClaseCache.setVisibilidad(visibilidad);
        tipoClaseCache.setClasePadre(clasePadre);

        if (campos != null) {
            for (NodoDeclaracion campo : campos) {
                tipoClaseCache.agregarCampo(campo.getNombre(), campo.getTipoDeclarado(), campo.getVisibilidad(), nombre);
            }
        }

        if (constructores != null && !constructores.isEmpty()) {
            for (NodoConstructor c : constructores) {
                tipoClaseCache.agregarConstructor(obtenerTiposParametros(c.getParametros()), c.getVisibilidad());
            }
        } else if (clasePadre != null || tieneCamposConValorInicial()) {
            tipoClaseCache.agregarConstructor(Collections.emptyList(), Visibilidad.PUBLIC);
        }

        if (metodos != null) {
            for (NodoFuncion m : metodos) {
                tipoClaseCache.agregarMetodo(m.getNombre(), crearTipoMetodo(m, tablaSimbolos));
            }
        }

        boolean ok = tablaSimbolos.registrarTipoCompuesto(tipoClaseCache);
        if (!ok) {
            errores.add(new ErrorSemantico(
                "La clase '" + nombre + "' ya fue definida previamente.",
                "Global", linea, columna
            ));
        } else {
            tablaSimbolos.registrarClase(nombre, tipoClaseCache, linea, columna);

            if (constructores != null && !constructores.isEmpty()) {
                for (NodoConstructor c : constructores) {
                    tablaSimbolos.registrarConstructorClase(nombre, tipoClaseCache, c.getLinea(), c.getColumna());
                }
            } else if (clasePadre != null || tieneCamposConValorInicial()) {
                tablaSimbolos.registrarConstructorClase(nombre, tipoClaseCache, linea, columna);
            }

            if (metodos != null) {
                for (NodoFuncion m : metodos) {
                    tablaSimbolos.registrarMetodoClase(nombre, m.getNombre(), crearTipoMetodo(m, tablaSimbolos), m.getLinea(), m.getColumna());
                }
            }
        }

        claseRegistrada = true;
        return tipoClaseCache;
    }

    public void enlazarHerencia(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        enlazarHerencia(tablaSimbolos, errores, null);
    }

    public void enlazarHerencia(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores, Map<String, NodoClase> mapaClases) {
        if (herenciaEnlazada) return;
        if (enlazandoHerencia) {
            errores.add(new ErrorSemantico(
                String.format("Herencia circular detectada involucrando a la clase '%s'.", nombre),
                "Global", linea, columna
            ));
            return;
        }

        if (clasePadre == null || clasePadre.isBlank()) {
            herenciaEnlazada = true;
            return;
        }

        enlazandoHerencia = true;
        try {
            if (mapaClases != null && mapaClases.containsKey(clasePadre)) {
                mapaClases.get(clasePadre).enlazarHerencia(tablaSimbolos, errores, mapaClases);
            }

            String ambito = "Global";
            Tipo tipoPadre = tablaSimbolos.buscarTipoCompuesto(clasePadre);
            if (tipoPadre == null || tipoPadre.getBase() != TipoBase.OBJETO_CLASE) {
                errores.add(new ErrorSemantico(
                    String.format("La clase padre '%s' de la clase '%s' no está definida ni importada.", clasePadre, nombre),
                    ambito, linea, columna
                ));
                return;
            }

            if (clasePadre.equals(nombre)) {
                errores.add(new ErrorSemantico(
                    String.format("Herencia circular detectada: la clase '%s' no puede extenderse a sí misma.", nombre),
                    ambito, linea, columna
                ));
                return;
            }

            if (tipoPadre.esSubclaseDe(nombre) || tablaSimbolos.tieneCicloHerencia(nombre)) {
                errores.add(new ErrorSemantico(
                    String.format("Herencia circular detectada entre las clases '%s' y '%s'.", nombre, clasePadre),
                    ambito, linea, columna
                ));
                return;
            }

            Tipo tipoClase = (tipoClaseCache != null) ? tipoClaseCache : tablaSimbolos.buscarTipoCompuesto(nombre);
            if (tipoClase != null) {
                tipoClase.setClasePadre(clasePadre);
                tipoClase.enlazarPadre(tipoPadre);
            }
        } finally {
            enlazandoHerencia = false;
            herenciaEnlazada = true;
        }
    }

    private List<Tipo> obtenerTiposParametros(List<Parametro> parametros) {
        List<Tipo> tipos = new ArrayList<>();
        if (parametros != null) {
            for (Parametro p : parametros) {
                tipos.add(p.getTipo());
            }
        }
        return tipos;
    }

    private Tipo crearTipoMetodo(NodoFuncion m) {
        return crearTipoMetodo(m, null);
    }

    private Tipo crearTipoMetodo(NodoFuncion m, TablaSimbolos tablaSimbolos) {
        Tipo retorno = m.getTipoRetorno() != null ? m.getTipoRetorno() : Tipo.VOID;
        if (tablaSimbolos != null) {
            retorno = tablaSimbolos.resolverTipo(retorno);
        }
        List<Tipo> params = new ArrayList<>();
        if (m.getParametros() != null) {
            for (Parametro p : m.getParametros()) {
                Tipo tp = p.getTipo();
                if (tablaSimbolos != null) {
                    tp = tablaSimbolos.resolverTipo(tp);
                }
                params.add(tp);
            }
        }
        Tipo tipoMetodo = new Tipo(m.getNombre(), params, retorno);
        tipoMetodo.setVisibilidad(m.getVisibilidad() != null ? m.getVisibilidad() : Visibilidad.DEFAULT);
        tipoMetodo.setClaseDeclarante(nombre);
        return tipoMetodo;
    }

    public void comprobarCuerpos(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Tipo tipoClase = (tipoClaseCache != null) ? tipoClaseCache : tablaSimbolos.buscarTipoCompuesto(nombre);
        if (tipoClase == null) {
            tipoClase = new Tipo(TipoBase.OBJETO_CLASE, nombre);
        }

        enlazarHerencia(tablaSimbolos, errores);

        tablaSimbolos.entrarClase(nombre);
        tablaSimbolos.abrirAmbito("Clase_" + nombre, true);
        try {
            tablaSimbolos.registrarSimbolo("this", tipoClase, "variable", linea, columna);

            Tipo tipoPadre = (clasePadre != null) ? tablaSimbolos.buscarTipoCompuesto(clasePadre) : null;
            if (tipoPadre != null) {
                tipoPadre.resolverTiposConstructores(tablaSimbolos);
                tipoPadre.resolverTiposMetodos(tablaSimbolos);
                tablaSimbolos.registrarSimbolo("super", tipoPadre, "variable", linea, columna);
            }
            tipoClase.resolverTiposConstructores(tablaSimbolos);
            tipoClase.resolverTiposMetodos(tablaSimbolos);

            // Validar @Override en métodos
            if (metodos != null) {
                for (NodoFuncion m : metodos) {
                    List<Tipo> params = obtenerTiposParametros(m.getParametros()).stream()
                            .map(tablaSimbolos::resolverTipo)
                            .toList();
                    Tipo mPadre = (tipoPadre != null) ? tipoPadre.buscarMetodoEnAncestros(m.getNombre(), params) : null;
                    if (m.isTieneOverride() && mPadre == null) {
                        errores.add(new ErrorSemantico(
                            String.format("El método '%s' en la clase '%s' tiene la anotación @Override pero no sobrescribe ningún método en la superclase.",
                                m.getNombre(), nombre),
                            "Clase_" + nombre, m.getLinea(), m.getColumna()
                        ));
                    } else if (!m.isTieneOverride() && mPadre != null) {
                        errores.add(new ErrorSemantico(
                            String.format("El método '%s' en la clase '%s' sobrescribe un método de la superclase pero no tiene la anotación @Override obligatoria.",
                                m.getNombre(), nombre),
                            "Clase_" + nombre, m.getLinea(), m.getColumna()
                        ));
                    } else if (mPadre != null) {
                        Tipo retM = tablaSimbolos.resolverTipo(m.getTipoRetorno() != null ? m.getTipoRetorno() : Tipo.VOID);
                        Tipo retPadre = tablaSimbolos.resolverTipo(mPadre.getTipoRetorno() != null ? mPadre.getTipoRetorno() : Tipo.VOID);
                        boolean esCovariante = retM.getBase() == TipoBase.OBJETO_CLASE
                                && retPadre.getBase() == TipoBase.OBJETO_CLASE
                                && retM.esSubclaseDe(retPadre.getNombreTipo());
                        boolean compatible = retM.esMismoTipo(retPadre) || esCovariante;
                        if (!compatible) {
                            errores.add(new ErrorSemantico(
                                String.format("El tipo de retorno '%s' del método '%s' no es compatible con el tipo de retorno '%s' del método sobrescrito en la superclase.",
                                    retM, m.getNombre(), retPadre),
                                "Clase_" + nombre, m.getLinea(), m.getColumna()
                            ));
                        }
                        Visibilidad visHijo = m.getVisibilidad() != null ? m.getVisibilidad() : Visibilidad.DEFAULT;
                        Visibilidad visPadre = mPadre.getVisibilidad() != null ? mPadre.getVisibilidad() : Visibilidad.DEFAULT;
                        if (visHijo.reduceVisibilidadRespectoA(visPadre)) {
                            errores.add(new ErrorSemantico(
                                String.format("El método '%s' en la clase '%s' no puede reducir la visibilidad del método sobrescrito en la superclase (de %s a %s).",
                                    m.getNombre(), nombre, visPadre, visHijo),
                                "Clase_" + nombre, m.getLinea(), m.getColumna()
                            ));
                        }
                    }
                }
            }

            if (clasePadre != null && tipoPadre != null) {
                boolean padreTieneCtorVacio = tieneConstructorVacioAccesible(tipoPadre, tablaSimbolos);

                if (constructores == null || constructores.isEmpty()) {
                    if (!padreTieneCtorVacio) {
                        errores.add(new ErrorSemantico(
                            String.format("La clase '%s' debe declarar un constructor e invocar explícitamente a super(...), ya que la superclase '%s' no posee un constructor vacío accesible.",
                                nombre, clasePadre),
                            "Clase_" + nombre, linea, columna
                        ));
                    }
                } else {
                    for (NodoConstructor c : constructores) {
                        validarSuperEnConstructor(c, padreTieneCtorVacio, tablaSimbolos, errores);
                    }
                }
            }

            if (campos != null) {
                for (NodoDeclaracion campo : campos) {
                    campo.comprobar(tablaSimbolos, errores);
                    Tipo tResuelto = tablaSimbolos.resolverTipo(campo.getTipoDeclarado());
                    if (tipoClase.buscarCampo(campo.getNombre()) != null) {
                        tipoClase.actualizarTipoCampo(campo.getNombre(), tResuelto);
                    }
                }
            }

            tipoClase.resolverTiposConstructores(tablaSimbolos);
            tipoClase.resolverTiposMetodos(tablaSimbolos);

            if (constructores != null) {
                for (NodoConstructor c : constructores) {
                    c.comprobar(tablaSimbolos, errores);
                }
            }

            if (metodos != null) {
                for (NodoFuncion m : metodos) {
                    m.comprobar(tablaSimbolos, errores);
                }
            }
        } finally {
            tablaSimbolos.cerrarAmbito();
            tablaSimbolos.salirClase();
        }
    }

    private boolean tieneConstructorVacioAccesible(Tipo tipoPadre, TablaSimbolos tablaSimbolos) {
        if (tipoPadre == null) return false;
        List<Tipo> ctorVacio = tipoPadre.buscarConstructor(Collections.emptyList());
        if (ctorVacio == null) return false;
        Visibilidad vis = tipoPadre.obtenerVisibilidadConstructor(ctorVacio);
        return tablaSimbolos.esAccesible(vis, tipoPadre.getNombreTipo(), nombre);
    }

    private void validarSuperEnConstructor(NodoConstructor ctor, boolean padreTieneCtorVacio,
                                          TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        List<NodoAST> cuerpo = ctor.getCuerpo();
        if (cuerpo == null || cuerpo.isEmpty()) {
            if (!padreTieneCtorVacio) {
                errores.add(new ErrorSemantico(
                    String.format("El constructor de la clase '%s' debe invocar explícitamente a super(...), ya que la superclase '%s' no posee un constructor vacío accesible.",
                        nombre, clasePadre),
                    "Clase_" + nombre, ctor.getLinea(), ctor.getColumna()
                ));
            }
            return;
        }

        int countSuper = 0;
        int primerSuperIdx = -1;
        for (int i = 0; i < cuerpo.size(); i++) {
            if (cuerpo.get(i) instanceof NodoLlamadaSuper) {
                countSuper++;
                if (primerSuperIdx == -1) primerSuperIdx = i;
            }
        }

        if (countSuper > 0) {
            if (primerSuperIdx != 0) {
                if (cuerpo.get(primerSuperIdx) instanceof NodoLlamadaSuper superNode) {
                    errores.add(new ErrorSemantico(
                        "La llamada a super(...) debe ser la primera instrucción del constructor.",
                        "Clase_" + nombre, superNode.getLinea(), superNode.getColumna()
                    ));
                }
            }
            if (countSuper > 1) {
                for (int i = primerSuperIdx + 1; i < cuerpo.size(); i++) {
                    if (cuerpo.get(i) instanceof NodoLlamadaSuper sDup) {
                        errores.add(new ErrorSemantico(
                            "Solo se permite una llamada a super(...) en el constructor.",
                            "Clase_" + nombre, sDup.getLinea(), sDup.getColumna()
                        ));
                    }
                }
            }
        } else {
            if (!padreTieneCtorVacio) {
                errores.add(new ErrorSemantico(
                    String.format("El constructor de la clase '%s' debe invocar explícitamente a super(...), ya que la superclase '%s' no posee un constructor vacío accesible.",
                        nombre, clasePadre),
                    "Clase_" + nombre, ctor.getLinea(), ctor.getColumna()
                ));
            }
        }
    }

    public boolean tieneCamposConValorInicial() {
        if (campos == null) return false;
        for (NodoDeclaracion campo : campos) {
            if (campo.getValorInicial() != null) return true;
        }
        return false;
    }

    public List<NodoFuncion> aFunciones() {
        List<NodoFuncion> funciones = new ArrayList<>();
        if (metodos != null) {
            for (NodoFuncion metodo : metodos) {
                metodo.setEsMetodo(true);
                metodo.setClaseContenedora(nombre);
                funciones.add(metodo);
            }
        }

        List<NodoConstructor> listaConstructores = constructores;
        boolean sintetizarCtor = (listaConstructores == null || listaConstructores.isEmpty())
                && (clasePadre != null || tieneCamposConValorInicial());

        if (sintetizarCtor) {
            listaConstructores = List.of(new NodoConstructor(nombre, Collections.emptyList(), Collections.emptyList(), linea, columna));
        }

        if (listaConstructores != null) {
            for (NodoConstructor constructor : listaConstructores) {
                List<NodoAST> nuevoCuerpo = new ArrayList<>();
                List<NodoAST> cuerpoOriginal = constructor.getCuerpo();
                boolean tieneSuperExplicito = false;

                if (cuerpoOriginal != null && !cuerpoOriginal.isEmpty() && cuerpoOriginal.get(0) instanceof NodoLlamadaSuper superLlamada) {
                    tieneSuperExplicito = true;
                    nuevoCuerpo.add(superLlamada);
                } else if (clasePadre != null && !clasePadre.isBlank()) {
                    nuevoCuerpo.add(new NodoLlamadaSuper(Collections.emptyList(), constructor.getLinea(), constructor.getColumna()));
                }

                if (campos != null) {
                    for (NodoDeclaracion campo : campos) {
                        if (campo.getValorInicial() != null) {
                            NodoAST accesoThis = new NodoAccesoMiembro(
                                new NodoIdentificador("this", campo.getLinea(), campo.getColumna()),
                                campo.getNombre(),
                                campo.getLinea(),
                                campo.getColumna()
                            );
                            nuevoCuerpo.add(new NodoAsignacion(accesoThis, campo.getValorInicial(), campo.getLinea(), campo.getColumna()));
                        }
                    }
                }

                if (cuerpoOriginal != null) {
                    int inicio = tieneSuperExplicito ? 1 : 0;
                    for (int i = inicio; i < cuerpoOriginal.size(); i++) {
                        nuevoCuerpo.add(cuerpoOriginal.get(i));
                    }
                }

                NodoFuncion fn = new NodoFuncion(
                    nombre + "_constructor",
                    constructor.getParametros(),
                    Tipo.VOID,
                    nuevoCuerpo,
                    constructor.getLinea(),
                    constructor.getColumna());
                fn.setEsMetodo(true);
                fn.setClaseContenedora(nombre);
                funciones.add(fn);
            }
        }
        return funciones;
    }

    @Override
    public Tipo comprobar(TablaSimbolos tablaSimbolos, List<ErrorSemantico> errores) {
        Tipo tipo = registrarEnTablaSimbolos(tablaSimbolos, errores);
        comprobarCuerpos(tablaSimbolos, errores);
        return tipo;
    }
}
