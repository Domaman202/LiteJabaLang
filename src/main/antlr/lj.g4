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

try
    : 'try' LITERAL newline? t=body
    ;

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
    | try
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

math_expr
    : num_value
    | '(' math_expr ')'
    | left=math_expr oper=('*'|'/') math_expr
    | right=math_expr oper=('+'|'-') math_expr
    ;

logic_expr
    : BOOL
    | '(' logic_expr ')'
    | math_expr oper=('<='|'<'|'>'|'>=') math_expr
    | logic_expr oper=('=='|'!=') logic_expr
    ;

//

value
    : NULL
    | NUM
    | STRING
    | var_ref
    | method_ref
    | pop
    | math_expr
    | logic_expr
    ;

num_value
    : NUM
    | var_ref
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

newline
    : NEWLINE*
    ;

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

BOOL
    : 'true'
    | 'false'
    ;

LITERAL
    : ('_'|'a'..'z'|'A'..'Z') ('_'|'a'..'z'|'A'..'Z'|'0'..'9')*
    ;

NEWLINE
    : ('\r'? '\n'|'\r'|';')+
    ;

WS
    : [ \t] + -> skip
    ;

COMMENT
    :  '#' (~'\n')* (NEWLINE|EOF) -> skip
    ;