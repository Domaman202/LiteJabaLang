package ru.DmN.lj.debugger;

import java.io.PrintStream;

public class StdLibrary extends Module {
    public StdLibrary(PrintStream out) {
        super("std");

        this.addMethod("println", "VO", (contexts, context) -> out.println(contexts.peek().stack.pop()));
    }

    protected void addMethod(String name, String desc, Method.Native.NM method) {
        this.methods.add(new Method.Native(name, desc, method));
    }
}
