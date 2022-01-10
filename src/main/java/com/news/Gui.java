package com.news;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.table.*;
import javax.swing.text.DefaultCaret;

public class Gui extends JFrame {
    SQLite sqlite = new SQLite();
    Search search = new Search();
    ExportToExcel exp = new ExportToExcel();
    private final long autoStartTimer = 60000L; // 60 секунд
    static final String[] intervals = {"1 min", "5 min", "15 min", "30 min", "45 min", "1 hour", "2 hours", "4 hours", "8 hours", "12 hours", "24 hours", "48 hours"};
    static ImageIcon logoIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/logo.png")));
    static ImageIcon send = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/send.png")));
    static ImageIcon send2 = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/send2.png")));
    static ImageIcon send3 = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/send3.png")));
    static ImageIcon searchIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/search.png")));
    static ImageIcon stopIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/stop.png")));
    static ImageIcon clearIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/clear.png")));
    static ImageIcon excelIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/excel.png")));
    static ImageIcon createIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/create.png")));
    static ImageIcon deleteIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/delete.png")));
    static ImageIcon fontIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/font.png")));
    static int q = 1;
    static double timeStart;
    static double timeEnd;
    static boolean isSelTitle = true;
    static boolean isSelLink = true;
    static boolean isOnlyLastNews = false;
    static boolean isInKeywords = false;
    static String findWord = "";
    static String sendTo;
    static JScrollPane scrollPane;
    static JTable table;
    static JTable tableForAnalysis;
    static DefaultTableModel model;
    static DefaultTableModel modelForAnalysis;
    static JTextField topKeyword;
    static JTextField sendEmailTo;
    static JTextField addKeywordToList;
    static JTextArea animationStatus;
    static JComboBox<String> keywordsCbox;
    static JComboBox<String> newsIntervalCbox;
    static JLabel labelSign;
    static JLabel labelSum;
    static JLabel labelInfo;
    static JLabel timeLbl;
    static JLabel searchAnimation;
    static JLabel connectToBdLabel;
    static JLabel lblLogSourceSqlite;
    static JButton searchBtnTop;
    static JButton searchBtnBottom;
    static JButton stopBtnTop;
    static JButton stopBtnBottom;
    static JButton sendEmailBtn;
    static JButton smiBtn;
    static JButton logBtn;
    static JButton exclBtn;
    static Checkbox todayOrNotCbx;
    static Checkbox searchInTitleCbx;
    static Checkbox searchInLinkCbx;
    static Checkbox autoUpdateNewsTop;
    static Checkbox autoUpdateNewsBottom;
    static Checkbox autoSendMessage;
    static Checkbox filterNewsChbx;
    static JProgressBar progressBar;
    static Timer timer;
    static TimerTask timerTask;
    static AtomicBoolean wasClickInTableForAnalysis = new AtomicBoolean(false);
    static AtomicBoolean guiInTray = new AtomicBoolean(false);

