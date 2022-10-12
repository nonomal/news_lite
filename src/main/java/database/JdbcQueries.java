package database;

import gui.Gui;
import model.*;
import utils.Common;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcQueries {
    private static final int WORD_FREQ_MATCHES = 2;
    private final Connection connection = SQLite.connection;

    /* INSERT */
    // Вставка заголовков разбитых на слова
    public void addCutTitlesForAnalysis(String title) {
        try {
            String query = "INSERT INTO NEWS_DUAL(TITLE) VALUES (?)";
            PreparedStatement statement = connection.prepareStatement(query);
            String[] substr = title.split(" ");

            for (String s : substr) {
                if (s.length() > 3) {
                    statement.setString(1, s);
                    statement.executeUpdate();
                }
            }
            statement.close();
        } catch (Exception e) {
            Common.console("addCutTitlesForAnalysis error: " + e.getMessage());
        }
    }

    // Вставка ключевого слова
    public void addKeyword(String word) {
        try {
            String query = "INSERT INTO keywords(word) VALUES (?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, word);
            statement.executeUpdate();
            statement.close();

            Common.console("\"" + word + "\" добавлено в список ключевых слов");
        } catch (Exception e) {
            Common.console("addKeyword error: " + e.getMessage());
        }
    }

    // Вставка нового источника
    public void addNewSource(String source, String link) {
        try {
            //if (result == JOptionPane.YES_OPTION) {
            String query = "INSERT INTO rss_list(source, link, is_active) VALUES (?, ?, 1)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, source);
            statement.setString(2, link);
            statement.executeUpdate();
            statement.close();

            Common.console("source added");
        } catch (Exception e) {
            Common.console("addNewSource error: " + e.getMessage());
        }
    }

    // Вставка слова для исключения из анализа частоты употребления слов
    public void addExcludedWord(String word) {
        try {
            String query = "INSERT INTO exclude(word) VALUES (?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, word);
            statement.executeUpdate();
            statement.close();

            Common.console("\"" + word + "\" excluded from analysis");
        } catch (Exception e) {
            Common.console("addExcludedWord error: " + e.getMessage());
        }
    }

    // Вставка избранных заголовков
    public void addFavoriteTitle(String title, String link) {
        try {
            String query = "INSERT INTO favorites(title, link) VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, title);
            statement.setString(2, link);
            statement.executeUpdate();
            statement.close();

            Common.console("title added to favorites");
        } catch (Exception e) {
            Common.console("addFavoriteTitles error: " + e.getMessage());
        }
    }

    // вставка кода по заголовку для отсеивания ранее обнаруженных новостей
    public void addTitles(String title, String type) {
        try {
            String query = "INSERT INTO titles(title, type) VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, title);
            statement.setString(2, type);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            Common.console("addTitles error: " + e.getMessage());
        }
    }

    // сохранение всех заголовков в архив
    public void addAllTitlesToArchive(String title, String date, String link, String source, String describe) {
        try {
            String query = "INSERT INTO all_news(title, news_date, link, source, hash, describe) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, title);
            statement.setString(2, date);
            statement.setString(3, link);
            statement.setString(4, source);
            statement.setString(5, Common.getHash(source + title));
            statement.setString(6, describe);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            Common.console("addAllTitlesToArchive error: " + e.getMessage());
        }
    }

    // вставка слова для исключения содержащих его заголовков
    public void addWordToExcludeTitles(String word) {
        if (word != null && word.length() > 0) {
            try {
                String query = "INSERT INTO excluded_headlines(word) VALUES (?)";
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setString(1, word);
                statement.executeUpdate();
                statement.close();

                Common.console("\"" + word + "\" excluded from search");
            } catch (Exception e) {
                Common.console("addWordToExcludeTitles error: " + e.getMessage());
            }
        }
    }

    // вставка нового события
    public void addDate(String type, String description, int day, int month, int year) {
        try {
            String query = "INSERT INTO dates(type, description, day, month, year) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, type);
            statement.setString(2, description);
            statement.setInt(3, day);
            statement.setInt(4, month);
            statement.setInt(5, year);
            statement.executeUpdate();
            statement.close();

            Common.console("Событие добавлено: " + type + " " + description);
        } catch (Exception e) {
            Common.console("addDate error: " + e.getMessage());
        }
    }

    /* SELECT */
    // Источники новостей
    public List<Source> getSources(String type) {
        List<Source> sources = new ArrayList<>();
        String query = "SELECT id, source, link, is_active, position FROM rss_list " +
                "WHERE is_active = 1 ORDER BY position";

        if (type.equals("all")) {
            query = "SELECT id, source, link, is_active, position FROM rss_list " +
                    "ORDER BY is_active DESC, id";
        }

        try {
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                sources.add(Source.builder()
                        .id(rs.getInt("id"))
                        .source(rs.getString("source"))
                        .link(rs.getString("link"))
                        .isActive(rs.getBoolean("is_active"))
                        .position(rs.getInt("position"))
                        .build());
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("getSources error: " + e.getMessage());
        }
        return sources;
    }

    // Список исключённые из анализа слова
    public List<Excluded> getExcludedWords() {
        List<Excluded> excludedWords = new ArrayList<>();
        try {
            String query = "SELECT id, word FROM exclude ORDER BY word";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                excludedWords.add(new Excluded(
                        rs.getInt("id"),
                        rs.getString("word")));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("getExcludedWords error: " + e.getMessage());
        }
        return excludedWords;
    }

    // Список значимых дат
    public List<Dates> getDates() {
        List<Dates> dates = new ArrayList<>();
        try {
            String query = "SELECT type, description, day, month, year FROM dates ORDER BY month, day";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                dates.add(new Dates(
                        rs.getString("type"),
                        rs.getString("description"),
                        rs.getInt("day"),
                        rs.getInt("month"),
                        rs.getInt("year")
                ));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("getExcludedWords error: " + e.getMessage());
        }
        return dates;
    }

    // Список исключённые из поиска слов
    public List<Excluded> getExcludedTitlesWords() {
        List<Excluded> excludedWords = new ArrayList<>();
        try {
            String query = "SELECT id, word FROM excluded_headlines ORDER BY id";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                excludedWords.add(new Excluded(rs.getInt("id"), rs.getString("word")));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("getExcludedTitlesWords error: " + e.getMessage());
        }
        return excludedWords;
    }

    // Список ключевых слов для поиска (0 - не активные, 1 - активные, 2 - все)
    public List<Keyword> getKeywords(int isActive) {
        List<Keyword> keywords = new ArrayList<>();
        try {
            String query = "SELECT word, is_active FROM keywords ORDER BY is_active DESC, word";

            if (isActive == 0 || isActive == 1) {
                query = "SELECT word, is_active FROM keywords WHERE is_active = " + isActive + " ORDER BY word";
            }
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                Keyword keyword = new Keyword(
                        rs.getString("word"),
                        rs.getBoolean("is_active")
                );
                keywords.add(keyword);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("getKeywords error: " + e.getMessage());
        }
        return keywords;
    }

    // Проверка наличия ключевого слова
    public boolean isKeywordExists(String word) {
        int isExists = 0;
        try {
            String query = "SELECT MAX(1) FROM keywords WHERE exists (select word from keywords where word = ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, word);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                isExists = rs.getInt(1);
            }

            statement.close();
        } catch (Exception e) {
            Common.console("isKeywordExists error: " + e.getMessage());
        }
        return isExists == 1;
    }

    // Список избранных новостей TODO добавить проверку на дубли
    public List<Favorite> getFavorites() {
        List<Favorite> favorites = new ArrayList<>();
        try {
            String query = "SELECT title, link, add_date FROM favorites ORDER BY add_date";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                favorites.add(new Favorite(
                                rs.getString("title"),
                                rs.getString("link"),
                                rs.getString("add_date")
                        )
                );
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("getExcludedTitlesWords error: " + e.getMessage());
        }
        return favorites;
    }

    // Список слов с переводом
    public List<String> getRandomWords() {
        List<String> words = new ArrayList<>();
        try {
            String query = "SELECT lang1||' - '||lang2 words FROM dictionary WHERE is_active = 1";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                words.add(rs.getString("words"));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("getRandomWord error: " + e.getMessage());
        }
        return words;
    }

    // Link, Describe by hash code
    public String getLinkOrDescribeByHash(String source, String title, String type) {
        String response = "no data found";
        String query = null;
        try {
            if (type.equals("link")) {
                query = "SELECT link FROM all_news WHERE hash = ?";
            } else if (type.equals("describe")) {
                query = "SELECT describe FROM all_news WHERE hash = ?";
            }

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, Common.getHash(source + title));

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                response = rs.getString(type);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("getLinkByHash error: " + e.getMessage());
        }
        return response;
    }

    /* REMOVE */
    // удаление слов из разных таблиц
    public void removeItem(String item, int activeWindow) {
        try {
            String query = null;
            if (activeWindow == 2) {
                query = "DELETE FROM rss_list WHERE source = ?";
            } else if (activeWindow == 3) {
                query = "DELETE FROM exclude WHERE word = ?";
            } else if (activeWindow == 4) {
                query = "DELETE FROM excluded_headlines WHERE word = ?";
            } else if (activeWindow == 5) {
                query = "DELETE FROM keywords WHERE word = ?";
            } else if (activeWindow == 6) {
                query = "DELETE FROM favorites WHERE title = ?";
            } else if (activeWindow == 7) {
                query = "DELETE FROM dates WHERE type||' '||description = ?";
            }

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, item);
            statement.executeUpdate();
            statement.close();

            Common.console("Удалён элемент: " + item);
        } catch (Exception e) {
            Common.console("removeItem error: " + e.getMessage());
        }

    }

    // удаление дубликатов новостей
    public void removeDuplicates() {
        try {
            String query = "DELETE FROM all_news WHERE ROWID NOT IN (SELECT MIN(ROWID) " +
                    "FROM all_news GROUP BY source, title, news_date)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.console("deleteDuplicates error: " + e.getMessage());
        }
    }

    // удаляем все пустые строки
    public void removeEmptyRows() {
        try {
            String query = "DELETE FROM NEWS_DUAL WHERE TITLE = ''";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.console("deleteEmptyRows error: " + e.getMessage());
        }
    }

    // Очистка данных любой передаваемой таблицы
    public void removeFromTable(String tableName) {
        try {
            String query = "DELETE FROM " + tableName;
            PreparedStatement statement = connection.prepareStatement(query);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.console("deleteFromTable error: " + e.getMessage());
        }
    }

    /* DIFFERENT */
    // отсеивание ранее найденных заголовков при включённом чекбоксе
    public boolean isTitleExists(String title, String type) {
        int isExists = 0;
        try {
            String query = "SELECT MAX(1) FROM titles " +
                    "WHERE EXISTS (SELECT title FROM titles t WHERE t.title = ? AND t.type = ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, title);
            statement.setString(2, type);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                isExists = rs.getInt(1);
            }
            rs.close();
            statement.close();

        } catch (Exception e) {
            Common.console("isTitleExists error: " + e.getMessage());
        }
        return isExists == 1;
    }

    // отсеивание ненужных заголовков
    public List<String> excludedTitles() {
        List<String> titles = new ArrayList<>();

        try {
            String query = "SELECT word FROM excluded_headlines";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                titles.add(rs.getString(1).toLowerCase());
            }
            rs.close();
            statement.close();

        } catch (Exception e) {
            Common.console("excludedTitles error: " + e.getMessage());
        }
        return titles;
    }

    // Заполнение таблицы анализа
    public void setAnalysis() {
        try {
            String query = "SELECT SUM, TITLE FROM V_NEWS_DUAL WHERE SUM > ? " +
                    "AND TITLE NOT IN (SELECT WORD FROM ALL_TITLES_TO_EXCLUDE) " +
                    "ORDER BY SUM DESC";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, WORD_FREQ_MATCHES);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String word = rs.getString("TITLE");
                int sum = rs.getInt("SUM");
                Object[] row = new Object[]{word, sum};
                Gui.modelForAnalysis.addRow(row);
            }
            removeFromTable("NEWS_DUAL");
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("setAnalysis error: " + e.getMessage());
        }
    }

    // новостей в архиве всего
    public int archiveNewsCount() {
        int countNews = 0;
        try {
            String query = "SELECT COUNT(*) FROM ALL_NEWS";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                countNews = rs.getInt(1);
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("archiveNewsCount error: " + e.getMessage());
        }
        return countNews;
    }

    // обновление статуса чекбокса is_active для ресурсов SELECT id, source, link FROM rss_list where is_active = 1  ORDER BY id
    public void updateIsActiveCheckboxes(boolean check, String name, String type) {
        String query = null;
        try {
            if (type.equals("rss")) {
                query = "UPDATE rss_list SET is_active = ? WHERE source = ?";
            } else if (type.equals("keywords")) {
                query = "UPDATE keywords SET is_active = ? WHERE word = ?";
            }

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setBoolean(1, check);
            statement.setString(2, name);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.console("updateIsActiveStatus error: " + e.getMessage());
        }
    }
}
