grammar y;

tokens {
    SANGRIA,
    FIN_SANGRIA
}

programa
    : seccionEstructuras? seccionFunciones? EOF
    ;

seccionEstructuras
    : SEC_ESTRUCTURAS NUEVA_LINEA definicionEstructura*
    ;

definicionEstructura
    : ESTRUCTURA ID DOS_PUNTOS bloqueCampos
    ;

bloqueCampos
    : NUEVA_LINEA SANGRIA campoEstructura+ FIN_SANGRIA
    ;

campoEstructura
    : tipo ID (CORCHETE_IZQ expresion CORCHETE_DER)* finLinea
    ;

seccionFunciones
    : SEC_FUNCIONES NUEVA_LINEA definicionFuncion*
    ;

definicionFuncion
    : ID PAR_IZQ parametros? PAR_DER (FLECHA tipo)? DOS_PUNTOS bloque
    ;

parametros
    : parametro (COMA parametro)*
    ;

parametro
    : tipo (CORCHETE_IZQ CORCHETE_DER)? ID
    ;

tipo
    : TIPO_ENTERO          # TipoEntero
    | TIPO_FLOTANTE        # TipoFlotante
    | TIPO_CARACTER        # TipoCaracter
    | TIPO_BOOL            # TipoBool
    | TIPO_CADENA          # TipoCadena
    | ID                   # TipoEstructura
    ;

bloque
    : NUEVA_LINEA SANGRIA instruccion+ FIN_SANGRIA
    ;

finLinea
    : PUNTO_Y_COMA? NUEVA_LINEA
    ;

instruccion
    : declaracionVariable finLinea                                      # InstDeclaracion
    | asignacion finLinea                                               # InstAsignacion
    | incrementoDecremento finLinea                                     # InstIncDec
    | llamadaFuncion finLinea                                           # InstLlamada
    | IMPRIMIR PAR_IZQ (expresion (COMA expresion)*)? PAR_DER finLinea  # InstImprimir
    | RETORNAR expresion? finLinea                                      # InstRetornar
    | ROMPER finLinea                                                   # InstRomper
    | CONTINUAR finLinea                                                # InstContinuar
    | condicionalSi                                                     # InstSi
    | seleccionElegir                                                   # InstElegir
    | cicloPara                                                         # InstPara
    | cicloMientras                                                     # InstMientras
    | cicloHacer                                                        # InstHacer
    ;

declaracionVariable
    : tipo ID (CORCHETE_IZQ expresion CORCHETE_DER)* (ASIGNACION valorInicial)?
    ;

asignacion
    : acceso ASIGNACION valorInicial
    ;

valorInicial
    : expresion            # ValorExpresion
    | listaLiteral         # ValorLista
    ;

listaLiteral
    : LLAVE_IZQ elementoLista (COMA elementoLista)* LLAVE_DER
    ;

elementoLista
    : expresion            # ElementoExpresion
    | listaLiteral         # ElementoAnidado
    ;

acceso
    : ID (PUNTO ID | CORCHETE_IZQ expresion CORCHETE_DER | PAR_IZQ argumentos? PAR_DER)*
    ;

llamadaFuncion
    : ID PAR_IZQ argumentos? PAR_DER
    ;

argumentos
    : expresion (COMA expresion)*
    ;

incrementoDecremento
    : acceso (INCREMENTO | DECREMENTO)
    ;

condicionalSi
    : SI PAR_IZQ expresion PAR_DER ENTONCES bloque
      (SINO PAR_IZQ expresion PAR_DER ENTONCES bloque)*
      (CONTRARIO bloque)?
    ;

seleccionElegir
    : ELEGIR PAR_IZQ expresion PAR_DER LLAVE_IZQ
      seccionCaso* seccionSiempre?
      LLAVE_DER finLinea?
    ;

seccionCaso
    : CASO literal DOS_PUNTOS instruccionSuelta*
    ;

seccionSiempre
    : SIEMPRE DOS_PUNTOS instruccionSuelta*
    ;

instruccionSuelta
    : ( declaracionVariable
      | asignacion
      | incrementoDecremento
      | llamadaFuncion
      | IMPRIMIR PAR_IZQ (expresion (COMA expresion)*)? PAR_DER
      | RETORNAR expresion?
      | ROMPER
      | CONTINUAR
      ) PUNTO_Y_COMA?
    ;

cicloPara
    : PARA PAR_IZQ paraInicio? PUNTO_Y_COMA expresion? PUNTO_Y_COMA paraPaso? PAR_DER DOS_PUNTOS bloque
    ;

