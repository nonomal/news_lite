package database;

import gui.Gui;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
public class Utilities {

    public RSSInfoFromUI getRssInfoFromUi() {
        JTextField sourceName = new JTextField();
        JTextField rssLink = new JTextField();
        Object[] newSource = {"Source:", sourceName, "Link to rss:", rssLink};
        int result = JOptionPane.showConfirmDialog(Gui.scrollPane, newSource,
                "New source", JOptionPane.OK_CANCEL_OPTION);
        return new RSSInfoFromUI(sourceName, rssLink, result);
    }

    public int getNextMaxId(Connection connection) throws SQLException {
        Statement maxIdSt = connection.createStatement();
        ResultSet rs = maxIdSt.executeQuery("SELECT MAX(ID) AS ID FROM RSS_LIST");
        int maxIdInSource = 0;
        while (rs.next()) {
            maxIdInSource = rs.getInt("ID");
        }
        rs.close();
        maxIdSt.close();
        return maxIdInSource + 1;
    }
}
