package ru.DmN.lj.compiler;

public enum Opcode {
    PUSH,
    POP,
    SWAP,
    DUP,

    ADD,
    MUL,
    DIV,
    NEG,

    GREAT,
    EQUALS,

    AND,
    OR,
    NOT,

    CALL,
    RETURN,

    JMP,
    CJMP,

    NATIVE,

    BREAKPOINT
}
