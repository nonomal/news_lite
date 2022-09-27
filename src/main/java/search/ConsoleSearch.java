package search;

import com.sun.syndication.feed.synd.SyndEntry;
import database.JdbcQueries;
import database.SQLite;
import email.EmailManager;
import exception.IncorrectEmail;
import gui.Gui;
import lombok.extern.slf4j.Slf4j;
import model.Source;
import model.TableRow;
import utils.Common;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ConsoleSearch extends SearchUtils {
    SQLite sqlite;
    JdbcQueries jdbcQueries;
    int newsCount = 0;
    public static AtomicBoolean isStop;
    public static AtomicBoolean isSearchNow;
    public static AtomicBoolean isSearchFinished;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MMM HH:mm", Locale.ENGLISH);
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
                TableRow tableRow;

                for (Source source : jdbcQueries.getSources("active")) {
                    try {
                        try {
                            if (isStop.get()) return;
                            for (Object message : new Parser().parseFeed(source.getLink()).getEntries()) {
                                SyndEntry entry = (SyndEntry) message;
                                String title = entry.getTitle();
                                Date pubDate = entry.getPublishedDate();
                                String newsDescribe = entry.getDescription().getValue()
                                        .trim()
                                        .replaceAll(("<p>|</p>|<br />"), "");
                                if (isHref(newsDescribe)) newsDescribe = title;

                                tableRow = new TableRow(
                                        source.getSource(),
                                        title,
                                        newsDescribe,
                                        DATE_FORMAT.format(pubDate),
                                        entry.getLink());

                                for (String arg : args) {
                                    if (arg.equals(args[0]) || arg.equals(args[1]))
                                        continue;

                                    if (tableRow.getTitle().toLowerCase().contains(arg.toLowerCase())
                                            && tableRow.getTitle().length() > 15) {

                                        // отсеиваем новости которые были обнаружены ранее
                                        if (jdbcQueries.isTitleExists(tableRow.getTitle(), "console")) {
                                            continue;
                                        }

                                        int dateDiff = Common.compareDatesOnly(new Date(), pubDate);
                                        if (dateDiff != 0) {
                                            newsCount++;

                                            // Подготовка данных для отправки результатов на почту
                                            dataForEmail.add(newsCount + ") " +
                                                    tableRow.getTitle() + "\n" +
                                                    tableRow.getLink() + "\n" +
                                                    tableRow.getDescribe() + "\n" +
                                                    tableRow.getSource() + " - " +
                                                    tableRow.getDate());

                                            System.out.println(newsCount + ") " + tableRow.getTitle());

                                            jdbcQueries.addTitles(tableRow.getTitle(), "console");
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
                //jdbcQueries.deleteEmptyRows(); TODO

                // Автоматическая отправка результатов
                if (dataForEmail.size() > 0) {
                    Common.IS_SENDING.set(false);
                    System.out.println("sending an email..");
                    new EmailManager().sendMessage();
                }
                //jdbcQueries.deleteDuplicates(); TODO
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
