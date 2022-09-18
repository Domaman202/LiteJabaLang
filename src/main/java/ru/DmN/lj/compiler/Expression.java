package ru.DmN.lj.compiler;

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

public class Expression {
    public final ParserRuleContext src;
    public final Type type;

    public Expression(Type type, ParserRuleContext src) {
        this.type = type;
        this.src = src;
    }

    public enum Type {
        VALUE,
        VARIABLE,
        METHOD,
        MODULE,

        PUSH,

        CALL,
        CJMP,
        JMP,
        RETURN,

        OPCODE,

        ASSIGN,
        LABEL,

        NATIVE
    }

    public static class ModuleExpr extends Expression {
        public String name;
        public List<Expression> expressions = new ArrayList<>();

        public ModuleExpr(ParserRuleContext src, String name) {
            super(Type.MODULE, src);
            this.name = name;
        }
    }

    public static class MethodExpr extends Expression {
        public String module, name, desc;
        public List<Expression> expressions = new ArrayList<>();

        public MethodExpr(ParserRuleContext src, String module, String name, String desc) {
            super(Type.METHOD, src);
            this.module = module;
            this.name = name;
            this.desc = desc;
        }

        public void init(ModuleExpr module, ru.DmN.lj.compiler.ljParser.MethodContext ctx) throws GrammaticalException {
            this.parseBody(ctx.body());
        }

        public void parseBody(ru.DmN.lj.compiler.ljParser.BodyContext body) throws GrammaticalException {
            for (var expr : body.any_expr()) {
                Expression parsed;
                //
                if (expr.push() != null)
                    parsed = new PushExpr(expr.push(), parseValue(expr.push().value()));
                else if (expr.call() != null) {
                    var call = expr.call();
                    var ref = call.method_ref();
                    if (ref == null || ref.module_ == null || ref.name == null || ref.desc == null)
                        throw new GrammaticalException(call);
                    parsed = new CallExpr(call, ref.module_.getText(), ref.name.getText(), ref.desc.getText());
                } else if (expr.return_() != null) {
                    var ret = expr.return_();
                    parsed = new ReturnExpr(ret, parseValue(ret.value()));
                } else if (expr.assign() != null) {
                    var ass = expr.assign();
                    var var = ass.var_ref();
                    if (var == null || var.module_ == null || var.name == null)
                        throw new GrammaticalException(ass);
                    parsed = new AssignExpr(ass, var.module_.getText(), var.name.getText(), parseValue(ass.value()));
                } else if (expr.variable() != null) {
                    var var = expr.variable();
                    parsed = new VariableExpr(var, ".", var.name.getText(), Expression.parseValue(var.value()));
                } else if (expr.opcode() != null) {
                    var opcode = expr.opcode();
                    parsed = new OpcodeExpr(opcode, Opcode.valueOf(opcode.LITERAL().getText().toUpperCase()));
                } else if (expr.label() != null) {
                    var label = expr.label();
                    if (label.LITERAL() == null)
                        throw new GrammaticalException(label);
                    parsed = new LabelExpr(label, label.LITERAL().getText());
                } else if (expr.jmp() != null) {
                    var jmp = expr.jmp();
                    var label = jmp.LITERAL();
                    if (label == null || jmp.type == null)
                        throw new GrammaticalException(jmp);
                    parsed = new JmpExpr(jmp, jmp.type.getText().equals("jmp") ? Type.JMP : Type.CJMP, label.getText());
                } else throw new RuntimeException("todo:");
                //
                this.expressions.add(parsed);
            }
        }
    }

    public static class VariableExpr extends Expression {
        public String module;
        public String name;
        public Expression value;

        public VariableExpr(ParserRuleContext src, String module, String name, Expression value) {
            super(Type.VARIABLE, src);
            this.module = module;
            this.name = name;
            this.value = value;
        }
    }

    public static class LabelExpr extends Expression {
        public String label;

        public LabelExpr(ParserRuleContext src, String label) {
            super(Type.LABEL, src);
            this.label = label;
        }
    }

    public static class JmpExpr extends Expression {
        public String label;

        public JmpExpr(ParserRuleContext src, Type type, String label) {
            super(type, src);
            this.label = label;
        }
    }

    public static class AssignExpr extends Expression {
        public String module;
        public String name;
        public Expression value;

        public AssignExpr(ParserRuleContext src, String module, String name, Expression value) {
            super(Type.ASSIGN, src);
            this.module = module;
            this.name = name;
            this.value = value;
        }
    }

    public static class OpcodeExpr extends Expression {
        public Opcode opcode;

        public OpcodeExpr(ParserRuleContext src, Opcode opcode) {
            super(Type.OPCODE, src);
            this.opcode = opcode;
        }
    }

    public static class ReturnExpr extends Expression {
        public Expression value;

        public ReturnExpr(ParserRuleContext src, Expression value) {
            super(Type.RETURN, src);
            this.value = value;
        }
    }

    public static class CallExpr extends Expression {
        public String module;
        public String name;
        public String desc;

        public CallExpr(ParserRuleContext src, String module, String name, String desc) {
            super(Type.CALL, src);
            this.module = module;
            this.name = name;
            this.desc = desc;
        }
    }

    public static class PushExpr extends Expression {
        public Expression value;

        public PushExpr(ParserRuleContext src, Expression value) {
            super(Type.PUSH, src);
            this.value = value;
        }
    }

    public static class ValueExpr extends Expression {
        public Object value;

        public ValueExpr(ParserRuleContext src, Object value) {
            super(Type.VALUE, src);
            this.value = value;
        }
    }

    public static Expression parseValue(ru.DmN.lj.compiler.ljParser.ValueContext value) throws GrammaticalException {
        if (value == null)
            return new ValueExpr(null, null);
        if (value.NULL() != null)
            return new ValueExpr(value, null);
        if (value.NUM() != null) {
            var num = value.NUM();
            return new ValueExpr(value, Double.valueOf(num.getText()));
        }
        if (value.STRING() != null) {
            var str = value.STRING();
            return new ValueExpr(value, str.getText());
        }
        if (value.var_ref() != null) {
            var var = value.var_ref();
            return new VariableExpr(value, var.module_.getText(), var.name.getText(), null);
        }
        if (value.pop() != null)
            return new OpcodeExpr(value, Opcode.POP);
        throw new GrammaticalException(value);
//        throw new InvalidValueException(ExprPosition.of(value), "todo:");
    }
}
