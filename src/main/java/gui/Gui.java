package gui;

import database.JdbcQueries;
import database.SQLite;
import email.EmailManager;
import gui.buttons.Icons;
import gui.buttons.SetButton;
import gui.checkboxes.SetCheckbox;
import search.Search;
import utils.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.awt.GraphicsDevice.WindowTranslucency.TRANSLUCENT;

public class Gui extends JFrame {
    final SQLite sqLite = new SQLite();
    final JdbcQueries jdbcQueries = new JdbcQueries();
    final Search search = new Search();
    private static final Object[] MAIN_TABLE_HEADERS = {"Num", "Source", "Title", "Date", "Link", "Description"};
    private static final String[] TABLE_FOR_ANALYZE_HEADERS = {"top 10", "freq.", " "};
    private static final String GUI_FONT_NAME = "Tahoma";
    private static final Font GUI_FONT = new Font(GUI_FONT_NAME, Font.PLAIN, 11);
    private static final Font GUI_FONT_BOLD = new Font(GUI_FONT_NAME, Font.BOLD, 11);
    private static final String[] INTERVALS = {"1 min", "5 min", "15 min", "30 min", "45 min", "1 hour", "2 hours",
            "4 hours", "8 hours", "12 hours", "24 hours", "48 hours", "72 hours", "all"};
    private static final long AUTO_START_TIMER = 60000L; // 60 секунд
    public static final AtomicBoolean WAS_CLICK_IN_TABLE_FOR_ANALYSIS = new AtomicBoolean(false);
    public static final AtomicBoolean GUI_IN_TRAY = new AtomicBoolean(false);
    public static int newsCount = 1;
    private static final int toolTipShowLength = 75;
    public static boolean isOnlyLastNews = false;
    public static String findWord;
    public static JScrollPane scrollPane;
    public static JTable table, tableForAnalysis;
    public static DefaultTableModel model, modelForAnalysis;
    public static JTextField topKeyword;
    public static JTextArea consoleTextArea;
    public static JComboBox<String> newsInterval;
    public static JLabel labelSum, lblLogSourceSqlite, appInfo, excludedTitlesLabel, favoritesLabel,
            excludedLabel, datesLabel;
    public static JButton searchBtnTop, searchBtnBottom, stopBtnTop, stopBtnBottom, sendCurrentResultsToEmail, smiBtn,
            anotherBtn, btnShowKeywordsList;
    public static Checkbox autoUpdateNewsTop, autoUpdateNewsBottom, autoSendMessage, onlyNewNews;
    public static JProgressBar progressBar;
    public static Timer timer;
    public static TimerTask timerTask;

    public Gui() {
        int guiRed = Integer.parseInt(jdbcQueries.getSetting("gui_red"));
        int guiGreen = Integer.parseInt(jdbcQueries.getSetting("gui_green"));
        int guiBlue = Integer.parseInt(jdbcQueries.getSetting("gui_blue"));

        this.setResizable(false);
        this.getContentPane().setBackground(new Color(guiRed, guiGreen, guiBlue));
        this.setTitle("Avandy News");
        this.setIconImage(Icons.LOGO_ICON.getImage());
        this.setFont(GUI_FONT);
        //this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setBounds(360, 180, 1181, 575);
        this.getContentPane().setLayout(null);

        // Прозрачность и оформление окна
        this.setUndecorated(true);
        // Проверка поддерживает ли операционная система прозрачность окон
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        GraphicsDevice gd = ge.getDefaultScreenDevice();
        boolean isUniformTranslucencySupported = gd.isWindowTranslucencySupported(TRANSLUCENT);
        if (isUniformTranslucencySupported) {
            String transparencyValue = new JdbcQueries().getSetting("transparency");
            if (transparencyValue != null) {
                this.setOpacity(Float.parseFloat(transparencyValue));
            } else {
                this.setOpacity(0.9f);
            }
        }

        //Table
        scrollPane = new JScrollPane();
        scrollPane.setBounds(10, 40, 860, 502);
        getContentPane().add(scrollPane);
        model = new DefaultTableModel(new Object[][]{
        }, MAIN_TABLE_HEADERS) {
            final boolean[] columnEditable = new boolean[]{
                    false, false, false, false, false, false
            };

            public boolean isCellEditable(int row, int column) {
                return columnEditable[column];
            }

            // Сортировка
            final Class[] types_unique = {Integer.class, String.class, String.class,
                    /*Date.class*/ String.class, String.class, String.class};

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
                if (tip.length() > toolTipShowLength) {
                    return tip;
                } else {
                    return null;
                }
            }
        };
        //headers
        JTableHeader header = table.getTableHeader();
        int mainHeaderFontSize = Integer.parseInt(jdbcQueries.getSetting("font_size"));
        int mainHeaderRowHeight = Integer.parseInt(jdbcQueries.getSetting("row_height"));

        header.setFont(new Font(GUI_FONT_NAME, Font.BOLD, 13));
        header.setForeground(new Color(241, 217, 84));
        //Cell alignment
        DefaultTableCellRenderer Renderer = new DefaultTableCellRenderer();
        Renderer.setHorizontalAlignment(JLabel.CENTER);

