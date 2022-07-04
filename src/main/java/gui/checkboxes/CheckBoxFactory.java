package gui.checkboxes;

import lombok.Getter;

import java.awt.*;

@Getter
public abstract class CheckBoxFactory {
    protected Font font;
    protected int x;
    protected int y;
    protected int width;
    protected static int height = 20;

    public CheckBoxFactory(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    public abstract void checkBoxSetting(Checkbox checkbox);
}
