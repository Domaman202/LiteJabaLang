package ru.DmN.lj.debugger;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import ru.DmN.lj.compiler.Compiler;
import ru.DmN.lj.compiler.TokenException;

import java.io.*;
import java.util.BitSet;

public class SimpleConsoleDebugger {
    public static SimpleDebugger debugger;

    public static void main(String[] args) throws IOException {
            program:
            while (true) {
                System.out.print("#|");
                var cmd = readLine().split(" ");
                switch (cmd[0]) {
                    case "check" -> {
                        if (cmd.length < 2)
                            System.out.println("Пожалуйста укажите имя файла!");
                        else compile(cmd[1]);
                    }
                    case "load" -> {
                        if (cmd.length < 2)
                            System.out.println("Пожалуйста укажите имя файла!");
                        else load(cmd[1]);
                    }
                    case "eval" -> {
                        if (cmd.length < 3)
                            System.out.println("Пожалуйста укажите `имя файла` и `имя исполняемого модуля`!");
                        else eval(cmd[1], cmd[2]);
                    }
                    case "reset" -> resetDebugger();
                    case "exit" -> {
                        break program;
                    }
                    case "help" -> printHelp0();
                    default -> {
                        System.out.println("Неизвестная комманда `" + cmd[0] + "` !");
                        printHelp0();
                    }
                }
        }
    }

    public static void printHelp0() {
        System.out.println("""
                [Список комманд>
                | check <file> - проверяет файл на возможность компиляции
                | load <file> - загружает модуль из указанного файла
                | eval <file> - исполняет код из указанного файла
                | reset - сбрасывает отладчик
                | exit - завершает работу отладчика
                | help - выводит список комманд
                [>
                """);
    }

    public static void eval(String file, String module) throws IOException {
        if (load(file)) {
            try {
                System.out.println("Программа завершилась с кодом " + debugger.run(module, new String[0]) + "!");
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public static boolean load(String file) throws IOException {
        var compiler = compile(file);
        if (compiler == null)
            return false;
        var modules = compiler.modules;
        modules.forEach(debugger::load);
        System.out.println("Из файла <" + file + "> загружены модули: " + modules);
        return true;
    }

    public static Compiler compile(String file) throws IOException {
        var compiler = new Compiler();
        try (var stream = new FileInputStream(file)) {
            compiler.compile(new String(stream.readAllBytes()), new ANTLRErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                    System.out.println("При компиляции файла <" +
                            file +
                            "> произошла ошибка!\nСинтаксическая ошибка [" +
                            line +
                            "|" +
                            charPositionInLine +
                            "]: " +
                            msg);
                    throw new TokenException();
                }

                @Override
                public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
                }

                @Override
                public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {
                }

                @Override
                public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
                }
            });
            System.out.println("Файл <" + file + "> скомпилирован успешно!");
            return compiler;
        } catch (TokenException ignored) {
            return null;
        } catch (FileNotFoundException e) {
            System.out.println("Произошла ошибка при открытии файла " + e.getMessage());
            return null;
        }
    }

    public static void resetDebugger() {
        debugger = new SimpleDebugger();
        debugger.modules.add(new StdLibrary(System.in, System.out));
        debugger.modules.add(new DebugLibrary());
        debugger.breakPointListener = ((contexts, ctx) -> {
            System.out.println("Сработала точка останова [" + ctx.method.expressions.get(ctx.i).src.getStart().getLine() + "]");

            program:
            while (true) {
                System.out.print("#|");
                var cmd = readLine().split(" ");
                switch (cmd[0]) {
                    case "eval" -> {
                        System.out.println("Вы вошли в режим `eval`!\nВведите код, чтобы запустить напишите `:eval`, чтобы отменить запуск и выйти из режима напишите `:exit`!\n");
                        var code = new StringBuilder();

                        evaluator:
                        while (true) {
                            System.out.print("|>");
                            var in = readLine();
                            cmd = in.split(" ");
                            switch (cmd[0]) {
                                default -> code.append(in).append('\n');
                                case ":eval" -> {
                                    var module = "tmp_" + System.currentTimeMillis();
                                    var tmp = File.createTempFile("eval.", ".lj");
                                    try (var stream = new FileOutputStream(tmp)) {
                                        stream.write("module ".getBytes());
                                        stream.write(module.getBytes());
                                        stream.write("\nfun main|IO|>".getBytes());
                                        stream.write(code.toString().getBytes());
                                        stream.write("<|\nend".getBytes());
                                    }
                                    eval(tmp.toString(), module);
                                    code = new StringBuilder();
                                }
                                case ":exit" -> {
                                    break evaluator;
                                }
                            }
                        }
                    }
                    case "variables" -> {
                        if (cmd.length > 1) {
                            var i = Integer.parseInt(cmd[1]);
                            if (i > 0 && contexts.size() - i < 0)
                                System.out.println("Неверный номер вызова!");
                            else {
                                var variables = (i == 0 ? ctx : contexts.get(contexts.size() - i)).variables;
                                var sb = new StringBuilder("[");
                                variables.forEach((key, value) -> sb.append("\n\t").append(key).append(" = ").append(value));
                                sb.append("\n]");
                                System.out.println(sb);
                            }
                        } else System.out.println("Пожалуйста, укажите номер вызова!");
                    }
                    case "stack" -> {
                        if (cmd.length > 1) {
                            var i = Integer.parseInt(cmd[1]);
                            if (i > 0 && contexts.size() - i < 0)
                                System.out.println("Неверный номер вызова!");
                            else System.out.println((i == 0 ? ctx : contexts.get(contexts.size() - i)).stack);
                        } else System.out.println("Пожалуйста, укажите номер вызова!");
                    }
                    case "stacktrace" -> {
                        var sb = new StringBuilder("[\n\t");
                        contexts.forEach(e -> sb.append(e.method).append("\n\t"));
                        sb.append(ctx.method).append("\n]");
                        System.out.println(sb);
                    }
                    case "exit" -> {
                        break program;
                    }
                    case "help" -> printHelp1();
                    default -> {
                        System.out.println("Неизвестная комманда `" + cmd[0] + "` !");
                        printHelp1();
                    }
                }
            }
        });
    }

    public static void printHelp1() {
        System.out.println("""
                [Список комманд>
                | eval - запускает режим `eval`
                | variables <i> - выводит переменные вызова №i (0 - текущий, 1 - предыдущий и т.д.)
                | stacktrace - выводит стек вызовов
                | stack <i> - выводит стек вызова №i (0 - текущий, 1 - предыдущий и т.д.)
                | exit - продолжает выполнение функции
                | help - выводит список комманд
                [>
                """);
    }

    public static String readLine() throws IOException {
        return new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    static {
        resetDebugger();
    }
}
