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
    public void shouldGetExcludedListIncludeMoreThanNull() {
        List<Excluded> excluded = jdbcQueries.getExcludedWords();
        Assertions.assertTrue(excluded.size() > 0);
    }

    @Test
    public void shouldGetRandomWords() {
        List<String> excluded = jdbcQueries.getRandomWords();
        Assertions.assertTrue(excluded.size() > 0);
    }

    @Test
    public void shouldGetLink() {
        String link = jdbcQueries.getLinkOrDescribeByHash("Эксперт",
                "Премьером Великобритании станет Риши Сунак", "link");
        Assertions.assertEquals("https://expert.ru/2022/10/24/premerom-velikobritanii-stanet-rishi-sunak/" +
                "?utm_source=mis&utm_medium=vk&utm_campaign=rss&utm_term=/2022/10/24/" +
                "premerom-velikobritanii-stanet-rishi-sunak/", link);
    }

    @Test
    public void shouldGetDescribe() {
        String hash = jdbcQueries.getLinkOrDescribeByHash("Эксперт",
                "Премьером Великобритании станет Риши Сунак", "describe");
        Assertions.assertEquals("Лидер Палаты общин британского парламента Пенни Мордонт отказалась от борьбы за пост", hash);
    }
}