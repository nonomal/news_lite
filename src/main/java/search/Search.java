package search;

import com.sun.syndication.feed.synd.SyndEntry;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class Search {
    private int newsCount = 0;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MMM HH:mm", Locale.ENGLISH);
    private final SQLite sqLite;
    private final JdbcQueries jdbcQueries;
    public static AtomicBoolean isStop, isSearchNow, isSearchFinished;
    public static final List<String> dataForEmail = new ArrayList<>();

    public Search() {
        sqLite = new SQLite();
        jdbcQueries = new JdbcQueries();
        isStop = new AtomicBoolean(false);
        isSearchNow = new AtomicBoolean(false);
        isSearchFinished = new AtomicBoolean(false);
    }

    public void mainSearch(String pSearchType) {

        if (!isSearchNow.get()) {
            boolean isWord = pSearchType.equals("word");
            boolean isWords = pSearchType.equals("words");

            isSearchNow.set(true);
            Search.isStop.set(false);
            LocalTime timeStart = LocalTime.now();

            int modelRowCount = Gui.model.getRowCount();
            dataForEmail.clear();
            if (!Gui.GUI_IN_TRAY.get()) Gui.model.setRowCount(0);
            if (!Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.get()) Gui.modelForAnalysis.setRowCount(0);
            newsCount = 0;
            Gui.labelSum.setText("" + newsCount);
            Gui.sendEmailBtn.setIcon(Icons.SEND_EMAIL_ICON);
            isSearchFinished = new AtomicBoolean(false);

            if (isWord) {
                Gui.searchBtnTop.setVisible(false);
                Gui.stopBtnTop.setVisible(true);
            } else if (isWords) {
                Gui.searchBtnBottom.setVisible(false);
                Gui.stopBtnBottom.setVisible(true);
            }

            new Thread(Common::fillProgressLine).start();
            try {
                sqLite.transaction("BEGIN TRANSACTION");
                TableRow tableRow;
                List<String> excludedTitles = jdbcQueries.excludedTitles();

                // Актуальные источники новостей
                for (Source source : jdbcQueries.getSources("active")) {
                    if (isStop.get()) return;
                    try {
                        try {
                            for (Object message : new Parser().parseFeed(source.getLink()).getEntries()) {
                                SyndEntry entry = (SyndEntry) message;
                                String title = entry.getTitle();
                                Date pubDate = entry.getPublishedDate();
                                String newsDescribe = entry.getDescription().getValue()
                                        .trim()
                                        .replaceAll(("<p>|</p>|<br />|&#"), "");
                                if (isHref(newsDescribe)) newsDescribe = title;

                                tableRow = new TableRow(
                                        source.getSource(),
                                        title,
                                        newsDescribe,
                                        DATE_FORMAT.format(pubDate),
                                        entry.getLink());

                                if (isWord) {
                                    Gui.findWord = Gui.topKeyword.getText().toLowerCase();
                                    String newsTitle = tableRow.getTitle().toLowerCase();

                                    if (newsTitle.contains(Gui.findWord) && newsTitle.length() > 15) {

                                        // исключение не интересующих заголовков в UI
                                        for (String excludedTitle : excludedTitles) {
                                            if (excludedTitle.length() > 2 && newsTitle.contains(excludedTitle)) {
                                                tableRow.setTitle("# " + excludedTitle);
                                            }
                                        }

                                        //отсеиваем новости, которые уже были найдены ранее при включенном чекбоксе
                                        if (jdbcQueries.isTitleExists(title, pSearchType)) {
                                            continue;
                                        }

                                        //Data for a table
                                        int dateDiff = Common.compareDatesOnly(new Date(), pubDate);

                                        // вставка всех новостей в архив (ощутимо замедляет общий поиск)
                                        jdbcQueries.addAllTitlesToArchive(title, pubDate.toString());
                                        if (dateDiff != 0) {
                                            searchProcess(tableRow, pSearchType, title);
                                        }
                                    }
                                } else if (isWords) {
                                    for (Keyword keyword : jdbcQueries.getKeywords(1)) {
                                        if (tableRow.getTitle().toLowerCase().contains(keyword.getKeyword().toLowerCase())
                                                && tableRow.getTitle().length() > 15) {

                                            // отсеиваем новости которые были обнаружены ранее
                                            if (jdbcQueries.isTitleExists(title, pSearchType)) {
                                                continue;
                                            }

                                            //Data for a table
                                            int dateDiff = Common.compareDatesOnly(new Date(), pubDate);

                                            if (dateDiff != 0) {
                                                searchProcess(tableRow, pSearchType, tableRow.getTitle());
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
                jdbcQueries.deleteEmptyRows();

                // Заполняем таблицу анализа
                if (!Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.get()) jdbcQueries.setAnalysis();

                // Автоматическая отправка результатов
                if (Gui.autoSendMessage.getState() && (Gui.model.getRowCount() > 0) && dataForEmail.size() > 0) {
                    Gui.sendEmailBtn.doClick();
                }

                jdbcQueries.deleteDuplicates();
                Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.set(false);
                if (isWord)
                    Common.console("info: number of news items in the archive = " + jdbcQueries.archiveNewsCount());
                log.info("number of news items in the archive = " + jdbcQueries.archiveNewsCount());
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

    private void searchProcess(TableRow tableRow, String searchType, String title) {
        newsCount++;
        Gui.labelSum.setText(String.valueOf(newsCount));

        // Подготовка данных для отправки результатов на почту
        dataForEmail.add(newsCount + ") " +
                title + "\n" +
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

        jdbcQueries.addTitlesNewsDual(title);
        jdbcQueries.addTitles(title, searchType);
    }

    private boolean isHref(String newsDescribe) {
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
}
