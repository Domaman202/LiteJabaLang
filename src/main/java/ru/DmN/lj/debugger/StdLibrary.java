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
        // int <-> double
        this.addMethod("convert", "ID", ((contexts, context) -> {
            var stack = contexts.peek().stack;
            stack.push((int) (double) stack.pop());
        }));
        this.addMethod("convert", "DI", ((contexts, context) -> {
            var stack = contexts.peek().stack;
            stack.push(((double) (int) stack.pop()));
        }));
    }
}
