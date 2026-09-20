grammar zetariano;

unidadCompilacion
    : definicionClase+ EOF
    ;

definicionClase
    : modificador? CLASE ID LLAVE_IZQ miembro* LLAVE_DER
    ;

modificador
    : PUBLICO
    ;

miembro
    : declaracionCampo     # MiembroCampo
    | constructor          # MiembroConstructor
    | metodo               # MiembroMetodo
    ;

declaracionCampo
    : modificador? tipo ID (ASIGNACION inicializador)? PUNTO_Y_COMA
    ;


constructor
    : modificador? ID PAR_IZQ parametrosFormales? PAR_DER bloque
    ;

metodo
    : modificador? tipoRetorno ID PAR_IZQ parametrosFormales? PAR_DER bloque
    ;

tipoRetorno
    : tipo                 # RetornoTipo
    | VACIO                # RetornoVacio
    ;

parametrosFormales
    : parametroFormal (COMA parametroFormal)*
    ;

parametroFormal
    : tipo ID
    ;

tipo
    : tipoBase (CORCHETE_IZQ CORCHETE_DER)*
    ;

tipoBase
    : TIPO_ENTERO          # BaseEntero
    | TIPO_DECIMAL         # BaseDecimal
    | TIPO_CARACTER        # BaseCaracter
    | TIPO_BOOLEANO        # BaseBooleano
    | TIPO_CADENA          # BaseCadena
    | ID                   # BaseClase
    ;

bloque
    : LLAVE_IZQ instruccion* LLAVE_DER
    ;

instruccion
    : bloque                                                   # InstBloque
    | declaracionLocal                                         # InstDeclaracionLocal
    | asignacion PUNTO_Y_COMA                                  # InstAsignacion
    | incrementoDecremento PUNTO_Y_COMA                        # InstIncDec
    | condicionalSi                                            # InstSi
    | seleccionSwitch                                          # InstSwitch
    | cicloFor                                                 # InstFor
    | cicloWhile                                               # InstWhile
    | cicloDoWhile                                             # InstDoWhile
    | ROMPER PUNTO_Y_COMA                                      # InstRomper
    | CONTINUAR PUNTO_Y_COMA                                   # InstContinuar
    | RETORNAR expresion? PUNTO_Y_COMA                         # InstRetornar
    | impresion PUNTO_Y_COMA                                       # InstImpresion
    | LEER_LINEA PAR_IZQ PAR_DER PUNTO_Y_COMA                      # InstLeerLinea
    | encadenado PUNTO_Y_COMA                                      # InstExpresion
    | PUNTO_Y_COMA                                             # InstVacia
    ;

declaracionLocal
    : declaracionLocalBase PUNTO_Y_COMA
    ;

declaracionLocalBase
    : tipo ID (ASIGNACION inicializador)?
    ;

inicializador
    : expresion            # InicExpresion
    | listaInicializador   # InicLista
    ;

listaInicializador
    : LLAVE_IZQ inicializador (COMA inicializador)* LLAVE_DER
    ;

asignacion
    : encadenado operadorAsignacion expresion
    ;

operadorAsignacion
    : ASIGNACION           # OpAsigna
    | MAS_IGUAL            # OpAsignaSuma
    | MENOS_IGUAL          # OpAsignaResta
    | POR_IGUAL            # OpAsignaProducto
    | ENTRE_IGUAL          # OpAsignaDivision
    | MOD_IGUAL            # OpAsignaModulo
    ;

incrementoDecremento
    : encadenado (INCREMENTO | DECREMENTO)   # IncDecPostfijo
    | (INCREMENTO | DECREMENTO) encadenado   # IncDecPrefijo
    ;

condicionalSi
    : SI PAR_IZQ expresion PAR_DER instruccion (SINO instruccion)?
    ;

seleccionSwitch
    : SWITCH PAR_IZQ expresion PAR_DER LLAVE_IZQ
      seccionCaso* seccionDefecto?
      LLAVE_DER
    ;

seccionCaso
    : CASO expresion DOS_PUNTOS instruccion*
    ;

seccionDefecto
    : DEFECTO DOS_PUNTOS instruccion*
    ;

