package com.news;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class Search {
    static AtomicBoolean isStop = new AtomicBoolean(false);
    static AtomicBoolean isSearchNow = new AtomicBoolean(false);
    static AtomicBoolean isSearchFinished;
    static double searchTime;
    static int j = 1;
    static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    static LocalDateTime now = LocalDateTime.now();
    static String today = dtf.format(now);
    static SimpleDateFormat date_format = new SimpleDateFormat("dd.MM.yyyy HH:mm");

    //Main search
    public static void mainSearch() {
        if (!isSearchNow.get()) {
            Common.console("status: main search started");
            Main.LOGGER.log(Level.INFO, "Main search started");
            //выборка актуальных источников перед поиском из БД
            SQLite.selectSources("smi");
            isSearchNow.set(true);
            Gui.timeStart = System.currentTimeMillis();
            Common.text = "";
            Gui.labelInfo.setText("");
            Search.j = 1;
            Gui.model.setRowCount(0);
            Gui.model_for_analysis.setRowCount(0);
            Gui.q = 0;
            Gui.labelSum.setText("" + Gui.q);
            Search.isStop.set(false);
            Gui.find_word = Gui.textField.getText().toLowerCase();
            Gui.searchBtnTop.setVisible(false);
            Gui.stopBtnTop.setVisible(true);
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

                for (Common.smi_number = 0; Common.smi_number < Common.smi_link.size(); Common.smi_number++) {
                    try {
                        try {
                            RssFeedParser parser = new RssFeedParser(Common.smi_link.get(Common.smi_number));
                            if (isStop.get()) return;
                            Feed feed = parser.readFeed();
                            for (FeedMessage message : feed.getMessages()) {
                                j++;
                                if (message.toString().toLowerCase().contains(Gui.find_word) && message.getTitle().length() > 15) {
                                    //отсеиваем новости, которые уже были найдены ранее
                                    if (SQLite.isTitleExists(Common.sha256(message.getTitle() + message.getPubDate()))) {
                                        continue;
                                    }

                                    //Data for a table
                                    if (message.getPubDate() != null) {
                                        Date docDate = date_format.parse(message.getPubDate());
                                        Date curent_date = new Date();
                                        int date_diff = Common.compareDatesOnly(curent_date, docDate);

                                        // вставка в архив всех новостей
                                        try {
                                            if (!SQLite.isTitleInArchiveExists(message.getTitle() + message.getPubDate())) {
                                                SQLite.insertAllTitles(message.getTitle(), message.getPubDate());
                                            }
                                        } catch (Exception s) {
                                            //System.out.println(s.getMessage());
                                        }

                                        if (Gui.todayOrNotChbx.getState() && (date_diff != 0)) {
                                            Common.concatText(message.toString());
                                            Object[] row = new Object[]{
                                                    Gui.q,
                                                    Common.smi_source.get(Common.smi_number),
                                                    message.getTitle(),
                                                    message.getPubDate(),
                                                    message.getLink()
                                            };
                                            Gui.model.addRow(row);

                                            //SQLite
                                            String[] subStr = message.getTitle().split(" ");
                                            for (String s : subStr) {
                                                if (s.length() > 3) {
                                                    assert st != null;
                                                    st.setString(1, Common.delNoLetter(s).toLowerCase());
                                                    st.executeUpdate();
                                                }
                                            }
                                            SQLite.insertTitleIn256(Common.sha256(message.getTitle() + message.getPubDate()));

                                        } else if (!Gui.todayOrNotChbx.getState()) {
                                            Common.concatText(message.toString());
                                            Object[] row = new Object[]{
                                                    Gui.q,
                                                    Common.smi_source.get(Common.smi_number),
                                                    message.getTitle(),
                                                    message.getPubDate(),
                                                    message.getLink()
                                            };
                                            Gui.model.addRow(row);

                                            // SQLite
                                            String[] subStr = message.getTitle().split(" ");
                                            for (String s : subStr) {
                                                if (s.length() > 3) {
                                                    assert st != null;
                                                    st.setString(1, Common.delNoLetter(s).toLowerCase());
                                                    st.executeUpdate();
                                                }
                                            }
                                            SQLite.insertTitleIn256(Common.sha256(message.getTitle() + message.getPubDate()));
                                        }
                                    }
                                }
                                if (isStop.get()) return;
                            }
                            if (!Gui.isOnlyLastNews) SQLite.deleteFrom256();
                        } catch (Exception no_rss) {
                            Gui.labelInfo.setText("RssList: " + (char) 34 + Common.smi_link.get(Common.smi_number) + (char) 34 + " is not available");
                        }
                    } catch (Exception e) {
                        Common.console("status: to many news.. please restart the application!");
                        isStop.set(true);
                    }
                }
                isSearchFinished.set(true);
                Gui.progressBar.setValue(100);
                Gui.table.setAutoCreateRowSorter(true);
                Gui.table_for_analysis.setAutoCreateRowSorter(true);
                Gui.search_animation.setText("total news: ");
                Gui.searchBtnTop.setVisible(true);
                Gui.stopBtnTop.setVisible(false);

                // коммитим транзакцию
                String q_commit = "COMMIT";
                Statement st_commit = SQLite.connection.createStatement();
                st_commit.executeUpdate(q_commit);
                // удаляем все пустые строки
                String q_del = "delete from news_dual where title = ''";
                Statement st_del = SQLite.connection.createStatement();
                st_del.executeUpdate(q_del);
                // Заполняем таблицу анализа
                SQLite.selectSqlite();
                //Search time
                Gui.timeEnd = System.currentTimeMillis();
                searchTime = (Gui.timeEnd - Gui.timeStart) / 1000;
                DecimalFormat f = new DecimalFormat("##.00");
                Common.console("status: search completed in " + f.format(searchTime) + " s.");
                isSearchNow.set(false);

                //auto send after search
                if (Gui.autoSendMessage.getState() && (Gui.model.getRowCount() > 0)) {
                    Gui.sendEmailBtn.doClick();
                }

                Common.console("info: number of news items in the archive = " + SQLite.archiveNewsCount());
                Main.LOGGER.log(Level.INFO, "Main search finished");
            } catch (Exception e) {
                try {
                    String q_commit = "ROLLBACK";
                    Statement st_commit = SQLite.connection.createStatement();
                    st_commit.executeUpdate(q_commit);
                } catch (SQLException sql) {
                    sql.printStackTrace();
                }
                e.printStackTrace();
                isStop.set(true);
            }
        }
    }

    //Search by keywords
    public static void keywordsSearch() {
        if (!isSearchNow.get()) {
            Common.console("status: keywords search started");
            Main.LOGGER.log(Level.INFO, "Keywords search started");
            //выборка актуальных источников перед поиском из БД
            SQLite.selectSources("smi");
            isSearchNow.set(true);
            Gui.timeStart = System.currentTimeMillis();
            Common.text = "";
            Gui.labelInfo.setText("");
            Search.j = 1;
            Gui.model.setRowCount(0);
            Gui.model_for_analysis.setRowCount(0);
            Gui.q = 0;
            Gui.labelSum.setText("" + Gui.q);
            Search.isStop.set(false);
            Gui.searchBtnBottom.setVisible(false);
            Gui.stopBtnBottom.setVisible(true);
            isSearchFinished = new AtomicBoolean(false);
            Common.statusLabel(isSearchFinished, "Searching");
            Gui.sendEmailBtn.setIcon(Gui.send);
            new Thread(Common::fill).start();
            try {
                PreparedStatement st = SQLite.connection.prepareStatement("insert into news_dual(title) values (?)");
                String q_begin = "BEGIN TRANSACTION";
                Statement st_begin = SQLite.connection.createStatement();
                st_begin.executeUpdate(q_begin);
                for (Common.smi_number = 0; Common.smi_number < Common.smi_link.size(); Common.smi_number++) {
                    try {
                        try {
                            RssFeedParser parser = new RssFeedParser(Common.smi_link.get(Common.smi_number));
                            if (isStop.get()) return;
                            Feed feed = parser.readFeed();
                            for (FeedMessage message : feed.getMessages()) {
                                j++;
                                for (String it : Common.getKeywordsFromFile()) {
                                    if (message.toString().toLowerCase().contains(it.toLowerCase()) && message.getTitle().length() > 15) {
                                        // отсеиваем новости которые были обнаружены ранее
                                        if (SQLite.isTitleExists(Common.sha256(message.getTitle() + message.getPubDate()))) {
                                            continue;
                                        }
                                        //Data for a table
                                        if (message.getPubDate() != null) {
                                            Date docDate = date_format.parse(message.getPubDate());
                                            Date curent_date = new Date();
                                            int date_diff = Common.compareDatesOnly(curent_date, docDate);

                                            if (Gui.todayOrNotChbx.getState() && (date_diff != 0)) {
                                                Common.concatText(message.toString());
                                                Object[] row = new Object[]{
                                                        Gui.q,
                                                        Common.smi_source.get(Common.smi_number),
                                                        message.getTitle(),
                                                        message.getPubDate(),
                                                        message.getLink()
                                                };
                                                Gui.model.addRow(row);

                                                //SQLite
                                                String[] subStr = message.getTitle().split(" ");
                                                for (String s: subStr) {
                                                    if (s.length() > 3) {
                                                        assert st != null;
                                                        st.setString(1, Common.delNoLetter(s).toLowerCase());
                                                        st.executeUpdate();
                                                    }
                                                }
                                                SQLite.insertTitleIn256(Common.sha256(message.getTitle() + message.getPubDate()));
                                            } else if (!Gui.todayOrNotChbx.getState()) {
                                                Common.concatText(message.toString());
                                                Object[] row = new Object[]{
                                                        Gui.q,
                                                        Common.smi_source.get(Common.smi_number),
                                                        message.getTitle(),
                                                        message.getPubDate(),
                                                        message.getLink()
                                                };
                                                Gui.model.addRow(row);

                                                //SQLite
                                                String[] subStr = message.getTitle().split(" ");
                                                for (String s: subStr) {
                                                    if (s.length() > 3) {
                                                        assert st != null;
                                                        st.setString(1, Common.delNoLetter(s).toLowerCase());
                                                        st.executeUpdate();
                                                    }
                                                }
                                                SQLite.insertTitleIn256(Common.sha256(message.getTitle() + message.getPubDate()));
                                            }
                                        }
                                    }
                                }
                                if (isStop.get()) return;
                            }
                            if (!Gui.isOnlyLastNews) SQLite.deleteFrom256();
                        } catch (Exception no_rss) {
                            Gui.labelInfo.setText("RssList: " + (char) 34 + Common.smi_link.get(Common.smi_number) + (char) 34 + " is not available");
                        }
                    } catch (Exception e) {
                        Common.console("status: to many news.. Please restart the application!");
                        isStop.set(true);
                    }
                }
                isSearchFinished.set(true);
                Gui.progressBar.setValue(100);
                Gui.table.setAutoCreateRowSorter(true);
                Gui.table_for_analysis.setAutoCreateRowSorter(true);
                Gui.search_animation.setText("total news: ");
                Gui.searchBtnBottom.setVisible(true);
                Gui.stopBtnBottom.setVisible(false);

                String q_commit = "COMMIT";
                Statement st_commit = SQLite.connection.createStatement();
                st_commit.executeUpdate(q_commit);
                String q_del = "delete from news_dual where title = ''";
                Statement st_del = SQLite.connection.createStatement();
                st_del.executeUpdate(q_del);
                SQLite.selectSqlite();

                //Search time
                Gui.timeEnd = System.currentTimeMillis();
                searchTime = (Gui.timeEnd - Gui.timeStart) / 1000;
                DecimalFormat f = new DecimalFormat("##.00");
                Common.console("status: search completed in " + f.format(searchTime) + " s.");
                isSearchNow.set(false);

                //auto send after search
                if (Gui.autoSendMessage.getState() && (Gui.model.getRowCount() > 0)) {
                    Gui.sendEmailBtn.doClick();
                }

                Main.LOGGER.log(Level.INFO, "Keywords search finished");
            } catch (Exception e) {
                try {
                    String q_commit = "ROLLBACK";
                    Statement st_commit = SQLite.connection.createStatement();
                    st_commit.executeUpdate(q_commit);
                } catch (SQLException sql) {
                    sql.printStackTrace();
                }
                e.printStackTrace();
                isStop.set(true);
            }
        }
    }
}
