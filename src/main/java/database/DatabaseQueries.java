package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseQueries {

    // вставка кода по заголовку для отсеивания ранее обнаруженных новостей TODO
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

    // сохранение всех заголовков TODO
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

    // отсеивание заголовков TODO
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


}
