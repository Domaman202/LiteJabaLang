package ru.DmN.lj.compiler;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;

public class Compiler {
    public final List<Expression.ModuleExpr> modules = new ArrayList<>();
    public final Parser parser = new Parser();

    public Compiler() {
    }

    public synchronized void compile(String code) {
        var lexer = new ru.DmN.lj.compiler.ljLexer(CharStreams.fromString(code));
        var stream = new CommonTokenStream(lexer);
        var parser = new ru.DmN.lj.compiler.ljParser(stream);
        var walker = new ParseTreeWalker();
        walker.walk(this.parser, parser.file());
    }

    public class Parser extends ru.DmN.lj.compiler.ljBaseListener {
        @Override
        public void enterModule(ru.DmN.lj.compiler.ljParser.ModuleContext ctx) {
            var module = new Expression.ModuleExpr(ctx, ctx.LITERAL().getText());
            Compiler.this.modules.add(module);
            //
            for (var variable : ctx.variable())
                module.expressions.add(new Expression.VariableExpr(variable, module.name, variable.name.getText(), Expression.parseValue(variable.value())));
            //
            for (var method : ctx.method()) {
                if (method.name == null || method.desc == null)
                    throw new GrammaticalException(method);
                var m = new Expression.MethodExpr(method, module.name, method.name.getText(), method.desc.getText());
                module.expressions.add(m);
                m.init(module, method);
            }
        }

        @Override
        public void visitErrorNode(ErrorNode node) {
            throw new TokenException(node);
        }
    }

    public static class Translator {

    }
}