paraInicio
    : declaracionVariable  # ParaInicioDeclaracion
    | asignacion           # ParaInicioAsignacion
    ;

paraPaso
    : asignacion
    | incrementoDecremento
    ;

cicloMientras
    : MIENTRAS PAR_IZQ expresion PAR_DER HACER bloque
    ;

cicloHacer
    : HACER DOS_PUNTOS bloque MIENTRAS PAR_IZQ expresion PAR_DER finLinea
    ;

expresion
    : PAR_IZQ expresion PAR_DER                                                    # ExprParentesis
    | op=(NEGACION | MENOS) expresion                                              # ExprUnaria
    | expresion op=(POR | ENTRE) expresion                                         # ExprMultiplicativa
    | expresion op=(MAS | MENOS) expresion                                         # ExprAditiva
    | expresion op=(MENOR_QUE | MAYOR_QUE | MENOR_IGUAL | MAYOR_IGUAL) expresion  # ExprRelacional
    | expresion op=(IGUAL_IGUAL | DIFERENTE) expresion                             # ExprIgualdad
    | expresion Y_LOGICO expresion                                                 # ExprConjuncion
    | expresion O_LOGICO expresion                                                 # ExprDisyuncion
    | LEER PAR_IZQ PAR_DER                                                         # ExprLeer
    | acceso                                                                       # ExprAcceso
    | literal                                                                      # ExprLiteral
    ;

literal
    : LIT_ENTERO         # LitEntero
    | LIT_DECIMAL        # LitDecimal
    | LIT_CADENA         # LitCadena
    | LIT_CARACTER       # LitCaracter
    | LIT_VERDADERO      # LitVerdadero
    | LIT_FALSO          # LitFalso
    ;

SEC_ESTRUCTURAS : '%' [ \t]* 'estructuras';
SEC_FUNCIONES   : '%' [ \t]* 'funciones';

ESTRUCTURA  : 'estructura';
SI          : 'si';
ENTONCES    : 'entonces';
SINO        : 'sino';
CONTRARIO   : 'contrario';
ELEGIR      : 'elegir';
CASO        : 'caso';
SIEMPRE     : 'siempre';
MIENTRAS    : 'mientras';
HACER       : 'hacer';
PARA        : 'para';
ROMPER      : 'romper';
CONTINUAR   : 'continuar';
RETORNAR    : 'retornar';
IMPRIMIR    : 'imprimir';
LEER        : 'leer';

TIPO_ENTERO     : 'entero';
TIPO_FLOTANTE   : 'flotante';
TIPO_CARACTER   : 'caracter';
TIPO_BOOL       : 'bool';
TIPO_CADENA     : 'cadena';

LIT_VERDADERO   : 'verdadero';
LIT_FALSO       : 'falso';

INCREMENTO      : '++';
DECREMENTO      : '--';
FLECHA          : '->';
IGUAL_IGUAL     : '==';
DIFERENTE       : '!=';
MENOR_IGUAL     : '<=';
MAYOR_IGUAL     : '>=';
Y_LOGICO        : '&&';
O_LOGICO        : '||';
NEGACION        : '!';
ASIGNACION      : '=';
MAS             : '+';
MENOS           : '-';
POR             : '*';
ENTRE           : '/';
MENOR_QUE       : '<';
MAYOR_QUE       : '>';

PAR_IZQ         : '(';
PAR_DER         : ')';
LLAVE_IZQ       : '{';
LLAVE_DER       : '}';
CORCHETE_IZQ    : '[';
CORCHETE_DER    : ']';
PUNTO_Y_COMA    : ';';
COMA            : ',';
PUNTO           : '.';
DOS_PUNTOS      : ':';

LIT_DECIMAL
    : [0-9]+ '.' [0-9]+
    ;

LIT_ENTERO
    : [0-9]+
    ;

LIT_CADENA
    : ('"' | '“' | '”') (~["“”\r\n\\] | '\\' .)* ('"' | '“' | '”')
    ;

LIT_CARACTER
    : '\'' (~['\r\n\\] | '\\' .) '\''
    ;

ID
    : [a-zA-Z_][a-zA-Z0-9_]*
    ;

COMENTARIO_LINEA
    : '//' ~[\r\n]* -> skip
    ;

COMENTARIO_BLOQUE
    : '/*' .*? '*/' -> skip
    ;

NUEVA_LINEA
    : ('\r'? '\n' [ \t]*)+
    ;

ESPACIOS
    : [ \t]+ -> skip
    ;