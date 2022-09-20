package ru.DmN.lj.debugger;

import java.io.*;

public class StdLibrary extends Module {
    public StdLibrary(InputStream in, PrintStream out) {
        super("std");

        // input/output
        this.addMethod("readln", "O", (contexts, context) -> {
            try {
                contexts.peek().stack.push(new BufferedReader(new InputStreamReader(in)).readLine());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        this.addMethod("println", "VO", (contexts, context) -> out.println(contexts.peek().stack.pop()));
        //
        this.addMethod("exception", "OO", ((contexts, context) -> {
            var stack = contexts.peek().stack;
            stack.push(new LJException((String) stack.pop()));
        }));
        // CONVERT
        // str <-> int
        this.addMethod("convert", "IO", ((contexts, context) -> {
            var stack = contexts.peek().stack;
            stack.push(Integer.parseInt((String) stack.pop()));
        }));
        this.addMethod("convert", "OI", ((contexts, context) -> {
            var stack = contexts.peek().stack;
            stack.push(String.valueOf((int) stack.pop()));
        }));
        // str <-> double
        this.addMethod("convert", "DO", ((contexts, context) -> {
            var stack = contexts.peek().stack;
            stack.push(Double.parseDouble((String) stack.pop()));
        }));
        this.addMethod("convert", "OD", ((contexts, context) -> {
            var stack = contexts.peek().stack;
            stack.push(String.valueOf((double) stack.pop()));
        }));
    }

    protected void addMethod(String name, String desc, Method.Native.NM method) {
        this.methods.add(new Method.Native(name, desc, method));
    }
}
