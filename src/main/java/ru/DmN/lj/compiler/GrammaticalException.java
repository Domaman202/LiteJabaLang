package ru.DmN.lj.compiler;

public class GrammaticalException extends Exception {
    public final ExprPosition pos;

    public GrammaticalException(ExprPosition pos, String msg) {
        super(msg);
        this.pos = pos;
    }
}
