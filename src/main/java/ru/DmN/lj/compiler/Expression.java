package ru.DmN.lj.compiler;

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Expression {
    public ParserRuleContext src;
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

        ARRAY,
        ARRAY_ACCESS,

        TRY,

        NATIVE
    }

    public static List<Expression> parseBody(Map<String, String> alias, ru.DmN.lj.compiler.ljParser.BodyContext body) throws GrammaticalException {
        var exprs = new ArrayList<Expression>();
        //
        for (var expr : body.any_expr())
            exprs.addAll(parseExpr(alias, expr));
        //
        return exprs;
    }

    public static List<Expression> parseExpr(Map<String, String> alias, ru.DmN.lj.compiler.ljParser.Any_exprContext expr) {
        var exprs = new ArrayList<Expression>();
        Expression parsed;
        //
        if (expr.any_expr() != null)
            return parseExpr(alias, expr.any_expr());
        else if (expr.push() != null) {
            var push = expr.push();
            for (var value : push.value())
                exprs.add(new PushExpr(push, parseValue(alias, value)));
            return exprs;
        } else if (expr.call() != null) {
            var call = expr.call();
            var ref = call.method_ref();
            if (ref == null || ref.module_ref() == null || ref.name == null || ref.desc == null)
                throw new GrammaticalException(call);
            var module = ref.module_ref().getText();
            if (alias != null)
                module = alias.getOrDefault(module, module);
            parsed = new CallExpr(call, module, ref.name.getText(), ref.desc.getText());
        } else if (expr.return_() != null) {
            var ret = expr.return_();
            parsed = new ReturnExpr(ret, parseValue(alias, ret.value()));
        } else if (expr.assign() != null) {
            var ass = expr.assign();
            var var = ass.var_ref();
            if (var == null) {
                var arr = ass.array_access();
                if (arr == null)
                    throw new GrammaticalException(ass);
                parsed = new AssignExpr(ass, parseArrayAccess(alias, ass, arr), parseValue(alias, ass.value()));
            } else {
                if (var.LITERAL() == null)
                    throw new GrammaticalException(ass);
                String module = var.module_ref() == null ? null : var.module_ref().getText();
                if (module != null && alias != null)
                    module = alias.getOrDefault(module, module);
                parsed = new AssignExpr(ass, module, var.LITERAL().getText(), parseValue(alias, ass.value()));
            }
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
            return exprs;
        } else throw new RuntimeException("todo:");
        //
        exprs.add(parsed);
        return exprs;
    }

    public static class ModuleExpr extends Expression {
        public String name;
        public final List<ModuleExpr> sub = new ArrayList<>();
        public final List<Expression> expressions = new ArrayList<>();

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
            if (this.expressions.get(this.expressions.size() - 1).type != Type.RETURN && ctx.desc.getText().startsWith("V"))
                this.expressions.add(new ReturnExpr(null, null));
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

    public static class ArrayAccessExpr extends Expression {
        public VariableExpr arr0;
        public ArrayExpr arr1;
        public ArrayAccessExpr arr2;
        public Expression key;

        public ArrayAccessExpr(ParserRuleContext src, VariableExpr arr, Expression key) {
            super(Type.ARRAY_ACCESS, src);
            this.arr0 = arr;
            this.key = key;
        }

        public ArrayAccessExpr(ParserRuleContext src, ArrayExpr arr, Expression key) {
            super(Type.ARRAY_ACCESS, src);
            this.arr1 = arr;
            this.key = key;
        }

        public ArrayAccessExpr(ParserRuleContext src, ArrayAccessExpr arr, Expression key) {
            super(Type.ARRAY_ACCESS, src);
            this.arr2 = arr;
            this.key = key;
        }
    }

    public static class ArrayExpr extends Expression {
        public Map<Expression, Expression> value;

        public ArrayExpr(ParserRuleContext src, Map<Expression, Expression> value) {
            super(Type.ARRAY, src);
            this.value = value;
        }
    }

    public static class AssignExpr extends Expression {
        public String module;
        public String name;
        public ArrayAccessExpr arr;
        public Expression value;

        public AssignExpr(ParserRuleContext src, String module, String name, Expression value) {
            super(Type.ASSIGN, src);
            this.module = module;
            this.name = name;
            this.value = value;
        }

        public AssignExpr(ParserRuleContext src, ArrayAccessExpr arr, Expression value) {
            super(Type.ASSIGN, src);
            this.arr = arr;
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
            SUB,
            REM,
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
        if (value.STRING() != null) {
            var str = value.STRING().getText();
            return new ValueExpr(value, str.substring(1, str.length() - 1));
        } if (value.array_access() != null)
            return parseArrayAccess(alias, value, value.array_access());
        if (value.array() != null)
            return parseArray(alias, value, value.array(), null);
        if (value.named_array() != null)
            return parseArray(alias, value, null, value.named_array());
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
            case "%" -> MathExpr.Operation.REM;
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

    public static VariableExpr parseVarRef(Map<String, String> alias, ParserRuleContext src, ru.DmN.lj.compiler.ljParser.Var_refContext var) {
        var module_ = var.module_ref();
        String module;
        if (module_ == null)
            module = null;
        else {
            module = var.module_ref().getText();
            if (alias != null)
                module = alias.getOrDefault(module, module);
        }
        return new VariableExpr(var, module, var.LITERAL().getText(), null);
    }

    public static ArrayAccessExpr parseArrayAccess(Map<String, String> alias, ParserRuleContext src, ru.DmN.lj.compiler.ljParser.Array_accessContext ctx) {
        var val = parseValue(alias, ctx.value());
        if (ctx.var_ref() != null)
            return new ArrayAccessExpr(src, parseVarRef(alias, src, ctx.var_ref()), val);
        if (ctx.array_access() != null)
            return new ArrayAccessExpr(src, parseArrayAccess(alias, src, ctx.array_access()), val);
        return new ArrayAccessExpr(src, parseArray(alias, src, ctx.array(), ctx.named_array()), val);
    }

    public static ArrayExpr parseArray(Map<String, String> alias, ParserRuleContext src, ru.DmN.lj.compiler.ljParser.ArrayContext arr, ru.DmN.lj.compiler.ljParser.Named_arrayContext narr) {
        if (arr != null) {
            var v = new HashMap<Expression, Expression>();
            for (var i = 0; i < arr.value().size(); i++)
                v.put(new ValueExpr(src, (double) i), parseValue(alias, arr.value(i)));
            return new ArrayExpr(src, v);
        } else if (narr == null)
            throw new GrammaticalException(src);
        var v = new HashMap<Expression, Expression>();
        for (var i = 0; i < narr.value().size(); i++)
            v.put(parseValue(alias, narr.value(i++)), parseValue(alias, narr.value(i)));
        return new ArrayExpr(src, v);
    }

    public static ModuleExpr parseModule(List<ModuleExpr> modules, ParserRuleContext src, String fname) {
        var npart = fname.split("\\.");
        var module = modules.stream().filter(m -> m.name.equals(npart[0])).findFirst().orElseGet(() -> {
            var m = new ModuleExpr(null, npart[0]);
            modules.add(m);
            return m;
        });
        name:
        for (var i = 1; i < npart.length; i++) {
            var name = npart[i];
            for (var m : module.sub) {
                if (m.name.equals(name)) {
                    module = m;
                    continue name;
                }
            }
            var n = new ModuleExpr(src, name);
            module.sub.add(n);
            module = n;
        }
        module.src = src;
        return module;
    }
}
