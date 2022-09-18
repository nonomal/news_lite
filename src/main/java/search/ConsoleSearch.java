package search;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import database.JdbcQueries;
import database.SQLite;
import email.EmailManager;
import gui.Gui;
import lombok.extern.slf4j.Slf4j;
import utils.Common;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ConsoleSearch extends SearchUtils {
    SQLite sqLite = new SQLite();
    JdbcQueries jdbcQueries = new JdbcQueries();
    public static List<String> excludeFromSearch;
    public static AtomicBoolean isStop;
    public static AtomicBoolean isSearchNow;
    public static AtomicBoolean isSearchFinished;
    public static int j = 1;
    final SimpleDateFormat date_format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    public static final ArrayList<String> dataForEmail = new ArrayList<>();
    int newsCount = 0;
    final Date minDate = Common.MIN_PUB_DATE.getTime();
    int checkDate;

    public ConsoleSearch() {
        excludeFromSearch = Common.EXCLUDE_WORDS;
        isStop = new AtomicBoolean(false);
        isSearchNow = new AtomicBoolean(false);
        isSearchFinished = new AtomicBoolean(false);
    }

    public void searchByConsole(String[] keywordsFromConsole) {
        JdbcQueries sqlite = new JdbcQueries();
        if (!isSearchNow.get()) {
            dataForEmail.clear();
            sqlite.selectSources("smi", SQLite.connection);
            isSearchNow.set(true);
            ConsoleSearch.j = 1;
            newsCount = 0;

            try {
                // начало транзакции
                sqLite.transaction("BEGIN TRANSACTION");

                Parser parser = new Parser();
                for (Common.SMI_ID = 0; Common.SMI_ID < Common.SMI_LINK.size(); Common.SMI_ID++) {
                    try {
                        try {
                            if (isStop.get()) return;
                            SyndFeed feed = parser.parseFeed(Common.SMI_LINK.get(Common.SMI_ID));
                            for (Object message : feed.getEntries()) {
                                j++;
                                SyndEntry entry = (SyndEntry) message;
                                SyndContent content = entry.getDescription();
                                String smi_source = Common.SMI_SOURCE.get(Common.SMI_ID);
                                String title = entry.getTitle();
                                assert content != null;
                                String newsDescribe = content.getValue()
                                        .trim()
                                        .replace("<p>", "")
                                        .replace("</p>", "")
                                        .replace("<br />", "");
                                if (isHref(newsDescribe)) newsDescribe = title;
                                Date pubDate = entry.getPublishedDate();
                                String dateToEmail = date_format.format(pubDate);
                                String link = entry.getLink();

                                // отсеиваем новости ранее 01.01.2022
                                if (pubDate.after(minDate)) checkDate = 1;
                                else checkDate = 0;

                                for (String it : keywordsFromConsole) {
                                    if (it.equals(keywordsFromConsole[0]) || it.equals(keywordsFromConsole[1]))
                                        continue;

                                    if (title.toLowerCase().contains(it.toLowerCase()) && title.length() > 15 && checkDate == 1) {
                                        // отсеиваем новости которые были обнаружены ранее
                                        if (sqlite.isTitleExists(title, SQLite.connection)
                                                && SQLite.isConnectionToSQLite) {
                                            continue;
                                        }

                                        //Data for a table
                                        Date currentDate = new Date();
                                        int date_diff = Common.compareDatesOnly(currentDate, pubDate);

                                        if (date_diff != 0) { // если новость between Main.minutesIntervalForConsoleSearch and currentDate
                                            newsCount++;
                                            dataForEmail.add(newsCount + ") " + title + "\n" + link + "\n" + newsDescribe + "\n" +
                                                    smi_source + " - " + dateToEmail);
                                            /**/
                                            System.out.println(newsCount + ") " + title);
                                            /**/
                                            sqlite.insertTitleIn256(title, SQLite.connection);
                                        }
                                    }
                                }
                            }
                            // удалять новости, чтобы были вообще все, даже те, которые уже были обнаружены
                            //sqlite.deleteFrom256();
                        } catch (Exception ignored) {
                        }
                    } catch (Exception ignored) {
                    }
                }
                isSearchNow.set(false);

                // коммит транзакции
                sqLite.transaction("COMMIT");

                // удаляем все пустые строки
                jdbcQueries.deleteEmptyRows(SQLite.connection);

                // Автоматическая отправка результатов
                if (dataForEmail.size() > 0) {
                    Common.IS_SENDING.set(false);
                    System.out.println("sending an email..");
                    new EmailManager().sendMessage();
                }
                sqlite.deleteDuplicates(SQLite.connection);
                Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.set(false);
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    sqLite.transaction("ROLLBACK");
                } catch (SQLException sql) {
                    sql.printStackTrace();
                }
            }
        }
    }
}
