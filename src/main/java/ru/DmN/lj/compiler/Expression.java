package ru.DmN.lj.compiler;

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        LOGIC,
        MATH,

        TRY,

        NATIVE
    }

    public static List<Expression> parseBody(Map<String, String> alias, ru.DmN.lj.compiler.ljParser.BodyContext body) throws GrammaticalException {
        var exprs = new ArrayList<Expression>();
        //
        for (var expr : body.any_expr()) {
            Expression parsed;
            //
            if (expr.push() != null)
                parsed = new PushExpr(expr.push(), parseValue(alias, expr.push().value()));
            else if (expr.call() != null) {
                var call = expr.call();
                var ref = call.method_ref();
                if (ref == null || ref.module_ == null || ref.name == null || ref.desc == null)
                    throw new GrammaticalException(call);
                var module = ref.module_.getText();
                if (alias != null)
                    module = alias.getOrDefault(module, module);
                parsed = new CallExpr(call, module, ref.name.getText(), ref.desc.getText());
            } else if (expr.return_() != null) {
                var ret = expr.return_();
                parsed = new ReturnExpr(ret, parseValue(alias, ret.value()));
            } else if (expr.assign() != null) {
                var ass = expr.assign();
                var var = ass.var_ref();
                if (var == null || var.module_ == null || var.name == null)
                    throw new GrammaticalException(ass);
                var module = var.module_.getText();
                if (alias != null)
                    module = alias.getOrDefault(module, module);
                parsed = new AssignExpr(ass, module, var.name.getText(), parseValue(alias, ass.value()));
            } else if (expr.variable() != null) {
                var var = expr.variable();
                parsed = new VariableExpr(var, ".", var.LITERAL().getText(), Expression.parseValue(alias, var.value()));
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
            } else if (expr.try_() != null) {
                var try_ = expr.try_();
                exprs.add(new TryExpr(try_, try_.LITERAL().getText()));
                exprs.addAll(parseBody(alias, try_.body()));
                exprs.add(new OpcodeExpr(try_, Opcode.TRY_END));
                continue;
            } else {
                throw new RuntimeException("todo:");
            }
            //
            exprs.add(parsed);
        }
        //
        return exprs;
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

        public void init(Map<String, String> alias, ModuleExpr module, ru.DmN.lj.compiler.ljParser.MethodContext ctx) throws GrammaticalException {
            this.expressions = Expression.parseBody(alias, ctx.body());
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

    public static class TryExpr extends Expression {
        public String catch_;

        public TryExpr(ParserRuleContext src, String catch_) {
            super(Type.TRY, src);
            this.catch_ = catch_;
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

    public static class LogicExpr extends Expression {
        public Operation operation;
        public Expression left, right;

        public LogicExpr(ParserRuleContext src, Operation operation, Expression left, Expression right) {
            super(Type.LOGIC, src);
            this.operation = operation;
            this.left = left;
            this.right = right;
        }

        public enum Operation {
            GREAT(OperType.MATH), // >
            LESS(OperType.MATH), // <

            EQUAL(OperType.ANY), // ==
            NE(OperType.ANY), // !=

            GE(OperType.MATH), // >=
            LE(OperType.MATH); // <=

            public final OperType type;

            Operation(OperType type) {
                this.type = type;
            }
        }

        public enum OperType {
            LOGIC,
            MATH,
            ANY
        }
    }

    public static class MathExpr extends Expression {
        public Operation operation;
        public Expression left, right;

        public MathExpr(ParserRuleContext src, Operation operation, Expression left, Expression right) {
            super(Type.MATH, src);
            this.operation = operation;
            this.left = left;
            this.right = right;
        }

        public enum Operation {
            MUL,
            DIV,
            ADD,
            SUB
        }
    }

    public static class ValueExpr extends Expression {
        public Object value;

        public ValueExpr(ParserRuleContext src, Object value) {
            super(Type.VALUE, src);
            this.value = value;
        }
    }

    public static Expression parseValue(Map<String, String> alias, ru.DmN.lj.compiler.ljParser.ValueContext value) throws GrammaticalException {
        if (value == null)
            return new ValueExpr(null, null);
        if (value.NULL() != null)
            return new ValueExpr(value, null);
        if (value.NUM() != null)
            return new ValueExpr(value, Double.valueOf(value.NUM().getText()));
        if (value.STRING() != null)
            return new ValueExpr(value, value.STRING().getText());
        if (value.var_ref() != null)
            return parseVarRef(alias, value, value.var_ref());
        if (value.pop() != null)
            return new OpcodeExpr(value, Opcode.POP);
        if (value.logic_expr() != null)
            return parseLogic(alias, value, value.logic_expr());
        if (value.math_expr() != null)
            return parseMath(alias, value, value.math_expr());
        throw new GrammaticalException(value);
    }

    public static Expression parseLogic(Map<String, String> alias, ParserRuleContext src, ru.DmN.lj.compiler.ljParser.Logic_exprContext logic) {
        if (logic.BOOL() != null)
            return new ValueExpr(src, logic.BOOL().getText().equals("true"));
        if (logic.logic_expr().size() == 1)
            return parseLogic(alias, src, logic.logic_expr().get(0));
        var oper = switch (logic.oper.getText()) {
            case ">" -> LogicExpr.Operation.GREAT;
            case "<" -> LogicExpr.Operation.LESS;
            case "==" -> LogicExpr.Operation.EQUAL;
            case "!=" -> LogicExpr.Operation.NE;
            case ">=" -> LogicExpr.Operation.GE;
            case "<=" -> LogicExpr.Operation.LE;
            default -> throw new IllegalStateException("Unexpected value: " + logic.oper.getText());
        };
        if (logic.logic_expr().size() == 2)
            return new LogicExpr(src, oper, parseLogic(alias, src, logic.logic_expr(0)), parseLogic(alias, src, logic.logic_expr(1)));
        return new LogicExpr(src, oper, parseMath(alias, src, logic.math_expr(0)), parseMath(alias, src, logic.math_expr(1)));
    }

    public static Expression parseMath(Map<String, String> alias, ParserRuleContext src, ru.DmN.lj.compiler.ljParser.Math_exprContext math) {
        if (math.num_value() != null)
            return parseNum(alias, math.num_value());
        if (math.math_expr().size() == 1)
            return parseMath(alias, src, math.math_expr().get(0));
        var oper = switch (math.oper.getText()) {
            case "*" -> MathExpr.Operation.MUL;
            case "/" -> MathExpr.Operation.DIV;
            case "+" -> MathExpr.Operation.ADD;
            case "-" -> MathExpr.Operation.SUB;
            default -> throw new IllegalStateException("Unexpected value: " + math.oper.getText());
        };
        return new MathExpr(src, oper, parseMath(alias, src, math.math_expr(0)), parseMath(alias, src, math.math_expr(1)));
    }

    public static Expression parseNum(Map<String, String> alias, ru.DmN.lj.compiler.ljParser.Num_valueContext num) {
        if (num.NUM() != null)
            return new ValueExpr(num, Double.valueOf(num.NUM().getText()));
        if (num.var_ref() != null)
            return parseVarRef(alias, num, num.var_ref());
        if (num.pop() != null)
            return new OpcodeExpr(num, Opcode.POP);
        throw new GrammaticalException(num);
    }

    public static Expression parseVarRef(Map<String, String> alias, ParserRuleContext src, ru.DmN.lj.compiler.ljParser.Var_refContext var) {
        var module = var.module_.getText();
        if (alias != null)
            module = alias.getOrDefault(module, module);
        return new VariableExpr(var, module, var.name.getText(), null);
    }
}
