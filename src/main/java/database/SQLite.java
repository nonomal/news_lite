package database;

import lombok.extern.slf4j.Slf4j;
import utils.Common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Slf4j
public class SQLite {
    public static Connection connection;
    public static boolean isConnectionToSQLite;

    public void openConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + Common.DIRECTORY_PATH + "news.db");
            isConnectionToSQLite = true;
            log.info("Connected to SQLite");
        } catch (Exception e) {
            log.error("Connection open failed:\n" + e.getMessage());
        }
    }

    public void closeConnection() {
        try {
            if (isConnectionToSQLite) {
                connection.close();
                log.info("Connection closed");
            }
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
