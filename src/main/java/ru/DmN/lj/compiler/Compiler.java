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
    public final List<ExprPosition> errors0 = new ArrayList<>();
    public final List<ExprPosition> errors1 = new ArrayList<>();

    public Compiler(String code) {
        var lexer = new ru.DmN.lj.compiler.ljLexer(CharStreams.fromString(code));
        var stream = new CommonTokenStream(lexer);
        var parser = new ru.DmN.lj.compiler.ljParser(stream);
        var walker = new ParseTreeWalker();
        walker.walk(this.parser, parser.file());
    }

    public class Parser extends ru.DmN.lj.compiler.ljBaseListener {
        public final List<ExprPosition> errors = new ArrayList<>();

        @Override
        public void enterModule(ru.DmN.lj.compiler.ljParser.ModuleContext ctx) {
            var module = new Expression.ModuleExpr(ExprPosition.of(ctx), ExprPosition.of(ctx.LITERAL().getSymbol()), ctx.LITERAL().getText());
            Compiler.this.modules.add(module);
            //
            for (var variable : ctx.variable()) {
                try {
                    module.expressions.add(new Expression.VariableExpr(ExprPosition.of(variable), ExprPosition.of(variable.name), module.name, variable.name.getText(), Expression.parseValue(variable.value())));
                } catch (Expression.InvalidValueException e) {
                    errors0.add(e.pos);
                }
            }
            //
            for (var method : ctx.method()) {
                if (method.name == null)
                    errors1.add(ExprPosition.of(method));
                else if (method.desc == null)
                    errors1.add(new ExprPosition(method.start.getLine(), method.start.getStartIndex(), method.name.getStopIndex() + 1));
                else {
                    var m = new Expression.MethodExpr(ExprPosition.of(method), ExprPosition.of(method.name), ExprPosition.of(method.desc), method.stop.getStopIndex(), module.name, method.name.getText(), method.desc.getText());
                    module.expressions.add(m);
                    //
                    try {
                        m.init(module, method);
                    } catch (Expression.InvalidValueException e) {
                        errors0.add(e.pos);
                    }catch (InvalidExprException e) {
                        errors1.add(e.pos);
                    }
                }
            }
        }

        @Override
        public void visitErrorNode(ErrorNode node) {
            errors.add(ExprPosition.of(node.getSymbol()));
        }

        public static class InvalidExprException extends RuntimeException {
            public final ExprPosition pos;

            public InvalidExprException(ExprPosition pos) {
                this.pos = pos;
            }
        }
    }

    public static class Translator {

    }
}
