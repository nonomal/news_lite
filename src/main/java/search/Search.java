package search;

import com.sun.syndication.feed.synd.SyndEntry;
import database.JdbcQueries;
import database.SQLite;
import gui.Gui;
import gui.buttons.Icons;
import model.Keyword;
import model.Source;
import model.TableRow;
import utils.Common;
import utils.Parser;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class Search {
    private int newsCount = 0;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MMM HH:mm", Locale.ENGLISH);
    private final SQLite sqLite;
    private final JdbcQueries jdbcQueries;
    public static AtomicBoolean isStop, isSearchNow, isSearchFinished;
    public static final List<TableRow> emailAndExcelData = new ArrayList<>();

    public Search() {
        sqLite = new SQLite();
        jdbcQueries = new JdbcQueries();
        isStop = new AtomicBoolean(false);
        isSearchNow = new AtomicBoolean(false);
        isSearchFinished = new AtomicBoolean(false);
    }

    public void mainSearch(String searchType) {
        if (!isSearchNow.get()) {
            boolean isWord = searchType.equals("word");
            boolean isWords = searchType.equals("words");

            isSearchNow.set(true);
            Search.isStop.set(false);
            Gui.sendCurrentResultsToEmail.setVisible(false);
            LocalTime timeStart = LocalTime.now();

            int modelRowCount = Gui.model.getRowCount();
            emailAndExcelData.clear();
            if (!Gui.GUI_IN_TRAY.get()) Gui.model.setRowCount(0);
            if (!Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.get()) Gui.modelForAnalysis.setRowCount(0);
            newsCount = 0;
            int excludedCount = 0;
            Gui.labelSum.setText("" + newsCount);
            Gui.sendCurrentResultsToEmail.setIcon(Icons.SEND_EMAIL_ICON);
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
                                if (Common.isHref(newsDescribe)) newsDescribe = title;

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

                                        // замена не интересующих заголовков в UI на # + слово исключение
                                        for (String excludedTitle : excludedTitles) {
                                            if (excludedTitle.length() > 2 && newsTitle.contains(excludedTitle)) {
                                                tableRow.setTitle("# " + excludedTitle);
                                            }
                                        }

                                        //отсеиваем новости, которые уже были найдены ранее при включенном чекбоксе
                                        if (jdbcQueries.isTitleExists(title, searchType)) {
                                            continue;
                                        }

                                        //Data for a table
                                        int dateDiff = Common.compareDatesOnly(new Date(), pubDate);

                                        // вставка всех без исключения новостей в архив
                                        jdbcQueries.addAllTitlesToArchive(title,
                                                pubDate.toString(),
                                                tableRow.getLink(),
                                                tableRow.getSource(),
                                                tableRow.getDescribe());


                                        if (dateDiff != 0 && !tableRow.getTitle().contains("#")) {
                                            searchProcess(tableRow, searchType);
                                        }

                                        if (dateDiff != 0 && tableRow.getTitle().contains("#")) {
                                            ++excludedCount;
                                        }
                                    }
                                } else if (isWords) {
                                    for (Keyword keyword : jdbcQueries.getKeywords(1)) {
                                        if (tableRow.getTitle().toLowerCase().contains(keyword.getKeyword().toLowerCase())
                                                && tableRow.getTitle().length() > 15) {

                                            // отсеиваем новости которые были обнаружены ранее
                                            if (jdbcQueries.isTitleExists(title, searchType)) {
                                                continue;
                                            }

                                            //Data for a table
                                            int dateDiff = Common.compareDatesOnly(new Date(), pubDate);

                                            if (dateDiff != 0) {
                                                searchProcess(tableRow, searchType);
                                            }
                                        }
                                    }
                                }
                                if (isStop.get()) return;
                            }
                            if (!Gui.isOnlyLastNews)
                                jdbcQueries.removeFromTable("TITLES");
                        } catch (Exception noRss) {
                            String smi = source.getLink()
                                    .replaceAll(("https://|http://|www."), "");
                            smi = smi.substring(0, smi.indexOf("/"));
                            Common.console("rss is not available: " + smi);
                        }
                    } catch (Exception e) {
                        Common.console("error: restart the application!");
                        isStop.set(true);
                    }
                }

                //Время поиска
                if (!Gui.GUI_IN_TRAY.get()) Common.console("search completed in " +
                        Duration.between(timeStart, LocalTime.now()).getSeconds() + " s.");
                isSearchNow.set(false);

                // Итоги поиска
                if (isWord) {
                    int excludedPercent = (int) Math.round((excludedCount / ((double) newsCount
                            + excludedCount)) * 100);
                    String label = String.format("total: %d, excluded: %d (%s)", newsCount, excludedCount,
                            excludedPercent + "%");
                    Gui.labelSum.setText(label);

                    int x = Common.getXForEmailIcon(label.length(), 26,1003, 6);
                    Gui.sendCurrentResultsToEmail.setBounds(x, 277, 30, 22);

                } else if (isWords) {
                    Gui.labelSum.setText("total: " + newsCount);
                    if (newsCount >= 100) {
                        Gui.sendCurrentResultsToEmail.setBounds(923, 277, 30, 22);
                    } else {
                        Gui.sendCurrentResultsToEmail.setBounds(917, 277, 30, 22);
                    }

                }

                isSearchFinished.set(true);
                Gui.progressBar.setValue(100);

                // итоги в трей
                if (newsCount != 0 && newsCount != modelRowCount && Gui.GUI_IN_TRAY.get())
                    Common.trayMessage("News found: " + newsCount + ", excluded: " + excludedCount);

                if (Gui.model.getRowCount() > 0) Gui.sendCurrentResultsToEmail.setVisible(true);

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
                jdbcQueries.removeEmptyRows();

                // Заполняем таблицу анализа
                if (!Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.get()) jdbcQueries.setAnalysis();

                // Автоматическая отправка результатов
                if (Gui.autoSendMessage.getState() && (Gui.model.getRowCount() > 0) && emailAndExcelData.size() > 0) {
                    Gui.sendCurrentResultsToEmail.doClick();
                }

                // Удаление дубликатов заголовков
                jdbcQueries.removeDuplicates();

                Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.set(false);
                if (isWord)
                    Gui.appInfo.setText("news archive: " + jdbcQueries.archiveNewsCount());
            } catch (Exception e) {
                try {
                    sqLite.transaction("ROLLBACK");
                } catch (SQLException i) {
                    i.printStackTrace();
                }
                isStop.set(true);
            }
        }
    }

    private void searchProcess(TableRow tableRow, String searchType) {
        newsCount++;
        Gui.labelSum.setText(String.valueOf(newsCount));

        Gui.model.addRow(new Object[]{
                newsCount,
                tableRow.getSource(),
                tableRow.getTitle(),
                tableRow.getDate(),
                tableRow.getLink(),
                tableRow.getDescribe()
        });

        // Данные для отправки результатов на почту и выгрузки эксель-файла + исключённые из показа
        emailAndExcelData.add(tableRow);

        jdbcQueries.addCutTitlesForAnalysis(tableRow.getTitle());
        if (Gui.isOnlyLastNews) {
            jdbcQueries.addTitles(tableRow.getTitle(), searchType);
        }
    }
}
