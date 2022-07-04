package gui.checkboxes;

import java.awt.*;

public class SetCheckbox extends CheckBoxFactory {

    public SetCheckbox(int x, int y, int w) {
        super(x, y, w);
    }

    @Override
    public void checkBoxSetting(Checkbox checkbox) {
        checkbox.setState(false);
        checkbox.setFocusable(false);
        checkbox.setForeground(Color.WHITE);
        checkbox.setBounds(x, y, width, height);
        checkbox.setFont(new Font("Tahoma", Font.PLAIN, 11));
    }
}
