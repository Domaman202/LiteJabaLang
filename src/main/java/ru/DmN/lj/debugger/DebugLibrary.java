package ru.DmN.lj.debugger;

public class DebugLibrary extends Module {
    public DebugLibrary() {
        super("debug");

        this.addMethod("get_context", "OI", ((contexts, context) -> {
            var stack = contexts.peek().stack;
            stack.push(JOW.wrap(contexts.get((int) (double) stack.pop())));
        }));
    }
}
