package gui.buttons;

import gui.Gui;
import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

@UtilityClass
public class Icons {
    public static final ImageIcon LOGO_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Gui.class.getResource("/icons/logo.png")));
    public static final ImageIcon SEND_EMAIL_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Gui.class.getResource("/icons/send.png")));
    public static final ImageIcon WHEN_MOUSE_ON_SEND_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Gui.class.getResource("/icons/send2.png")));
    public static final ImageIcon WHEN_SENT_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Gui.class.getResource("/icons/send3.png")));
    public static final ImageIcon SEARCH_KEYWORDS_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Gui.class.getResource("/icons/search.png")));
    public static final ImageIcon STOP_SEARCH_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Gui.class.getResource("/icons/stop.png")));
    public static final ImageIcon CLEAR_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Gui.class.getResource("/icons/clear.png")));
    public static final ImageIcon EXCEL_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Gui.class.getResource("/icons/excel.png")));
    public static final ImageIcon ADD_KEYWORD_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Gui.class.getResource("/icons/create.png")));
    public static final ImageIcon DELETE_FROM_KEYWORDS_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Gui.class.getResource("/icons/delete.png")));
    public static final ImageIcon FONT_COLOR_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Gui.class.getResource("/icons/font.png")));
    public static final ImageIcon BACK_GROUND_COLOR_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Gui.class.getResource("/icons/bg.png")));
    public static final ImageIcon EXIT_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Gui.class.getResource("/icons/exit.png")));
    public static final ImageIcon WHEN_MOUSE_ON_EXIT_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Gui.class.getResource("/icons/exit2.png")));
    public static final ImageIcon TRAY_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Gui.class.getResource("/icons/tray.png")));
    public static final ImageIcon WHEN_MOUSE_ON_TRAY_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Gui.class.getResource("/icons/tray2.png")));
    public static final URL APP_IN_TRAY_BUTTON_ICON = Gui.class.getResource("/icons/tray3.png");
}
