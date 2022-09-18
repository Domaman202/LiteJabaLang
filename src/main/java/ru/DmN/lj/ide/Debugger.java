package ru.DmN.lj.ide;

import ru.DmN.lj.compiler.Compiler;
import ru.DmN.lj.debugger.SimpleDebugger;
import ru.DmN.lj.debugger.StdLibrary;

import javax.swing.*;
import java.awt.event.WindowEvent;
import java.io.OutputStream;
import java.io.PrintStream;

import static ru.DmN.lj.ide.Main.ALL_COMPONENTS;

public class Debugger extends JFrame implements WindowListenerImpl {
    public static final Debugger INSTANCE = new Debugger();
    public static SimpleDebugger debugger;
    public static final DebuggerPrintStream out = new DebuggerPrintStream();
    public static JTextArea console;

    public Debugger() {
        this.addWindowListener(this);

        this.setSize(640, 360);

        console = new JTextArea();
        this.add(console);

        ALL_COMPONENTS.add(console);
        ALL_COMPONENTS.add(this.getContentPane());
        ALL_COMPONENTS.add(this);
    }

    public static void open() {
        console.setText(null);
        debugger = new SimpleDebugger();
        //
        if (CodeInput.compiler == null)
            CodeInput.compiler = new Compiler(CodeInput.INSTANCE.getText());
        if (CodeInput.compiler.modules.isEmpty())
            return;
        //
        INSTANCE.setVisible(true);
        //
        debugger.modules.add(new StdLibrary(out));
        debugger.run(CodeInput.compiler.modules.get(0), null);
    }

    @Override
    public void windowClosing(WindowEvent e) {
        this.setVisible(false);
    }

    public static class DebuggerPrintStream extends PrintStream {
        public DebuggerPrintStream() {
            super(new OutputStream() {
                @Override
                public void write(int b) {
                    throw new UnsupportedOperationException();
                }
            });
        }

        @Override
        public void println(String x) {
            console.append(x);
            console.append("\n");
        }

        @Override
        public void println(Object x) {
            console.append(String.valueOf(x));
            console.append("\n");
        }
    }
}
