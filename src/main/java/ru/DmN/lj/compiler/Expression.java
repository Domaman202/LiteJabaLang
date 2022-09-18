package ru.DmN.lj.compiler;

import java.util.ArrayList;
import java.util.List;

public class Expression {
    public ExprPosition pos;
    public Type type;

    public Expression(Type type) {
        this.type = type;
    }

    public Expression(Type type, ExprPosition pos) {
        this.pos = pos;
        this.type = type;
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
        public ExprPosition pname;
        public String name;
        public List<Expression> expressions = new ArrayList<>();

        public ModuleExpr(ExprPosition pos, ExprPosition pname, String name) {
            super(Type.MODULE, pos);
            this.pname = pname;
            this.name = name;
        }
    }

    public static class MethodExpr extends Expression {
        public ExprPosition pname, pdesc;
        public int pend;
        public String module, name, desc;
        public List<Expression> expressions = new ArrayList<>();

        public MethodExpr(ExprPosition pos, ExprPosition pname, ExprPosition pdesc, int pend, String module, String name, String desc) {
            super(Type.METHOD, pos);
            this.pname = pname;
            this.pdesc = pdesc;
            this.pend = pend;
            this.module = module;
            this.name = name;
            this.desc = desc;
        }

        public void init(ModuleExpr module, ru.DmN.lj.compiler.ljParser.MethodContext ctx) throws InvalidValueException {
            this.parseBody(ctx.body());
        }

        public void parseBody(ru.DmN.lj.compiler.ljParser.BodyContext body) throws InvalidValueException {
            for (var expr : body.any_expr()) {
                Expression parsed;
                //
                if (expr.push() != null)
                    parsed = new PushExpr(ExprPosition.of(expr.push()), parseValue(expr.push().value()));
                else if (expr.call() != null) {
                    var call = expr.call().method_ref();
                    if (call.module_ == null)
                        throw new Compiler.Parser.InvalidExprException(ExprPosition.of(expr.call()));
                    if (call.name == null)
                        throw new Compiler.Parser.InvalidExprException(new ExprPosition(call.start.getLine(), expr.call().start.getStartIndex(), call.module_.getStopIndex() + 1));
                    if (call.desc == null || call.desc.getStartIndex() == -1)
                        throw new Compiler.Parser.InvalidExprException(new ExprPosition(call.start.getLine(), expr.call().start.getStartIndex(), call.name.getStopIndex() + 1));
                    parsed = new CallExpr(ExprPosition.of(expr.call()), ExprPosition.of(call.module_), ExprPosition.of(call.name), ExprPosition.of(call.desc), call.module_.getText(), call.name.getText(), call.desc.getText());
                } else if (expr.return_() != null) {
                    var val = expr.return_().value();
                    parsed = new ReturnExpr(ExprPosition.of(expr), parseValue(val));
                } else if (expr.assign() != null) {
                    var var = expr.assign().var_ref();
                    var val = parseValue(expr.assign().value());
                    if (val == null)
                        throw new Compiler.Parser.InvalidExprException(new ExprPosition(var.start.getLine(), var.start.getStartIndex(), expr.stop.getStopIndex()));
                    parsed = new AssignExpr(ExprPosition.of(var.module_), ExprPosition.of(var.name), var.module_.getText(), var.name.getText(), val);
                } else if (expr.variable() != null) {
                    var var = expr.variable();
                    parsed = new VariableExpr(ExprPosition.of(expr), ExprPosition.of(var.name), ".", var.name.getText(), Expression.parseValue(var.value()));
                } else if (expr.opcode() != null) {
                    var opcode = expr.opcode();
                    parsed = new OpcodeExpr(ExprPosition.of(expr), Opcode.valueOf(opcode.LITERAL().getText().toUpperCase()));
                } else if (expr.label() != null) {
                    var label = expr.label().LITERAL();
                    parsed = new LabelExpr(ExprPosition.of(expr), ExprPosition.of(label), label.getText());
                } else if (expr.jmp() != null) {
                    var jmp = expr.jmp();
                    var label = jmp.LITERAL();
                    parsed = new JmpExpr(ExprPosition.of(expr), ExprPosition.of(label), jmp.type.getText().equals("jmp") ? Type.JMP : Type.CJMP, label.getText());
                } else throw new RuntimeException("todo:");
                //
                this.expressions.add(parsed);
            }
        }
    }

