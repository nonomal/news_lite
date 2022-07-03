package database;

import lombok.extern.slf4j.Slf4j;
import main.Main;

import java.sql.Connection;
import java.sql.DriverManager;

@Slf4j
public class SQLite {
    public static Connection connection;
    public static boolean isConnectionToSQLite;

    // Открытие соединения с базой данных
    public void openSQLiteConnection() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + Main.DIRECTORY_PATH + "news.db");
            isConnectionToSQLite = true;
            log.info("Connected to SQLite");
            Thread.sleep(1000L);
        } catch (Exception e) {
            log.error("Connection open failed:\n" + e.getMessage());
        }
    }

    // закрытие соединения DatabaseQueries
    public void closeSQLiteConnection() {
        try {
            if (isConnectionToSQLite) {
                connection.close();
                log.info("Connection closed");
            }
        } catch (Exception e) {
            log.error("Connection closed failed:\n" + e.getMessage());
        }
    }
}
