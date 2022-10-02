import database.JdbcQueries;
import database.SQLite;
import model.Excluded;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class JdbcQueriesTest {
    private SQLite sqLite;
    private JdbcQueries jdbcQueries;

    @BeforeEach
    public void init() {
        sqLite = new SQLite();
        sqLite.openConnection();
        jdbcQueries = new JdbcQueries();
    }

    @AfterEach
    public void after() {
        sqLite.closeConnection();
    }

    @Test
    public void shouldExcludedListIncludeMoreThanNull() {
        List<Excluded> excluded = jdbcQueries.getExcludedWords();
        Assertions.assertTrue(excluded.size() > 0);
    }
}
