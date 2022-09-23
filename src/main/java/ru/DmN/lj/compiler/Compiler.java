package ru.DmN.lj.compiler;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Compiler {
    public final List<Expression.ModuleExpr> modules = new ArrayList<>();
    public final Parser parser = new Parser();

    public Compiler() {
    }

    public synchronized void compile(String code) throws TokenException {
        compile(code, null);
    }

    public synchronized void compile(String code, ANTLRErrorListener el) throws TokenException {
        var lexer = new ru.DmN.lj.compiler.ljLexer(CharStreams.fromString(code));
        var stream = new CommonTokenStream(lexer);
        var parser = new ru.DmN.lj.compiler.ljParser(stream);
        var walker = new ParseTreeWalker();
        //
        if (el != null) {
            lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
            parser.removeErrorListener(ConsoleErrorListener.INSTANCE);
            lexer.addErrorListener(el);
            parser.addErrorListener(el);
        }
        //
        walker.walk(this.parser, parser.file());
    }

    public class Parser extends ru.DmN.lj.compiler.ljBaseListener {
        public final Map<String, String> alias = new HashMap<>();

        @Override
        public void enterModule(ru.DmN.lj.compiler.ljParser.ModuleContext ctx) {
            var moduleName = ctx.module_ref().getText();
            var module = Expression.parseModule(Compiler.this.modules, ctx, moduleName);
            //
            this.alias.put("this", moduleName);
            //
            for (var a : ctx.alias())
                this.alias.put(a.new_.getText(), a.old.getText());
            //
            for (var variable : ctx.variable())
                module.expressions.add(new Expression.VariableExpr(variable, moduleName, variable.LITERAL().getText(), Expression.parseValue(this.alias, variable.value())));
            //
            for (var method : ctx.method()) {
                if (method.name == null || method.desc == null)
                    throw new GrammaticalException(method);
                var m = new Expression.MethodExpr(method, module.name, method.name.getText(), method.desc.getText());
                module.expressions.add(m);
                m.init(this.alias, module, method);
            }
        }

        @Override
        public void exitModule(ru.DmN.lj.compiler.ljParser.ModuleContext ctx) {
            this.alias.clear();
        }
    }

    public static class Translator {

    }
}
