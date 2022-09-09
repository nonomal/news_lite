package search;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import database.JdbcQueries;
import database.JdbcTemplateQueries;
import database.SQLite;
import gui.Gui;
import gui.buttons.Icons;
import lombok.extern.slf4j.Slf4j;
import main.Main;
import org.jetbrains.annotations.NotNull;
import utils.Common;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class Search extends SearchUtils {
    SQLite sqLite = new SQLite();
    public static List<String> excludeFromSearch;
    public static AtomicBoolean isStop;
    public static AtomicBoolean isSearchNow;
    public static AtomicBoolean isSearchFinished;
    public static int j = 1;
    final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    final LocalDateTime now = LocalDateTime.now();
    public final String today = dtf.format(now);
    final SimpleDateFormat date_format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    final SimpleDateFormat dateFormatHoursFirst = new SimpleDateFormat("dd.MMM HH:mm", Locale.ENGLISH);
    public static final ArrayList<String> dataForEmail = new ArrayList<>();
    int newsCount = 0;
    final Date minDate = Main.MIN_PUB_DATE.getTime();
    int checkDate;
    LocalTime timeStart;
    LocalTime timeEnd;
    Duration searchTime;
    public static List<String> titlesList = new ArrayList<>();

    public Search() {
        excludeFromSearch = Common.getExcludeWordsFromFile();
        isStop = new AtomicBoolean(false);
        isSearchNow = new AtomicBoolean(false);
        isSearchFinished = new AtomicBoolean(false);
    }

    public void mainSearch(@NotNull String pSearchType) {
        JdbcQueries jdbcQueries = new JdbcQueries();
        JdbcTemplateQueries jdbcTemplateQueries = new JdbcTemplateQueries();
        boolean isWord = pSearchType.equals("word");
        boolean isWords = pSearchType.equals("words");

        if (!isSearchNow.get()) {
            int modelRowCount = Gui.model.getRowCount();
            dataForEmail.clear();
            //выборка актуальных источников перед поиском из БД
            jdbcTemplateQueries.selectSources("smi");
            isSearchNow.set(true);
            timeStart = LocalTime.now();
            Search.j = 1;
            if (!Gui.GUI_IN_TRAY.get()) Gui.model.setRowCount(0);
            if (!Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.get()) Gui.modelForAnalysis.setRowCount(0);
            newsCount = 0;
            Gui.labelSum.setText("" + newsCount);
            Search.isStop.set(false);
            Gui.findWord = Gui.topKeyword.getText().toLowerCase();

            if (isWord) {
                Gui.searchBtnTop.setVisible(false);
                Gui.stopBtnTop.setVisible(true);
            } else if (isWords) {
                Gui.searchBtnBottom.setVisible(false);
                Gui.stopBtnBottom.setVisible(true);
            }

            isSearchFinished = new AtomicBoolean(false);
            Gui.sendEmailBtn.setIcon(Icons.SEND_EMAIL_ICON);
            new Thread(Common::fill).start();
            try {
                PreparedStatement st = SQLite.connection.prepareStatement(JdbcQueries.NEWS_DUAL_QUERY);
                sqLite.transactionCommand("BEGIN TRANSACTION");

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

                                // отсеиваем новости ранее 01.01.2021
                                if (pubDate.after(minDate)) checkDate = 1;
                                else checkDate = 0;

                                if (isWord) {
                                    if (title.toLowerCase().contains(Gui.findWord.toLowerCase())
                                            && title.length() > 15 && checkDate == 1
                                            && !title.toLowerCase().contains(excludeFromSearch.get(0))
                                            && !title.toLowerCase().contains(excludeFromSearch.get(1))
                                            && !title.toLowerCase().contains(excludeFromSearch.get(2))
                                    ) {
                                        //отсеиваем новости, которые уже были найдены ранее
                                        //if (titlesList.contains(title)) continue;
                                        if (jdbcQueries.isTitleExists(title, SQLite.connection)) {
                                            continue;
                                        }

                                        //Data for a table
                                        Date currentDate = new Date();
                                        int date_diff = Common.compareDatesOnly(currentDate, pubDate);

                                        mainSearchProcess(st, smi_source, title, newsDescribe, pubDate, dateToEmail, link, date_diff);
                                    }
                                } else if (isWords) {
                                    for (String it : Common.getKeywordsFromFile()) {
                                        if (title.toLowerCase().contains(it.toLowerCase()) && title.length() > 15 && checkDate == 1) {

                                            //if (titlesList.contains(title)) continue;
                                            if (jdbcQueries.isTitleExists(title, SQLite.connection)) {
                                                continue;
                                            }

                                            //Data for a table
                                            Date currentDate = new Date();
                                            int date_diff = Common.compareDatesOnly(currentDate, pubDate);

                                            mainSearchProcess(st, smi_source, title, newsDescribe, pubDate, dateToEmail, link, date_diff);
                                        }
                                    }
                                }
                                if (isStop.get()) return;
                            }
                        } catch (Exception no_rss) {
                            String smi = Common.SMI_LINK.get(Common.SMI_ID)
                                    .replaceAll(("https://|http://|www."), "");
                            smi = smi.substring(0, smi.indexOf("/"));
                            Common.console("rss is not available: " + smi);
                            log.warn("rss is not available: " + smi);
                        }
                    } catch (Exception e) {
                        Common.console("status: to many news.. please restart the application!");
                        log.warn("error: restart the application!");
                        isStop.set(true);
                    }
                }
                st.close();
                jdbcQueries.insertTitleIn256(titlesList, SQLite.connection);

                //Время поиска
                timeEnd = LocalTime.now();
                searchTime = Duration.between(timeStart, timeEnd);
                if (!Gui.GUI_IN_TRAY.get()) Common.console("status: search completed in " +
                        searchTime.getSeconds() + " s.");
                isSearchNow.set(false);

                Gui.labelSum.setText("total: " + newsCount);

                isSearchFinished.set(true);
                Gui.progressBar.setValue(100);
                Gui.table.setAutoCreateRowSorter(true);
                Gui.tableForAnalysis.setAutoCreateRowSorter(true);

                // итоги в трей
                if (newsCount != 0 && newsCount != modelRowCount && Gui.GUI_IN_TRAY.get())
                    Common.trayMessage("News found: " + newsCount);
                log.info("News found: " + newsCount);

                if (isWord) {
                    Gui.searchBtnTop.setVisible(true);
                    Gui.stopBtnTop.setVisible(false);
                } else if (isWords) {
                    Gui.searchBtnBottom.setVisible(true);
                    Gui.stopBtnBottom.setVisible(false);
                }

                // коммит транзакции
                sqLite.transactionCommand("COMMIT");

                // удаляем все пустые строки
                jdbcTemplateQueries.deleteEmptyRows();

                // при убранной галке "только последние новости" очищается временная таблица
                if (!Gui.isOnlyLastNews) {
                    titlesList.clear();
                    jdbcTemplateQueries.deleteFrom256();
                }

                // Заполняем таблицу анализа
                if (!Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.get()) jdbcTemplateQueries.selectSqlite();

                // Автоматическая отправка результатов
                if (Gui.autoSendMessage.getState() && (Gui.model.getRowCount() > 0)) {
                    Gui.sendEmailBtn.doClick();
                }

                Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.set(false);
            } catch (Exception e) {
                log.warn(e.getMessage());
                try {
                    sqLite.transactionCommand("ROLLBACK");
                } catch (SQLException i) {
                    log.warn(i.getMessage());
                }
                isStop.set(true);
            }
        }
    }

    private void mainSearchProcess(PreparedStatement st, String source, String title,
                                   String describe, Date pubDate, String dateToEmail, String link,
                                   int dateDiff) throws SQLException {
        if (dateDiff != 0) {
            newsCount++;
            Gui.labelSum.setText(String.valueOf(newsCount));
            dataForEmail.add(newsCount + ") " + title + "\n" + link + "\n" + describe + "\n" +
                    source + " - " + dateToEmail);

            Gui.model.addRow(new Object[]{
                    newsCount, source, title, dateFormatHoursFirst.format(pubDate), link
            });

            String[] subStr = title.split(" ");
            for (String s : subStr) {
                if (s.length() > 3) {
                    assert st != null;
                    st.setString(1, Common.delNoLetter(s).toLowerCase());
                    st.executeUpdate();
                }
            }
            titlesList.add(title);
            //databaseQueries.insertTitleIn256(title, SQLite.connection);
        }
    }
}
