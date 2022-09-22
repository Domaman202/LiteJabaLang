package ru.DmN.lj.compiler;

public enum Opcode {
    PUSH,
    POP,
    SWAP,
    DUP,

    ADD,
    MUL,
    DIV,
    REM,
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

    TRY,
    TRY_END,
    THROW,

    NATIVE,

    BREAKPOINT
}
