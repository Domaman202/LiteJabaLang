package ru.DmN.lj.debugger;

import ru.DmN.lj.compiler.Expression;
import ru.DmN.lj.compiler.GrammaticalException;
import ru.DmN.lj.compiler.Opcode;

import java.util.*;

public class SimpleDebugger {
    public final List<Module> modules = new ArrayList<>();
    public BreakPointListener breakPointListener;

    public int run(String module, String[] args) {
        var m = this.findModule(module);
        var main = m.methods.stream().filter(method -> method.name.equals("main") && method.desc.equals("IO")).findFirst();
        if (main.isPresent()) {
            m.init(this);
            return ((Number) this.run(m, main.get())).intValue();
        }
        else throw new RuntimeException("Метод `main|IO` не найден");
    }

    public void load(Expression.ModuleExpr module) {
        this.modules.add(this.parse(module));
    }

    public Object run(Module module, Method method, Object ... args) {
        var contexts = new Stack<RunContext>();
        //
        {
            var ctx = new RunContext(method);
            for (var arg : args)
                ctx.stack.push(arg);
            contexts.push(ctx);
        }
        //
        while (contexts.size() > 0) {
            for (var context = contexts.pop(); context.i < context.method.expressions.size(); context.i++) {
                if (!context.linit)
                    this.linit(context);
                var expr = context.method.expressions.get(context.i);
                switch (expr.type) {
                    case VARIABLE -> {
                        var var = (Expression.VariableExpr) expr;
                        context.variables.put(var.name, this.parseValue(context, var.value));
                    }
                    //
                    case PUSH -> context.stack.push(this.parseValue(context, ((Expression.PushExpr) expr).value));
                    //
                    case OPCODE -> {
                        var stack = context.stack;;
                        switch (((Expression.OpcodeExpr) expr).opcode) {
                            case POP -> stack.pop();
                            case SWAP -> {
                                var x = stack.pop();
                                var y = stack.pop();
                                stack.push(x);
                                stack.push(y);
                            }
                            case DUP -> stack.push(stack.peek());
                            //
                            case ADD -> stack.push((double) stack.pop() + (double) stack.pop());
                            case SUB -> stack.push((double) stack.pop() - (double) stack.pop());
                            case MUL -> stack.push((double) stack.pop() * (double) stack.pop());
                            case DIV -> stack.push((double) stack.pop() / (double) stack.pop());
                            case REM -> stack.push((double) stack.pop() % (double) stack.pop());
                            case NEG -> stack.push(- (double) stack.pop());
                            //
                            case GREAT -> stack.push((double) stack.pop() > (double) stack.pop());
                            case EQUALS -> stack.push(stack.pop() == stack.pop());
                            //
                            case AND -> stack.push((boolean) stack.pop() && (boolean) stack.pop());
                            case OR -> stack.push((boolean) stack.pop() || (boolean) stack.pop());
                            case NOT -> stack.push(! (boolean) stack.pop());
                            //
                            case NATIVE -> ((Method.Native) context.method).method.run(contexts, context);
                            //
                            case THROW -> {
                                var ctx = this.findCatch(contexts, context);
                                context = ctx;
                                context.i = (int) ctx.variables.get(ctx.catch_.pop());
                            }
                            case TRY_END -> context.catch_.pop();
                            //
                            case BREAKPOINT -> {
                                try {
                                    this.breakPointListener.listen(contexts, context);
                                } catch (Throwable t) {
                                    throw new RuntimeException(t);
                                }
                            }
                            //
                            default -> throw new UnsupportedOperationException();
                        }
                    }
                    //
                    case CALL -> {
                        context.i++;
                        contexts.push(context);
                        var e = ((Expression.CallExpr) expr);
                        context = new RunContext(this.findModule(e.module).methods.stream().filter(m -> m.name.equals(e.name) && m.desc.equals(e.desc)).findFirst().orElseThrow(() -> new RuntimeException("Метод " + e.name + "|" + e.desc + " не найден!")));
                        context.i--;
                    }
                    case RETURN -> {
                        var ret = ((Expression.ReturnExpr) expr).value;
                        if (contexts.isEmpty())
                            return this.parseValue(context, ret);
                        context = contexts.pop();
                        context.i--;
                        if (ret != null)
                            context.stack.push(this.parseValue(context, ret));
                    }
                    //
                    case JMP -> context.i = (int) context.variables.get(((Expression.JmpExpr) expr).label);
                    case CJMP -> {
                        if ((boolean) context.stack.pop())
                            context.i = (int) context.variables.get(((Expression.JmpExpr) expr).label);
                    }
                    //
                    case LABEL -> context.variables.put(((Expression.LabelExpr) expr).label, context.i);
                    case ASSIGN -> {
                        var asg = (Expression.AssignExpr) expr;
                        var value = this.parseValue(context, asg.value);
                        if (asg.module == null)
                            if (asg.name == null)
                                ((Map<Object, Object>) parseValue(context, asg.arr.arr0 == null ? (asg.arr.arr1 == null ? asg.arr.arr2 : asg.arr.arr1) : asg.arr.arr0)).put(this.parseValue(context, asg.arr.key), value);
                            else context.variables.put(asg.name, value);
                        else
                            findModule(asg.module).variables.put(asg.name, value);
                    }
                    //
                    case TRY -> context.catch_.push(((Expression.TryExpr) expr).catch_);
                    //
                    case NATIVE -> ((Method.Native) context.method).method.run(contexts, context);
                    //
                    default -> throw new UnsupportedOperationException();
                }
            }
        }
        if (method.desc.charAt(0) == 'V')
            return null;
        throw new RuntimeException("Метод " + method.name + "|" + method.desc + " не имеет опкода возврата!");
    }

