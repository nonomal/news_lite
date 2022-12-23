package gui.buttons;

import lombok.experimental.UtilityClass;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

@UtilityClass
public class Icons {
    public static final URL APP_IN_TRAY_BUTTON_ICON = Icons.class.getResource("/icons/in_tray.png");
    public static final ImageIcon LOGO_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/logo.png")));
    public static final ImageIcon SEND_EMAIL_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/send.png")));
    public static final ImageIcon WHEN_MOUSE_ON_SEND_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/send2.png")));
    public static final ImageIcon WHEN_SENT_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/send3.png")));
    public static final ImageIcon SEARCH_KEYWORDS_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/search.png")));
    public static final ImageIcon STOP_SEARCH_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/stop.png")));
    public static final ImageIcon DELETE_UNIT = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/exit.png")));
    public static final ImageIcon EXIT_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/exit.png")));
    public static final ImageIcon WHEN_MOUSE_ON_EXIT_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/exit2.png")));
    public static final ImageIcon HIDE_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/hide.png")));
    public static final ImageIcon WHEN_MOUSE_ON_HIDE_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/hide2.png")));
    public static final ImageIcon LIST_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/list.png")));
    public static final ImageIcon WHEN_MOUSE_ON_LIST_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/list2.png")));
    public static final ImageIcon GITHUB_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/github.png")));
    public static final ImageIcon TRANSLATOR_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/translator.png")));
    public static final ImageIcon MAPS_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/maps.png")));
    public static final ImageIcon TRADING_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/trading.png")));
    public static final ImageIcon SETTINGS_BUTTON_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/settings.png")));
    public static final ImageIcon SETTINGS_EXPORT_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/excel.png")));
    public static final ImageIcon SETTINGS_COPY_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/copy.png")));
    public static final ImageIcon SETTINGS_DESCRIBE_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/describe.png")));
    public static final ImageIcon SETTINGS_TRANSLATE_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/translate.png")));
    public static final ImageIcon STAR_ICON = new ImageIcon(Toolkit.getDefaultToolkit()
            .createImage(Icons.class.getResource("/icons/star.png")));
}
