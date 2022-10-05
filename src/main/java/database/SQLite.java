package database;

import exception.NotConnectedToDatabase;
import utils.Common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SQLite {
    public static Connection connection;

    public void openConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            // config.txt
            // default  db_path=C:\Users\User\News\news.db
            // work     db_path=C:\Users\User\Seafile\Seafile\files\java\news\news.db
            // home     db_path=
            connection = DriverManager.getConnection("jdbc:sqlite:" + Common.getPathToDatabase());
        } catch (Exception e) {
            throw new NotConnectedToDatabase("Failed to connect to database");
        }
    }

    public void closeConnection() {
        try {
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void transaction(String command) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(command);
        statement.execute();
        statement.close();
    }
}
