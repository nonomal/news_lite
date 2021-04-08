import java.sql.PreparedStatement;
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

    private static String delNoLetter(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i)))
                sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    //Main search
    public static void mainSearch() {
        if (!isSearchNow.get()) {
            Main.LOGGER.log(Level.INFO, "Main search started");
            //выборка актуальных источников перед поиском из БД
            SQLite.selectSources();
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
                                        }
                                    }

                                    // SQLite
                                    String[] subStr = message.getTitle().split(" ");
                                    for (String s : subStr) {
                                        //System.out.println(delNoLetter(s).toLowerCase());
                                        if (s.length() > 3) {
                                            assert st != null;
                                            st.setString(1, delNoLetter(s).toLowerCase());
                                            st.executeUpdate();
                                        }
                                    }

                                }
                                if (isStop.get()) return;
                            }
                        } catch (Exception no_rss) {
                            Gui.labelInfo.setText("RssList: " + (char) 34 + Common.smi_link.get(Common.smi_number) + (char) 34 + " is not available");
                        }
                    } catch (Exception e) {
                        Common.console("[avandy@mrprogre ~]$ to many news.. please restart the application!");
                        isStop.set(true);
                    }
                }
                isSearchFinished.set(true);
                Gui.progressBar.setValue(100);
                Gui.table.setAutoCreateRowSorter(true);
                Gui.search_animation.setText("total news: ");
                Gui.searchBtnTop.setVisible(true);
                Gui.stopBtnTop.setVisible(false);

                String q_commit = "COMMIT";
                Statement st_commit = SQLite.connection.createStatement();
                st_commit.executeUpdate(q_commit);
                SQLite.selectSqlite();
                SQLite.deleteTitles();

                //Search time
                Gui.timeEnd = System.currentTimeMillis();
                searchTime = (Gui.timeEnd - Gui.timeStart) / 1000;
                DecimalFormat f = new DecimalFormat("##.00");
                Common.console("[avandy@mrprogre ~]$ search completed in " + f.format(searchTime) + " s.");
                isSearchNow.set(false);

                //auto send after search
                if (Gui.autoSendMessage.getState() && (Gui.model.getRowCount() > 0)) {
                    Gui.sendEmailBtn.doClick();
                }
            } catch (Exception e) {
                e.printStackTrace();
                isStop.set(true);
            }
        }
    }

    //Search by keywords
    public static void keywordsSearch() {
        if (!isSearchNow.get()) {
            Main.LOGGER.log(Level.INFO, "Keywords search started");
            //выборка актуальных источников перед поиском из БД
            SQLite.selectSources();
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
                                                    //System.out.println(delNoLetter(s).toLowerCase());
                                                    if (s.length() > 3) {
                                                        assert st != null;
                                                        st.setString(1, delNoLetter(s).toLowerCase());
                                                        st.executeUpdate();
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (isStop.get()) return;
                            }
                        } catch (Exception no_rss) {
                            Gui.labelInfo.setText("RssList: " + (char) 34 + Common.smi_link.get(Common.smi_number) + (char) 34 + " is not available");
                        }
                    } catch (Exception e) {
                        Common.console("[avandy@mrprogre ~]$ to many news.. Please restart the application!");
                        isStop.set(true);
                    }
                }
                isSearchFinished.set(true);
                Gui.progressBar.setValue(100);
                Gui.table.setAutoCreateRowSorter(true);
                Gui.search_animation.setText("total news: ");
                Gui.searchBtnBottom.setVisible(true);
                Gui.stopBtnBottom.setVisible(false);

                String q_commit = "COMMIT";
                Statement st_commit = SQLite.connection.createStatement();
                st_commit.executeUpdate(q_commit);
                SQLite.selectSqlite();
                SQLite.deleteTitles();

                //Search time
                Gui.timeEnd = System.currentTimeMillis();
                searchTime = (Gui.timeEnd - Gui.timeStart) / 1000;
                DecimalFormat f = new DecimalFormat("##.00");
                Common.console("[avandy@mrprogre ~]$ search completed in " + f.format(searchTime) + " s.");
                isSearchNow.set(false);

                //auto send after search
                if (Gui.autoSendMessage.getState() && (Gui.model.getRowCount() > 0)) {
                    Gui.sendEmailBtn.doClick();
                }
            } catch (Exception e) {
                e.printStackTrace();
                isStop.set(true);
            }
        }
    }
}