    public RunContext findCatch(Stack<RunContext> contexts, RunContext this_) {
        RunContext ctx = this_;
        while (ctx.catch_.size() == 0)
            ctx = contexts.pop();
        return ctx;
    }

    public Module findModule(String fname) {
        var nparts = fname.split("\\.");

        Module module = null;
        var modules = this.modules;
        for (var name : nparts) {
            module = modules.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow(() -> new RuntimeException("Модуль `" + name + "` не найден!"));
            modules = module.sub;
        }

        return module;
    }

    public Module findModule(List<Module> modules, String name) {
        return modules.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow(() -> new RuntimeException("Модуль `" + name + "` не найден!"));
    }

    public void linit(RunContext context) {
        var method = context.method;
        for (int i = 0; i < method.expressions.size(); i++) {
            var expr = method.expressions.get(i);
            if (expr.type == Expression.Type.LABEL)
                context.variables.put(((Expression.LabelExpr) expr).label, i);
        }
        context.linit = true;
    }

    public Module parse(Expression.ModuleExpr expr) {
        var module = new Module(expr.name);
        // submodules
        expr.sub.forEach(m -> module.sub.add(parse(m)));
        // body
        for (var e : expr.expressions) {
            switch (e.type) {
                case VARIABLE -> module.variables.put(((Expression.VariableExpr) e).name, null);
                case METHOD -> {
                    var mt = (Expression.MethodExpr) e;
                    var method = new Method(mt.name, mt.desc);
                    method.expressions.addAll(mt.expressions);
                    module.methods.add(method);
                }
                default -> throw new RuntimeException("Лол: " + expr.getClass());
            }
        }
        //
        return module;
    }

    public Object parseValue(RunContext context, Expression expr) {
        if (expr == null)
            return null;
        return switch (expr.type) {
            case VALUE -> ((Expression.ValueExpr) expr).value;
            case VARIABLE -> {
                var var = (Expression.VariableExpr) expr;
                if (var.module == null)
                    yield context.variables.get(var.name);
                else yield this.findModule(var.module).variables.get(var.name);
            }
            case OPCODE -> {
                if (((Expression.OpcodeExpr) expr).opcode == Opcode.POP)
                    yield context.stack.pop();
                throw new UnsupportedOperationException();
            }
            case LOGIC -> {
                var logic = (Expression.LogicExpr) expr;
                var left = parseValue(context, logic.left);
                var right = parseValue(context, logic.right);
                yield switch (logic.operation) {
                    case GREAT -> (double) left > (double) right;
                    case LESS -> (double) left < (double) right;
                    case EQUAL -> left == right;
                    case NE -> left != right;
                    case GE -> (double) left >= (double) right;
                    case LE -> (double) left <= (double) right;
                };
            }
            case MATH -> {
                var math = (Expression.MathExpr) expr;
                var left = parseValue(context, math.left);
                var right = parseValue(context, math.right);
                yield switch (math.operation) {
                    case MUL -> (double) left * (double) right;
                    case DIV -> (double) left / (double) right;
                    case REM -> (double) left % (double) right;
                    case ADD -> (double) left + (double) right;
                    case SUB -> (double) left - (double) right;
                };
            }
            case ARRAY -> {
                var value = ((Expression.ArrayExpr) expr).value;
                var parsed = new HashMap<>();
                for (var entry : value.entrySet())
                    parsed.put(this.parseValue(context, entry.getKey()), this.parseValue(context, entry.getValue()));
                yield parsed;
            }
            case ARRAY_ACCESS -> {
                var aa = ((Expression.ArrayAccessExpr) expr);
                var arr = aa.arr0 == null ? (aa.arr1 == null ? aa.arr2 : aa.arr1) : aa.arr0;
                yield ((Map<Object, Object>) parseValue(context, arr)).get(parseValue(context, aa.key));
            }
            default -> throw new UnsupportedOperationException();
        };
    }

    public static class RunContext {
        public final Method method;
        public int i;
        public final Stack<Object> stack = new Stack<>();
        public final Map<String, Object> variables = new HashMap<>();
        public final Deque<String> catch_ = new ArrayDeque<>();
        public boolean linit = false;

        public RunContext(Method method) {
            this.method = method;
        }
    }

    @FunctionalInterface
    public interface BreakPointListener {
        void listen(Stack<RunContext> contexts, RunContext context) throws Throwable;
    }
}
