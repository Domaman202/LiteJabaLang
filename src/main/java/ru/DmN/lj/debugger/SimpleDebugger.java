package ru.DmN.lj.debugger;

import ru.DmN.lj.compiler.Expression;

import java.util.*;

public class SimpleDebugger {
    public final List<Module> modules = new ArrayList<>();

    public int run(Expression.ModuleExpr m, String[] args) {
        var module = this.parse(m);
        this.modules.add(module);
        var main = module.methods.stream().filter(method -> method.name.equals("main") && method.desc.equals("IO")).findFirst();
        if (main.isPresent()) {
            module.init(this);
            return ((Number) this.run(module, main.get())).intValue();
        }
        else throw new RuntimeException("Метод `main|IO` не найден");
    }

    public Object run(Module module, Method method) {
        var contexts = new Stack<RunContext>();
        //
        contexts.push(new RunContext(method));
        //
        while (contexts.size() > 0) {
            for (var context = contexts.pop(); context.i < context.method.expressions.size(); context.i++) {
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
                            case PUSH,RETURN,CALL,JMP -> throw new UnsupportedOperationException();
                            //
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
                            case MUL -> stack.push((double) stack.pop() * (double) stack.pop());
                            case DIV -> stack.push((double) stack.pop() / (double) stack.pop());
                            case NEG -> stack.push(- (double) stack.pop());
                            //
                            case GREAT -> stack.push((double) stack.pop() > (double) stack.pop());
                            case EQUALS -> stack.push(stack.pop() == stack.pop());
                            //
                            case AND -> stack.push((boolean) stack.pop() && (boolean) stack.pop());
                            case OR -> stack.push((boolean) stack.pop() || (boolean) stack.pop());
                            case NOT -> stack.push(! (boolean) stack.pop());
                        }
                    }
                    //
                    case CALL -> {
                        context.i++;
                        contexts.push(context);
                        var e = ((Expression.CallExpr) expr);
                        context = new RunContext(this.modules.stream().filter(m -> m.name.equals(e.module)).findFirst().orElseThrow(() -> new RuntimeException("Модуль " + e.module + " не найден!")).methods.stream().filter(m -> m.name.equals(e.name) && m.desc.equals(e.desc)).findFirst().get());
                        context.i--;
                    }
                    case JMP -> context.i = (int) context.variables.get(((Expression.JmpExpr) expr).label);
                    case CJMP -> {
                        if ((boolean) context.stack.pop())
                            context.i = (int) context.variables.get(((Expression.JmpExpr) expr).label);
                    }
                    case RETURN -> {
                        return this.parseValue(context, ((Expression.ReturnExpr) expr).value);
                    }
                    //
                    case LABEL -> context.variables.put(((Expression.LabelExpr) expr).label, context.i);
                    case ASSIGN -> {
                        var asg = (Expression.AssignExpr) expr;
                        var value = this.parseValue(context, asg.value);
                        if (asg.module.equals("."))
                            context.variables.put(asg.name, value);
                        else this.modules.stream().filter(m -> m.name.equals(asg.module)).findFirst().orElseThrow(() -> new RuntimeException("Модуль `" + asg.module + "` не найден!")).variables.put(asg.name, value);
                    }
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

    public Module parse(Expression.ModuleExpr m) {
        var module = new Module(m.name);
        for (var expr : m.expressions) {
            if (expr.type == Expression.Type.VARIABLE)
                module.variables.put(((Expression.VariableExpr) expr).name, null);
            else if (expr.type == Expression.Type.METHOD) {
                var mt = (Expression.MethodExpr) expr;
                var method = new Method(mt.name, mt.desc);
                method.expressions.addAll(mt.expressions);
                module.methods.add(method);
            } else throw new RuntimeException("Лол");
        }
        return module;
    }

    public Object parseValue(RunContext context, Expression expr) {
        return switch (expr.type) {
            case VALUE -> ((Expression.ValueExpr) expr).value;
            case VARIABLE -> {
                var var = (Expression.VariableExpr) expr;
                if (var.module.equals("."))
                    yield context.variables.get(var.name);
                else yield this.modules.stream().filter(m -> m.name.equals(var.module)).findFirst().orElseThrow(() -> new RuntimeException("Модуль `" + var.module + "` не найден!")).variables.get(var.name);
            }
            default -> throw new UnsupportedOperationException();
        };
    }

    public static class RunContext {
        public final Method method;
        public int i;
        public final Stack<Object> stack = new Stack<>();
        public final Map<String, Object> variables = new HashMap<>();

        public RunContext(Method method) {
            this.method = method;
        }
    }
}