package search;

import database.SQLite;

import java.sql.PreparedStatement;
import java.sql.SQLException;


public class SearchUtils {

    // удаление лишних символов в описании новости
    public boolean isHref(String newsDescribe) {
        return newsDescribe.contains("<img")
                || newsDescribe.contains("href")
                || newsDescribe.contains("<div")
                || newsDescribe.contains("&#34")
                || newsDescribe.contains("<p lang")
                || newsDescribe.contains("&quot")
                || newsDescribe.contains("<span")
                || newsDescribe.contains("<ol")
                || newsDescribe.equals("");
    }

    // для транзакций
    public void transactionCommand(String command) throws SQLException {
        PreparedStatement statement = SQLite.connection.prepareStatement(command);
        statement.execute();
        statement.close();
    }

    // удаляем все пустые строки
    public void deleteEmptyRows() throws SQLException {
        String query = "DELETE FROM NEWS_DUAL WHERE TITLE = ''";
        PreparedStatement delete = SQLite.connection.prepareStatement(query);
        delete.executeUpdate();
    }
}
