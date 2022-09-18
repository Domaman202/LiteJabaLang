package ru.DmN.lj.compiler.test;

import ru.DmN.lj.compiler.Compiler;
import ru.DmN.lj.debugger.SimpleDebugger;
import ru.DmN.lj.debugger.StdLibrary;

public class Main {
    public static void main(String[] args) {
        var compiler = new Compiler("""
                module TM
                    var text

                    fun init|V
                    |>
                        TM$text = "Сало!"
                        return
                    <|

                    fun main|IO
                    |>
                        var str = TM$text
                        push .$str
                        call std$println|VO

                        push 12
                        push 21
                        opcode add
                        push -1
                        opcode add
                        call std$println|VO
                        
                        // start loop
                        push 0
                        label loop
                        
                        opcode dup
                        call std$println|VO
                        
                        push 1
                        opcode add
                        
                        opcode dup
                        push 10
                        opcode great
                        cjmp loop
                        // end loop

                        return 0
                    <|
                end
                """);

        var debugger = new SimpleDebugger();
        debugger.modules.add(new StdLibrary(System.out));

        System.out.println(debugger.run(compiler.modules.get(0), args));
    }
}
