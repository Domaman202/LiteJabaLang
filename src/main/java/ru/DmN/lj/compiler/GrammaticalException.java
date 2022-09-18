package ru.DmN.lj.compiler;

import org.antlr.v4.runtime.ParserRuleContext;

public class GrammaticalException extends RuntimeException {
    public final ParserRuleContext ctx;

    public GrammaticalException(ParserRuleContext ctx) {
        this.ctx = ctx;
    }

    public GrammaticalException(ParserRuleContext ctx, String msg) {
        super(msg);
        this.ctx = ctx;
    }
}
