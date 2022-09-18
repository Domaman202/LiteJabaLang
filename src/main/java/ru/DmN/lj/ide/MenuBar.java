package ru.DmN.lj.ide;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static ru.DmN.lj.ide.Main.ALL_COMPONENTS;

public class MenuBar extends JMenuBar {
    protected JMenu file = new JMenu("File");
    protected JMenu code = new JMenu("Code");

    public MenuBar() {
        this.add(file);
        this.add(code);

        var load = new LoadButton();
        this.file.add(load);

        var save = new SaveButton();
        this.file.add(save);

        var settings = new SettingsMenu();
        this.file.add(settings);

        var styleCheck = new StyleCheck();
        this.code.add(styleCheck);

        this.code.add(AutoStyleCheck.INSTANCE);

        var debug = new Debug();
        this.code.add(debug);

        ALL_COMPONENTS.add(this);
        ALL_COMPONENTS.add(file);
        ALL_COMPONENTS.add(code);
        ALL_COMPONENTS.add(load);
        ALL_COMPONENTS.add(save);
        ALL_COMPONENTS.add(settings);
        ALL_COMPONENTS.add(styleCheck);
        ALL_COMPONENTS.add(AutoStyleCheck.INSTANCE);
        ALL_COMPONENTS.add(debug);
    }

    public static class Debug extends JMenuItem implements ActionListener {
        public Debug() {
            super("Debug");
            this.addActionListener(this);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Debugger.open();
        }
    }

    public static class AutoStyleCheck extends JMenuToggleButton {
        public static final AutoStyleCheck INSTANCE = new AutoStyleCheck();

        public AutoStyleCheck() {
            super("Auto check style");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            Settings.autoCheckstyle = this.state;
            if (Settings.autoCheckstyle)
                CodeInput.Document.checkStyle();
        }
    }

    public static class StyleCheck extends JMenuButton {
        public StyleCheck() {
            super("Check style");
            this.setAccelerator(KeyStroke.getKeyStroke('G', InputEvent.CTRL_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            CodeInput.Document.checkStyle();
        }
    }

    public static class SettingsMenu extends JMenuButton {
        public SettingsMenu() {
            super("Settings");
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            Settings.INSTANCE.setVisible(!Settings.INSTANCE.isVisible());
        }
    }

    public static class SaveButton extends JMenuButton {
        public SaveButton() {
            super("Save");
            this.setAccelerator(KeyStroke.getKeyStroke('S', InputEvent.CTRL_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            var chooser = new JFileChooser();
            chooser.setDialogTitle("Specify a file to save");
            chooser.setName("code.lj");
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (var file = new FileOutputStream(chooser.getSelectedFile())) {
                    file.write(CodeInput.INSTANCE.getText().getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static class LoadButton extends JMenuButton {
        public LoadButton() {
            super("Load");
            this.setAccelerator(KeyStroke.getKeyStroke('O', InputEvent.CTRL_DOWN_MASK));
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            var chooser = new JFileChooser();
            chooser.setDialogTitle("Specify a file to open");
            chooser.setName("code.lj");
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try (var file = new FileInputStream(chooser.getSelectedFile())) {
                    CodeInput.INSTANCE.setText(new String(file.readAllBytes()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
