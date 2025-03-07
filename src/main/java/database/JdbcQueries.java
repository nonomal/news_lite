package database;

import gui.Gui;
import main.Login;
import model.*;
import utils.Common;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class JdbcQueries {
    private final Connection connection = SQLite.connection;

    /* INSERT */
    // Вставка ключевого слова
    public void addKeyword(String word) {
        try {
            String query = "INSERT INTO keywords(word, user_id) VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, word);
            statement.setInt(2, Login.userId);
            statement.executeUpdate();
            statement.close();

            Common.console("\"" + word + "\" added to keyword list");
        } catch (Exception e) {
            Common.console("addKeyword error: " + e.getMessage());
        }
    }

    // Вставка нового источника
    public void addNewSource(String source, String link) {
        if (link.contains("/") && link.contains(".")) {
            try {
                String query = "INSERT INTO rss_list(source, link, is_active, user_id) VALUES (?, ?, 1, ?)";
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setString(1, source);
                statement.setString(2, link);
                statement.setInt(3, Login.userId);
                statement.executeUpdate();
                statement.close();

                Common.console("source added");
            } catch (Exception e) {
                Common.console("addNewSource error: " + e.getMessage());
            }
        } else {
            Common.console("enter a valid URL");
        }
    }

    // Вставка слова для исключения из анализа частоты употребления слов
    public void addExcludedWord(String word) {
        try {
            String query = "INSERT INTO exclude(word, user_id) VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, word);
            statement.setInt(2, Login.userId);
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
            String query = "INSERT INTO favorites(title, link, user_id) VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, title);
            statement.setString(2, link);
            statement.setInt(3, Login.userId);
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
            if (!e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                Common.console("addAllTitlesToArchive error: " + e.getMessage());
            }
        }
    }

    // вставка слова для исключения содержащих его заголовков
    public void addWordToExcludeTitles(String word) {
        if (word != null && word.length() > 0) {
            try {
                String query = "INSERT INTO excluded_headlines(word, user_id) VALUES (?, ?)";
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setString(1, word);
                statement.setInt(2, Login.userId);
                statement.executeUpdate();
                statement.close();
                Common.console("the word \"" + word + "\" is excluded from heading search");
            } catch (SQLException ignored) {
            } catch (Exception e) {
                Common.console("addWordToExcludeTitles error: " + e.getMessage());
            }
        }
    }

    // вставка нового события
    public void addDate(String type, String description, int day, int month, int year) {
        try {
            String query = "INSERT INTO dates(type, description, day, month, year, user_id) VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, type);
            statement.setString(2, description);
            statement.setInt(3, day);
            statement.setInt(4, month);
            statement.setInt(5, year);
            statement.setInt(6, Login.userId);
            statement.executeUpdate();
            statement.close();

            Common.console("event added: " + type + " " + description);
        } catch (Exception e) {
            Common.console("addDate error: " + e.getMessage());
        }
    }

    public void addUser(String username, String password) {
        try {
            String query = "INSERT INTO users(username, password) VALUES (?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);
            statement.setString(2, password);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            if (!e.getMessage().contains("SQLITE_CONSTRAINT_UNIQUE")) {
                Common.console("addAllTitlesToArchive error: " + e.getMessage());
            }
        }
    }

    /* SELECT */
    // Источники новостей
    public List<Source> getSources(String type) {
        List<Source> sources = new ArrayList<>();
        try {
            String query = "SELECT id, source, link, is_active, position FROM rss_list " +
                    "WHERE is_active = 1 AND user_id = ? " +
                    "ORDER BY position";

            if (type.equals("all")) {
                query = "SELECT id, source, link, is_active, position FROM rss_list WHERE user_id = ? " +
                        "ORDER BY is_active DESC, id";
            } else if (type.equals("console")) {
                query = "SELECT id, source, link, is_active, position FROM rss_list WHERE user_id = ?";
            }

            PreparedStatement statement = connection.prepareStatement(query);
            if (type.equals("console")) {
                statement.setInt(1, 0);
            } else {
                statement.setInt(1, Login.userId);
            }

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

    // Настройки по ключу
    public String getSetting(String key) {
        String setting = null;
        String query = "SELECT value FROM settings WHERE key = ? AND (user_id IS NULL OR user_id = ?)";

        try {
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, key);
            statement.setInt(2, Login.userId);
            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                setting = rs.getString("value");
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("getSettings error: " + e.getMessage());
        }
        return setting;
    }

    // Список исключённые из анализа слова
    public List<Excluded> getExcludedWords() {
        List<Excluded> excludedWords = new ArrayList<>();
        try {
            String query = "SELECT id, word FROM exclude WHERE user_id = ? ORDER BY word";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, Login.userId);

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
    public List<Dates> getDates(int isActive) {
        List<Dates> dates = new ArrayList<>();
        try {
            String query = "SELECT type, description, day, month, year, is_active FROM dates " +
                    "WHERE user_id = ? " +
                    "ORDER BY month, day";

            if (isActive == 0) {
                query = "SELECT type, description, day, month, year, is_active FROM dates " +
                        "WHERE is_active = 1 AND user_id = ? ORDER BY month, day";
            }

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, Login.userId);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                dates.add(new Dates(
                        rs.getString("type"),
                        rs.getString("description"),
                        rs.getInt("day"),
                        rs.getInt("month"),
                        rs.getInt("year"),
                        rs.getBoolean("is_active")
                ));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("getDates error: " + e.getMessage());
        }
        return dates;
    }

    // Список исключённые из поиска слов
    public List<Excluded> getExcludedTitlesWords() {
        List<Excluded> excludedWords = new ArrayList<>();
        try {
            String query = "SELECT id, word FROM excluded_headlines WHERE user_id = ? ORDER BY word";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, Login.userId);

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

    public List<String> getAllUsers() {
        List<String> users = new ArrayList<>();
        try {
            String query = "SELECT username FROM users WHERE id != 0 ORDER BY id";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                users.add(rs.getString("username"));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("getAllUsers error: " + e.getMessage());
        }
        return users;
    }

    // Список ключевых слов для поиска (0 - не активные, 1 - активные, 2 - все)
    public List<Keyword> getKeywords(int isActive) {
        List<Keyword> keywords = new ArrayList<>();
        try {
            String query = "SELECT word, is_active FROM keywords WHERE user_id = ? ORDER BY is_active DESC, word";

            if (isActive == 0 || isActive == 1) {
                query = "SELECT word, is_active FROM keywords WHERE is_active = " + isActive +
                        " and user_id = ? ORDER BY word";
            }
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, Login.userId);

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
            String query = "SELECT MAX(1) FROM keywords WHERE exists (SELECT word FROM keywords " +
                    "WHERE word = ? AND user_id = ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, word);
            statement.setInt(2, Login.userId);

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

    public boolean isUserExists(String username) {
        int isExists = 0;
        try {
            String query = "SELECT MAX(1) FROM users WHERE exists (SELECT id FROM users WHERE username = ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);

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

    // Список избранных новостей
    public List<Favorite> getFavorites() {
        List<Favorite> favorites = new ArrayList<>();
        try {
            String query = "SELECT title, link, add_date FROM favorites WHERE user_id = ? ORDER BY add_date";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, Login.userId);

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

    // Список избранных новостей
    public void getNewsForTopTen(String word) {
        try {
            String query = "SELECT source, title, describe, news_date, link FROM all_news " +
                    "WHERE lower(title) like '%'|| ? ||'%' " +
                    "ORDER BY substr(news_date, 24)||" +
                    "case\n" +
                    "   when substr(news_date, 5, 3) = 'Jan' then '01'" +
                    "   when substr(news_date, 5, 3) = 'Feb' then '02'" +
                    "   when substr(news_date, 5, 3) = 'Mar' then '03'" +
                    "   when substr(news_date, 5, 3) = 'Apr' then '04'" +
                    "   when substr(news_date, 5, 3) = 'May' then '05'" +
                    "   when substr(news_date, 5, 3) = 'Jun' then '06'" +
                    "   when substr(news_date, 5, 3) = 'Jul' then '07'" +
                    "   when substr(news_date, 5, 3) = 'Aug' then '08'" +
                    "   when substr(news_date, 5, 3) = 'Sep' then '09'" +
                    "   when substr(news_date, 5, 3) = 'Oct' then '10'" +
                    "   when substr(news_date, 5, 3) = 'Nov' then '11'" +
                    "   when substr(news_date, 5, 3) = 'Dec' then '12'" +
                    "end ||" +
                    "substr(news_date, 9, 2)||" +
                    "substr(news_date, 12, 2)||" +
                    "substr(news_date, 15, 2)" +
                    "DESC";

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, word);

            ResultSet rs = statement.executeQuery();
            int i = 1;
            while (rs.next()) {
                //Wed Dec 14 17:03:41 MSK 2022
                String date = rs.getString("news_date");
                //String year = date.substring(24);
                String month = date.substring(4, 7);
                String day = date.substring(8, 10);
                String hour = date.substring(11, 13);
                String minute = date.substring(14, 16);

                Gui.model.addRow(new Object[]{
                        i++,
                        rs.getString("source"),
                        rs.getString("title"),
                        day + "." + month + " " + hour + ":" + minute,
                        rs.getString("link"),
                        rs.getString("describe")
                });
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("getNewsForTopTen error: " + e.getMessage());
        }
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


    // Список слов с переводом
    public List<String> getExcludedWordsFromAnalysis() {
        List<String> words = new ArrayList<>();
        try {
            String query = "SELECT word FROM exclude";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                words.add(rs.getString("word"));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("getRandomWord error: " + e.getMessage());
        }
        return words;
    }

    /* REMOVE */
    // удаление слов из разных таблиц
    public void removeItem(String item, int activeWindow) {
        try {
            String query = null;
            if (activeWindow == 2) {
                query = "DELETE FROM rss_list WHERE source = ? and user_id = ?";
            } else if (activeWindow == 3) {
                query = "DELETE FROM exclude WHERE word = ? and user_id = ?";
            } else if (activeWindow == 4) {
                query = "DELETE FROM excluded_headlines WHERE word = ? and user_id = ?";
            } else if (activeWindow == 5) {
                query = "DELETE FROM keywords WHERE word = ? AND user_id = ?";
            } else if (activeWindow == 6) {
                query = "DELETE FROM favorites WHERE title = ? AND user_id = ?";
            } else if (activeWindow == 7) {
                query = "DELETE FROM dates WHERE type||' '||description = ? AND main.dates.user_id = ?";
            }

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, item);
            statement.setInt(2, Login.userId);

            statement.executeUpdate();
            statement.close();

            Common.console("item deleted: " + item);
        } catch (Exception e) {
            Common.console("removeItem error: " + e.getMessage());
        }

    }

    // удаление дубликатов новостей
//    public void removeDuplicates() {
//        try {
//            String query = "DELETE FROM all_news WHERE ROWID NOT IN (SELECT MIN(ROWID) " +
//                    "FROM all_news GROUP BY source, title, news_date)";
//            PreparedStatement statement = connection.prepareStatement(query);
//            statement.executeUpdate();
//            statement.close();
//        } catch (Exception e) {
//            Common.console("deleteDuplicates error: " + e.getMessage());
//        }
//    }

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

    // Очистка данных любой передаваемой таблицы
    public void removeFromUsers(String username) {
        int userId = getUserIdByUsername(username);
        try {
            // "on delete cascade" doesn't work
            String [] removeCascade = {
                    "DELETE FROM dates WHERE user_id = ?",
                    "DELETE FROM exclude WHERE user_id = ?",
                    "DELETE FROM excluded_headlines WHERE user_id = ?",
                    "DELETE FROM favorites WHERE user_id = ?",
                    "DELETE FROM keywords WHERE user_id = ?",
                    "DELETE FROM rss_list WHERE user_id = ?",
                    "DELETE FROM settings WHERE user_id = ?",
                    "DELETE FROM users where id = ?"
            };
            connection.setAutoCommit(false);
            for (String query : removeCascade) {
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setInt(1, userId);
                statement.executeUpdate();
                statement.close();
            }
            connection.setAutoCommit(true);
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

    // Обновление настроек
    public void updateSettings(String key, String value) {
        try {
            String query = "UPDATE settings SET value = ? WHERE key = ? AND user_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, value);
            statement.setString(2, key);
            statement.setInt(3, Login.userId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.console("updateSettings error: " + e.getMessage());
        }
    }

    // обновление статуса чекбокса
    public void updateIsActiveCheckboxes(boolean check, String name, String type) {
        String query = null;
        try {
            if (type.equals("rss")) {
                query = "UPDATE rss_list SET is_active = ? WHERE source = ? AND user_id = ?";
            } else if (type.equals("keywords")) {
                query = "UPDATE keywords SET is_active = ? WHERE word = ? and user_id = ?";
            }

            PreparedStatement statement = connection.prepareStatement(query);
            statement.setBoolean(1, check);
            statement.setString(2, name);
            statement.setInt(3, Login.userId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.console("updateIsActiveStatus error: " + e.getMessage());
        }
    }

    public void updateIsActiveDates(boolean check, String type, String description) {
        try {
            String query = "UPDATE dates SET is_active = ? WHERE type = ? and description = ? and main.dates.user_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setBoolean(1, check);
            statement.setString(2, type);
            statement.setString(3, description);
            statement.setInt(4, Login.userId);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.console("updateIsActiveStatus error: " + e.getMessage());
        }
    }

    public int getUserIdByUsername(String username) {
        int id = 0;
        try {
            String query = "SELECT id FROM users WHERE username = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            id = resultSet.getInt("id");

            statement.close();
        } catch (Exception e) {
            Common.console("getUserHashPassword error: " + e.getMessage());
        }
        return id;
    }

    public String getUserHashPassword(String username) {
        String password = "";
        try {
            String query = "SELECT password FROM users WHERE username = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            password = resultSet.getString("password");

            statement.close();
        } catch (Exception e) {
            Common.console("getUserHashPassword error: " + e.getMessage());
        }
        return password;
    }

    public List<String> getInitQueries() {
        List<String> queries = new ArrayList<>();
        try {
            String query = "SELECT query FROM init_data";
            PreparedStatement statement = connection.prepareStatement(query);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                queries.add(rs.getString("query"));
            }
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("getInitQueries error: " + e.getMessage());
        }
        return queries;
    }

    public void initUser() {
        List<String> queries = getInitQueries();
        try {
            connection.setAutoCommit(false);
            PreparedStatement statement;
            for (String query : queries) {
                statement = connection.prepareStatement(query);
                statement.setInt(1, Login.userId);
                statement.executeUpdate();
            }
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            Common.console("initUser error: " + e.getMessage());
        }
    }

    public void updateUserPassword(int id, String newPassword) {
        try {
            String query = "UPDATE users SET password = ? WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, newPassword);
            statement.setInt(2, id);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.console("updateUserPassword error: " + e.getMessage());
        }
    }

}
