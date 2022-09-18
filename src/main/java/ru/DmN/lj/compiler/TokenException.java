package ru.DmN.lj.compiler;

import org.antlr.v4.runtime.tree.TerminalNode;

public class TokenException extends RuntimeException {
    public final TerminalNode node;

    public TokenException(TerminalNode node) {
        this.node = node;
    }
}
