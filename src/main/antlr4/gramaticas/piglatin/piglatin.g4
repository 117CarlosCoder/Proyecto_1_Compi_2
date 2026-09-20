grammar piglatin;

programa
    :importacion* variables? principal EOF
    ;

importacion
    : IMPORT ruta PUNTO_Y_COMA?
    ;

ruta
    :ID (PUNTO ID)*
    ;

variables
    :VARIABLES (declaracionVariable | declaracionArreglo)*
    ;

principal
    : MAIOR instruccion* FIN_PROGRAMA PUNTO_Y_COMA
    ;

instruccion
    : declaracionVariable                     # InstDeclaracionVariable
    | declaracionArreglo                      # InstDeclaracionArreglo
    | asignacion PUNTO_Y_COMA                 # InstAsignacion
    | impresion                               # InstImpresion
    | lectura                                 # InstLectura
    | condicionalSi                           # InstCondicionalSi
    | cicloDum                                # InstCicloDum
    | cicloFacere                             # InstCicloFacere
    | cicloPer                                # InstCicloPer
    | controlCiclo PUNTO_Y_COMA               # InstControlCiclo
    | encadenado PUNTO_Y_COMA                 # InstExpresion
    | incrementoDecremento PUNTO_Y_COMA       # InstIncrementoDecremento
    ;

declaracionVariable
    : ESTO ID DOS_PUNTOS tipoDeclarable valorInicial? PUNTO_Y_COMA   # DeclaracionTipada
    | ESTO ID DOS_PUNTOS NOVUS ID PAR_IZQ argumentos? PAR_DER PUNTO_Y_COMA  # DeclaracionObjeto
    | ESTO ID DOS_PUNTOS literalBooleano PUNTO_Y_COMA                # DeclaracionBooleana
    ;

declaracionArreglo
    : SERIES ID (CORCHETE_IZQ expresion CORCHETE_DER)+ DOS_PUNTOS tipoDeclarable listaValores? PUNTO_Y_COMA
    ;

tipoDeclarable
    : tipoPrimitivo                # TipoDeclPrimitivo
    | ID                           # TipoDeclNombrado
    ;

tipoPrimitivo
    : TIPO_NUMERUS          # PrimNumerus
    | TIPO_DECIMALIS        # PrimDecimalis
    | TIPO_TEXTUM           # PrimTextum
    | TIPO_LITTERA          # PrimLittera
    | TIPO_BOOLEANUS        # PrimBooleanus
    ;

valorInicial
    : expresion            # ValorExpresion
    | listaValores         # ValorLista
    ;

listaValores
    : LLAVE_IZQ elementoLista (COMA elementoLista)* LLAVE_DER
    ;

elementoLista
    : expresion            # ElementoExpresion
    | listaValores         # ElementoAnidado
    ;

asignacion
    : encadenado ASIGNACION valorInicial
    ;

impresion
    : IMPRIME expresion (IMPRIME expresion)* PUNTO_Y_COMA
    ;

lectura
    : ID? LEE PUNTO_Y_COMA?
    ;

condicionalSi
    : SI PAR_IZQ expresion PAR_DER bloque
      (ALITER (PAR_IZQ expresion PAR_DER)? bloque)*
      FIN_BLOQUE PUNTO_Y_COMA
    ;

bloque
    : LLAVE_IZQ instruccion* LLAVE_DER
    ;

cicloDum
    : DUM PAR_IZQ expresion PAR_DER bloque FIN_BLOQUE PUNTO_Y_COMA
    ;

cicloFacere
    : FACERE bloque DUM PAR_IZQ expresion PAR_DER PUNTO_Y_COMA
    ;

cicloPer
    : PER PAR_IZQ perInicio PUNTO_Y_COMA expresion PUNTO_Y_COMA perPaso PAR_DER
      bloque (FIN_BLOQUE PUNTO_Y_COMA)?
    ;

perInicio
    : ESTO ID DOS_PUNTOS tipoPrimitivo expresion   # PerInicioDeclaracion
    | asignacion                                    # PerInicioAsignacion
    ;

perPaso
    : asignacion
    | incrementoDecremento
    ;

incrementoDecremento
    : encadenado (INCREMENTO | DECREMENTO)
    ;

controlCiclo
    : INTERRUMPIR
    | PERGE
    ;

encadenado
    : ID sufijo*
    ;

sufijo
    : PUNTO ID                                          # SufijoMiembro
    | CORCHETE_IZQ expresion CORCHETE_DER               # SufijoIndice
    | PAR_IZQ argumentos? PAR_DER                       # SufijoLlamada
    ;

argumentos
    : expresion (COMA expresion)*
    ;

expresion
    : PAR_IZQ expresion PAR_DER                                          # ExprParentesis
    | NOVUS ID PAR_IZQ argumentos? PAR_DER                               # ExprNovus
    | op=(NEGACION | MENOS) expresion                                    # ExprUnaria
    | expresion op=(POR | ENTRE) expresion                               # ExprMultiplicativa
    | expresion op=(MAS | MENOS) expresion                               # ExprAditiva
    | expresion op=(MENOR_QUE | MAYOR_QUE | MENOR_IGUAL | MAYOR_IGUAL) expresion  # ExprRelacional
    | expresion op=(IGUAL_IGUAL | DIFERENTE) expresion                   # ExprIgualdad
    | expresion Y_LOGICO expresion                                       # ExprConjuncion
    | expresion O_LOGICO expresion                                       # ExprDisyuncion
    | encadenado                                                         # ExprEncadenado
    | literal                                                            # ExprLiteral
    ;

literal
    : LIT_ENTERO         # LitEntero
    | LIT_DECIMAL        # LitDecimal
    | LIT_CADENA         # LitCadena
    | LIT_CARACTER       # LitCaracter
    | literalBooleano    # LitBooleano
    ;

literalBooleano
    : LIT_VERUM
    | LIT_FALSUS
    ;

VARIABLES : ('VARIABILES' | 'VARIABLES') [ \t]* '>' ;

MAIOR     : 'MAIOR' [ \t]* '>' ;

IMPORT        : 'import';
ESTO          : 'esto';
SERIES        : 'series';
NOVUS         : 'novus';
SI            : 'si';
ALITER        : 'aliter';
DUM           : 'dum';
FACERE        : 'facere';
PER           : 'per';
FIN_BLOQUE    : 'finis';
FIN_PROGRAMA  : 'FINIS';
INTERRUMPIR : 'interrumpe' | 'interrumpir';
PERGE         : 'perge';


TIPO_NUMERUS    : 'numerus';
TIPO_DECIMALIS  : 'decimalis';
TIPO_TEXTUM     : 'textum';
TIPO_LITTERA    : 'littera';
TIPO_BOOLEANUS  : 'booleanus';

LIT_VERUM   : 'verum';
LIT_FALSUS  : 'falsus';


IMPRIME         : '>>';
LEE             : '<<';
INCREMENTO      : '++';
DECREMENTO      : '--';
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

COMENTARIO_BLOQUE : '##' .*? '##' -> skip ;

ESPACIOS
    : [ \t\r\n]+ -> skip
    ;
