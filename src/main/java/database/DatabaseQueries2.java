package database;

import gui.Gui;
import model.ExcludeWord;
import model.RssSource;
import model.VNewsDual;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import utils.Common;

import javax.swing.*;
import java.util.List;

public class DatabaseQueries2 {
    private static final int WORD_FREQ_MATCHES = 2;
    ApplicationContext context = new ClassPathXmlApplicationContext("context.xml");
    private final JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

//    public static void main(String[] args) {
//        System.out.println(new DatabaseQueries2().getRssList().get(1).getSource());
//    }


    // вставка кода по заголовку для отсеивания ранее обнаруженных новостей TODO
//    public void insertTitleIn256(String pTitle) {
//        jdbcTemplate.update("INSERT INTO titles256(title) VALUES (?)", pTitle);
//    }

    // сохранение всех заголовков  TODO
//    public void insertAllTitles(String pTitle, String pDate) {
//        String query = "INSERT INTO ALL_NEWS(TITLE, NEWS_DATE) VALUES (?, ?)";
//        jdbcTemplate.update(query, pTitle, pDate);
//    }

    // запись данных по актуальным источникам из базы в массивы для поиска
    public void selectSources(@NotNull String pDialog) {
        switch (pDialog) {
            case "smi":
                //sources
                Common.SMI_SOURCE.clear();
                Common.SMI_LINK.clear();

                String query = "SELECT ID, SOURCE, LINK, IS_ACTIVE FROM RSS_LIST WHERE IS_ACTIVE = 1  ORDER BY ID";
                List<RssSource> rssItems = jdbcTemplate.query(query, new BeanPropertyRowMapper<>(RssSource.class));

                for (RssSource rss : rssItems) {
                    Common.SMI_SOURCE.add(rss.getSource());
                    Common.SMI_LINK.add(rss.getLink());
                }
                break;
            case "excl":
                //excluded words
                Common.EXCLUDED_WORDS.clear();
                String excludedQuery = "SELECT WORD FROM EXCLUDE";
                List<ExcludeWord> excludedWords = jdbcTemplate.query(excludedQuery, new BeanPropertyRowMapper<>(ExcludeWord.class));

                for (ExcludeWord excludedWord : excludedWords) {
                    Common.EXCLUDED_WORDS.add(excludedWord.getWord());
                }
                break;
            case "active_smi":
                Common.SMI_SOURCE.clear();
                Common.SMI_LINK.clear();
                Common.SMI_IS_ACTIVE.clear();

                String queryRss = "SELECT ID, SOURCE, LINK, IS_ACTIVE FROM RSS_LIST ORDER BY ID";
                List<RssSource> rssListItems = jdbcTemplate.query(queryRss, new BeanPropertyRowMapper<>(RssSource.class));

                for (RssSource rss : rssListItems) {
                    Common.SMI_SOURCE.add(rss.getSource());
                    Common.SMI_LINK.add(rss.getLink());
                    Common.SMI_IS_ACTIVE.add(rss.getIsActive());
                }
                break;
        }
    }

    // Количество новостей в архиве
    public Integer archiveNewsCount() {
        String query = "SELECT COUNT(*) FROM ALL_NEWS";
        return jdbcTemplate.queryForObject(query, Integer.class);
    }

    public List<RssSource> getRssList() {
        String query = "SELECT * FROM RSS_LIST";
        return jdbcTemplate.query(query, new BeanPropertyRowMapper<>(RssSource.class));
    }

    // Получить id для нового источника
    public Integer getNewRssId() {
        String query = "SELECT MAX(ID) + 1 AS ID FROM RSS_LIST";
        return jdbcTemplate.queryForObject(query, Integer.class);
    }

    // вставка нового источника
    public void insertNewSource() {
        // Диалоговое окно добавления источника новостей в базу данных
        JTextField rss = new JTextField();
        JTextField link = new JTextField();
        Object[] newSource = {"Source:", rss, "Link to rss:", link};
        int result = JOptionPane.showConfirmDialog(Gui.scrollPane, newSource,
                "New source", JOptionPane.OK_CANCEL_OPTION);

        // вставка нового источника
        if (result == JOptionPane.YES_OPTION) {
            String query = "INSERT INTO rss_list(id, source, link, is_active) VALUES (?, ?, ?, ?)";
            jdbcTemplate.update(query, getNewRssId(), rss.getText(), link.getText(), 1);

            Common.console("status: source added: " + rss.getText());
        } else {
            Common.console("status: adding source canceled");
        }
    }

    // удаление источника
    public void deleteSource(String source) {
        String query = "DELETE FROM rss_list WHERE source = ?";
        jdbcTemplate.update(query, source);
    }

    // удаление слова исключенного из поиска
    public void deleteExcluded(String source) {
        String query = "DELETE FROM EXCLUDE WHERE WORD = ?";
        jdbcTemplate.update(query, source);
    }

    public void deleteFrom256() {
        jdbcTemplate.update("DELETE FROM TITLES256");
    }

    // Delete from news_dual
    public void deleteTitles() {
        jdbcTemplate.update("DELETE FROM NEWS_DUAL");
    }

    // удаляем все пустые строки
    public void deleteEmptyRows() {
        jdbcTemplate.update("DELETE FROM NEWS_DUAL WHERE TITLE = ''");
    }

    // удаление дубликатов новостей
    public void deleteDuplicates() {
        jdbcTemplate.update("DELETE FROM ALL_NEWS WHERE ROWID NOT IN (SELECT MIN(ROWID) " +
                "FROM ALL_NEWS GROUP BY TITLE, NEWS_DATE)");
    }

    // обновление статуса чекбокса is_active для ресурсов
    public void updateIsActiveStatus(boolean pBoolean, String pSource) {
        jdbcTemplate.update("UPDATE RSS_LIST SET IS_ACTIVE = ? WHERE SOURCE = ?", pBoolean, pSource);
    }

    // вставка нового слова для исключения из анализа частоты употребления слов
    public void insertNewExcludedWord(String pWord) {
        jdbcTemplate.update("INSERT INTO exclude(word) VALUES (?)", pWord);
        Common.console("status: word \"" + pWord + "\" excluded from analysis");
    }

    // Заполнение таблицы анализа
    public void selectSqlite() {
        String query = "SELECT SUM, TITLE FROM V_NEWS_DUAL WHERE SUM > ? " +
                "AND TITLE NOT IN (SELECT WORD FROM ALL_TITLES_TO_EXCLUDE) ORDER BY SUM DESC";
        List<VNewsDual> newsTitles = jdbcTemplate.query(query, new BeanPropertyRowMapper<>(VNewsDual.class), WORD_FREQ_MATCHES);

        for (VNewsDual title : newsTitles) {
            Object[] row = new Object[]{title.getTitle(), title.getSum()};
            Gui.modelForAnalysis.addRow(row);
        }
        deleteTitles();
    }

}
