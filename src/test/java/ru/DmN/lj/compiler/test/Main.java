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

        // Создаём отладчик
        var debugger = new SimpleDebugger();
        // Добавляем модуль "std"
        debugger.modules.add(new StdLibrary(System.in, System.out));
        // Добавляем обработчик "точки останова"
        debugger.breakPointListener = ((contexts, context) -> System.out.println("BreakPoint: " + context.method));

        debugger.load(compiler.modules.get(0));
        System.out.println(debugger.run("ru.DmN.TM", args));
    }
}
