package search;

import com.sun.syndication.feed.synd.SyndEntry;
import database.JdbcQueries;
import database.SQLite;
import email.EmailManager;
import exception.IncorrectEmail;
import gui.Gui;
import model.Source;
import model.TableRow;
import utils.Common;
import utils.Parser;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConsoleSearch {
    SQLite sqlite;
    JdbcQueries jdbcQueries;
    int newsCount = 0;
    public static AtomicBoolean isStop;
    public static AtomicBoolean isSearchNow;
    public static AtomicBoolean isSearchFinished;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MMM HH:mm", Locale.ENGLISH);
    public static final ArrayList<String> dataForEmail = new ArrayList<>();
    public static final AtomicBoolean IS_CONSOLE_SEARCH = new AtomicBoolean(false);
    public static String sendEmailToConsole;
    public static String sendEmailFromConsole;
    public static String sendEmailFromPwdConsole;
    public static int minutesIntervalConsole;

    public ConsoleSearch() {
        sqlite = new SQLite();
        jdbcQueries = new JdbcQueries();
        isStop = new AtomicBoolean(false);
        isSearchNow = new AtomicBoolean(false);
        isSearchFinished = new AtomicBoolean(false);
    }

    /*
      java -jar ./news.jar from@mail.ru from_password to@mail.ru 160 world russia fifa
      main arguments for console search:
      args1 = email from
      args2 = email from pwd
      args3 = email to
      args4 = interval in minutes
      args5 = keyword1, keyword2 ... argsN = search keywords
    */
    public void searchByConsole(String[] args) {
        IS_CONSOLE_SEARCH.set(true);
        sendEmailFromConsole = args[0];
        sendEmailFromPwdConsole = args[1];
        sendEmailToConsole = args[2];
        if (!sendEmailToConsole.contains("@")) {
            throw new IncorrectEmail("incorrect e-mail");
        }
        minutesIntervalConsole = Integer.parseInt(args[3]);
        System.out.println(Arrays.toString(args));

        if (!isSearchNow.get()) {
            dataForEmail.clear();
            isSearchNow.set(true);
            newsCount = 0;

            try {
                // начало транзакции
                sqlite.transaction("BEGIN TRANSACTION");
                TableRow tableRow;

                for (Source source : jdbcQueries.getSources("console")) {
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

                                tableRow = new TableRow(
                                        source.getSource(),
                                        title,
                                        newsDescribe,
                                        DATE_FORMAT.format(pubDate),
                                        entry.getLink());

                                for (String arg : args) {
                                    if (arg.equals(args[0]) || arg.equals(args[1]) || arg.equals(args[2])
                                            || arg.equals(args[3]))
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

                // Автоматическая отправка результатов
                if (dataForEmail.size() > 0) {
                    Common.IS_SENDING.set(false);
                    System.out.println("sending an email..");
                    new EmailManager().sendMessage();
                } else {
                    System.out.println("news headlines not found");
                }
                Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.set(false);

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    sqlite.transaction("ROLLBACK");
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
