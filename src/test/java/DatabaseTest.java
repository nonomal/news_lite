import database.JdbcQueries;
import database.SQLite;
import exception.IncorrectEmail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.Common;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseTest {
    private SQLite sqlite;

    @BeforeEach
    public void init() throws SQLException {
        sqlite = new SQLite();
        sqlite.openConnection();
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + Common.DIRECTORY_PATH + "news.db");
        new JdbcQueries().deleteFromTable("TITLES", connection);
    }

    @AfterEach
    public void after() {
        sqlite.closeConnection();
    }

    @Test
    public void shouldFindAndSendResultsToEmail() throws IOException {
        String[] args = {"rps_project@mail.ru", "30", "а", "е"};
        Main.main(args);
    }

    @Test
    public void shouldThrowIncorrectEmailException() {
        String[] args = {"rps_mail.ru", "30", "а", "е"};

        IncorrectEmail ex = assertThrows(IncorrectEmail.class, () -> Main.main(args));
        assertEquals("incorrect e-mail", ex.getMessage());
    }

    @Test
    public void shouldOpenSQLiteConnectionTest() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + Common.DIRECTORY_PATH + "news.db");
            assertFalse(connection.isClosed());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