    public static class VariableExpr extends Expression {
        public ExprPosition pname;
        public String module;
        public String name;
        public Expression value;

        public VariableExpr(ExprPosition pos, ExprPosition pname, String module, String name, Expression value) {
            super(Type.VARIABLE, pos);
            this.pname = pname;
            this.module = module;
            this.name = name;
            this.value = value;
        }
    }

    public static class LabelExpr extends Expression {
        public ExprPosition plabel;
        public String label;

        public LabelExpr(ExprPosition pos, ExprPosition plabel, String label) {
            super(Type.LABEL, pos);
            this.plabel = plabel;
            this.label = label;
        }
    }

    public static class JmpExpr extends Expression {
        public ExprPosition plabel;
        public String label;

        public JmpExpr(ExprPosition pos, ExprPosition plabel, Type type, String label) {
            super(type, pos);
            this.plabel = plabel;
            this.label = label;
        }
    }

    public static class AssignExpr extends Expression {
        public ExprPosition pname;
        public String module;
        public String name;
        public Expression value;

        public AssignExpr(ExprPosition pos, ExprPosition pname, String module, String name, Expression value) {
            super(Type.ASSIGN, pos);
            this.pname = pname;
            this.module = module;
            this.name = name;
            this.value = value;
        }
    }

    public static class OpcodeExpr extends Expression {
        public Opcode opcode;

        public OpcodeExpr(ExprPosition pos, Opcode opcode) {
            super(Type.OPCODE, pos);
            this.opcode = opcode;
        }
    }

    public static class ReturnExpr extends Expression {
        public Expression value;

        public ReturnExpr(ExprPosition pos, Expression value) {
            super(Type.RETURN, pos);
            this.value = value;
        }
    }

    public static class CallExpr extends Expression {
        public ExprPosition pmodule, pname, pdesc;
        public String module;
        public String name;
        public String desc;

        public CallExpr(ExprPosition pos, ExprPosition pmodule, ExprPosition pname, ExprPosition pdesc, String module, String name, String desc) {
            super(Type.CALL, pos);
            this.pmodule = pmodule;
            this.pname = pname;
            this.pdesc = pdesc;
            this.module = module;
            this.name = name;
            this.desc = desc;
        }
    }

    public static class PushExpr extends Expression {
        public Expression value;

        public PushExpr(ExprPosition pos, Expression value) {
            super(Type.PUSH, pos);
            this.value = value;
        }
    }

    public static class ValueExpr extends Expression {
        public Object value;

        public ValueExpr(ExprPosition pos, Object value) {
            super(Type.VALUE, pos);
            this.value = value;
        }
    }

    public static Expression parseValue(ru.DmN.lj.compiler.ljParser.ValueContext value) throws InvalidValueException {
        if (value == null)
            return new ValueExpr(null, null);
        if (value.NULL() != null)
            return new ValueExpr(ExprPosition.of(value.NULL()), null);
        if (value.NUM() != null) {
            var num = value.NUM();
            return new ValueExpr(ExprPosition.of(num), Double.valueOf(num.getText()));
        }
        if (value.STRING() != null) {
            var str = value.STRING();
            return new ValueExpr(ExprPosition.of(str), str.getText());
        }
        if (value.var_ref() != null) {
            var var = value.var_ref();
            return new VariableExpr(ExprPosition.of(var.module_), ExprPosition.of(var.name), var.module_.getText(), var.name.getText(), null);
        }
        if (value.pop() != null)
            return new OpcodeExpr(ExprPosition.of(value.pop()), Opcode.POP);
        return null;
//        throw new InvalidValueException(ExprPosition.of(value), "todo:");
    }

    public static class InvalidValueException extends Exception {
        public final ExprPosition pos;

        public InvalidValueException(ExprPosition pos, String text) {
            super(text);
            this.pos = pos;
        }
    }
}
