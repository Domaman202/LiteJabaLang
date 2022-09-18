package ru.DmN.lj.ide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

public class Main extends JFrame implements WindowListenerImpl {
    public static final List<Component> ALL_COMPONENTS = new ArrayList<>();
    public static Main INSTANCE;

    private Main() throws ClassNotFoundException {
        INSTANCE = this;

        this.setJMenuBar(new MenuBar());
        new CodeInput();

        this.addWindowListener(this);

        this.setSize(640, 360);
        this.setVisible(true);

        ALL_COMPONENTS.add(this.getContentPane());
        ALL_COMPONENTS.add(this);

        Class.forName("ru.DmN.lj.ide.Debugger", true, Main.class.getClassLoader());

        Settings.changeTheme();
    }

    public static void main(String[] args) throws ClassNotFoundException {
        new Main();
    }

    @Override
    public void windowOpened(WindowEvent e) {
        Settings.load();
    }

    @Override
    public void windowClosing(WindowEvent e) {
        for (var component : ALL_COMPONENTS)
            if (component instanceof Window frame)
                frame.dispose();
        Settings.save();
    }
}
