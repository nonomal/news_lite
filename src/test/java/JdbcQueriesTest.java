import database.JdbcQueries;
import database.SQLite;
import model.Excluded;
import model.Favorite;
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
    public void shouldGetFavorites() {
        List<Favorite> excluded = jdbcQueries.getFavorites();
        Assertions.assertTrue(excluded.size() > 0);
    }

    @Test
    public void shouldGetLink() {
        String link = jdbcQueries.getLinkOrDescribeByHash("C-Main",
                "Selectel заключил соглашение с правительством Ленобласти о развитии ИТ-инфраструктуры региона", "link");
        Assertions.assertEquals("https://www.cnews.ru/news/line/2022-10-06_selectel_zaklyuchil_soglashenie", link);
    }

    @Test
    public void shouldGetDescribe() {
        String hash = jdbcQueries.getLinkOrDescribeByHash("C-Main",
                "Selectel заключил соглашение с правительством Ленобласти о развитии ИТ-инфраструктуры региона", "describe");
        Assertions.assertEquals("Провайдер ИТ-инфраструктуры Selectel заключил соглашение с правительством " +
                "Ленинградской области о сотрудничестве...", hash);
    }
}