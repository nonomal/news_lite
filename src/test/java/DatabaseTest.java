import database.JdbcQueries;
import database.SQLite;
import exception.IncorrectEmail;
import main.Main;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.Common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseTest {
    private SQLite sqLite;

    @BeforeEach
    public void init() {
        sqLite = new SQLite();
        sqLite.openConnection();
        new JdbcQueries().removeFromTable("TITLES");
    }

    @AfterEach
    public void after() {
        sqLite.closeConnection();
    }

    @Test
    public void shouldFindAndSendResultsToEmail() {
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