        table.getColumnModel().getColumn(0).setCellRenderer(Renderer);
        table.setRowHeight(mainHeaderRowHeight);
        table.setColumnSelectionAllowed(true);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setFont(new Font(GUI_FONT_NAME, Font.PLAIN, mainHeaderFontSize));
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setMaxWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(490);
        table.getColumnModel().getColumn(3).setPreferredWidth(98);
        table.getColumnModel().getColumn(3).setMaxWidth(98);
        table.removeColumn(table.getColumnModel().getColumn(5)); // Скрыть описание заголовка
        table.removeColumn(table.getColumnModel().getColumn(4)); // Скрыть ссылку заголовка

        table.setAutoCreateRowSorter(true);
        scrollPane.setViewportView(table);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.convertRowIndexToModel(table.rowAtPoint(new Point(e.getX(), e.getY()))); // при сортировке строк оставляет верные данные
                    int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
                    if (col == 2 || col == 4 || col == 5) {
                        String url = (String) table.getModel().getValueAt(row, 4);
                        openPage(url);
                    }
                }
            }
        });

        //Table for analysis
        JScrollPane scrollForAnalysis = new JScrollPane();
        scrollForAnalysis.setBounds(880, 40, 290, 236);
        getContentPane().add(scrollForAnalysis);

        modelForAnalysis = new DefaultTableModel(new Object[][]{}, TABLE_FOR_ANALYZE_HEADERS) {
            final boolean[] column_for_analysis = new boolean[]{false, false, true};

            public boolean isCellEditable(int row, int column) {
                return column_for_analysis[column];
            }

            // Сортировка
            final Class[] types_unique = {String.class, Integer.class, Button.class};

            @Override
            public Class getColumnClass(int columnIndex) {
                return this.types_unique[columnIndex];
            }
        };
        tableForAnalysis = new JTable(modelForAnalysis);
        JTableHeader header_for_analysis = tableForAnalysis.getTableHeader();
        header_for_analysis.setFont(new Font(GUI_FONT_NAME, Font.BOLD, 13));
        //Cell alignment
        DefaultTableCellRenderer rendererForAnalysis = new DefaultTableCellRenderer();
        rendererForAnalysis.setHorizontalAlignment(JLabel.CENTER);
        tableForAnalysis.getColumnModel().getColumn(1).setCellRenderer(rendererForAnalysis);
        //tableForAnalysis.getColumnModel().getColumn(1).setCellRenderer(rendererForAnalysis);
        tableForAnalysis.getColumn(" ").setCellRenderer(new ButtonColumn(tableForAnalysis, 2));
        tableForAnalysis.setRowHeight(21);
        tableForAnalysis.setColumnSelectionAllowed(true);
        tableForAnalysis.setCellSelectionEnabled(true);
        tableForAnalysis.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tableForAnalysis.setFont(new Font(GUI_FONT_NAME, Font.PLAIN, 14));
        tableForAnalysis.getColumnModel().getColumn(0).setPreferredWidth(140);
        tableForAnalysis.getColumnModel().getColumn(1).setPreferredWidth(40);
        tableForAnalysis.getColumnModel().getColumn(1).setMaxWidth(40);
        tableForAnalysis.getColumnModel().getColumn(2).setMaxWidth(30);
        tableForAnalysis.setAutoCreateRowSorter(true);
        scrollForAnalysis.setViewportView(tableForAnalysis);

        // запуск поиска по слову из таблицы анализа
        tableForAnalysis.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tableForAnalysis.convertRowIndexToModel(tableForAnalysis.rowAtPoint(new Point(e.getX(), e.getY())));
                    int col = tableForAnalysis.columnAtPoint(new Point(e.getX(), e.getY()));
                    if (col == 0) {
                        Gui.topKeyword.setText((String) tableForAnalysis.getModel().getValueAt(row, 0));
                        jdbcQueries.removeFromTable("TITLES");
                        searchBtnTop.doClick();
                        WAS_CLICK_IN_TABLE_FOR_ANALYSIS.set(true);
                    }
                }
            }
        });

        /* TOP-LEFT ACTION PANEL */
        int topLeftActionX = 10;
        int topLeftActionY = 9;

        //Input keyword
        JLabel lblNewLabel = new JLabel("find:");
        lblNewLabel.setForeground(new Color(255, 179, 131));
        lblNewLabel.setBounds(topLeftActionX, topLeftActionY, 36, 19);
        lblNewLabel.setFont(new Font(GUI_FONT_NAME, Font.BOLD, 15));
        lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
        getContentPane().add(lblNewLabel);

        //Keyword field
        topKeyword = new JTextField(findWord);
        topKeyword.setBounds(topLeftActionX + 42, topLeftActionY, 100, 22);
        topKeyword.setFont(new Font(GUI_FONT_NAME, Font.BOLD, 13));
        getContentPane().add(topKeyword);

        //Search
        searchBtnTop = new JButton();
        searchBtnTop.setIcon(Icons.SEARCH_KEYWORDS_ICON);
        searchBtnTop.setBackground(new Color(154, 237, 196));
        searchBtnTop.setFont(new Font(GUI_FONT_NAME, Font.BOLD, 10));
        searchBtnTop.setBounds(topLeftActionX + 147, topLeftActionY, 30, 22);
        getContentPane().add(searchBtnTop);
        // Search by Enter
        getRootPane().setDefaultButton(searchBtnTop);
        searchBtnTop.requestFocus();
        searchBtnTop.doClick();
        searchBtnTop.addActionListener(e -> new Thread(() -> search.mainSearch("word")).start());

        //Stop addNewSource
        stopBtnTop = new JButton();
        stopBtnTop.setIcon(Icons.STOP_SEARCH_ICON);
        stopBtnTop.setBackground(new Color(255, 208, 202));
        stopBtnTop.setBounds(topLeftActionX + 147, topLeftActionY, 30, 22);
        stopBtnTop.addActionListener(e -> {
            try {
                Search.isSearchFinished.set(true);
                Search.isStop.set(true);
                Common.console("search stopped");
                searchBtnTop.setVisible(true);
                stopBtnTop.setVisible(false);
                Search.isSearchNow.set(false);
                try {
                    sqLite.transaction("ROLLBACK");
                } catch (SQLException ignored) {
                }
            } catch (Exception t) {
                Common.console("there is no threads to stop");
            }
        });
        getContentPane().add(stopBtnTop);

        // Интервалы для поиска новостей
        newsInterval = new JComboBox<>(INTERVALS);
        newsInterval.setFont(GUI_FONT);
        newsInterval.setBounds(topLeftActionX + 185, topLeftActionY, 75, 20); //516
        getContentPane().add(newsInterval);

        // latest news
        onlyNewNews = new Checkbox("only new");
        SetCheckbox setCheckbox = new SetCheckbox(topLeftActionX + 270, topLeftActionY, 65);
        setCheckbox.checkBoxSetting(onlyNewNews);
        getContentPane().add(onlyNewNews);
        onlyNewNews.addItemListener(e -> {
            isOnlyLastNews = onlyNewNews.getState();
            if (!isOnlyLastNews) {
                jdbcQueries.removeFromTable("TITLES");
            }
        });

        // Автоматическая отправка письма с результатами
        autoSendMessage = new Checkbox("auto send");
        setCheckbox = new SetCheckbox(topLeftActionX + 338, topLeftActionY, 66);
        setCheckbox.checkBoxSetting(autoSendMessage);
        getContentPane().add(autoSendMessage);

        // Автозапуск поиска по слову каждые 60 секунд
        autoUpdateNewsTop = new Checkbox("auto update");
        SetCheckbox setCheckbox1 = new SetCheckbox(topLeftActionX + 410, topLeftActionY, 75);
        setCheckbox1.checkBoxSetting(autoUpdateNewsTop);

        getContentPane().add(autoUpdateNewsTop);
        autoUpdateNewsTop.addItemListener(e -> {
            if (autoUpdateNewsTop.getState()) {
                timer = new Timer(true);
                timerTask = new MyTimerTask();
                timer.scheduleAtFixedRate(timerTask, 0, AUTO_START_TIMER);
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

        // Диалоговое окно со списком исключенных слов из поиска
        excludedTitlesLabel = new JLabel("excluded");
        excludedTitlesLabel.setEnabled(false);
        excludedTitlesLabel.setHorizontalAlignment(SwingConstants.LEFT);
        excludedTitlesLabel.setForeground(new Color(255, 179, 131));
        excludedTitlesLabel.setFont(GUI_FONT);
        excludedTitlesLabel.setBounds(topLeftActionX + 733, topLeftActionY + 3, 44, 14);
        getContentPane().add(excludedTitlesLabel);
        openDialog(excludedTitlesLabel, "excludedTitlesByWordsDialog");

        // Диалоговое окно со списком избранных заголовков
        favoritesLabel = new JLabel("favorites");
        favoritesLabel.setEnabled(false);
        favoritesLabel.setHorizontalAlignment(SwingConstants.LEFT);
        favoritesLabel.setForeground(new Color(114, 237, 161));
        favoritesLabel.setFont(GUI_FONT);
        favoritesLabel.setBounds(topLeftActionX + 783, topLeftActionY + 3, 44, 14);
        getContentPane().add(favoritesLabel);
        openDialog(favoritesLabel, "favoriteTitlesDialog");

        // Диалоговое окно со списком избранных заголовков
        datesLabel = new JLabel("dates");
        datesLabel.setEnabled(false);
        datesLabel.setHorizontalAlignment(SwingConstants.LEFT);
        datesLabel.setForeground(new Color(237, 114, 114));
        datesLabel.setFont(GUI_FONT);
        datesLabel.setBounds(topLeftActionX + 833, topLeftActionY + 3, 27, 14);
        getContentPane().add(datesLabel);
        openDialog(datesLabel, "datesDialog");

        /* TOP-RIGHT CLOSE*/
        /* Сворачивание в трей */
        JButton toTrayBtn = new JButton(Icons.HIDE_BUTTON_ICON);
        toTrayBtn.setFocusable(false);
        toTrayBtn.setContentAreaFilled(false);
        toTrayBtn.setBorderPainted(false);
        toTrayBtn.setBounds(1126, 4, 24, 22);
        if (SystemTray.isSupported()) {
            getContentPane().add(toTrayBtn);
        }
        toTrayBtn.addActionListener(e -> {
            GUI_IN_TRAY.set(true);
            setVisible(false);
            if (autoUpdateNewsBottom.getState()) consoleTextArea.setText("");
        });
        animation(toTrayBtn, Icons.HIDE_BUTTON_ICON, Icons.WHEN_MOUSE_ON_HIDE_BUTTON_ICON);

        // Сворачивание приложения в трей
        try {
            BufferedImage Icon = ImageIO.read(Objects.requireNonNull(Icons.APP_IN_TRAY_BUTTON_ICON));
            final TrayIcon trayIcon = new TrayIcon(Icon, "Avandy News");
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

            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        setVisible(true);
                        GUI_IN_TRAY.set(false);
                        setExtendedState(JFrame.NORMAL);
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        trayIcon.setPopupMenu(trayMenu);
                    }
                }
            });
        } catch (IOException | AWTException e) {
            e.printStackTrace();
        }

        // Exit button
        JButton exitBtn = new JButton(Icons.EXIT_BUTTON_ICON);
        exitBtn.setBounds(1151, 3, 24, 22);
        exitBtn.setContentAreaFilled(false);
        exitBtn.setBorderPainted(false);
        exitBtn.setFocusable(false);
        getContentPane().add(exitBtn);
        exitBtn.addActionListener((e) -> {
            Search.isSearchFinished.set(true);
            Common.saveState();
            sqLite.closeConnection();
            System.exit(0);
        });
        animation(exitBtn, Icons.EXIT_BUTTON_ICON, Icons.WHEN_MOUSE_ON_EXIT_BUTTON_ICON);

        /* KEYWORDS BOTTOM SEARCH */
        int bottomLeftX = 110;
        int bottomLeftY = 545;

        // label
        JLabel lblKeywordsSearch = new JLabel();
        lblKeywordsSearch.setText("search by keywords");
        lblKeywordsSearch.setForeground(new Color(255, 255, 153));
        lblKeywordsSearch.setFont(GUI_FONT);
        lblKeywordsSearch.setBounds(bottomLeftX - 100, bottomLeftY + 4, 120, 14);
        getContentPane().add(lblKeywordsSearch);

        // Открыть список ключевых слов для поиска
        btnShowKeywordsList = new JButton();
        btnShowKeywordsList.setBorderPainted(false);
        SetButton setButton = new SetButton(Icons.LIST_BUTTON_ICON, null, bottomLeftX - 3, bottomLeftY);
        setButton.buttonSetting(btnShowKeywordsList, null);
        btnShowKeywordsList.addActionListener(e -> new Dialogs("keywordsDialog"));
        animation(btnShowKeywordsList, Icons.LIST_BUTTON_ICON, Icons.WHEN_MOUSE_ON_LIST_BUTTON_ICON);
        getContentPane().add(btnShowKeywordsList);

        //Bottom search by keywords
        searchBtnBottom = new JButton();
        setButton = new SetButton(Icons.SEARCH_KEYWORDS_ICON, new Color(154, 237, 196), bottomLeftX + 30, bottomLeftY);
        setButton.buttonSetting(searchBtnBottom, "Search by keywords");
        searchBtnBottom.addActionListener(e -> new Thread(() -> search.mainSearch("words")).start());
        getContentPane().add(searchBtnBottom);

        //Stop (bottom)
        stopBtnBottom = new JButton();
        setButton = new SetButton(Icons.STOP_SEARCH_ICON, new Color(255, 208, 202), bottomLeftX + 30, bottomLeftY);
        setButton.buttonSetting(stopBtnBottom, null);
        stopBtnBottom.addActionListener(e -> {
            try {
                Search.isSearchFinished.set(true);
                Search.isStop.set(true);
                Common.console("search stopped");
                searchBtnBottom.setVisible(true);
                stopBtnBottom.setVisible(false);
                Search.isSearchNow.set(false);
                try {
                    sqLite.transaction("ROLLBACK");
                } catch (SQLException ignored) {
                }
            } catch (Exception t) {
                Common.console("there is no threads to stop");
            }
        });
        getContentPane().add(stopBtnBottom);

        // Автозапуск поиска по ключевым словам каждые 60 секунд
        autoUpdateNewsBottom = new Checkbox("auto update");
        setCheckbox1 = new SetCheckbox(bottomLeftX + 70, bottomLeftY + 1, 75);
        setCheckbox1.checkBoxSetting(autoUpdateNewsBottom);
        getContentPane().add(autoUpdateNewsBottom);
        autoUpdateNewsBottom.addItemListener(e -> {
            if (autoUpdateNewsBottom.getState()) {
                timer = new Timer(true);
                timerTask = new MyTimerTask();
                timer.scheduleAtFixedRate(timerTask, 0, AUTO_START_TIMER);
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
                } catch (Exception ignored) {

                }
            }
        });

        /* CONSOLE */
        //Console - textarea
        consoleTextArea = new JTextArea();
        consoleTextArea.setForeground(Color.BLACK);
        // авто скроллинг
        DefaultCaret caret = (DefaultCaret) consoleTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
        consoleTextArea.setAutoscrolls(true);
        consoleTextArea.setLineWrap(true);
        consoleTextArea.setEditable(false);
        consoleTextArea.setBounds(20, 11, 145, 51);
        consoleTextArea.setFont(new Font(GUI_FONT_NAME, Font.BOLD, 13));
        consoleTextArea.setBackground(new Color(222, 222, 222)); // 83, 82, 82
        getContentPane().add(consoleTextArea);

        //Console - scroll
        JScrollPane consoleScroll = new JScrollPane(consoleTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        consoleScroll.setBounds(879, 303, 290, 148);
        consoleScroll.setBorder(null);
        getContentPane().add(consoleScroll);

        //Console - label
        JLabel clearConsoleLabel = new JLabel();
        clearConsoleLabel.setEnabled(false);
        clearConsoleLabel.setText("clear");
        clearConsoleLabel.setForeground(new Color(255, 255, 153));
        clearConsoleLabel.setFont(GUI_FONT);
        clearConsoleLabel.setBounds(1145, 452, 24, 14);
        getContentPane().add(clearConsoleLabel);
        clearConsoleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!clearConsoleLabel.isEnabled()) {
                    clearConsoleLabel.setEnabled(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (clearConsoleLabel.isEnabled()) {
                    clearConsoleLabel.setEnabled(false);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    consoleTextArea.setText("");
                }
            }
        });

        // Шкала прогресса
        progressBar = new JProgressBar();
        progressBar.setFocusable(false);
        progressBar.setMaximum(100);
        progressBar.setBorderPainted(false);
        progressBar.setForeground(new Color(10, 255, 41));
        progressBar.setBackground(new Color(1, 1, 1));
        progressBar.setBounds(10, 37, 860, 1);
        getContentPane().add(progressBar);

        // Диалоговое окно со списком исключенных слов из анализа
        excludedLabel = new JLabel("excluded");
        excludedLabel.setEnabled(false);
        excludedLabel.setHorizontalAlignment(SwingConstants.LEFT);
        excludedLabel.setForeground(new Color(255, 255, 153));
        excludedLabel.setFont(GUI_FONT);
        excludedLabel.setBounds(1126, 278, 44, 14);
        getContentPane().add(excludedLabel);
        openDialog(excludedLabel, "excludedFromAnalysisDialog");

        /* BOTTOM RIGHT AREA */
        //Amount of news
        labelSum = new JLabel();
        labelSum.setBounds(880, 282, 180, 13);
        labelSum.setFont(GUI_FONT);
        labelSum.setForeground(new Color(255, 255, 153));
        labelSum.setBackground(new Color(240, 255, 240));
        getContentPane().add(labelSum);

        //Send current results e-mail
        sendCurrentResultsToEmail = new JButton();
        sendCurrentResultsToEmail.setVisible(false);
        setButton = new SetButton(Icons.SEND_EMAIL_ICON, new Color(255, 255, 153), 1038, 277);
        setButton.buttonSetting(sendCurrentResultsToEmail, "Send current search results");
        sendCurrentResultsToEmail.setFocusable(false);
        sendCurrentResultsToEmail.setContentAreaFilled(false);
        sendCurrentResultsToEmail.setBorderPainted(false);
        sendCurrentResultsToEmail.addActionListener(e -> {
            if (model.getRowCount() > 0) {
                Common.console("sending e-mail");
                Common.IS_SENDING.set(false);
                new Thread(Common::fillProgressLine).start();
                EmailManager email = new EmailManager();
                new Thread(email::sendMessage).start();
            }
        });
        getContentPane().add(sendCurrentResultsToEmail);
        animation(sendCurrentResultsToEmail, Icons.SEND_EMAIL_ICON, Icons.WHEN_MOUSE_ON_SEND_ICON);

        // Диалоговое окно со списком источников "sources"
        smiBtn = new JButton();
        smiBtn.setFocusable(false);
        smiBtn.setContentAreaFilled(true);
        smiBtn.setBorderPainted(false);
        smiBtn.setFocusable(false);
        smiBtn.setBounds(883, 479, 14, 14);
        smiBtn.setBackground(new Color(221, 255, 221));
        getContentPane().add(smiBtn);
        smiBtn.addActionListener((e) -> new Dialogs("sourcesDialog"));
        animation(smiBtn, "sources");

        // Случайное английское слово
        anotherBtn = new JButton();
        anotherBtn.setContentAreaFilled(true);
        anotherBtn.setBorderPainted(false);
        anotherBtn.setFocusable(false);
        anotherBtn.setBackground(new Color(248, 206, 165));
        anotherBtn.setBounds(902, 479, 14, 14);
        getContentPane().add(anotherBtn);
        anotherBtn.addActionListener(e -> new RandomWord().get());
        animation(anotherBtn, "english words");

        // SQLite
        JButton sqliteBtn = new JButton();
        sqliteBtn.setToolTipText("Press CTRL+V in SQLite to open the database");
        sqliteBtn.setFocusable(false);
        sqliteBtn.setContentAreaFilled(true);
        sqliteBtn.setBorderPainted(false);
        sqliteBtn.setBackground(new Color(244, 181, 181));
        sqliteBtn.setBounds(921, 479, 14, 14);
        getContentPane().add(sqliteBtn);
        sqliteBtn.addActionListener(e -> {
            // запуск JdbcQueries
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                try {
                    Desktop.getDesktop().open(new File(Common.DIRECTORY_PATH + "sqlite3.exe"));
                } catch (IOException io) {
                    io.printStackTrace();
                }
            }

            // копируем адрес базы в JdbcQueries в системный буфер для быстрого доступа
            String pathToBase = (".open " + Common.getPathToDatabase()).replace("\\", "/");
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(pathToBase), null);
        });
        animation(sqliteBtn, "sqlite");

        //Открыть settings
        JButton settingsDirectoryBtn = new JButton();
        settingsDirectoryBtn.setIcon(Icons.SETTINGS_BUTTON_ICON);
        settingsDirectoryBtn.setFocusable(false);
        settingsDirectoryBtn.setContentAreaFilled(true);
        settingsDirectoryBtn.setBorderPainted(false);
        settingsDirectoryBtn.setBackground(new Color(219, 229, 252));
        settingsDirectoryBtn.setBounds(940, 479, 14, 14);
        getContentPane().add(settingsDirectoryBtn);
        animation(settingsDirectoryBtn, "settings");

        ActionListener actionListener = e -> {
            String emailFromValue = jdbcQueries.getSetting("email_from");
            String fromPwdValue = jdbcQueries.getSetting("from_pwd");
            String emailToValue = jdbcQueries.getSetting("email_to");
            String transparencyValue = jdbcQueries.getSetting("transparency");
            Integer fontSizeValue = Integer.valueOf(jdbcQueries.getSetting("font_size"));
            Integer rowHeightValue = Integer.valueOf(jdbcQueries.getSetting("row_height"));

            Color color = new Color(255, 255, 255);
            JTextField emailFrom = new JTextField(emailFromValue);
            emailFrom.setBackground(color);
            JPasswordField emailFromPwd = new JPasswordField(fromPwdValue);
            emailFromPwd.setBackground(color);
            emailFromPwd.setForeground(Color.BLACK);
            JTextField emailTo = new JTextField(emailToValue);
            emailTo.setBackground(color);
            JTextField transparency = new JTextField();
            transparency.setBackground(color);
            transparency.setText(String.valueOf(transparencyValue));
            JTextField pathToDatabase = new JTextField();
            pathToDatabase.setBackground(color);
            pathToDatabase.setText(Common.getPathToDatabase());
            JComboBox<Integer> fontSizeCombobox = new JComboBox<>(new Integer[]{13, 14, 15, 16, 17, 18, 19, 20});
            fontSizeCombobox.setSelectedItem(fontSizeValue);
            JComboBox<Integer> rowHeightCombobox = new JComboBox<>(new Integer[]{19, 20, 21, 22, 23, 24, 25, 26, 27, 28,
                    29, 30, 31, 32, 33});
            rowHeightCombobox.setSelectedItem(rowHeightValue);
            JButton guiColorButton = new JButton("select");
            // Выбор цвета фона UI
            guiColorButton.addActionListener(x -> {
                Color backgroundColorValue = JColorChooser.showDialog(null, "Gui background", Color.black);
                if (backgroundColorValue != null) {
                    this.setBackground(backgroundColorValue);
                    Common.saveColor("gui-bg", backgroundColorValue);
                }
            });

            // Выбор цвета фона таблиц
            JButton tablesColorButton = new JButton("select");
            tablesColorButton.addActionListener(et -> {
                Color tablesColor = JColorChooser.showDialog(null, "Tables background", Color.black);
                if (tablesColor != null) {
                    table.setBackground(tablesColor);
                    tableForAnalysis.setBackground(tablesColor);
                    Common.saveColor("table-bg", tablesColor);
                }
            });

            // Выбор цвета шрифта в таблице
            JButton fontColorButton = new JButton("select");
            fontColorButton.addActionListener(ef -> {
                Color fontColor = JColorChooser.showDialog(null, "Font color", Color.black);
                if (fontColor != null) {
                    table.setForeground(fontColor);
                    tableForAnalysis.setForeground(fontColor);
                    Common.saveColor("font", fontColor);
                }
            });

            JPanel settingsPanel = new JPanel();
            settingsPanel.setLayout(new GridLayout(9, 1));
            settingsPanel.add(new JLabel("Email from"));
            settingsPanel.add(emailFrom);
            settingsPanel.add(new JLabel("Email from password"));
            settingsPanel.add(emailFromPwd);
            settingsPanel.add(new JLabel("Email to"));
            settingsPanel.add(emailTo);
            settingsPanel.add(new JLabel("Font size"));
            settingsPanel.add(fontSizeCombobox);
            settingsPanel.add(new JLabel("Row height"));
            settingsPanel.add(rowHeightCombobox);
            settingsPanel.add(new JLabel("Transparency"));
            settingsPanel.add(transparency);
            settingsPanel.add(new JLabel("Interface color"));
            settingsPanel.add(guiColorButton);
            settingsPanel.add(new JLabel("Tables color"));
            settingsPanel.add(tablesColorButton);
            settingsPanel.add(new JLabel("Font color"));
            settingsPanel.add(fontColorButton);

            Object[] newSource = {
                    settingsPanel,
                    "Path to database:", pathToDatabase
            };

            int result = JOptionPane.showConfirmDialog(settingsDirectoryBtn, newSource,
                    "Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                jdbcQueries.updateSettings("email_to", emailTo.getText());
                jdbcQueries.updateSettings("email_from", emailFrom.getText());
                jdbcQueries.updateSettings("from_pwd", String.valueOf(emailFromPwd.getPassword()));
                jdbcQueries.updateSettings("transparency", transparency.getText());
                Common.delSettings("db_path");
                Common.writeToConfigTxt("db_path", pathToDatabase.getText());
                jdbcQueries.updateSettings("db_path", pathToDatabase.getText());
                jdbcQueries.updateSettings("font_size", fontSizeCombobox.getSelectedItem().toString());
                jdbcQueries.updateSettings("row_height", rowHeightCombobox.getSelectedItem().toString());

                // удалить старый frame
                this.dispose();
                Common.showGui();
            }
        };
        settingsDirectoryBtn.addActionListener(actionListener);

        // Источники, sqlite лейбл
        lblLogSourceSqlite = new JLabel("");
        lblLogSourceSqlite.setForeground(Color.WHITE);
        lblLogSourceSqlite.setFont(GUI_FONT);
        lblLogSourceSqlite.setBounds(961, 479, 70, 14);
        getContentPane().add(lblLogSourceSqlite);

        appInfo = new JLabel();
        appInfo.setText("news archive: " + jdbcQueries.archiveNewsCount());
        appInfo.setForeground(Color.LIGHT_GRAY);
        appInfo.setFont(GUI_FONT);
        appInfo.setBounds(1060, 479, 200, 14);
        getContentPane().add(appInfo);

        // Border different bottoms
        Box queryTableBox = Box.createVerticalBox();
        queryTableBox.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
        queryTableBox.setBounds(879, 473, 290, 26);
        getContentPane().add(queryTableBox);

        showWebPagesIcons();
        showRightClickMenu();

        this.setVisible(true);
    }

    private void showWebPagesIcons() {
        int webX = 758;
        int webY = 547;
        int webHeight = 18;
        int webWidth = 18;

        String urlTradingView = "https://www.tradingview.com/";
        JLabel tradingView = new JLabel("trading");
        tradingView.setToolTipText(urlTradingView);
        tradingView.setIcon(Icons.TRADING_BUTTON_ICON);
        tradingView.setEnabled(false);
        tradingView.setFont(GUI_FONT_BOLD);
        tradingView.setBounds(webX, webY, webWidth, webHeight);
        getContentPane().add(tradingView);
        animation(tradingView, urlTradingView);

        String urlTranslator = "https://translate.google.com/";
        JLabel translator = new JLabel();
        translator.setIcon(Icons.TRANSLATOR_BUTTON_ICON);
        translator.setToolTipText(urlTranslator);
        translator.setEnabled(false);
        translator.setFont(GUI_FONT_BOLD);
        translator.setBounds(webX + 24, webY, webWidth, webHeight);
        getContentPane().add(translator);
        animation(translator, urlTranslator);

        String urlMaps = "https://yandex.ru/maps";
        JLabel maps = new JLabel("maps");
        maps.setIcon(Icons.MAPS_BUTTON_ICON);
        maps.setToolTipText(urlMaps);
        maps.setEnabled(false);
        maps.setFont(GUI_FONT_BOLD);
        maps.setBounds(webX + 48, webY, webWidth, webHeight);
        getContentPane().add(maps);
        animation(maps, urlMaps);

        String urlYandex = "https://ya.ru/";
        JLabel yandex = new JLabel();
        yandex.setIcon(Icons.YANDEX_BUTTON_ICON);
        yandex.setToolTipText(urlYandex);
        yandex.setEnabled(false);
        yandex.setFont(GUI_FONT_BOLD);
        yandex.setBounds(webX + 72, webY, webWidth, webHeight);
        getContentPane().add(yandex);
        animation(yandex, urlYandex);

        String urlGithub = "https://github.com/mrprogre";
        JLabel github = new JLabel();
        github.setIcon(Icons.GITHUB_BUTTON_ICON);
        github.setToolTipText(urlGithub);
        github.setEnabled(false);
        github.setFont(GUI_FONT_BOLD);
        github.setBounds(webX + 96, webY, webWidth, webHeight);
        getContentPane().add(github);
        animation(github, urlGithub);
    }

    private void showRightClickMenu() {
        final JPopupMenu popup = new JPopupMenu();

        // Add to favorites (menu)
        JMenuItem menuFavorite = new JMenuItem("Add to favorites");
        menuFavorite.addActionListener((e) -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String source = (String) table.getValueAt(row, 1);
                String title = (String) table.getValueAt(row, 2);
                jdbcQueries.addFavoriteTitle(title, jdbcQueries.getLinkOrDescribeByHash(source, title, "link"));
            }
        });
        popup.add(menuFavorite);

        // Show describe (menu)
        JMenuItem menuDescribe = new JMenuItem("Describe");
        menuDescribe.addActionListener((e) -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String source = (String) table.getValueAt(row, 1);
                String title = (String) table.getValueAt(row, 2);
                Common.console(jdbcQueries.getLinkOrDescribeByHash(source, title, "describe"));
            }
        });
        popup.add(menuDescribe);

        // Copy (menu)
        JMenuItem menuCopy = new JMenuItem("Copy");
        menuCopy.addActionListener((e) -> {
            StringBuilder sbf = new StringBuilder();
            int cols = table.getSelectedColumnCount();
            int rows = table.getSelectedRowCount();
            int[] selectedRows = table.getSelectedRows();
            int[] selectedColumns = table.getSelectedColumns();
            for (int i = 0; i < rows; ++i) {
                for (int j = 0; j < cols; ++j) {
                    sbf.append(table.getValueAt(selectedRows[i], selectedColumns[j]));
                    if (j < cols - 1) {
                        sbf.append("\t");
                    }
                }
                sbf.append("\n");
            }
            StringSelection stsel = new StringSelection(sbf.toString());
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(stsel, stsel);
        });
        popup.add(menuCopy);


        // Delete row (menu)
        JMenuItem menuDeleteRow = new JMenuItem("Remove");
        menuDeleteRow.addActionListener(e -> {
            int row = table.convertRowIndexToModel(table.getSelectedRow());
            if (row != -1) model.removeRow(row);
        });
        popup.add(menuDeleteRow);

        // Export titles to excel file (menu)
        JMenuItem exportToXls = new JMenuItem("Export all");
        exportToXls.addActionListener((e) -> {
            if (model.getRowCount() != 0) {
                new Thread(new ExportToExcel()::exportResultsToExcel).start();
            } else {
                Common.console("there is no data to export");
            }
        });
        popup.add(exportToXls);

        // Clear news list
        JMenuItem menuRemoveAll = new JMenuItem("Remove all");
        menuRemoveAll.addActionListener(e -> {
            try {
                if (model.getRowCount() == 0) {
                    Common.console("no data to clear");
                    return;
                }
                model.setRowCount(0);
                modelForAnalysis.setRowCount(0);
                newsCount = 0;
                labelSum.setText("" + newsCount);
                Gui.sendCurrentResultsToEmail.setVisible(false);
            } catch (Exception t) {
                Common.console(t.getMessage());
                t.printStackTrace();
            }
        });
        popup.add(menuRemoveAll);

        // Translate from ENG to RUS (menu)
        JMenuItem menuTranslate = new JMenuItem("Translate");
        menuTranslate.setVisible(false);
        menuTranslate.addActionListener((e) -> {
            int rowIndex = table.getSelectedRow();
            int colIndex = 2;
            String tip = (String) table.getValueAt(rowIndex, colIndex);
            new Thread(() -> Common.console(new Translator().translate("en", "ru", tip))).start();
        });
        popup.add(menuTranslate);

        // Mouse right click listener
        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JTable source = (JTable) e.getSource();
                    int row = source.convertRowIndexToModel(source.rowAtPoint(e.getPoint()));
                    int column = source.columnAtPoint(e.getPoint());
                    if (source.isRowSelected(row)) {
                        source.changeSelection(row, column, false, false);
                    }

                    // Показывать кнопку с переводом только для английских заголовков
                    String tip = (String) model.getValueAt(row, 2);
                    Pattern pattern = Pattern.compile(".*\\p{InCyrillic}.*");
                    Matcher matcher = pattern.matcher(tip);
                    menuTranslate.setVisible(!matcher.find());

                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void animation(JButton exitBtn, ImageIcon off, ImageIcon on) {
        exitBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                exitBtn.setIcon(on);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                exitBtn.setIcon(off);
            }
        });
    }

    private void animation(JButton button, String label) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                lblLogSourceSqlite.setText(label);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                lblLogSourceSqlite.setText("");
            }
        });
    }

    private void animation(JLabel label, String url) {
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!label.isEnabled()) {
                    label.setEnabled(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (label.isEnabled()) {
                    label.setEnabled(false);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    openPage(url);
                }
            }
        });
    }

    private void openDialog(JLabel label, String dialog) {
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!label.isEnabled()) {
                    label.setEnabled(true);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (label.isEnabled()) {
                    label.setEnabled(false);
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    new Dialogs(dialog);
                }
            }
        });
    }

    public static void openPage(String url) {
        if (!url.equals("no data found")) {
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
            }
        }
    }
}