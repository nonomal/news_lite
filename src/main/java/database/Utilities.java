package database;

import gui.Gui;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.sql.*;

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
        int maxIdInSource = 0;
        PreparedStatement maxIdSt = connection.prepareStatement("SELECT MAX(ID) AS ID FROM RSS_LIST");

        ResultSet rs = maxIdSt.executeQuery();
        while (rs.next()) {
            maxIdInSource = rs.getInt("ID");
        }
        rs.close();
        maxIdSt.close();
        return maxIdInSource + 1;
    }
}
