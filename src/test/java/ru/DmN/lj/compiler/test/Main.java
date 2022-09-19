package ru.DmN.lj.compiler.test;

import ru.DmN.lj.compiler.Compiler;
import ru.DmN.lj.debugger.SimpleDebugger;
import ru.DmN.lj.debugger.StdLibrary;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        var compiler = new Compiler();

        try (var stream = Main.class.getClassLoader().getResourceAsStream("main.lj")) {
            compiler.compile(new String(stream.readAllBytes()));
        }

        var debugger = new SimpleDebugger();
        debugger.modules.add(new StdLibrary(System.out));

        System.out.println(debugger.run(compiler.modules.get(0), args));
    }
}
