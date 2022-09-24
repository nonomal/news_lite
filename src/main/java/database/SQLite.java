package database;

import exception.NotConnectedToDatabase;
import lombok.extern.slf4j.Slf4j;
import utils.Common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
public class SQLite {
    public static Connection connection;

    public void openConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + Common.DIRECTORY_PATH + "news.db");
            log.info("Connected to database");
        } catch (Exception e) {
            log.error("Failed to connect to database:\n" + e.getMessage());
            throw new NotConnectedToDatabase("Failed to connect to database");
        }
    }

    public void closeConnection() {
        try {
                log.info("Connection closed");
                connection.close();
        } catch (Exception e) {
            log.error("Connection closed failed:\n" + e.getMessage());
        }
    }

    public void transaction(String command) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(command);
        statement.execute();
        statement.close();
    }
}
