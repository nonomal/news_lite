package gui.buttons;

import lombok.Getter;

import javax.swing.*;
import java.awt.*;

@Getter
public abstract class iconButtonFactory {
    protected ImageIcon icon;
    protected Color color;
    protected int x;
    protected int y;
    protected static int width = 30;
    protected static int  height = 22;

    public iconButtonFactory(ImageIcon icon, int x, int y) {
        this.icon = icon;
        this.x = x;
        this.y = y;
    }

    public abstract void buttonSetting(JButton button, String tooltipText);
}
