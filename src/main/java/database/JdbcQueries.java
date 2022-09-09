package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class JdbcQueries {
    public static final String NEWS_DUAL_QUERY = "INSERT INTO NEWS_DUAL(TITLE) VALUES (?)";

    // вставка кода по заголовку для отсеивания ранее обнаруженных новостей
    public void insertTitles(List<String> titles, Connection connection) {
        if (SQLite.isConnectionToSQLite) {
            String query = "INSERT INTO titles256(title) VALUES (?)";

            for (String title : titles) {
                try {
                    PreparedStatement ps = connection.prepareStatement(query);
                    ps.setString(1, title);
                    ps.executeUpdate();
                } catch (SQLException t) {
                    t.printStackTrace();
                }
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


}
