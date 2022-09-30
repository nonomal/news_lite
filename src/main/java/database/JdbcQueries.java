package database;

import gui.Gui;
import lombok.extern.slf4j.Slf4j;
import model.Excluded;
import model.Keyword;
import model.Source;
import utils.Common;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JdbcQueries {
    private static final int WORD_FREQ_MATCHES = 2;
    private final Connection connection = SQLite.connection;
    private PreparedStatement statement;

    // Вставка заголовков разбитых на слова
    public void addTitlesNewsDual(String title) {
        try {
            String query = "INSERT INTO NEWS_DUAL(TITLE) VALUES (?)";
            statement = connection.prepareStatement(query);
            String[] substr = title.split(" ");

            for (String s : substr) {
                if (s.length() > 3) {
                    statement.setString(1, s);
                    statement.executeUpdate();
                }
            }
            statement.close();
        } catch (Exception e) {
            Common.console("addTitlesNewsDual error: " + e.getMessage());
        }
    }

    // Заполнение таблицы анализа
    public void setAnalysis() {
        try {
            String query = "SELECT SUM, TITLE FROM V_NEWS_DUAL WHERE SUM > ? " +
                    "AND TITLE NOT IN (SELECT WORD FROM ALL_TITLES_TO_EXCLUDE) " +
                    "ORDER BY SUM DESC";
            statement = connection.prepareStatement(query);
            statement.setInt(1, WORD_FREQ_MATCHES);

            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                String word = rs.getString("TITLE");
                int sum = rs.getInt("SUM");
                Object[] row = new Object[]{word, sum};
                Gui.modelForAnalysis.addRow(row);
            }
            deleteFromTable("NEWS_DUAL");
            rs.close();
            statement.close();
        } catch (Exception e) {
            Common.console("setAnalysis error: " + e.getMessage());
        }
    }

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
            statement = connection.prepareStatement(query);
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
            String query = "SELECT id, word FROM exclude ORDER BY id DESC";
            statement = connection.prepareStatement(query);

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

    // Список исключённые из поиска слов
    public List<Excluded> getExcludedTitlesWords() {
        List<Excluded> excludedWords = new ArrayList<>();
        try {
            String query = "SELECT id, word FROM exclude_from_main_search ORDER BY id DESC";
            statement = connection.prepareStatement(query);

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
            statement = connection.prepareStatement(query);

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

    // Вставка ключевого слова
    public void addKeyword(String word) {
        try {
            String query = "INSERT INTO keywords(word) VALUES (?)";
            statement = connection.prepareStatement(query);
            statement.setString(1, word);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.console("addKeyword error: " + e.getMessage());
        }
    }

    // Проверка наличия ключевого слова
    public boolean isKeywordExists(String word) {
        int isExists = 0;
        try {
            String query = "SELECT MAX(1) FROM keywords WHERE exists (select word from keywords where word = ?)";
            statement = connection.prepareStatement(query);
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

    // вставка нового источника
    public void addNewSource() {
        try {
            // Диалоговое окно добавления источника новостей в базу данных
            JTextField rss = new JTextField();
            JTextField link = new JTextField();
            Object[] newSource = {"Source:", rss, "Link to rss:", link};
            int result = JOptionPane.showConfirmDialog(Gui.scrollPane, newSource,
                    "New source", JOptionPane.OK_CANCEL_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                String query = "INSERT INTO rss_list(source, link, is_active) VALUES (?, ?, 1)";
                statement = connection.prepareStatement(query);
                statement.setString(1, rss.getText());
                statement.setString(2, link.getText());
                statement.executeUpdate();
                statement.close();

                Common.console("status: source added");
                log.info("New source added: " + rss.getText());
            } else {
                Common.console("status: adding source canceled");
            }
        } catch (Exception e) {
            Common.console("addNewSource error: " + e.getMessage());
        }
    }

    // вставка слова для исключения из анализа частоты употребления слов
    public void addExcludedWord(String word) {
        try {
            String query = "INSERT INTO exclude(word) VALUES (?)";
            statement = connection.prepareStatement(query);
            statement.setString(1, word);
            statement.executeUpdate();
            statement.close();

            Common.console("status: word \"" + word + "\" excluded from analysis");
            log.info("New word excluded from analysis: " + word);
        } catch (Exception e) {
            Common.console("addExcludedWord error: " + e.getMessage());
        }
    }

    // вставка кода по заголовку для отсеивания ранее обнаруженных новостей
    public void addTitles(String title, String type) {
        try {
            String query = "INSERT INTO titles(title, type) VALUES (?, ?)";
            statement = connection.prepareStatement(query);
            statement.setString(1, title);
            statement.setString(2, type);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            Common.console("addTitles error: " + e.getMessage());
        }
    }

    // сохранение всех заголовков в архив
    public void addAllTitlesToArchive(String title, String date) {
        try {
            String query = "INSERT INTO ALL_NEWS(TITLE, NEWS_DATE) VALUES (?, ?)";
            statement = connection.prepareStatement(query);
            statement.setString(1, title);
            statement.setString(2, date);
            statement.executeUpdate();
            statement.close();
        } catch (SQLException e) {
            Common.console("addAllTitlesToArchive error: " + e.getMessage());
        }
    }

    // отсеивание ранее найденных заголовков при включённом чекбоксе
    public boolean isTitleExists(String title, String type) {
        int isExists = 0;
        try {
            String query = "SELECT MAX(1) FROM titles " +
                    "WHERE EXISTS (SELECT title FROM titles t WHERE t.title = ? AND t.type = ?)";
            statement = connection.prepareStatement(query);
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
            String query = "SELECT word FROM exclude_from_main_search";
            statement = connection.prepareStatement(query);

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

    // вставка слова для исключения содержащих его заголовков
    public void addWordToExcludeTitles(String word) {
        if (word != null && word.length() > 0) {
            try {
                String query = "INSERT INTO exclude_from_main_search(word) VALUES (?)";
                statement = connection.prepareStatement(query);
                statement.setString(1, word);
                statement.executeUpdate();
                statement.close();

                Common.console("status: word \"" + word + "\" excluded from search");
                log.info("New word excluded from search: " + word);
            } catch (Exception e) {
                Common.console("addWordToExcludeTitles error: " + e.getMessage());
            }
        }
    }

    // новостей в архиве всего
    public int archiveNewsCount() {
        int countNews = 0;
        try {
            String query = "SELECT COUNT(*) FROM ALL_NEWS";
            statement = connection.prepareStatement(query);

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

    // удаление источника
    public void deleteSource(String source) {
        try {
            String query = "DELETE FROM rss_list WHERE source = ?";
            statement = connection.prepareStatement(query);
            statement.setString(1, source);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.console("deleteSource error: " + e.getMessage());
        }
    }

    // удаление слова исключенного из поиска
    public void deleteExcluded(String word, int activeWindow) {
        try {
            String query = "DELETE FROM exclude WHERE word = ?";

            if (activeWindow == 4) {
                query = "DELETE FROM exclude_from_main_search WHERE word = ?";
            } else if (activeWindow == 5) {
                deleteKeyword(word);
            }

            statement = connection.prepareStatement(query);
            statement.setString(1, word);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.console("deleteExcluded error: " + e.getMessage());
        }
    }

    // удаление ключевого слова
    public void deleteKeyword(String word) {
        try {
            String query = "DELETE FROM keywords WHERE word = ?";
            statement = connection.prepareStatement(query);
            statement.setString(1, word);
            statement.executeUpdate();
            statement.close();
        } catch (
                Exception e) {
            Common.console("deleteKeyword error: " + e.getMessage());
        }
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

            statement = connection.prepareStatement(query);
            statement.setBoolean(1, check);
            statement.setString(2, name);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.console("updateIsActiveStatus error: " + e.getMessage());
        }
    }

    // удаление дубликатов новостей
    public void deleteDuplicates() {
        try {
            String query = "DELETE FROM ALL_NEWS WHERE ROWID NOT IN (SELECT MIN(ROWID) " +
                    "FROM ALL_NEWS GROUP BY TITLE, NEWS_DATE)";
            statement = connection.prepareStatement(query);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.console("deleteDuplicates error: " + e.getMessage());
        }
    }

    // удаляем все пустые строки
    public void deleteEmptyRows() {
        try {
            String query = "DELETE FROM NEWS_DUAL WHERE TITLE = ''";
            statement = connection.prepareStatement(query);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.console("deleteEmptyRows error: " + e.getMessage());
        }
    }

    public void deleteFromTable(String tableName) {
        try {
            String query = "DELETE FROM " + tableName;
            statement = connection.prepareStatement(query);
            statement.executeUpdate();
            statement.close();
        } catch (Exception e) {
            Common.console("deleteFromTable error: " + e.getMessage());
        }
    }
}
