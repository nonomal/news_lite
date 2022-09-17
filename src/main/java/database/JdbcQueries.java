package database;

import gui.Gui;
import lombok.extern.slf4j.Slf4j;
import utils.Common;

import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
                Object[] row2 = new Object[]{word, sum};
                Gui.modelForAnalysis.addRow(row2);
            }
            deleteFromTable("NEWS_DUAL", connection);
            rs.close();
            st.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // запись данных по актуальным источникам из базы в массивы для поиска
    public void selectSources(String pDialog, Connection connection) {
        if (SQLite.isConnectionToSQLite) {
            switch (pDialog) {
                case "smi":
                    //sources
                    Common.SMI_SOURCE.clear();
                    Common.SMI_LINK.clear();
                    try {
                        String query = "SELECT id, source, link FROM rss_list WHERE is_active = 1 ORDER BY position";
                        PreparedStatement st = connection.prepareStatement(query);
                        ResultSet rs = st.executeQuery();

                        while (rs.next()) {
                            //int id = rs.getInt("id");
                            String source = rs.getString("source");
                            String link = rs.getString("link");
                            Common.SMI_SOURCE.add(source);
                            Common.SMI_LINK.add(link);
                        }
                        rs.close();
                        st.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case "excl":
                    //excluded words
                    Common.EXCLUDED_WORDS.clear();
                    try {
                        String query = "SELECT word FROM exclude";
                        PreparedStatement st = connection.prepareStatement(query);

                        ResultSet rs = st.executeQuery();
                        while (rs.next()) {
                            String word = rs.getString("word");
                            Common.EXCLUDED_WORDS.add(word);
                        }
                        rs.close();
                        st.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case "active_smi":
                    Common.SMI_SOURCE.clear();
                    Common.SMI_LINK.clear();
                    Common.SMI_IS_ACTIVE.clear();
                    try {
                        String query = "SELECT id, source, link, is_active FROM RSS_LIST ORDER BY position";
                        PreparedStatement st = connection.prepareStatement(query);

                        ResultSet rs = st.executeQuery();
                        while (rs.next()) {
                            //int id = rs.getInt("id");
                            String source = rs.getString("source");
                            String link = rs.getString("link");
                            boolean isActive = rs.getBoolean("is_active");

                            Common.SMI_SOURCE.add(source);
                            Common.SMI_LINK.add(link);
                            Common.SMI_IS_ACTIVE.add(isActive);
                        }
                        rs.close();
                        st.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }

        }
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
                System.out.println(e.getMessage());
            }
        }
    }

    // вставка нового слова для исключения из анализа частоты употребления слов
    public void insertNewExcludedWord(String pWord, Connection connection) {
        if (SQLite.isConnectionToSQLite) {
            try {
                String query = "INSERT INTO exclude(word) " + "VALUES (?)";
                PreparedStatement st = connection.prepareStatement(query);
                st.setString(1, pWord);
                st.executeUpdate();
                st.close();

                Common.console("status: word \"" + pWord + "\" excluded from analysis");
                log.info("New word excluded from analysis");
            } catch (Exception e) {
                e.printStackTrace();
                Common.console("status: " + e.getMessage());
            }
        }
    }

    // вставка кода по заголовку для отсеивания ранее обнаруженных новостей
    public void insertTitleIn256(String pTitle, Connection connection) {
        if (SQLite.isConnectionToSQLite) {
            try {
                String query256 = "INSERT INTO titles256(title) VALUES (?)";
                PreparedStatement st256 = connection.prepareStatement(query256);
                st256.setString(1, pTitle);
                st256.executeUpdate();
                st256.close();
            } catch (SQLException t) {
                t.printStackTrace();
            }
        }
    }

    // сохранение всех заголовков
    public void insertAllTitles(String pTitle, String pDate, Connection connection) {
        if (SQLite.isConnectionToSQLite) {
            try {
                String query = "INSERT INTO ALL_NEWS(TITLE, NEWS_DATE) VALUES (?, ?)";
                PreparedStatement st = connection.prepareStatement(query);
                st.setString(1, pTitle);
                st.setString(2, pDate);
                st.executeUpdate();
                st.close();
            } catch (SQLException ignored) {
            }
        }
    }

    // отсеивание заголовков
    public boolean isTitleExists(String pString256, Connection connection) {
        int isExists = 0;
        if (SQLite.isConnectionToSQLite) {
            try {
                String query = "SELECT MAX(1) FROM TITLES256 WHERE EXISTS (SELECT TITLE FROM TITLES256 T WHERE T.TITLE = ?)";
                PreparedStatement st = connection.prepareStatement(query);
                st.setString(1, pString256);

                ResultSet rs = st.executeQuery();
                while (rs.next()) {
                    isExists = rs.getInt(1);
                }
                rs.close();
                st.close();

            } catch (Exception e) {
                e.printStackTrace();
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
            } catch (Exception ignored) {
            }
        }
        return countNews;
    }

    // удаление источника
    public void deleteSource(String p_source, Connection connection) {
        if (SQLite.isConnectionToSQLite) {
            try {
                String query = "DELETE FROM rss_list WHERE source = ?";
                PreparedStatement st = connection.prepareStatement(query);
                st.setString(1, p_source);

                st.executeUpdate();
                st.close();
            } catch (Exception e) {
                Common.console("status: " + e.getMessage());
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
