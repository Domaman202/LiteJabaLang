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
    : 'module' module_ref newline ((alias|variable|method) newline)* 'end'
    ;

method
    : 'fun' name=LITERAL '|' desc=LITERAL newline body
    ;

variable
    : 'var' LITERAL ('=' val=value)?
    ;

//

alias
    : 'alias' new=module_ref '=' old=module_ref
    ;

//

try
    : 'try' LITERAL newline? t=body
    ;

body
    : '|>' newline (any_expr newline)* '<|'
    ;

any_expr
    : '(' any_expr ')'
    | variable
    | push
    | call
    | return
    | opcode
    | label
    | jmp
    | assign
    | try
    ;

// array

array
    : '[' value? (',' value)* ']'
    ;
named_array
    : '[' value ':' value (',' value ':' value)* ']'
    ;

array_access
    : (var_ref|array|named_array) '[' value ']'
    | array_access '[' value ']'
    ;

//

assign
    : (var_ref|array_access) '=' value
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
    : 'push' value (',' value)*
    ;

//

math_expr
    : num_value
    | '(' math_expr ')'
    | left=math_expr oper=('*'|'/'|'%') math_expr
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
    | array_access
    | array
    | named_array
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
    : module_ref '$' name=LITERAL '|' desc=LITERAL
    ;

var_ref
    : (module_ref '$')? LITERAL
    ;

module_ref
    : LITERAL ('.' LITERAL)*
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
    : '-'? ('0'..'9')+ ('.' ('0'..'9')+)?
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