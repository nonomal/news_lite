package database;

import lombok.Getter;

import javax.swing.*;

@Getter
public class RSSInfoFromUI {
    private final JTextField sourceName;
    private final JTextField rssLink;
    private final int result;

    public RSSInfoFromUI(JTextField rssName, JTextField rssSource, int result) {
        this.sourceName = rssName;
        this.rssLink = rssSource;
        this.result = result;
    }

}
