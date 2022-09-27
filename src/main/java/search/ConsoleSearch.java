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
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ConsoleSearch extends SearchUtils {
    SQLite sqlite;
    JdbcQueries jdbcQueries;
    int newsCount = 0;
    public static AtomicBoolean isStop;
    public static AtomicBoolean isSearchNow;
    public static AtomicBoolean isSearchFinished;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    public static final ArrayList<String> dataForEmail = new ArrayList<>();
    public static final AtomicBoolean IS_CONSOLE_SEARCH = new AtomicBoolean(false);
    public static String sendEmailToFromConsole;
    public static int minutesIntervalConsole;

    public ConsoleSearch() {
        sqlite = new SQLite();
        jdbcQueries = new JdbcQueries();
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
        System.out.println(Arrays.toString(args));

        if (!isSearchNow.get()) {
            dataForEmail.clear();
            isSearchNow.set(true);
            newsCount = 0;

            try {
                // начало транзакции
                sqlite.transaction("BEGIN TRANSACTION");

                Parser parser = new Parser();
                for (Source source : jdbcQueries.getSources("active")) {
                    try {
                        try {
                            if (isStop.get()) return;
                            SyndFeed feed = parser.parseFeed(source.getLink());
                            for (Object message : feed.getEntries()) {
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
                                String dateToEmail = dateFormat.format(pubDate);
                                String link = entry.getLink();

                                for (String arg : args) {
                                    if (arg.equals(args[0]) || arg.equals(args[1]))
                                        continue;

                                    if (title.toLowerCase().contains(arg.toLowerCase()) && title.length() > 15) {
                                        // отсеиваем новости которые были обнаружены ранее
                                        if (jdbcQueries.isTitleExists(title, "console")) {
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
                                            jdbcQueries.addTitles(title, "console");
                                        }
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    } catch (Exception ignored) {
                    }
                }
                isSearchNow.set(false);

                // коммит транзакции
                sqlite.transaction("COMMIT");

                // удаляем все пустые строки
                jdbcQueries.deleteEmptyRows();

                // Автоматическая отправка результатов
                if (dataForEmail.size() > 0) {
                    Common.IS_SENDING.set(false);
                    System.out.println("sending an email..");
                    new EmailManager().sendMessage();
                }
                jdbcQueries.deleteDuplicates();
                Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.set(false);

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
