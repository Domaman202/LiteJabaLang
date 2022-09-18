package ru.DmN.lj.ide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.*;

import static ru.DmN.lj.ide.Main.ALL_COMPONENTS;

public class Settings extends JFrame implements WindowListenerImpl {
    public static final Settings INSTANCE = new Settings();
    public static boolean autoCheckstyle;
    public static boolean darkTheme;

    public static void changeTheme() {
        var back = darkTheme ? Color.DARK_GRAY : Color.WHITE;
        var front = darkTheme ? Color.WHITE : Color.DARK_GRAY;
        ALL_COMPONENTS.forEach(c -> {
            c.setBackground(back);
            c.setForeground(front);
        });
        CodeInput.Document.checkStyle();
    }

    public Settings() {
        this.addWindowListener(this);

        this.add(DarkThemeButton.INSTANCE);

        this.pack();

        ALL_COMPONENTS.add(DarkThemeButton.INSTANCE);
        ALL_COMPONENTS.add(this.getContentPane());
        ALL_COMPONENTS.add(this);
    }

    public static void save() {
        try (var stream = new ObjectOutputStream(new FileOutputStream("lj.settings"))) {
            stream.writeBoolean(autoCheckstyle);
            stream.writeBoolean(darkTheme);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void load() {
        try (var stream = new ObjectInputStream(new FileInputStream("lj.settings"))) {
            MenuBar.AutoStyleCheck.INSTANCE.state = autoCheckstyle = stream.readBoolean();
            DarkThemeButton.INSTANCE.getModel().setSelected(darkTheme = stream.readBoolean());
            Settings.changeTheme();
        } catch (FileNotFoundException ignored) {
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void windowClosing(WindowEvent e) {
        this.setVisible(false);
        save();
    }

    public static class DarkThemeButton extends JRadioButton implements ActionListener {
        public static final DarkThemeButton INSTANCE = new DarkThemeButton();

        protected DarkThemeButton() {
            super("Dark Theme");
            this.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            darkTheme = this.model.isSelected();
            changeTheme();
        }
    }
}
