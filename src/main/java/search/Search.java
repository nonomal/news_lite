package search;

import com.sun.syndication.feed.synd.SyndEntry;
import database.JdbcQueries;
import database.SQLite;
import gui.Gui;
import gui.buttons.Icons;
import model.Excluded;
import model.Keyword;
import model.Source;
import model.TableRow;
import utils.Common;
import utils.Parser;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Search {
    private int newsCount = 0;
    private static final int WORD_FREQ_MATCHES = 2;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MMM HH:mm", Locale.ENGLISH);
    private final SQLite sqLite;
    private final JdbcQueries jdbcQueries;
    public static AtomicBoolean isStop, isSearchNow, isSearchFinished;
    public static final List<TableRow> headlinesList = new ArrayList<>();
    public Map<String, Integer> wordsCount = new HashMap<>();
    public List<String> excludedWordsFromAnalysis;

    public Search() {
        sqLite = new SQLite();
        jdbcQueries = new JdbcQueries();
        isStop = new AtomicBoolean(false);
        isSearchNow = new AtomicBoolean(false);
        isSearchFinished = new AtomicBoolean(false);
        excludedWordsFromAnalysis = jdbcQueries.getExcludedWordsFromAnalysis();
    }

    public void mainSearch(String searchType) {
        if (!isSearchNow.get()) {
            boolean isWord = searchType.equals("word");
            boolean isWords = searchType.equals("words");
            boolean isTopTen = searchType.equals("top-ten");

            isSearchNow.set(true);
            Search.isStop.set(false);
            Gui.sendCurrentResultsToEmail.setVisible(false);
            LocalTime timeStart = LocalTime.now();

            int modelRowCount = Gui.model.getRowCount();
            headlinesList.clear();
            wordsCount.clear();
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

            try {
                TableRow tableRow;
                List<Excluded> excludedTitles = jdbcQueries.getExcludedTitlesWords();
                List<Source> sourcesList = jdbcQueries.getSources("active");
                sqLite.transaction("BEGIN TRANSACTION");

                // search animation
                new Thread(Common::fillProgressLine).start();
                Gui.model.addRow(new Object[]{});

                StringBuilder processString = new StringBuilder("[");
                for (int i = 1; i < sourcesList.size(); i++) {
                    processString.append(". ");
                }
                processString.append("]");
                String process = processString.toString();

                int q = 1;
                for (Source source : sourcesList) {
                    if (isStop.get()) return;

                    int processPercent = (int) Math.round((double) q++ / sourcesList.size() * 100);
                    Gui.model.setValueAt("Progress: [" + processPercent + "%] " + process, 0, 2);
                    process = process.replaceFirst(". ", "#");
                    Gui.model.setValueAt(source.getSource(), 0, 1);

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

                            if (isWord || isTopTen) {
                                Gui.findWord = Gui.topKeyword.getText().toLowerCase();
                                String newsTitle = tableRow.getTitle().toLowerCase();

                                if (newsTitle.contains(Gui.findWord) && newsTitle.length() > 15) {
                                    int dateDiff = Common.compareDatesOnly(new Date(), pubDate);

                                    if (dateDiff != 0) {
                                        // Данные за период для таблицы топ-10 без отсева заголовков
                                        getTopTenData(tableRow);
                                    }

                                    // вставка всех без исключения новостей в архив
                                    jdbcQueries.addAllTitlesToArchive(title,
                                            pubDate.toString(),
                                            tableRow.getLink(),
                                            tableRow.getSource(),
                                            tableRow.getDescribe());

                                    if (isTopTen) {
                                        if (dateDiff != 0) {
                                            searchProcess(tableRow, searchType);
                                        }
                                    } else {
                                        //отсеиваем новости, которые уже были найдены ранее при включенном чекбоксе
                                        if (jdbcQueries.isTitleExists(title, searchType)) {
                                            continue;
                                        }

                                        if (Gui.findWord.length() == 0) {
                                            // замена не интересующих заголовков в UI на # + слово исключение
                                            for (Excluded excludedTitle : excludedTitles) {
                                                if (excludedTitle.getWord().length() > 2 && newsTitle.contains(excludedTitle.getWord())) {
                                                    tableRow.setTitle("# " + excludedTitle);
                                                }
                                            }

                                            if (dateDiff != 0 && !tableRow.getTitle().contains("#")) {
                                                searchProcess(tableRow, searchType);
                                            }

                                            if (dateDiff != 0 && tableRow.getTitle().contains("#")) {
                                                ++excludedCount;
                                            }
                                        } else {
                                            if (dateDiff != 0) {
                                                searchProcess(tableRow, searchType);
                                            }
                                        }
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
                                            getTopTenData(tableRow);
                                        }
                                    }
                                }
                            }
                            if (isStop.get()) return;
                        }
                        if (!Gui.isOnlyLastNews) {
                            jdbcQueries.removeFromTable("TITLES");
                        }
                    } catch (Exception e) {
                        String smi = source.getLink()
                                .replaceAll(("https://|http://|www."), "");
                        smi = smi.substring(0, smi.indexOf("/"));
                        Common.console(smi + " is not available");
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
                    int x = Common.getXForEmailIcon(label.length());
                    Gui.sendCurrentResultsToEmail.setBounds(x, 277, 30, 22);
                } else if (isWords) {
                    Gui.labelSum.setText("total: " + newsCount);
                    int x = Common.getXForEmailIconKeywords(newsCount);
                    Gui.sendCurrentResultsToEmail.setBounds(x, 277, 30, 22);
                }

                // Сортировка DESC и заполнение таблицы анализа
                List<TableRow> list = headlinesList.stream()
                        .distinct()
                        .sorted(Collections.reverseOrder())
                        .collect(Collectors.toList());

                int i = 1;
                Gui.model.removeRow(0);
                for (TableRow row : list) {
                    Gui.model.addRow(new Object[]{
                            i++,
                            row.getSource(),
                            row.getTitle(),
                            row.getDate(),
                            row.getLink(),
                            row.getDescribe()
                    });
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

                // Удаление исключённых слов из мап для анализа
                for (String word : excludedWordsFromAnalysis) {
                    wordsCount.remove(word);
                }

                // Сортировка DESC и заполнение таблицы анализа
                wordsCount.entrySet().stream()
                        .filter(x -> x.getValue() > WORD_FREQ_MATCHES)
                        .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                        .forEach(x -> Gui.modelForAnalysis.addRow(new Object[]{x.getKey(), x.getValue()}));

                // Автоматическая отправка результатов
                if (Gui.autoSendMessage.getState() && (Gui.model.getRowCount() > 0) && headlinesList.size() > 0) {
                    Gui.sendCurrentResultsToEmail.doClick();
                }

                //jdbcQueries.removeDuplicates();

                Gui.WAS_CLICK_IN_TABLE_FOR_ANALYSIS.set(false);

                if (isWord) {
                    Gui.appInfo.setText("news archive: " + jdbcQueries.archiveNewsCount());
                }
            } catch (Exception e) {
                try {
                    sqLite.transaction("ROLLBACK");
                } catch (SQLException ignored) {
                }
                isStop.set(true);
            }
        }
    }

    private void searchProcess(TableRow tableRow, String searchType) {
        newsCount++;
        Gui.labelSum.setText(String.valueOf(newsCount));

        headlinesList.add(tableRow);

        if (Gui.isOnlyLastNews) {
            jdbcQueries.addTitles(tableRow.getTitle(), searchType);
        }
    }

    private void getTopTenData(TableRow tableRow) {
        String[] substr = tableRow.getTitle().split(" ");
        for (String s : substr) {
            if (s.length() > 3) {
                wordsCount.put(s, wordsCount.getOrDefault(s, 0) + 1);
            }
        }
    }

}