    public Gui() {
        setResizable(false);
        getContentPane().setBackground(new Color(42, 42, 42));
        setTitle("Avandy News");
        setIconImage(logoIco.getImage());
        setFont(new Font("SansSerif", Font.PLAIN, 12));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(500, 100, 817, 585);
        getContentPane().setLayout(null);

        //Action Listener for EXIT_ON_CLOSE
        addWindowListener(new WindowAdapter() {
            // закрытие окна
            @Override
            public void windowClosing(WindowEvent e) {
                SQLite.isConnectionToSQLite = false;
                Main.LOGGER.log(Level.INFO, "Application closed");
                Common.saveState();
                if (SQLite.isConnectionToSQLite) sqlite.closeSQLiteConnection();
            }
            // сворачивание в трей
            @Override
            public void windowIconified(WindowEvent pEvent) {
                guiInTray.set(true);
                setVisible(false);
                if (autoUpdateNewsBottom.getState()) animationStatus.setText("");
            }
            // разворачивание из трея
            public void windowDeiconified(WindowEvent pEvent) {
                guiInTray.set(false);
            }
        });

        //Input keyword
        JLabel lblNewLabel = new JLabel("Keyword:");
        lblNewLabel.setForeground(new Color(255, 179, 131));
        lblNewLabel.setBounds(10, 9, 71, 19);
        lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 15));
        lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
        getContentPane().add(lblNewLabel);

        //Table
        scrollPane = new JScrollPane();
        scrollPane.setBounds(10, 40, 781, 300);
        getContentPane().add(scrollPane);
        Object[] columns = {"Num", "Source", "Title (double click to open the link)", "Date", "Link"};
        model = new DefaultTableModel(new Object[][]{
        }, columns) {
            final boolean[] columnEditables = new boolean[]{
                    false, false, false, false, false
            };

            public boolean isCellEditable(int row, int column) {
                return columnEditables[column];
            }

            // Сортировка
            final Class[] types_unique = {Integer.class, String.class, String.class, /*Date.class*/ String.class, String.class};

            @Override
            public Class getColumnClass(int columnIndex) {
                return this.types_unique[columnIndex];
            }
        };
        table = new JTable(model) {
            // tooltips
            public String getToolTipText(MouseEvent e) {
                String tip = null;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = 2;
                try {
                    tip = (String) getValueAt(rowIndex, colIndex);
                } catch (RuntimeException ignored) {
                }
                assert tip != null;
                if (tip.length() > 80) {
                    return tip;
                } else return null;
            }
        };
        //headers
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Tahoma", Font.BOLD, 13));
        //Cell alignment
        DefaultTableCellRenderer Renderer = new DefaultTableCellRenderer();
        Renderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(Renderer);
        table.setRowHeight(23);
        table.setColumnSelectionAllowed(true);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(500);
        table.getColumnModel().getColumn(3).setPreferredWidth(45);
        table.getColumnModel().getColumn(4).setMaxWidth(10);
        scrollPane.setViewportView(table);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.convertRowIndexToModel(table.rowAtPoint(new Point(e.getX(), e.getY()))); // при сортировке строк оставляет верные данные
                    int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
                    if (col == 2|| col == 4) {
                        String url = (String) table.getModel().getValueAt(row, 4);
                        URI uri = null;
                        try {
                            uri = new URI(url);
                        } catch (URISyntaxException ex) {
                            ex.printStackTrace();
                            Main.LOGGER.log(Level.WARNING, ex.getMessage());
                        }
                        Desktop desktop = Desktop.getDesktop();
                        assert uri != null;
                        try {
                            desktop.browse(uri);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            Main.LOGGER.log(Level.WARNING, ex.getMessage());
                        }
                    }
                }
            }
        });

        // Label for table for analysis
        JLabel tableForAnalysisLabel = new JLabel();
        tableForAnalysisLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
        tableForAnalysisLabel.setText("word frequency:");
        tableForAnalysisLabel.setToolTipText("matches more than " + sqlite.getWordFreqMatches());
        tableForAnalysisLabel.setForeground(new Color(255, 255, 153));
        tableForAnalysisLabel.setBounds(11, 343, 190, 14);
        getContentPane().add(tableForAnalysisLabel);

        //Table for analysis
        JScrollPane scrollForAnalysis = new JScrollPane();
        scrollForAnalysis.setBounds(10, 360, 300, 120);
        getContentPane().add(scrollForAnalysis);

        String[] columnsForAnalysis = {"total", "words", "del"};
        modelForAnalysis = new DefaultTableModel(new Object[][]{}, columnsForAnalysis) {
            final boolean[] column_for_analysis = new boolean[]{false, false, true};
            public boolean isCellEditable(int row, int column) {
                return column_for_analysis[column];
            }

            // Сортировка
            final Class[] types_unique = {Integer.class, String.class, Button.class};

            @Override
            public Class getColumnClass(int columnIndex) {
                return this.types_unique[columnIndex];
            }
        };
        tableForAnalysis = new JTable(modelForAnalysis);
        JTableHeader header_for_analysis = tableForAnalysis.getTableHeader();
        header_for_analysis.setFont(new Font("Tahoma", Font.BOLD, 13));
        //Cell alignment
        DefaultTableCellRenderer rendererForAnalysis = new DefaultTableCellRenderer();
        rendererForAnalysis.setHorizontalAlignment(JLabel.CENTER);
        tableForAnalysis.getColumnModel().getColumn(0).setCellRenderer(rendererForAnalysis);
        tableForAnalysis.getColumnModel().getColumn(1).setCellRenderer(rendererForAnalysis);
        tableForAnalysis.getColumn("del").setCellRenderer(new ButtonColumn(tableForAnalysis, 2));
        tableForAnalysis.setRowHeight(20);
        tableForAnalysis.setColumnSelectionAllowed(true);
        tableForAnalysis.setCellSelectionEnabled(true);
        tableForAnalysis.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tableForAnalysis.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tableForAnalysis.getColumnModel().getColumn(0).setPreferredWidth(80);
        tableForAnalysis.getColumnModel().getColumn(1).setPreferredWidth(140);
        tableForAnalysis.getColumnModel().getColumn(2).setPreferredWidth(30);
        scrollForAnalysis.setViewportView(tableForAnalysis);

        // запуск поиска по слову из таблицы анализа
        tableForAnalysis.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tableForAnalysis.convertRowIndexToModel(tableForAnalysis.rowAtPoint(new Point(e.getX(), e.getY())));
                    int col = tableForAnalysis.columnAtPoint(new Point(e.getX(), e.getY()));
                    if (col == 1) {
                        Gui.topKeyword.setText((String) tableForAnalysis.getModel().getValueAt(row, 1));
                        searchBtnTop.doClick();
                        wasClickInTableForAnalysis.set(true);
                    }
                }

            }
        });

        //Keyword field
        topKeyword = new JTextField(findWord);
        topKeyword.setBounds(87, 9, 99, 21);
        topKeyword.setFont(new Font("Tahoma", Font.BOLD, 13));
        getContentPane().add(topKeyword);

        //Search addNewSource
        searchBtnTop = new JButton("");
        searchBtnTop.setIcon(searchIco);
        searchBtnTop.setBackground(new Color(154, 237, 196));
        searchBtnTop.setFont(new Font("Tahoma", Font.BOLD, 10));
        searchBtnTop.setBounds(192, 9, 30, 22);
        getContentPane().add(searchBtnTop);
        // Search by Enter
        getRootPane().setDefaultButton(searchBtnTop);
        searchBtnTop.requestFocus();
        searchBtnTop.doClick();
        searchBtnTop.addActionListener(e -> new Thread(() -> search.mainSearch("word")).start());

        //Stop addNewSource
        stopBtnTop = new JButton("");
        stopBtnTop.setIcon(stopIco);
        stopBtnTop.setBackground(new Color(255, 208, 202));
        stopBtnTop.setBounds(192, 9, 30, 22);
        stopBtnTop.addActionListener(e -> {
            try {
                Search.isSearchFinished.set(true);
                Search.isStop.set(true);
                searchAnimation.setText("Stopped");
                Common.console("status: search stopped");
                searchBtnTop.setVisible(true);
                stopBtnTop.setVisible(false);
                Search.isSearchNow.set(false);
                try {
                    String q_begin = "ROLLBACK";
                    Statement st_begin = SQLite.connection.createStatement();
                    st_begin.executeUpdate(q_begin);
                } catch (SQLException ignored) {
                }
            } catch (Exception t) {
                Common.console("status: there is no threads to stop");
            }
        });
        getContentPane().add(stopBtnTop);

        //Clear addNewSource
        JButton clearBtnTop = new JButton();
        clearBtnTop.setToolTipText("Clear the list");
        clearBtnTop.setBackground(new Color(250, 128, 114));
        clearBtnTop.setIcon(clearIco);
        clearBtnTop.setBounds(760, 9, 30, 22);

        clearBtnTop.addActionListener(e -> {
            try {
                if (model.getRowCount() == 0) {
                    Common.console("status: no data to clear");
                    return;
                }
                labelInfo.setText("");
                Search.j = 1;
                model.setRowCount(0);
                modelForAnalysis.setRowCount(0);
                q = 0;
                labelSum.setText("" + q);
                Common.console("status: clear");
            } catch (Exception t) {
                Common.console(t.getMessage());
                t.printStackTrace();
                Main.LOGGER.log(Level.WARNING, t.getMessage());
            }
        });
        getContentPane().add(clearBtnTop);

        //Amount of news
        labelSum = new JLabel();
        labelSum.setBounds(80, 530, 115, 13);
        labelSum.setFont(new Font("Tahoma", Font.PLAIN, 11));
        labelSum.setForeground(new Color(255, 255, 153));
        labelSum.setBackground(new Color(240, 255, 240));
        getContentPane().add(labelSum);

        //Another info
        labelInfo = new JLabel();
        labelInfo.setBounds(125, 585, 300, 13);
        labelInfo.setFont(new Font("Tahoma", Font.PLAIN, 11));
        labelInfo.setForeground(new Color(149, 255, 118));
        labelInfo.setBackground(new Color(240, 255, 240));
        getContentPane().add(labelInfo);

        //My sign
        labelSign = new JLabel(":mrprogre");
        labelSign.setForeground(new Color(255, 160, 122));
        labelSign.setEnabled(false);
        labelSign.setFont(new Font("Tahoma", Font.BOLD, 11));
        labelSign.setBounds(730, 525, 57, 14);
        getContentPane().add(labelSign);
        labelSign.addMouseListener(new MouseAdapter() {
            // наведение мышки на письмо
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!labelSign.isEnabled()) {
                    labelSign.setEnabled(true);
                }
            }

            // убрали мышку с письма
            @Override
            public void mouseExited(MouseEvent e) {
                if (labelSign.isEnabled()) {
                    labelSign.setEnabled(false);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    String url = "https://github.com/mrprogre";
                    URI uri = null;
                    try {
                        uri = new URI(url);
                    } catch (URISyntaxException ex) {
                        ex.printStackTrace();
                    }
                    Desktop desktop = Desktop.getDesktop();
                    assert uri != null;
                    try {
                        desktop.browse(uri);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        Main.LOGGER.log(Level.WARNING, ex.getMessage());
                    }

                }
            }
        });

        // Search in title
        searchInTitleCbx = new Checkbox("Search in title", true);
        searchInTitleCbx.setForeground(new Color(255, 227, 163));
        searchInTitleCbx.setFocusable(false);
        searchInTitleCbx.setFont(new Font("Serif", Font.BOLD, 12));
        searchInTitleCbx.setBounds(10, 490, 120, 13);
        getContentPane().add(searchInTitleCbx);
        searchInTitleCbx.addItemListener(e -> isSelTitle = searchInTitleCbx.getState());

        // Search in link
        searchInLinkCbx = new Checkbox("Search in link", false);
        searchInLinkCbx.setForeground(new Color(255, 227, 163));
        searchInLinkCbx.setFocusable(false);
        searchInLinkCbx.setFont(new Font("Serif", Font.BOLD, 12));
        searchInLinkCbx.setBounds(10, 509, 120, 13);
        getContentPane().add(searchInLinkCbx);
        searchInLinkCbx.addItemListener(e -> isSelLink = searchInLinkCbx.getState());

        //send e-mail to
        sendEmailTo = new JTextField("enter your email");
        sendEmailTo.setBounds(636, 365, 126, 21);
        sendEmailTo.setFont(new Font("Serif", Font.PLAIN, 12));
        getContentPane().add(sendEmailTo);
        //send e-mail to - label
        JLabel lblSendToEmail = new JLabel();
        lblSendToEmail.setText("send to:");
        lblSendToEmail.setForeground(new Color(255, 255, 153));
        lblSendToEmail.setFont(new Font("Tahoma", Font.PLAIN, 11));
        lblSendToEmail.setBounds(631, 342, 83, 14);
        getContentPane().add(lblSendToEmail);

        //Send e-mail addNewSource
        sendEmailBtn = new JButton();
        sendEmailBtn.setIcon(send);
        sendEmailBtn.setToolTipText("send the current result");
        sendEmailBtn.setFocusable(false);
        sendEmailBtn.setContentAreaFilled(false);
        sendEmailBtn.setBorderPainted(false);
        sendEmailBtn.setBackground(new Color(255, 255, 153));
        sendEmailBtn.setBounds(760, 364, 32, 23);
        sendEmailBtn.addActionListener(e -> {
            if (model.getRowCount() > 0 && sendEmailTo.getText().contains("@")) {
                Common.console("status: sending e-mail");
                sendTo = sendEmailTo.getText();
                Common.isSending.set(false);
                Common.statusLabel(Common.isSending, "sending");
                new Thread(Common::fill).start();
                EmailSender email = new EmailSender();
                new Thread(email::sendMessage).start();
            }
        });

        sendEmailBtn.addMouseListener(new MouseAdapter() {
            // наведение мышки на письмо
            @Override
            public void mouseEntered(MouseEvent e) {
                if (sendEmailBtn.getIcon() == send) {
                    sendEmailBtn.setIcon(send2);
                }
            }

            @Override
            // убрали мышку с письма
            public void mouseExited(MouseEvent e) {
                if (sendEmailBtn.getIcon() == send2) {
                    sendEmailBtn.setIcon(send);
                }
            }

        });
        getContentPane().add(sendEmailBtn);

        // Автоматическая отправка письма с результатами
        autoSendMessage = new Checkbox("auto send results");
        autoSendMessage.setState(false);
        autoSendMessage.setFocusable(false);
        autoSendMessage.setForeground(Color.WHITE);
        autoSendMessage.setFont(new Font("Arial", Font.PLAIN, 11));
        autoSendMessage.setBounds(636, 388, 135, 20);
        getContentPane().add(autoSendMessage);

        //Export to excel
        JButton exportBtn = new JButton();
        exportBtn.setIcon(excelIco);
        exportBtn.setToolTipText("Export news to excel");
        exportBtn.setBackground(new Color(255, 251, 183));
        exportBtn.setBounds(725, 9, 30, 22);
        exportBtn.addActionListener(e -> {
            if (model.getRowCount() != 0) {
                new Thread(exp::export_from_RSS_to_excel).start();
                Common.console("status: export");
            } else {
                Common.console("status: there is no data to export");
            }
        });
        getContentPane().add(exportBtn);

        // Выбор цвета шрифта в таблице
        JButton fontColorBtn = new JButton();
        fontColorBtn.setToolTipText("Font color");
        fontColorBtn.setBackground(new Color(189, 189, 189));
        fontColorBtn.setIcon(fontIco);
        fontColorBtn.setBounds(690, 9, 30, 22);
        fontColorBtn.addActionListener(e -> {
            Color color = JColorChooser.showDialog(null, "Color", Color.black);
            if (color != null) {
                try {
                    table.setForeground(color);
                    tableForAnalysis.setForeground(color);
                    Common.delSettings("fontColorRed");
                    Common.delSettings("fontColorGreen");
                    Common.delSettings("fontColorBlue");
                    Common.writeToConfig(String.valueOf(color.getRed()), "fontColorRed");
                    Common.writeToConfig(String.valueOf(color.getGreen()), "fontColorGreen");
                    Common.writeToConfig(String.valueOf(color.getBlue()), "fontColorBlue");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        getContentPane().add(fontColorBtn);


        /* BOTTOM SEARCH */

        //Add to combo box
        addKeywordToList = new JTextField();
        addKeywordToList.setFont(new Font("Serif", Font.PLAIN, 12));
        addKeywordToList.setBounds(321, 490, 60, 22);
        getContentPane().add(addKeywordToList);

        //Add to combo box
        JButton btnAddKeywordToList = new JButton("");
        getContentPane().add(btnAddKeywordToList);
        btnAddKeywordToList.addActionListener(e -> {
            if (addKeywordToList.getText().length() > 0) {
                String word = addKeywordToList.getText();
                for (int i = 0; i < keywordsCbox.getItemCount(); i++) {
                    if (word.equals(keywordsCbox.getItemAt(i))){
                        Common.console("info: список ключевых слов уже содержит: " + word);
                        isInKeywords = true;
                    } else {
                        isInKeywords = false;
                    }
                }
                if (!isInKeywords) {
                    Common.writeToConfig(word, "keyword");
                    keywordsCbox.addItem(word);
                    isInKeywords = false;
                }
                addKeywordToList.setText("");
            }
        });
        btnAddKeywordToList.setIcon(createIco);
        btnAddKeywordToList.setBounds(386, 490, 30, 22);

        //Delete from combo box
        JButton btnDelFromList = new JButton("");
        btnDelFromList.addActionListener(e -> {
            if (keywordsCbox.getItemCount() > 0) {
                try {
                    String item = (String) keywordsCbox.getSelectedItem();
                    keywordsCbox.removeItem(item);
                    Common.delSettings("keyword=" + Objects.requireNonNull(item));
                } catch (IOException io) {
                    io.printStackTrace();
                    Main.LOGGER.log(Level.WARNING, io.getMessage());
                }
            }

        });
        btnDelFromList.setIcon(deleteIco);
        btnDelFromList.setBounds(421, 490, 30, 22);
        getContentPane().add(btnDelFromList);

        //Keywords combo box
        keywordsCbox = new JComboBox<>();
        keywordsCbox.setFont(new Font("Arial", Font.PLAIN, 11));
        keywordsCbox.setModel(new DefaultComboBoxModel<>());
        keywordsCbox.setEditable(false);
        keywordsCbox.setBounds(456, 490, 90, 22);
        getContentPane().add(keywordsCbox);

        //Bottom search by keywords
        searchBtnBottom = new JButton("");
        searchBtnBottom.setIcon(searchIco);
        searchBtnBottom.setFont(new Font("Tahoma", Font.BOLD, 10));
        searchBtnBottom.setBackground(new Color(154, 237, 196));
        searchBtnBottom.setBounds(552, 490, 30, 22);
        //searchBtnBottom.addActionListener(e -> new Thread(Search::keywordsSearch).start());
        searchBtnBottom.addActionListener(e -> new Thread(() -> search.mainSearch("words")).start());
        getContentPane().add(searchBtnBottom);

        //Stop addNewSource (bottom)
        stopBtnBottom = new JButton("");
        stopBtnBottom.setIcon(stopIco);
        stopBtnBottom.setBackground(new Color(255, 208, 202));
        stopBtnBottom.setBounds(552, 490, 30, 22);
        stopBtnBottom.addActionListener(e -> {
            try {
                Search.isSearchFinished.set(true);
                Search.isStop.set(true);
                searchAnimation.setText("Stopped");
                Common.console("status: search stopped");
                searchBtnBottom.setVisible(true);
                stopBtnBottom.setVisible(false);
                Search.isSearchNow.set(false);
                try {
                    String q_begin = "ROLLBACK";
                    Statement st_begin = SQLite.connection.createStatement();
                    st_begin.executeUpdate(q_begin);
                } catch (SQLException ignored) {
                }
            } catch (Exception t) {
                Common.console("status: there is no threads to stop");
            }
        });
        getContentPane().add(stopBtnBottom);

        // Автозапуск поиска по ключевым словам каждые 30 секунд
        autoUpdateNewsBottom = new Checkbox("auto upd.");
        autoUpdateNewsBottom.setState(false);
        autoUpdateNewsBottom.setFocusable(false);
        autoUpdateNewsBottom.setForeground(Color.WHITE);
        autoUpdateNewsBottom.setFont(new Font("Arial", Font.PLAIN, 11));
        autoUpdateNewsBottom.setBounds(553, 516, 75, 20);
        getContentPane().add(autoUpdateNewsBottom);
        autoUpdateNewsBottom.addItemListener(e -> {
            if (autoUpdateNewsBottom.getState()) {
                timer = new Timer(true);
                timerTask = new MyTimerTask();
                timer.scheduleAtFixedRate(timerTask, 0, autoStartTimer);
                searchBtnBottom.setVisible(false);
                stopBtnBottom.setVisible(true);
                autoUpdateNewsTop.setVisible(false);
            } else {
                timer.cancel();
                searchBtnBottom.setVisible(true);
                stopBtnBottom.setVisible(false);
                autoUpdateNewsTop.setVisible(true);
                try {
                    stopBtnTop.doClick();
                } catch (Exception ignored){

                }
            }
        });

        //Console - textarea
        animationStatus = new JTextArea();
        // авто скроллинг
        DefaultCaret caret = (DefaultCaret) animationStatus.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        animationStatus.setCaretPosition(animationStatus.getDocument().getLength());
        animationStatus.setAutoscrolls(true);
        animationStatus.setLineWrap(true);
        animationStatus.setEditable(false);
        animationStatus.setBounds(320, 360, 300, 120);
        animationStatus.setFont(new Font("Tahoma", Font.PLAIN, 11));
        animationStatus.setForeground(SystemColor.white);
        animationStatus.setBackground(new Color(83, 82, 82));
        getContentPane().add(animationStatus);
        //Console - scroll
        JScrollPane console_scroll = new JScrollPane(animationStatus, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        console_scroll.setBounds(320, 360, 300, 120);
        getContentPane().add(console_scroll);
        //Console - label
        JLabel lblConsole = new JLabel();
        lblConsole.setText("console:");
        lblConsole.setForeground(new Color(255, 255, 153));
        lblConsole.setFont(new Font("Tahoma", Font.PLAIN, 11));
        lblConsole.setBounds(321, 342, 83, 14);
        getContentPane().add(lblConsole);

        // Clear console
        JButton clearConsoleBtn = new JButton();
        clearConsoleBtn.setIcon(clearIco);
        clearConsoleBtn.setToolTipText("Clear the console");
        clearConsoleBtn.setBackground(new Color(179, 221, 254));
        clearConsoleBtn.setBounds(588, 490, 32, 23);
        clearConsoleBtn.addActionListener(e -> animationStatus.setText(""));
        getContentPane().add(clearConsoleBtn);

        //Searching animation
        searchAnimation = new JLabel();
        searchAnimation.setForeground(new Color(255, 255, 153));
        searchAnimation.setFont(new Font("Tahoma", Font.PLAIN, 11));
        searchAnimation.setBackground(new Color(240, 255, 240));
        searchAnimation.setBounds(10, 530, 80, 13);
        getContentPane().add(searchAnimation);

        //Time label
        timeLbl = new JLabel();
        timeLbl.setForeground(new Color(255, 255, 153));
        timeLbl.setFont(new Font("Tahoma", Font.PLAIN, 11));
        timeLbl.setBounds(207, 480, 160, 20);
        getContentPane().add(timeLbl);

        // Шкала прогресса
        progressBar = new JProgressBar();
        progressBar.setFocusable(false);
        progressBar.setMaximum(100);
        progressBar.setBorderPainted(false);
        progressBar.setForeground(new Color(10, 255, 41));
        progressBar.setBackground(new Color(1, 1, 1));
        progressBar.setBounds(365, 349, 255, 3);
        getContentPane().add(progressBar);

        // Интервалы для поиска новостей
        newsIntervalCbox = new JComboBox<>();
        newsIntervalCbox.setFont(new Font("Arial", Font.PLAIN, 11));
        newsIntervalCbox.setBounds(378, 10, 75, 20);
        getContentPane().add(newsIntervalCbox);
        // запись интервалов в комбобокс
        Common.addIntervalsToCombobox(newsIntervalCbox);

        // Today or not
        todayOrNotCbx = new Checkbox("in the last");
        todayOrNotCbx.setState(true);
        todayOrNotCbx.setFocusable(false);
        todayOrNotCbx.setForeground(Color.WHITE);
        todayOrNotCbx.setFont(new Font("Arial", Font.PLAIN, 11));
        todayOrNotCbx.setBounds(310, 10, 64, 20);
        todayOrNotCbx.addItemListener(e -> newsIntervalCbox.setVisible(todayOrNotCbx.getState()));
        getContentPane().add(todayOrNotCbx);

        // Автозапуск поиска по слову каждые 60 секунд
        autoUpdateNewsTop = new Checkbox("auto update");
        autoUpdateNewsTop.setState(false);
        autoUpdateNewsTop.setFocusable(false);
        autoUpdateNewsTop.setForeground(Color.WHITE);
        autoUpdateNewsTop.setFont(new Font("Arial", Font.PLAIN, 11));
        autoUpdateNewsTop.setBounds(228, 10, 75, 20);
        getContentPane().add(autoUpdateNewsTop);
        autoUpdateNewsTop.addItemListener(e -> {
            if (autoUpdateNewsTop.getState()) {
                timer = new Timer(true);
                timerTask = new MyTimerTask();
                timer.scheduleAtFixedRate(timerTask, 0, autoStartTimer);
                searchBtnTop.setVisible(false);
                stopBtnTop.setVisible(true);
                autoUpdateNewsBottom.setVisible(false);
            } else {
                timer.cancel();
                searchBtnTop.setVisible(true);
                stopBtnTop.setVisible(false);
                autoUpdateNewsBottom.setVisible(true);
                stopBtnTop.doClick();
            }
        });

        // Источники, лог, sqlite лейбл
        lblLogSourceSqlite = new JLabel();
        lblLogSourceSqlite.setForeground(Color.WHITE);
        lblLogSourceSqlite.setFont(new Font("Arial", Font.PLAIN, 11));
        lblLogSourceSqlite.setBounds(730, 426, 60, 14);
        getContentPane().add(lblLogSourceSqlite);

        // Диалоговое окно со списком исключенных слов из анализа
        exclBtn = new JButton();
        exclBtn.setFocusable(false);
        exclBtn.setContentAreaFilled(true);
        exclBtn.setBorderPainted(false);
        exclBtn.setBackground(new Color(30, 27, 27));
        exclBtn.setBounds(297, 343, 14, 14);
        getContentPane().add(exclBtn);
        exclBtn.addActionListener((e) -> new Dialogs("exclDlg"));
        exclBtn.addMouseListener(new MouseAdapter() {
            // наведение мышки на кнопку
            @Override
            public void mouseEntered(MouseEvent e) {
                exclBtn.setBackground(new Color(128, 128, 128));
            }

            @Override
            // убрали мышку с кнопки
            public void mouseExited(MouseEvent e) {
                exclBtn.setBackground(new Color(30, 27, 27));
            }
        });
        //label
        JLabel excludedLabel = new JLabel("excluded list:");
        excludedLabel.setHorizontalAlignment(SwingConstants.LEFT);
        excludedLabel.setForeground(new Color(255, 255, 153));
        excludedLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
        excludedLabel.setBackground(new Color(240, 255, 240));
        excludedLabel.setBounds(231, 337, 130, 26);
        getContentPane().add(excludedLabel);

        // Диалоговое окно со списком источников
        smiBtn = new JButton();
        smiBtn.setFocusable(false);
        smiBtn.setContentAreaFilled(true);
        smiBtn.setBorderPainted(false);
        smiBtn.setFocusable(false);
        smiBtn.setBounds(636, 426, 14, 14);
        smiBtn.setBackground(new Color(221, 255, 221));
        getContentPane().add(smiBtn);
        smiBtn.addActionListener((e) -> new Dialogs("smiDlg"));
        smiBtn.addMouseListener(new MouseAdapter() {
            // наведение мышки на кнопку
            @Override
            public void mouseEntered(MouseEvent e) {
                smiBtn.setBackground(new Color(25, 226, 25));
                lblLogSourceSqlite.setText("sources");
            }

            @Override
            // убрали мышку с кнопки
            public void mouseExited(MouseEvent e) {
                smiBtn.setBackground(new Color(221, 255, 221));
                lblLogSourceSqlite.setText("");
            }
        });

        JButton addNewSource = new JButton();
        addNewSource.setFocusable(false);
        addNewSource.setContentAreaFilled(true);
        addNewSource.setBorderPainted(false);
        addNewSource.setBackground(new Color(243, 229, 255));
        addNewSource.setBounds(655, 426, 14, 14);
        getContentPane().add(addNewSource);
        addNewSource.addActionListener(e -> sqlite.insertNewSource());
        addNewSource.addMouseListener(new MouseAdapter() {
            // наведение мышки на кнопку
            @Override
            public void mouseEntered(MouseEvent e) {
                addNewSource.setBackground(new Color(153, 84, 241));
                lblLogSourceSqlite.setText("add source");
            }

            @Override
            // убрали мышку с кнопки
            public void mouseExited(MouseEvent e) {
                addNewSource.setBackground(new Color(243, 229, 255));
                lblLogSourceSqlite.setText("");
            }
        });

        //SQLite
        JButton sqliteBtn = new JButton();
        sqliteBtn.setToolTipText("press CTRL+v in SQLite to open the database");
        sqliteBtn.setFocusable(false);
        sqliteBtn.setContentAreaFilled(true);
        sqliteBtn.setBorderPainted(false);
        sqliteBtn.setBackground(new Color(244, 181, 181));
        sqliteBtn.setBounds(693, 426, 14, 14);
        getContentPane().add(sqliteBtn);
        sqliteBtn.addActionListener(e -> {
            // запуск SQLite
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                //Desktop.getDesktop().open(new File("src\\res\\sqlite3.exe"));
                try {
                    Desktop.getDesktop().open(new File("C:\\Users\\rps_p\\News\\sqlite3.exe"));
                } catch (IOException io) {
                    io.printStackTrace();
                }
            }

            // копируем адрес базы в SQLite в системный буфер для быстрого доступа
            String pathToBase = ".open C:/Users/rps_p/News/news.db"; //delete from rss_list where id = 0; select * from rss_list;
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(pathToBase), null);
        });
        sqliteBtn.addMouseListener(new MouseAdapter() {
            // наведение мышки на кнопку
            @Override
            public void mouseEntered(MouseEvent e) {
                sqliteBtn.setBackground(new Color(255, 50, 50));
                lblLogSourceSqlite.setText("sqlite");
            }

            @Override
            // убрали мышку с кнопки
            public void mouseExited(MouseEvent e) {
                sqliteBtn.setBackground(new Color(244, 181, 181));
                lblLogSourceSqlite.setText("");
            }
        });

        // Диалоговое окно лога
        logBtn = new JButton();
        logBtn.setContentAreaFilled(true);
        logBtn.setBorderPainted(false);
        logBtn.setFocusable(false);
        logBtn.setBackground(new Color(248, 206, 165));
        logBtn.setBounds(674, 426, 14, 14);
        getContentPane().add(logBtn);
        logBtn.addActionListener(e -> new Dialogs("logDlg"));
        logBtn.addMouseListener(new MouseAdapter() {
            // наведение мышки на кнопку
            @Override
            public void mouseEntered(MouseEvent e) {
                logBtn.setBackground(new Color(222, 114, 7));
                lblLogSourceSqlite.setText("log");
            }
            @Override
            // убрали мышку с кнопки
            public void mouseExited(MouseEvent e) {
                logBtn.setBackground(new Color(248, 206, 165));
                lblLogSourceSqlite.setText("");
            }
        });

        //Открыть папку с настройками
        JButton settingsDirectoryBtn = new JButton();
        settingsDirectoryBtn.setFocusable(false);
        settingsDirectoryBtn.setContentAreaFilled(true);
        settingsDirectoryBtn.setBorderPainted(false);
        settingsDirectoryBtn.setBackground(new Color(219, 229, 252));
        settingsDirectoryBtn.setBounds(712, 426, 14, 14);
        getContentPane().add(settingsDirectoryBtn);
        settingsDirectoryBtn.addActionListener(e -> {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                try {
                    Desktop.getDesktop().open(new File(Main.directoryPath));
                } catch (IOException io) {
                    io.printStackTrace();
                }
            }

        });
        settingsDirectoryBtn.addMouseListener(new MouseAdapter() {
            // наведение мышки на кнопку
            @Override
            public void mouseEntered(MouseEvent e) {
                settingsDirectoryBtn.setBackground(new Color(80, 124, 255));
                lblLogSourceSqlite.setText("files");
            }

            @Override
            // убрали мышку с кнопки
            public void mouseExited(MouseEvent e) {
                settingsDirectoryBtn.setBackground(new Color(219, 229, 252));
                lblLogSourceSqlite.setText("");
            }
        });

        // Border email
        Box verticalBox = Box.createVerticalBox();
        verticalBox.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
        verticalBox.setBounds(630, 360, 161, 51);
        getContentPane().add(verticalBox);

        // Border different bottoms
        Box queryTableBox = Box.createVerticalBox();
        queryTableBox.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
        queryTableBox.setBounds(631, 420, 161, 26);
        getContentPane().add(queryTableBox);

        //label
        connectToBdLabel = new JLabel("<html><p style=\"color:#bfbfbf\">SQLite is off</p></html>");
        connectToBdLabel.setHorizontalAlignment(SwingConstants.LEFT);
        connectToBdLabel.setForeground(new Color(255, 255, 153));
        connectToBdLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
        connectToBdLabel.setBackground(new Color(240, 255, 240));
        connectToBdLabel.setBounds(730, 477, 60, 26);
        getContentPane().add(connectToBdLabel);

        // latest news
        filterNewsChbx = new Checkbox("only the latest news");
        filterNewsChbx.setState(false);
        filterNewsChbx.setFocusable(false);
        filterNewsChbx.setForeground(Color.WHITE);
        filterNewsChbx.setFont(new Font("Arial", Font.PLAIN, 11));
        filterNewsChbx.setBounds(636, 457, 135, 20);
        getContentPane().add(filterNewsChbx);
        filterNewsChbx.addItemListener(e -> {
            isOnlyLastNews = filterNewsChbx.getState();
            if (!isOnlyLastNews){
                sqlite.deleteFrom256();
            }
        });

        // border
        Box latestNewsBorder = Box.createVerticalBox();
        latestNewsBorder.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
        latestNewsBorder.setBounds(631, 454, 161, 26);
        getContentPane().add(latestNewsBorder);

        // Сворачивание приложения в трей
        try {
            BufferedImage Icon = ImageIO.read(Objects.requireNonNull(Gui.class.getResourceAsStream("/icons/logo.png")));
            final TrayIcon trayIcon =  new TrayIcon(Icon, "Avandy News");
            SystemTray systemTray = SystemTray.getSystemTray();
            systemTray.add(trayIcon);

            final PopupMenu trayMenu = new PopupMenu();
            MenuItem itemShow = new MenuItem("Show");
            itemShow.addActionListener(e -> {
                setVisible(true);
                setExtendedState(JFrame.NORMAL);
            });
            trayMenu.add(itemShow);

            MenuItem itemClose = new MenuItem("Close");
            itemClose.addActionListener(e -> System.exit(0));
            trayMenu.add(itemClose);

            trayIcon.addMouseListener(new MouseAdapter(){
                @Override
                public void mouseClicked(MouseEvent e){
                    if (SwingUtilities.isLeftMouseButton(e)){
                        setVisible(true);
                        setExtendedState(JFrame.NORMAL);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e){
                   if (SwingUtilities.isRightMouseButton(e)){
                        trayIcon.setPopupMenu(trayMenu);
                    }
                }
            });
        } catch (IOException | AWTException e) {
            e.printStackTrace();
        }

        setVisible(true);
    }
}