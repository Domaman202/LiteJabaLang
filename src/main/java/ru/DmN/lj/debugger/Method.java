package ru.DmN.lj.debugger;

import ru.DmN.lj.compiler.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Method {
    public final String name, desc;
    public List<Expression> expressions = new ArrayList<>();

    public Method(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "[" + this.name + "|" + this.desc + "]";
    }

    public static class Native extends Method {
        public final NM method;

        public Native(String name, String desc, NM method) {
            super(name, desc);
            this.expressions = List.of(new Expression(Expression.Type.NATIVE, null));
            this.method = method;
        }

        @FunctionalInterface
        public interface NM {
            void run(Stack<SimpleDebugger.RunContext> contexts, SimpleDebugger.RunContext context);
        }
    }
}
