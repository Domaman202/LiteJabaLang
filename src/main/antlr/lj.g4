grammar lj;

@header {
package ru.DmN.lj.compiler;
}

// ПАРСЕР

file
    : (module newline)* EOF
    ;

//

module
    : 'module' LITERAL newline ((variable|method) newline)* 'end'
    ;

method
    : 'fun' name=LITERAL '|' desc=LITERAL newline body
    ;

variable
    : 'var' name=LITERAL ('=' val=value)?
    ;

//

body
    : '|>' newline (any_expr newline)* '<|'
    ;

any_expr
    : variable
    | push
    | call
    | return
    | opcode
    | label
    | jmp
    | assign
    ;

//

assign
    : var_ref '=' value
    ;

//

jmp
    : type=('jmp'|'cjmp') LITERAL
    ;

label
    : 'label' LITERAL
    ;

opcode
    : 'opcode' LITERAL
    ;

return
    : 'return' value?
    ;

call
    : 'call' method_ref
    ;

push
    : 'push' value
    ;

//

value
    : NULL
    | NUM
    | STRING
    | var_ref
    | method_ref
    | pop
    ;

pop
    : 'pop'
    ;

method_ref
    : module_=LITERAL '$' name=LITERAL '|' desc=LITERAL
    ;

var_ref
    : module_=('.'|LITERAL) '$' name=LITERAL
    ;

//

newline : NEWLINE* ;

// ЛЕКСЕР

NULL
    : 'null'
    ;

STRING
    : '"' .*? '"'
    ;

NUM
    : '-'? ('0'..'9')+
    ;

LITERAL
    : ('a'..'z'|'A'..'Z') ('a'..'z'|'A'..'Z'|'0'..'9')*
    ;

NEWLINE
    : ('\r'? '\n'|'\r'|';')+
    ;

WS
    : [ \t] + -> skip
    ;

COMMENT
    :  '//' (~'\n')* NEWLINE -> skip
    ;