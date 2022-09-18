package ru.DmN.lj.compiler;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

public record ExprPosition(int line, int offset, int end) {
    public static final ExprPosition NON = new ExprPosition(-1, -1, -1);

    public static ExprPosition of(TerminalNode node) {
        if (node == null)
            return NON;
        return of(node.getSymbol());
    }

    public static ExprPosition of(ParserRuleContext ctx) {
        if (ctx == null)
            return NON;
        var i = ctx.start.getStartIndex();
        return new ExprPosition(ctx.start.getLine(), i, ctx.getChild(0).getText().length() + i);
    }

    public static ExprPosition of(Token token) {
        if (token == null)
            return NON;
        return new ExprPosition(token.getLine(), token.getStartIndex(), token.getStopIndex());
    }
}
