package ru.DmN.lj.ide;

import ru.DmN.lj.compiler.Compiler;
import ru.DmN.lj.compiler.ExprPosition;
import ru.DmN.lj.compiler.Expression;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CodeInput extends JTextPane {
    public static final MutableAttributeSet attr = new SimpleAttributeSet();
    public static CodeInput INSTANCE;
    public static Compiler compiler;
    public static List<ExprPosition> errors0 = new ArrayList<>(); //
    public static List<ExprPosition> errors1 = new ArrayList<>(); // not completed expr

    public CodeInput() {
        INSTANCE = this;

        this.setStyledDocument(new Document());

        this.setAutoscrolls(true);
        Main.INSTANCE.add(this);
        var scroll = new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        Main.INSTANCE.add(scroll);

        Main.ALL_COMPONENTS.add(this);
        Main.ALL_COMPONENTS.add(scroll);
    }

    public static void setColor(ExprPosition pos, Color back, Color color, boolean end) {
        if (pos == null)
            return;
        var a = new SimpleAttributeSet();
        StyleConstants.setBackground(a, back);
        StyleConstants.setForeground(a, color);
        CodeInput.INSTANCE.getStyledDocument().setCharacterAttributes(pos.offset(), pos.end(), a, true);
        if (end)
            clearColor(pos.end() + 1);
    }

    public static void setColor(ExprPosition pos, Color color, boolean end) {
        if (pos == null)
            return;
        setColor(pos.offset(), pos.end(), color);
        if (end)
            clearColor(pos.end() + 1);
    }

    public static void setColor(int start, int stop, Color color) {
        var a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, color);
        CodeInput.INSTANCE.getStyledDocument().setCharacterAttributes(start, stop, a, true);
    }

    public static void clearColor(int start) {
        setColor(start, INSTANCE.getText().length(), Main.INSTANCE.getForeground());
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        StyleConstants.setBackground(attr, bg);
    }

    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
        this.setCaretColor(fg);
        StyleConstants.setForeground(attr, fg);
    }

    public Document getStyledDocument() {
        return (Document) super.getStyledDocument();
    }

    public static void checkStyle() {
        try {
            if (compiler != null) {
                for (var module : compiler.modules) {
                    if (module.pname.offset() == -1)
                        setColor(module.pos, new Color(127, 0, 0, 127), Color.MAGENTA, true);
                    else {
                        setColor(module.pos, Color.MAGENTA, false);
                        setColor(module.pname, Color.GRAY, true);
                    }

                    for (var mexpr : module.expressions) {
                        switch (mexpr.type) {
                            case VARIABLE -> drawVariable((Expression.VariableExpr) mexpr);
                            case METHOD -> {
                                var method = (Expression.MethodExpr) mexpr;
                                setColor(method.pos, Color.MAGENTA, false);
                                setColor(method.pname, Color.PINK, false);
                                setColor(method.pname.end() + 1, method.pdesc.offset(), Color.GRAY);
                                setColor(method.pdesc, Color.PINK, false);
                                setColor(method.pdesc.end() + 1, method.pend, Color.MAGENTA);

                                for (var expr : method.expressions) {
                                    switch (expr.type) {
                                        case VARIABLE -> drawVariable((Expression.VariableExpr) expr);
                                        case PUSH -> {
                                            setColor(expr.pos, Color.MAGENTA, false);
                                            drawValue(((Expression.PushExpr) expr).value, true);
                                        }
                                        case CALL -> {
                                            var call = (Expression.CallExpr) expr;
                                            setColor(call.pos, Color.MAGENTA, false);
                                            setColor(call.pmodule.end(), call.pmodule.end(), Color.GRAY);
                                            setColor(call.pmodule, Color.ORANGE, false);
                                            setColor(call.pmodule.end() + 1, call.pmodule.end() + 1, Color.GRAY);
                                            setColor(call.pname, Color.PINK, false);
                                            setColor(call.pname.end() + 1, call.pname.end() + 1, Color.GRAY);
                                            setColor(call.pdesc, Color.PINK, true);
                                        }
                                        case JMP, CJMP -> {
                                            setColor(expr.pos, Color.MAGENTA, false);
                                            setColor(((Expression.JmpExpr) expr).plabel, Color.GRAY, true);
                                        }
                                        case RETURN -> {
                                            var ret = (Expression.ReturnExpr) expr;
                                            setColor(ret.pos, Color.MAGENTA, true);
                                            if (ret.value != null)
                                                drawValue(ret.value, true);
                                        }
                                        case OPCODE -> setColor(expr.pos, Color.MAGENTA, false);
                                        case ASSIGN -> {
                                            var assign = (Expression.AssignExpr) expr;
                                            drawVarRef(assign.pos, assign.pname, true);
                                            if (assign.value != null)
                                                drawValue(assign.value, true);
                                        }
                                        case LABEL -> {
                                            var label = (Expression.LabelExpr) expr;
                                            setColor(label.pos, Color.MAGENTA, false);
                                            setColor(label.plabel, Color.GRAY, false);
                                            clearColor(label.pos.end() + 1);
                                        }
                                    }
                                }

                                var text = CodeInput.INSTANCE.getText();
                                if (method.pdesc.end() == method.pend || text.charAt(method.pend - 1) != '<' || text.charAt(method.pend) != '|')
                                    setColor(new ExprPosition(method.pos.line(), method.pos.offset(), method.pend + 2), new Color(127, 0, 0), Color.WHITE, true);
                                else {
                                    setColor(method.pend - 1, method.pend, Color.MAGENTA);
                                    clearColor(method.pend + 1);
                                }
                            }
                        }
                    }
                }

                errors0.addAll(compiler.parser.errors);
                errors0.addAll(compiler.errors0);
                errors1.addAll(compiler.errors1);
            }

            var doc = INSTANCE.getStyledDocument();
            var attr = new SimpleAttributeSet();
            StyleConstants.setBackground(attr, Color.RED);
            for (var err : errors0)
                doc.setCharacterAttributes(err.offset(), err.end() + 1, attr, true);
            for (var err : errors1)
                setColor(err, new Color(127, 0, 0), Color.WHITE, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void drawVarRef(ExprPosition pos, ExprPosition pname, boolean end) {
        setColor(pos, Color.ORANGE, false);
        setColor(pos.end() + 1, pos.end() + 1, Color.GRAY);
        setColor(pname, Color.PINK, end);
    }

    public static void drawVariable(Expression.VariableExpr var) {
        setColor(var.pos, Color.MAGENTA,false);
        setColor(var.pname, Color.GRAY, true);
        if (var.value != null)
            drawValue(var.value, true);
    }

    public static void drawValue(Expression expr, boolean end) {
        if (expr instanceof Expression.ValueExpr val)
            setColor(val.pos, Color.GREEN, end);
        else if (expr instanceof Expression.OpcodeExpr opcode)
            setColor(opcode.pos, Color.MAGENTA, end);
        else if (expr instanceof Expression.VariableExpr var)
            drawVarRef(var.pos, var.pname, end);
        else throw new RuntimeException("todo:");
    }

    public static class Document extends DefaultStyledDocument {
        @Override
        protected void fireInsertUpdate(DocumentEvent e) {
            super.fireInsertUpdate(e);
            if (Settings.autoCheckstyle)
                checkStyle();
            else this.clearStyle();
        }

        @Override
        protected void fireRemoveUpdate(DocumentEvent e) {
            super.fireRemoveUpdate(e);
            if (Settings.autoCheckstyle)
                checkStyle();
            else this.clearStyle();
        }

        public void clearStyle() {
            this.setCharacterAttributes(0, INSTANCE.getText().length(), attr, true);
            compiler = null;
            errors0.clear();
            errors1.clear();
        }

        public static void checkStyle() {
            INSTANCE.getStyledDocument().clearStyle();
            try {
                compiler = new Compiler(INSTANCE.getText());
            } catch (NullPointerException ignored) {
                ignored.printStackTrace();
            } catch (Compiler.Parser.InvalidExprException e) {
                errors1.add(e.pos);
            }
            CodeInput.checkStyle();
        }
    }
}
