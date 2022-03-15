package com.news;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import database.SQLite;
import gui.Gui;
import main.Main;
import utils.Common;
import email.EmailSender;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Search {
    public static List<String> excludeFromSearch = Common.getExcludeWordsFromFile();
    public static AtomicBoolean isStop = new AtomicBoolean(false);
    public static AtomicBoolean isSearchNow = new AtomicBoolean(false);
    public static AtomicBoolean isSearchFinished;
    double searchTime;
    public static int j = 1;
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    LocalDateTime now = LocalDateTime.now();
    public String today = dtf.format(now);
    SimpleDateFormat date_format = new SimpleDateFormat("dd.MM.yyyy HH:mm");
    SimpleDateFormat dateFormatHoursFirst = new SimpleDateFormat("dd.MMM HH:mm", Locale.ENGLISH);
    public static ArrayList<String> dataForEmail = new ArrayList<>();
    int newsCount = 0;
    Date minDate = Main.minPubDate.getTime();
    int checkDate;

    //Main search
    public void mainSearch(String pSearchType) {
        SQLite sqlite = new SQLite();
        if (!isSearchNow.get()) {
            int modelRowCount = Gui.model.getRowCount();
            dataForEmail.clear();
            if (!Gui.guiInTray.get()) Common.console("status: search started");
            //выборка актуальных источников перед поиском из БД
            sqlite.selectSources("smi");
            isSearchNow.set(true);
            Gui.timeStart = System.currentTimeMillis();
            Gui.labelInfo.setText("");
            Search.j = 1;
            if (!Gui.guiInTray.get()) Gui.model.setRowCount(0);
            if (!Gui.wasClickInTableForAnalysis.get()) Gui.modelForAnalysis.setRowCount(0);
            newsCount = 0;
            Gui.labelSum.setText("" + newsCount);
            Search.isStop.set(false);
            Gui.findWord = Gui.topKeyword.getText().toLowerCase();

            if (pSearchType.equals("word")) {
                Gui.searchBtnTop.setVisible(false);
                Gui.stopBtnTop.setVisible(true);
            } else if (pSearchType.equals("words")) {
                Gui.searchBtnBottom.setVisible(false);
                Gui.stopBtnBottom.setVisible(true);
            }

            isSearchFinished = new AtomicBoolean(false);
            Common.statusLabel(isSearchFinished, "Searching");
            Gui.sendEmailBtn.setIcon(Gui.send);
            new Thread(Common::fill).start();
            try {
                // начало транзакции
                PreparedStatement st = SQLite.connection.prepareStatement("insert into news_dual(title) values (?)");
                String q_begin = "BEGIN TRANSACTION";
                Statement st_begin = SQLite.connection.createStatement();
                st_begin.executeUpdate(q_begin);
                st_begin.close();

                SyndParser parser = new SyndParser();
                for (Common.smi_number = 0; Common.smi_number < Common.smi_link.size(); Common.smi_number++) {
                    try {
                        try {
                            if (isStop.get()) return;
                            SyndFeed feed = parser.parseFeed(Common.smi_link.get(Common.smi_number));
                            for (Object message : feed.getEntries()) {
                                j++;
                                SyndEntry entry = (SyndEntry) message;
                                SyndContent content = entry.getDescription();
                                String smi_source = Common.smi_source.get(Common.smi_number);
                                String title = entry.getTitle();
                                assert content != null;
                                String newsDescribe = content.getValue()
                                        .trim()
                                        .replace("<p>", "")
                                        .replace("</p>", "")
                                        .replace("<br />", "");
                                if (newsDescribe.contains("<img")
                                        || newsDescribe.contains("href")
                                        || newsDescribe.contains("<div")
                                        || newsDescribe.contains("&#34")
                                        || newsDescribe.contains("<p lang")
                                        || newsDescribe.contains("&quot")
                                        || newsDescribe.contains("<span")
                                        || newsDescribe.contains("<ol")
                                        || newsDescribe.equals("")
                                ) newsDescribe = title;
                                Date pubDate = entry.getPublishedDate();
                                String dateToEmail = date_format.format(pubDate);
                                String link = entry.getLink();

                                // отсеиваем новости ранее 01.01.2021
                                if (pubDate.after(minDate)) checkDate = 1;
                                else checkDate = 0;

                                if (pSearchType.equals("word")) {
                                    if (title.toLowerCase().contains(Gui.findWord.toLowerCase())
                                            && title.length() > 15 && checkDate == 1
                                            && !title.toLowerCase().contains(excludeFromSearch.get(0))
                                            && !title.toLowerCase().contains(excludeFromSearch.get(1))
                                            && !title.toLowerCase().contains(excludeFromSearch.get(2))
                                    ) {
                                        //отсеиваем новости, которые уже были найдены ранее
                                        if (sqlite.isTitleExists(Common.sha256(title + pubDate))
                                                && SQLite.isConnectionToSQLite) {
                                            continue;
                                        }

                                        //Data for a table
                                        Date curent_date = new Date();
                                        int date_diff = Common.compareDatesOnly(curent_date, pubDate);

                                        // вставка всех новостей в архив
                                        sqlite.insertAllTitles(title, pubDate.toString());

                                        if (Gui.todayOrNotCbx.getState() && (date_diff != 0)) {
                                            newsCount++;
                                            Gui.labelSum.setText(String.valueOf(newsCount));
                                            dataForEmail.add(newsCount + ") " + title + "\n" + link + "\n" + newsDescribe + "\n" +
                                                    smi_source + " - " + dateToEmail);

                                            Object[] row = new Object[]{
                                                    newsCount,
                                                    smi_source,
                                                    title,
                                                    dateFormatHoursFirst.format(pubDate),
                                                    link
                                            };
                                            Gui.model.addRow(row);

                                            //SQLite
                                            String[] subStr = title.split(" ");
                                            for (String s : subStr) {
                                                if (s.length() > 3) {
                                                    assert st != null;
                                                    st.setString(1, Common.delNoLetter(s).toLowerCase());
                                                    st.executeUpdate();
                                                }
                                            }
                                            sqlite.insertTitleIn256(Common.sha256(title + pubDate));

                                        } else if (!Gui.todayOrNotCbx.getState()) {
                                            newsCount++;
                                            Gui.labelSum.setText(String.valueOf(newsCount));
                                            dataForEmail.add(newsCount + ") " + title + "\n" + link + "\n" + newsDescribe + "\n" +
                                                    smi_source + " - " + dateToEmail);

                                            Object[] row = new Object[]{
                                                    newsCount,
                                                    smi_source,
                                                    title,
                                                    dateFormatHoursFirst.format(pubDate),
                                                    link
                                            };
                                            Gui.model.addRow(row);

                                            // SQLite
                                            String[] subStr = title.split(" ");
                                            for (String s : subStr) {
                                                if (s.length() > 3) {
                                                    assert st != null;
                                                    st.setString(1, Common.delNoLetter(s).toLowerCase());
                                                    st.executeUpdate();
                                                }
                                            }
                                            sqlite.insertTitleIn256(Common.sha256(title + pubDate));
                                        }
                                    }
                                } else if (pSearchType.equals("words")) {
                                    for (String it : Common.getKeywordsFromFile()) {
                                        if (title.toLowerCase().contains(it.toLowerCase()) && title.length() > 15 && checkDate == 1) {

                                            // отсеиваем новости которые были обнаружены ранее
                                            if (sqlite.isTitleExists(Common.sha256(title + pubDate)) && SQLite.isConnectionToSQLite) {
                                                continue;
                                            }

                                            //Data for a table
                                            Date curent_date = new Date();
                                            int date_diff = Common.compareDatesOnly(curent_date, pubDate);

                                            if (Gui.todayOrNotCbx.getState() && (date_diff != 0)) {
                                                newsCount++;
                                                Gui.labelSum.setText(String.valueOf(newsCount));
                                                dataForEmail.add(newsCount + ") " + title + "\n" + link + "\n" + newsDescribe + "\n" +
                                                        smi_source + " - " + dateToEmail);

                                                Object[] row = new Object[]{
                                                        newsCount,
                                                        smi_source,
                                                        title,
                                                        dateFormatHoursFirst.format(pubDate),
                                                        link
                                                };
                                                Gui.model.addRow(row);

                                                //SQLite
                                                String[] subStr = title.split(" ");
                                                for (String s : subStr) {
                                                    if (s.length() > 3) {
                                                        assert st != null;
                                                        st.setString(1, Common.delNoLetter(s).toLowerCase());
                                                        st.executeUpdate();
                                                    }
                                                }
                                                sqlite.insertTitleIn256(Common.sha256(title + pubDate));
                                            } else if (!Gui.todayOrNotCbx.getState()) {
                                                newsCount++;
                                                Gui.labelSum.setText(String.valueOf(newsCount));
                                                dataForEmail.add(newsCount + ") " + title + "\n" + link + "\n" + newsDescribe + "\n" +
                                                        smi_source + " - " + dateToEmail);

                                                Object[] row = new Object[]{
                                                        newsCount,
                                                        smi_source,
                                                        title,
                                                        dateFormatHoursFirst.format(pubDate),
                                                        link
                                                };
                                                Gui.model.addRow(row);

                                                //SQLite
                                                String[] subStr = title.split(" ");
                                                for (String s : subStr) {
                                                    if (s.length() > 3) {
                                                        assert st != null;
                                                        st.setString(1, Common.delNoLetter(s).toLowerCase());
                                                        st.executeUpdate();
                                                    }
                                                }
                                                sqlite.insertTitleIn256(Common.sha256(title + pubDate));
                                            }
                                        }
                                    }
                                }
                                if (isStop.get()) return;
                            }
                            if (!Gui.isOnlyLastNews && SQLite.isConnectionToSQLite) sqlite.deleteFrom256();
                        } catch (Exception no_rss) {
                            Gui.labelInfo.setText("RssList: " + (char) 34 + Common.smi_link.get(Common.smi_number) + (char) 34 + " is not available");
                        }
                    } catch (Exception e) {
                        Common.console("status: to many news.. please restart the application!");
                        isStop.set(true);
                    }
                }
                st.close();
                //Время поиска
                Gui.timeEnd = System.currentTimeMillis();
                searchTime = (Gui.timeEnd - Gui.timeStart) / 1000;
                DecimalFormat f = new DecimalFormat("##.00");
                if (!Gui.guiInTray.get()) Common.console("status: search completed in " + f.format(searchTime) + " s.");
                isSearchNow.set(false);

                isSearchFinished.set(true);
                Gui.progressBar.setValue(100);
                Gui.table.setAutoCreateRowSorter(true);
                Gui.tableForAnalysis.setAutoCreateRowSorter(true);
                Gui.searchAnimation.setText("total news: ");

                // итоги в трей
                if (newsCount != 0 && newsCount != modelRowCount && Gui.guiInTray.get())
                    Common.trayMessage("News found: " + newsCount);

                if (pSearchType.equals("word")) {
                    Gui.searchBtnTop.setVisible(true);
                    Gui.stopBtnTop.setVisible(false);
                } else if (pSearchType.equals("words")) {
                    Gui.searchBtnBottom.setVisible(true);
                    Gui.stopBtnBottom.setVisible(false);
                }

                // коммитим транзакцию
                String q_commit = "COMMIT";
                Statement st_commit = SQLite.connection.createStatement();
                st_commit.executeUpdate(q_commit);
                st_commit.close();

                // удаляем все пустые строки
                String q_del = "delete from news_dual where title = ''";
                Statement st_del = SQLite.connection.createStatement();
                st_del.executeUpdate(q_del);
                st_del.close();

                // Заполняем таблицу анализа
                if (!Gui.wasClickInTableForAnalysis.get()) sqlite.selectSqlite();

                // Автоматическая отправка результатов
                if (Gui.autoSendMessage.getState() && (Gui.model.getRowCount() > 0)) {
                    Gui.sendEmailBtn.doClick();
                }

                sqlite.deleteDuplicates();
                Gui.wasClickInTableForAnalysis.set(false);
                if (pSearchType.equals("word"))
                    Common.console("info: number of news items in the archive = " + sqlite.archiveNewsCount());
            } catch (Exception e) {
                try {
                    String q_begin = "ROLLBACK";
                    Statement st_begin = SQLite.connection.createStatement();
                    st_begin.executeUpdate(q_begin);
                    st_begin.close();
                } catch (SQLException ignored) {
                }
                isStop.set(true);
            }
        }
    }

    //Console search
    public void searchByConsole() {
        SQLite sqlite = new SQLite();
        if (!isSearchNow.get()) {
            dataForEmail.clear();
            sqlite.selectSources("smi");
            isSearchNow.set(true);
            Search.j = 1;
            newsCount = 0;

            try {
                // начало транзакции
                String q_begin = "BEGIN TRANSACTION";
                Statement st_begin = SQLite.connection.createStatement();
                st_begin.executeUpdate(q_begin);

                SyndParser parser = new SyndParser();
                for (Common.smi_number = 0; Common.smi_number < Common.smi_link.size(); Common.smi_number++) {
                    try {
                        try {
                            if (isStop.get()) return;
                            SyndFeed feed = parser.parseFeed(Common.smi_link.get(Common.smi_number));
                            for (Object message : feed.getEntries()) {
                                j++;
                                SyndEntry entry = (SyndEntry) message;
                                SyndContent content = entry.getDescription();
                                String smi_source = Common.smi_source.get(Common.smi_number);
                                String title = entry.getTitle();
                                assert content != null;
                                String newsDescribe = content.getValue()
                                        .trim()
                                        .replace("<p>", "")
                                        .replace("</p>", "")
                                        .replace("<br />", "");
                                if (newsDescribe.contains("<img")
                                        || newsDescribe.contains("href")
                                        || newsDescribe.contains("<div")
                                        || newsDescribe.contains("&#34")
                                        || newsDescribe.contains("<p lang")
                                        || newsDescribe.contains("&quot")
                                        || newsDescribe.contains("<span")
                                        || newsDescribe.contains("<ol")
                                        || newsDescribe.equals("")
                                ) newsDescribe = title;
                                Date pubDate = entry.getPublishedDate();
                                String dateToEmail = date_format.format(pubDate);
                                String link = entry.getLink();

                                // отсеиваем новости ранее 01.01.2021
                                if (pubDate.after(minDate)) checkDate = 1;
                                else checkDate = 0;

                                for (String it : Main.keywordsFromConsole) {
                                    if (it.equals(Main.keywordsFromConsole[0]) || it.equals(Main.keywordsFromConsole[1]))
                                        continue;

                                    if (title.toLowerCase().contains(it.toLowerCase()) && title.length() > 15 && checkDate == 1) {
                                        // отсеиваем новости которые были обнаружены ранее
                                        if (sqlite.isTitleExists(Common.sha256(title + pubDate)) && SQLite.isConnectionToSQLite) {
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
                                            sqlite.insertTitleIn256(Common.sha256(title + pubDate));
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

                // коммитим транзакцию
                String q_commit = "COMMIT";
                Statement st_commit = SQLite.connection.createStatement();
                st_commit.executeUpdate(q_commit);

                // удаляем все пустые строки
                String q_del = "delete from news_dual where title = ''";
                Statement st_del = SQLite.connection.createStatement();
                st_del.executeUpdate(q_del);

                // Автоматическая отправка результатов
                if (dataForEmail.size() > 0) {
                    Common.isSending.set(false);
                    EmailSender email = new EmailSender();
                    email.sendMessage();
                }
                sqlite.deleteDuplicates();
                Gui.wasClickInTableForAnalysis.set(false);

            } catch (Exception e) {
                e.printStackTrace();
                try {
                    String q_begin = "ROLLBACK";
                    Statement st_begin = SQLite.connection.createStatement();
                    st_begin.executeUpdate(q_begin);
                } catch (SQLException sql) {
                    sql.printStackTrace();
                }
            }
        }
    }
}
