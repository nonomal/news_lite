package database;

import gui.Gui;
import lombok.extern.slf4j.Slf4j;
import model.Excluded;
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

    // Заполняем таблицу анализа
    public void selectSqlite(Connection connection) {
        try {
            String query = "SELECT SUM, TITLE FROM V_NEWS_DUAL WHERE SUM > ? " +
                    "AND TITLE NOT IN (SELECT WORD FROM ALL_TITLES_TO_EXCLUDE) " +
                    "ORDER BY SUM DESC";
            PreparedStatement st = connection.prepareStatement(query);
            st.setInt(1, WORD_FREQ_MATCHES);

            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                String word = rs.getString("TITLE");
                int sum = rs.getInt("SUM");
                Object[] row = new Object[]{word, sum};
                Gui.modelForAnalysis.addRow(row);
            }
            deleteFromTable("NEWS_DUAL", connection);
            rs.close();
            st.close();
        } catch (Exception e) {
            Common.console("error: " + e.getMessage());
        }
    }

    // Источники новостей
    public List<Source> getSources(String type, Connection connection) {
        List<Source> sources = new ArrayList<>();
        String query;

        if (type.equals("all")) {
            query = "SELECT id, source, link, is_active, position FROM rss_list " +
                    "ORDER BY is_active DESC, id";
        } else {
            query = "SELECT id, source, link, is_active, position FROM rss_list " +
                    "WHERE is_active = 1 ORDER BY position";
        }

        if (SQLite.isConnectionToSQLite) {
            try {
                PreparedStatement st = connection.prepareStatement(query);
                ResultSet rs = st.executeQuery();

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
                st.close();
            } catch (Exception e) {
                Common.console("error: " + e.getMessage());
            }
        }
        return sources;
    }

    // исключённые из анализа слова
    public List<Excluded> getExcludedWords(Connection connection) {
        List<Excluded> excludedWords = new ArrayList<>();
        if (SQLite.isConnectionToSQLite) {
            try {
                String query = "SELECT id, word FROM exclude ORDER BY id DESC";
                PreparedStatement st = connection.prepareStatement(query);

                ResultSet rs = st.executeQuery();
                while (rs.next()) {
                    excludedWords.add(new Excluded(
                            rs.getInt("id"),
                            rs.getString("word")));
                }
                rs.close();
                st.close();
            } catch (Exception e) {
                Common.console("error: " + e.getMessage());
            }
        }
        return excludedWords;
    }

    // вставка нового источника
    public void insertNewSource(Connection connection) {
        if (SQLite.isConnectionToSQLite) {
            try {
                // Диалоговое окно добавления источника новостей в базу данных
                JTextField rss = new JTextField();
                JTextField link = new JTextField();
                Object[] newSource = {"Source:", rss, "Link to rss:", link};
                int result = JOptionPane.showConfirmDialog(Gui.scrollPane, newSource,
                        "New source", JOptionPane.OK_CANCEL_OPTION);

                if (result == JOptionPane.YES_OPTION) {
                    String query = "INSERT INTO RSS_LIST(SOURCE, LINK) VALUES (?, ?)";
                    PreparedStatement st = connection.prepareStatement(query);
                    st.setString(1, rss.getText());
                    st.setString(2, link.getText());
                    st.executeUpdate();
                    st.close();

                    Common.console("status: source added");
                    log.info("New source added: " + rss.getText());
                } else {
                    Common.console("status: adding source canceled");
                }
            } catch (Exception e) {
                Common.console("error: " + e.getMessage());
            }
        }
    }

    // вставка нового слова для исключения из анализа частоты употребления слов
    public void insertNewExcludedWord(String word, Connection connection) {
        if (SQLite.isConnectionToSQLite) {
            try {
                String query = "INSERT INTO exclude(word) VALUES (?)";
                PreparedStatement st = connection.prepareStatement(query);
                st.setString(1, word);
                st.executeUpdate();
                st.close();

                Common.console("status: word \"" + word + "\" excluded from analysis");
                log.info("New word excluded from analysis");
            } catch (Exception e) {
                Common.console("error: " + e.getMessage());
            }
        }
    }

    // вставка кода по заголовку для отсеивания ранее обнаруженных новостей
    public void insertTitles(String pTitle, Connection connection) {
        if (SQLite.isConnectionToSQLite) {
            try {
                String query = "INSERT INTO titles256(title) VALUES (?)";
                PreparedStatement st = connection.prepareStatement(query);
                st.setString(1, pTitle);
                st.executeUpdate();
                st.close();
            } catch (SQLException e) {
                Common.console("error: " + e.getMessage());
            }
        }
    }

    // сохранение всех заголовков
    public void insertAllTitlesToArchive(String title, String date, Connection connection) {
        if (SQLite.isConnectionToSQLite) {
            try {
                String query = "INSERT INTO ALL_NEWS(TITLE, NEWS_DATE) VALUES (?, ?)";
                PreparedStatement st = connection.prepareStatement(query);
                st.setString(1, title);
                st.setString(2, date);
                st.executeUpdate();
                st.close();
            } catch (SQLException e) {
                Common.console("error: " + e.getMessage());
            }
        }
    }

    // отсеивание заголовков
    public boolean isTitleExists(String title, Connection connection) {
        int isExists = 0;
        if (SQLite.isConnectionToSQLite) {
            try {
                String query = "SELECT MAX(1) FROM titles256 WHERE EXISTS (SELECT title FROM titles256 t WHERE t.title = ?)";
                PreparedStatement st = connection.prepareStatement(query);
                st.setString(1, title);

                ResultSet rs = st.executeQuery();
                while (rs.next()) {
                    isExists = rs.getInt(1);
                }
                rs.close();
                st.close();

            } catch (Exception e) {
                Common.console("error: " + e.getMessage());
            }
        }
        return isExists == 1;
    }

    // новостей в архиве всего
    public int archiveNewsCount(Connection connection) {
        int countNews = 0;
        if (SQLite.isConnectionToSQLite) {
            try {
                String query = "SELECT COUNT(*) FROM ALL_NEWS";
                PreparedStatement st = connection.prepareStatement(query);

                ResultSet rs = st.executeQuery();
                while (rs.next()) {
                    countNews = rs.getInt(1);
                }
                rs.close();
                st.close();
            } catch (Exception e) {
                Common.console("error: " + e.getMessage());
            }
        }
        return countNews;
    }

    // удаление источника
    public void deleteSource(String source, Connection connection) {
        if (SQLite.isConnectionToSQLite) {
            try {
                String query = "DELETE FROM rss_list WHERE source = ?";
                PreparedStatement st = connection.prepareStatement(query);
                st.setString(1, source);

                st.executeUpdate();
                st.close();
            } catch (Exception e) {
                Common.console("error: " + e.getMessage());
            }
        }
    }

    // удаление слова исключенного из поиска
    public void deleteExcluded(String p_source, Connection connection) {
        if (SQLite.isConnectionToSQLite) {
            try {
                String query = "DELETE FROM EXCLUDE WHERE WORD = ?";
                PreparedStatement st = connection.prepareStatement(query);
                st.setString(1, p_source);

                st.executeUpdate();
                st.close();
            } catch (Exception e) {
                Common.console("status: " + e.getMessage());
            }
        }
    }

    // обновление статуса чекбокса is_active для ресурсов SELECT id, source, link FROM rss_list where is_active = 1  ORDER BY id
    public void updateIsActiveStatus(boolean pBoolean, String pSource, Connection connection) {
        if (SQLite.isConnectionToSQLite) {
            try {
                String query = "UPDATE RSS_LIST SET IS_ACTIVE = ? WHERE SOURCE = ?";
                PreparedStatement st = connection.prepareStatement(query);
                st.setBoolean(1, pBoolean);
                st.setString(2, pSource);
                st.executeUpdate();
                st.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // удаление дубликатов новостей
    public void deleteDuplicates(Connection connection) {
        if (SQLite.isConnectionToSQLite) {
            try {
                String query = "DELETE FROM ALL_NEWS WHERE ROWID NOT IN (SELECT MIN(ROWID) " +
                        "FROM ALL_NEWS GROUP BY TITLE, NEWS_DATE)";
                PreparedStatement st = connection.prepareStatement(query);
                st.executeUpdate();
                st.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // удаляем все пустые строки
    public void deleteEmptyRows(Connection connection) throws SQLException {
        String query = "DELETE FROM NEWS_DUAL WHERE TITLE = ''";
        PreparedStatement delete = connection.prepareStatement(query);
        delete.executeUpdate();
    }

    public void deleteFromTable(String tableName, Connection connection) {
        try {
            String query = "DELETE FROM " + tableName;
            PreparedStatement st = connection.prepareStatement(query);
            st.executeUpdate();
            st.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
