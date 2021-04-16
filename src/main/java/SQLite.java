import javax.swing.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Level;

class SQLite {
    static Connection connection;
    static boolean isConnectionToSQLite;

    // Открытие соединения с базой данных
    static void open() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + Main.directoryPath + "news.db");
            createTables();
            createView();
            isConnectionToSQLite = true;
            Main.LOGGER.log(Level.INFO, "Connected");
            SQLite.deleteFromSources();
            SQLite.initialInsertSources();
            SQLite.selectSources();
        } catch (Exception e) {
            e.printStackTrace();
            Main.LOGGER.log(Level.WARNING, e.getMessage());
        }
    }

    // создание таблиц
    static void createTables() {
        try {
            String query = "CREATE TABLE IF NOT EXISTS rss_list(id number, source text, link text)";
            Statement st = connection.createStatement();
            st.executeUpdate(query);
            st.close();

            String sqlCreateTable = "CREATE TABLE IF NOT EXISTS news_dual(title varchar2(4000))";
            Statement stCreateTable = connection.createStatement();
            stCreateTable.executeUpdate(sqlCreateTable);
            stCreateTable.close();

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
            String query = "select SUM, TITLE from v_news_dual where sum > 2 and title not in (select word from exclude) order by sum desc";
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
                String q_begin = "BEGIN TRANSACTION";
                Statement st_begin = SQLite.connection.createStatement();
                st_begin.executeUpdate(q_begin);

                Statement st_insert = connection.createStatement();
                for (String s : init_inserts) {
                    st_insert.executeUpdate(s);
                }
                st_insert.close();

                String q_commit = "COMMIT";
                Statement st_commit = SQLite.connection.createStatement();
                st_commit.executeUpdate(q_commit);

                st_begin.close();
                st_commit.close();
            } catch (SQLException t) {
                t.printStackTrace();
            }
        }
    }

    // запись данных по актуальным источникам из базы в массивы для поиска
    static void selectSources() {
        Common.smi_source.clear();
        Common.smi_link.clear();
        if (isConnectionToSQLite) {
            try {
                Statement st = connection.createStatement();
                String query = "SELECT id, source, link FROM rss_list ORDER BY id";
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

                    Common.console("[avandy@mrprogre ~]$ source added");
                    Main.LOGGER.log(Level.INFO, "New source added");
                } else {
                    Common.console("[avandy@mrprogre ~]$ adding source canceled");
                }

            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
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
                Common.console("[avandy@mrprogre ~]$ " + e.getMessage());
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
