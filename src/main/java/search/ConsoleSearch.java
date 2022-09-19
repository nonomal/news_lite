package search;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import database.JdbcQueries;
import database.SQLite;
import email.EmailManager;
import exception.IncorrectEmail;
import gui.Gui;
import lombok.extern.slf4j.Slf4j;
import model.Source;
import utils.Common;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ConsoleSearch extends SearchUtils {
    SQLite sqlite = new SQLite();
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
    /**/
    public static final AtomicBoolean IS_CONSOLE_SEARCH = new AtomicBoolean(false);
    public static String sendEmailToFromConsole;
    public static int minutesIntervalConsole;

    public ConsoleSearch() {
        excludeFromSearch = Common.EXCLUDE_WORDS;
        isStop = new AtomicBoolean(false);
        isSearchNow = new AtomicBoolean(false);
        isSearchFinished = new AtomicBoolean(false);
    }

    public void searchByConsole(String[] args) {
        IS_CONSOLE_SEARCH.set(true);
        sendEmailToFromConsole = args[0];
        if (!sendEmailToFromConsole.contains("@")) {
            throw new IncorrectEmail("incorrect e-mail");
        }

        minutesIntervalConsole = Integer.parseInt(args[1]);
        sqlite.openConnection();
        System.out.println(Arrays.toString(args));

        if (!isSearchNow.get()) {
            dataForEmail.clear();
            List<Source> activeSources = jdbcQueries.getSources("active", SQLite.connection);
            isSearchNow.set(true);
            j = 1;
            newsCount = 0;

            try {
                // начало транзакции
                sqlite.transaction("BEGIN TRANSACTION");

                Parser parser = new Parser();
                for (Source source : activeSources) {
                    try {
                        try {
                            if (isStop.get()) return;
                            SyndFeed feed = parser.parseFeed(source.getLink());
                            for (Object message : feed.getEntries()) {
                                j++;
                                SyndEntry entry = (SyndEntry) message;
                                SyndContent content = entry.getDescription();
                                String smiSource = source.getSource();
                                String title = entry.getTitle();
                                assert content != null;
                                String newsDescribe = content.getValue()
                                        .trim()
                                        .replaceAll(("<p>|</p>|<br />"), "");
                                if (isHref(newsDescribe)) newsDescribe = title;
                                Date pubDate = entry.getPublishedDate();
                                String dateToEmail = date_format.format(pubDate);
                                String link = entry.getLink();

                                // отсеиваем новости ранее 01.01.2022
                                if (pubDate.after(minDate)) checkDate = 1;
                                else checkDate = 0;

                                for (String arg : args) {
                                    if (arg.equals(args[0]) || arg.equals(args[1]))
                                        continue;

                                    if (title.toLowerCase().contains(arg.toLowerCase()) && title.length() > 15 && checkDate == 1) {
                                        // отсеиваем новости которые были обнаружены ранее
                                        if (jdbcQueries.isTitleExists(title, SQLite.connection)
                                                && SQLite.isConnectionToSQLite) {
                                            continue;
                                        }

                                        //Data for a table
                                        Date currentDate = new Date();
                                        int date_diff = Common.compareDatesOnly(currentDate, pubDate);

                                        if (date_diff != 0) { // если новость between Main.minutesIntervalForConsoleSearch and currentDate
                                            newsCount++;
                                            dataForEmail.add(newsCount + ") " + title + "\n" + link + "\n" + newsDescribe + "\n" +
                                                    smiSource + " - " + dateToEmail);
                                            /**/
                                            System.out.println(newsCount + ") " + title);
                                            /**/
                                            jdbcQueries.insertTitles(title, SQLite.connection);
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
                sqlite.transaction("COMMIT");

                // удаляем все пустые строки
                jdbcQueries.deleteEmptyRows(SQLite.connection);

                // Автоматическая отправка результатов
                if (dataForEmail.size() > 0) {
                    Common.IS_SENDING.set(false);
                    System.out.println("sending an email..");
                    new EmailManager().sendMessage();
                }
                jdbcQueries.deleteDuplicates(SQLite.connection);
                Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.set(false);
                sqlite.closeConnection();
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    sqlite.transaction("ROLLBACK");
                } catch (SQLException sql) {
                    sql.printStackTrace();
                }
            }
        }
    }
}
