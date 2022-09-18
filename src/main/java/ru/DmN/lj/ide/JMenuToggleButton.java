package ru.DmN.lj.ide;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class JMenuToggleButton extends JMenuItem implements ActionListener {
    public boolean state;

    public JMenuToggleButton(String name) {
        super(name);
        this.addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        state = !state;
        this.setBackground(this.getBackground());
    }

    @Override
    public void setBackground(Color bg) {
        super.setBackground(new Color(127, state ? 255 : 0, state ? 127 : 64));
    }
}
