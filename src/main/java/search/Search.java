package search;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import database.JdbcQueries;
import database.SQLite;
import gui.Gui;
import gui.buttons.Icons;
import lombok.extern.slf4j.Slf4j;
import model.Keyword;
import model.Source;
import model.TableRow;
import utils.Common;

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
    JdbcQueries jdbcQueries = new JdbcQueries();
    public static List<String> excludeFromSearch;
    public static AtomicBoolean isStop;
    public static AtomicBoolean isSearchNow;
    public static AtomicBoolean isSearchFinished;
    final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    final LocalDateTime now = LocalDateTime.now();
    public final String today = dtf.format(now);
    final SimpleDateFormat date_format = new SimpleDateFormat("dd.MMM HH:mm", Locale.ENGLISH);
    public static final ArrayList<String> dataForEmail = new ArrayList<>();
    int newsCount = 0;
    LocalTime timeStart;

    public Search() {
        excludeFromSearch = Common.EXCLUDE_WORDS;
        isStop = new AtomicBoolean(false);
        isSearchNow = new AtomicBoolean(false);
        isSearchFinished = new AtomicBoolean(false);
    }

    public void mainSearch(String pSearchType) {
        boolean isWord = pSearchType.equals("word");
        boolean isWords = pSearchType.equals("words");

        if (!isSearchNow.get()) {
            int modelRowCount = Gui.model.getRowCount();
            dataForEmail.clear();
            isSearchNow.set(true);
            timeStart = LocalTime.now();
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
            new Thread(Common::fillProgressLine).start();
            try {
                sqLite.transaction("BEGIN TRANSACTION");
                TableRow tableRow;
                Parser parser = new Parser();

                // Актуальные источники новостей
                List<Source> activeSources = jdbcQueries.getSources("active");

                for (Source source : activeSources) {
                    try {
                        try {
                            if (isStop.get()) return;
                            SyndFeed feed = parser.parseFeed(source.getLink());
                            for (Object message : feed.getEntries()) {
                                SyndEntry entry = (SyndEntry) message;
                                SyndContent content = entry.getDescription();
                                String title = entry.getTitle();

                                String newsDescribe = content.getValue()
                                        .trim()
                                        .replaceAll(("<p>|</p>|<br />"), "");
                                if (isHref(newsDescribe)) newsDescribe = title;
                                Date pubDate = entry.getPublishedDate();

                                tableRow = new TableRow(
                                        source.getSource(),
                                        title,
                                        newsDescribe,
                                        date_format.format(pubDate),
                                        entry.getLink());

                                if (isWord) {
                                    if (tableRow.getTitle().toLowerCase().contains(Gui.findWord.toLowerCase())
                                            && tableRow.getTitle().length() > 15
                                            && !tableRow.getTitle().toLowerCase().contains(excludeFromSearch.get(0))
                                            && !tableRow.getTitle().toLowerCase().contains(excludeFromSearch.get(1))
                                            && !tableRow.getTitle().toLowerCase().contains(excludeFromSearch.get(2))
                                    ) {
                                        //отсеиваем новости, которые уже были найдены ранее
                                        if (jdbcQueries.isTitleExists(tableRow.getTitle(), pSearchType)) {
                                            continue;
                                        }

                                        //Data for a table
                                        int dateDiff = Common.compareDatesOnly(new Date(), pubDate);

                                        // вставка всех новостей в архив (ощутимо замедляет общий поиск)
                                        jdbcQueries.insertAllTitlesToArchive(tableRow.getTitle(), pubDate.toString());
                                        if (dateDiff != 0) {
                                            searchProcess(tableRow, pSearchType);
                                        }
                                    }
                                } else if (isWords) {
                                    for (Keyword keyword : Common.KEYWORDS_LIST) {
                                        if (tableRow.getTitle().toLowerCase().contains(keyword.getKeyword().toLowerCase())
                                                && tableRow.getTitle().length() > 15) {

                                            // отсеиваем новости которые были обнаружены ранее
                                            if (jdbcQueries.isTitleExists(tableRow.getTitle(), pSearchType)) {
                                                continue;
                                            }

                                            //Data for a table
                                            int dateDiff = Common.compareDatesOnly(new Date(), pubDate);

                                            if (dateDiff != 0) {
                                                searchProcess(tableRow, pSearchType);
                                            }
                                        }
                                    }
                                }
                                if (isStop.get()) return;
                            }
                            if (!Gui.isOnlyLastNews)
                                jdbcQueries.deleteFromTable("TITLES");
                        } catch (Exception noRss) {
                            String smi = source.getLink()
                                    .replaceAll(("https://|http://|www."), "");
                            smi = smi.substring(0, smi.indexOf("/"));
                            Common.console("rss is not available: " + smi);
                            log.warn("rss is not available: " + smi);
                        }
                    } catch (Exception e) {
                        Common.console("error: restart the application!");
                        log.warn("error: restart the application!");
                        isStop.set(true);
                    }
                }

                //Время поиска
                if (!Gui.GUI_IN_TRAY.get()) Common.console("status: search completed in " +
                        Duration.between(timeStart, LocalTime.now()).getSeconds() + " s.");
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
                sqLite.transaction("COMMIT");

                // удаляем все пустые строки
                jdbcQueries.deleteEmptyRows(SQLite.connection);

                // Заполняем таблицу анализа
                if (!Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.get()) jdbcQueries.selectSqlite(SQLite.connection);

                // Автоматическая отправка результатов
                if (Gui.autoSendMessage.getState() && (Gui.model.getRowCount() > 0)) {
                    Gui.sendEmailBtn.doClick();
                }

                jdbcQueries.deleteDuplicates(SQLite.connection);
                Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.set(false);
                if (isWord)
                    Common.console("info: number of news items in the archive = " + jdbcQueries.archiveNewsCount(SQLite.connection));
                log.info("number of news items in the archive = " + jdbcQueries.archiveNewsCount(SQLite.connection));
            } catch (Exception e) {
                log.warn(e.getMessage());
                try {
                    sqLite.transaction("ROLLBACK");
                } catch (SQLException i) {
                    log.warn(i.getMessage());
                }
                isStop.set(true);
            }
        }
    }

    private void searchProcess(TableRow tableRow, String searchType) {
        // Счётчик количества новостей
        newsCount++;
        Gui.labelSum.setText(String.valueOf(newsCount));

        // Подготовка данных для отправки результатов на почту
        dataForEmail.add(newsCount + ") " +
                tableRow.getTitle() + "\n" +
                tableRow.getLink() + "\n" +
                tableRow.getDescribe() + "\n" +
                tableRow.getSource() + " - " +
                tableRow.getDate());

        // Добавление строки в таблицу интерфейса
        Gui.model.addRow(new Object[]{
                newsCount,
                tableRow.getSource(),
                tableRow.getTitle(),
                tableRow.getDate(),
                tableRow.getLink()
        });

        jdbcQueries.insertTitlesNewsDual(tableRow.getTitle());
        jdbcQueries.insertTitles(tableRow.getTitle(), searchType);
    }


}
