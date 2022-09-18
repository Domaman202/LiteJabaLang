package ru.DmN.lj.ide;

import javax.swing.*;
import java.awt.event.ActionListener;

public abstract class JMenuButton extends JMenuItem implements ActionListener {
    public JMenuButton(String text) {
        super(text);
        this.addActionListener(this);
    }
}
