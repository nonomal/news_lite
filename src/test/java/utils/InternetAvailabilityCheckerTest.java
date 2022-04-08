package utils;

import main.Main;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class InternetAvailabilityCheckerTest {

    @Test
    void shouldToCheckInternetAvailableTest() throws IOException {
        Socket socket = new Socket();
        String hostName = "google.com";
        int port = 80;
        if (InternetAvailabilityChecker.isInternetAvailable()) {
            assertDoesNotThrow(() -> socket.connect(new InetSocketAddress(hostName, port), 3000),
                    "Выброшено исключение что нет подключения к интернету");
        } else {
            assertThrows(UnknownHostException.class,
                    () -> socket.connect(new InetSocketAddress(hostName, port), 3000),
                    "Не выброшено исключение о том, что нет подключения к интернету");
        }
    }

    @Test
    public void shouldOpenSQLiteConnectionTest() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + Main.directoryPath + "news.db");
            assertFalse(connection.isClosed());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}
