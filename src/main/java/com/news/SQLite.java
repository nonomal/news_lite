package com.news;

import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Level;

class SQLite {
    static Connection connection;
    static boolean isConnectionToSQLite;
    static int wordFreqMatches = 2;

    // Открытие соединения с базой данных
    static void open() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + Main.directoryPath + "news.db");
            //createTables();
            //createView();
            isConnectionToSQLite = true;
            Main.LOGGER.log(Level.INFO, "Connected");
            //SQLite.deleteFromSources();
            //SQLite.initialInsertSources();
            //SQLite.selectSources("smi");
            Thread.sleep(1000L);
            Gui.connect_to_bd_label.setText("<html><p style=\"color:#a4f5a4\">Connected to SQLite for word frequency analysis</p></html>");
        } catch (Exception e) {
            e.printStackTrace();
            Main.LOGGER.log(Level.WARNING, e.getMessage());
        }
    }

    // создание таблиц
    static void createTables() {
        try {
            String query = "CREATE TABLE IF NOT EXISTS rss_list(id number, source text, link text, is_active number)";
            Statement st = connection.createStatement();
            st.executeUpdate(query);
            st.close();

            String sqlCreateTable = "CREATE TABLE IF NOT EXISTS news_dual(title varchar2(4000))";
            Statement stCreateTable = connection.createStatement();
            stCreateTable.executeUpdate(sqlCreateTable);
            stCreateTable.close();

            //titles256
            String sqlCreateTable256 = "CREATE TABLE IF NOT EXISTS titles256(title varchar2(4000))";
            Statement stCreateTable256 = connection.createStatement();
            stCreateTable256.executeUpdate(sqlCreateTable256);
            stCreateTable256.close();

            //all_news
            String sqlCreateAllNews = "CREATE TABLE IF NOT EXISTS all_news(title varchar2(4000), news_date varchar2(4000))";
            Statement sqlCreateAllNewsSt = connection.createStatement();
            sqlCreateAllNewsSt.executeUpdate(sqlCreateAllNews);
            sqlCreateAllNewsSt.close();

            // слова, которые исключаются при анализе частоты употребления слов в новостях
            String sqlExclude = "CREATE TABLE IF NOT EXISTS exclude(word varchar2(69))";
            Statement stExclude = connection.createStatement();
            stExclude.executeUpdate(sqlExclude);
            stExclude.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // создание представления
    static void createView() {
        try {
            Statement st = connection.createStatement();
            String sql = "CREATE VIEW IF NOT EXISTS v_news_dual as\n" +
                    "select q.\"TITLE\",q.\"SUM\"\n" +
                    "  from (select title,\n" +
                    "               count(title) sum\n" +
                    "          from news_dual\n" +
                    "         group by title\n" +
                    "         order by sum desc) q";
            st.executeUpdate(sql);
            st.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Select
    static void selectSqlite() {
        try {
            Statement st = connection.createStatement();
            String query = "select SUM, TITLE from v_news_dual where sum > " + wordFreqMatches + " and title not in (select word from exclude) order by SUM desc";
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                String word = rs.getString("TITLE");
                int sum = rs.getInt("SUM");
                Object[] row2 = new Object[]{sum, word};
                Gui.model_for_analysis.addRow(row2);
            }
            SQLite.deleteTitles();
            rs.close();
            st.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Delete from news_dual
    static void deleteTitles() {
        try {
            Statement st = connection.createStatement();
            String query = "delete from news_dual";
            st.executeUpdate(query);
            st.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Delete from titles256
    static void deleteFrom256() {
        try {
            Statement st = connection.createStatement();
            String query = "delete from titles256";
            st.executeUpdate(query);
            st.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Считывание источников из файла sources.txt
    static String[] getSources() {
        ArrayList<String> lines = new ArrayList<>();
        String[] listOfSources = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(Main.sourcesPath), StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            listOfSources = lines.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return listOfSources;
    }

    // заполнение БД источниками новостей из файла sources.txt
    static void initialInsertSources() {
        String[] init_inserts = getSources();
        if (isConnectionToSQLite) {
            try {
                Statement st_insert = connection.createStatement();
                for (String s : init_inserts) {
                    st_insert.executeUpdate(s);
                }
                st_insert.close();
            } catch (SQLException t) {
                t.printStackTrace();
            }
        }
    }

    // запись данных по актуальным источникам из базы в массивы для поиска
    static void selectSources(String pDialog) {
        if (isConnectionToSQLite) {
            switch (pDialog) {
                case "smi":
                    //sources
                    Common.smi_source.clear();
                    Common.smi_link.clear();
                    try {
                        Statement st = connection.createStatement();
                        String query = "SELECT id, source, link FROM rss_list where is_active = 1  ORDER BY id";
                        ResultSet rs = st.executeQuery(query);
                        while (rs.next()) {
                            //int id = rs.getInt("id");
                            String source = rs.getString("source");
                            String link = rs.getString("link");
                            Common.smi_source.add(source);
                            Common.smi_link.add(link);
                        }
                        rs.close();
                        st.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case "excl":
                    //excluded words
                    Common.excludedWords.clear();
                    try {
                        Statement st = connection.createStatement();
                        String query = "SELECT word FROM exclude";
                        ResultSet rs = st.executeQuery(query);
                        while (rs.next()) {
                            String word = rs.getString("word");
                            Common.excludedWords.add(word);
                        }
                        rs.close();
                        st.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case "active_smi":
                    Common.smi_source.clear();
                    Common.smi_link.clear();
                    Common.smi_is_active.clear();
                    try {
                        Statement st = connection.createStatement();
                        String query = "SELECT id, source, link, is_active FROM rss_list ORDER BY id";
                        ResultSet rs = st.executeQuery(query);
                        while (rs.next()) {
                            //int id = rs.getInt("id");
                            String source = rs.getString("source");
                            String link = rs.getString("link");
                            boolean isActive = rs.getBoolean("is_active");

                            Common.smi_source.add(source);
                            Common.smi_link.add(link);
                            Common.smi_is_active.add(isActive);
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

    //очистка списка источников новостей
    static void deleteFromSources() {
        if (isConnectionToSQLite) {
            try {
                Statement st = connection.createStatement();
                String query = "DELETE FROM rss_list";
                st.executeUpdate(query);
                st.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // вставка нового источника
    static void insertNewSource() {
        if (isConnectionToSQLite) {
            int max_id_in_source = 0;
            int new_id;
            try {
                String max_id_query = "SELECT max(id) as id FROM rss_list";
                Statement max_id_st = connection.createStatement();
                ResultSet rs = max_id_st.executeQuery(max_id_query);
                while (rs.next()) {
                    max_id_in_source = rs.getInt("ID");
                }
                rs.close();
                max_id_st.close();
                new_id = max_id_in_source + 1;

                // Диалоговое окно добавления источника новостей в базу данных
                JTextField source_name = new JTextField();
                JTextField rss_link = new JTextField();
                Object[] new_source = {
                        "Source:", source_name,
                        "Link to rss:", rss_link
                };

                int result = JOptionPane.showConfirmDialog(Gui.scrollPane, new_source, "New source", JOptionPane.OK_CANCEL_OPTION);
                if (result == JOptionPane.YES_OPTION) {

                    //запись в БД
                    String query = "INSERT INTO rss_list(id, source, link) " + "VALUES (" + new_id + ", '" + source_name.getText() + "', '" + rss_link.getText() + "')";
                    Statement st = connection.createStatement();
                    st.executeUpdate(query);
                    st.close();

                    // запись в файл sources.txt
                    try (OutputStreamWriter writer = new OutputStreamWriter(
                            new FileOutputStream(Main.sourcesPath, true),
                            StandardCharsets.UTF_8)) {
                        String text = "\nINSERT INTO rss_list(id, source, link) VALUES (" + new_id + ", '" + source_name.getText() + "', '" + rss_link.getText() + "');";
                        writer.write(text);
                        writer.flush();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }

                    Common.console("status: source added");
                    Main.LOGGER.log(Level.INFO, "New source added");
                } else {
                    Common.console("status: adding source canceled");
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // вставка нового слова для исключения из анализа частоты употребления слов
    static void insertNewExcludedWord(String pWord) {
        if (isConnectionToSQLite) {
            try {
                //запись в БД
                String query = "INSERT INTO exclude(word) " + "VALUES ('" + pWord + "')";
                Statement st = connection.createStatement();
                st.executeUpdate(query);
                st.close();

                // запись в файл sources.txt
                try (OutputStreamWriter writer = new OutputStreamWriter(
                        new FileOutputStream(Main.excludedPath, true),
                        StandardCharsets.UTF_8)) {
                    //String text = "insert into exclude values ('" + pWord + "');\n";
                    writer.write(pWord + "\n");
                    writer.flush();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                Common.console("status: word \"" + pWord + "\" excluded from analysis");
                Main.LOGGER.log(Level.INFO, "New word excluded from analysis");
            } catch (Exception e) {
                e.printStackTrace();
                Common.console("status: " + e.getMessage());
            }
        }
    }

    // вставка кода по заголовку для отсеивания ранее обнаруженных новостей
    static void insertTitleIn256(String pTitle) {
        if (isConnectionToSQLite) {
            try {
                String query256 = "INSERT INTO titles256(title) VALUES ('" + pTitle + "')";
                Statement st256 = connection.createStatement();
                st256.executeUpdate(query256);
                st256.close();
            } catch (SQLException t) {
                t.printStackTrace();
            }
        }
    }

    // сохранение всех заголовков
    static void insertAllTitles(String pTitle, String pDate) {
        if (isConnectionToSQLite) {
            try {
                String q = "INSERT INTO all_news(title, news_date) VALUES ('" + pTitle + "', '" + pDate + "')";
                Statement st = connection.createStatement();
                st.executeUpdate(q);
                st.close();
            } catch (SQLException ignored) {
            }
        }
    }

    // отсеивание заголовков
    static boolean isTitleExists(String pString256) {
        int isExists = 0;
        if (isConnectionToSQLite) {
            try {
                Statement st = connection.createStatement();
                String query = "SELECT max(1) FROM titles256 where exists (select title from titles256 t where t.title = '" + pString256 + "')";
                ResultSet rs = st.executeQuery(query);

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

    // вставка только новых заголовков в архив всех новостей
    static boolean isTitleInArchiveExists(String pString256) {
        int isExists = 0;
        if (isConnectionToSQLite) {
            try {
                Statement st = connection.createStatement();
                String query = "SELECT max(1) FROM all_news where exists (select title||news_date from all_news t where t.title||t.news_date = '" + pString256 + "')";
                ResultSet rs = st.executeQuery(query);

                while (rs.next()) {
                    isExists = rs.getInt(1);
                }
                rs.close();
                st.close();
            } catch (Exception ignored) {
            }
        }
        return isExists == 1;
    }

    // новостей в архиве всего
    static int archiveNewsCount() {
        int countNews = 0;
        if (isConnectionToSQLite) {
            try {
                Statement st = connection.createStatement();
                String query = "SELECT count(*) FROM all_news";
                ResultSet rs = st.executeQuery(query);

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
    static void deleteSource(String p_source) {
        if (isConnectionToSQLite) {
            try {
                String query = "DELETE FROM rss_list WHERE source = '" + p_source + "'";
                Statement del_st = connection.createStatement();
                del_st.executeUpdate(query);
                del_st.close();
            } catch (Exception e) {
                Common.console("status: " + e.getMessage());
            }
        }
    }

    // удаление слова исключенного из поиска
    static void deleteExcluded(String p_source) {
        if (isConnectionToSQLite) {
            try {
                String query = "DELETE FROM exclude WHERE word = '" + p_source + "'";
                Statement del_st = connection.createStatement();
                del_st.executeUpdate(query);
                del_st.close();
            } catch (Exception e) {
                Common.console("status: " + e.getMessage());
            }
        }
    }

    // обновление статуса чекбокса is_active для ресурсов SELECT id, source, link FROM rss_list where is_active = 1  ORDER BY id
    static void updateIsActiveStatus(boolean pBoolean, String pSource) {
        if (isConnectionToSQLite) {
            try {
                Statement st = connection.createStatement();
                String query = "UPDATE rss_list SET is_active = " + pBoolean + " where source = '" + pSource + "'";
                st.executeUpdate(query);
                st.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // закрытие соединения с SQLite
    static void closeSQLiteConnection() {
        try {
            if (isConnectionToSQLite) connection.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
