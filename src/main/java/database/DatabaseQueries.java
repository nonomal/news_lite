package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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

    public void insertTitleIn256(List<String> titles, Connection connection) {
        if (SQLite.isConnectionToSQLite) {
            String query256 = "INSERT INTO titles256(title) VALUES (?)";
            PreparedStatement st256;

            for (String title : titles) {
                try {
                    st256 = connection.prepareStatement(query256);
                    st256.setString(1, title);
                    st256.executeUpdate();
                } catch (SQLException t) {
                    t.printStackTrace();
                }
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