cicloFor
    : PARA PAR_IZQ inicioFor? PUNTO_Y_COMA expresion? PUNTO_Y_COMA pasoFor? PAR_DER instruccion
    ;

inicioFor
    : declaracionLocalBase   # ForInicioDeclaracion
    | asignacion             # ForInicioAsignacion
    ;

pasoFor
    : asignacion
    | incrementoDecremento
    ;

cicloWhile
    : MIENTRAS PAR_IZQ expresion PAR_DER instruccion
    ;

cicloDoWhile
    : HACER instruccion MIENTRAS PAR_IZQ expresion PAR_DER PUNTO_Y_COMA
    ;

impresion
    : (IMPRIMIR | IMPRIMIR_LINEA) PAR_IZQ expresion? PAR_DER
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
    : PAR_IZQ expresion PAR_DER                                            # ExprParentesis
    | NUEVO tipoBase PAR_IZQ argumentos? PAR_DER                           # ExprNuevoObjeto
    | NUEVO tipoBase (CORCHETE_IZQ expresion CORCHETE_DER)+                # ExprNuevoArreglo
    | op=(NEGACION | MAS | MENOS) expresion                                # ExprUnaria
    | op=(INCREMENTO | DECREMENTO) expresion                               # ExprPrefijo
    | expresion op=(INCREMENTO | DECREMENTO)                               # ExprPostfijo
    | expresion op=(POR | ENTRE | MODULO) expresion                        # ExprMultiplicativa
    | expresion op=(MAS | MENOS) expresion                                 # ExprAditiva
    | expresion op=(MENOR_QUE | MAYOR_QUE | MENOR_IGUAL | MAYOR_IGUAL) expresion   # ExprRelacional
    | expresion op=(IGUAL_IGUAL | DIFERENTE) expresion                     # ExprIgualdad
    | expresion Y_LOGICO expresion                                         # ExprConjuncion
    | expresion O_LOGICO expresion                                         # ExprDisyuncion
    | <assoc=right> expresion INTERROGACION expresion DOS_PUNTOS expresion # ExprTernaria
    | LEER_LINEA PAR_IZQ PAR_DER                                           # ExprLeerLinea
    | encadenado                                                           # ExprEncadenado
    | literal                                                              # ExprLiteral
    ;

literal
    : LIT_ENTERO          # LitEntero
    | LIT_DECIMAL         # LitDecimal
    | LIT_CADENA          # LitCadena
    | LIT_CARACTER        # LitCaracter
    | LIT_BOOLEANO        # LitBooleano
    | NULO                # LitNulo
    ;

PUBLICO     : 'public';
CLASE       : 'class';
VACIO       : 'void';
NUEVO       : 'new';
NULO        : 'null';
SI          : 'if';
SINO        : 'else';
SWITCH      : 'switch';
CASO        : 'case';
DEFECTO     : 'default';
PARA        : 'for';
MIENTRAS    : 'while';
HACER       : 'do';
ROMPER      : 'break';
CONTINUAR   : 'continue';
RETORNAR    : 'return';

TIPO_ENTERO     : 'int';
TIPO_DECIMAL    : 'double';
TIPO_CARACTER   : 'char';
TIPO_BOOLEANO   : 'boolean';
TIPO_CADENA     : 'String';

IMPRIMIR        : 'print';
IMPRIMIR_LINEA  : 'println';
LEER_LINEA      : 'readln';

LIT_BOOLEANO    : 'true' | 'false';

INCREMENTO      : '++';
DECREMENTO      : '--';
MAS_IGUAL       : '+=';
MENOS_IGUAL     : '-=';
POR_IGUAL       : '*=';
ENTRE_IGUAL     : '/=';
MOD_IGUAL       : '%=';
IGUAL_IGUAL     : '==';
DIFERENTE       : '!=';
MENOR_IGUAL     : '<=';
MAYOR_IGUAL     : '>=';
Y_LOGICO        : '&&';
O_LOGICO        : '||';
NEGACION        : '!';
INTERROGACION   : '?';
ASIGNACION      : '=';
MAS             : '+';
MENOS           : '-';
POR             : '*';
ENTRE           : '/';
MODULO          : '%';
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

ESPACIOS
    : [ \t\r\n]+ -> skip
    ;
