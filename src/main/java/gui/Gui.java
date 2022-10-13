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
    private static final Font GUI_FONT = new Font("Tahoma", Font.PLAIN, 11);
    private static final Font GUI_FONT_BOLD = new Font("Tahoma", Font.BOLD, 11);
    private static final String[] INTERVALS = {"1 min", "5 min", "15 min", "30 min", "45 min", "1 hour", "2 hours",
            "4 hours", "8 hours", "12 hours", "24 hours", "48 hours", "72 hours", "all"};
    private static final long AUTO_START_TIMER = 60000L; // 60 секунд
    public static final AtomicBoolean WAS_CLICK_IN_TABLE_FOR_ANALYSIS = new AtomicBoolean(false);
    public static final AtomicBoolean GUI_IN_TRAY = new AtomicBoolean(false);
    public static int newsCount = 1;
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
    public static JButton searchBtnTop, searchBtnBottom, stopBtnTop, stopBtnBottom, sendEmailBtn, smiBtn,
            anotherBtn, btnShowKeywordsList;
    public static Checkbox autoUpdateNewsTop, autoUpdateNewsBottom, autoSendMessage, onlyNewNews;
    public static JProgressBar progressBar;
    public static Timer timer;
    public static TimerTask timerTask;

    public Gui() {
        this.setResizable(false);
        this.getContentPane().setBackground(new Color(42, 42, 42));
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
            this.setOpacity(Common.transparency);
        }

        //Table
        scrollPane = new JScrollPane();
        scrollPane.setBounds(10, 40, 860, 500);
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
                if (tip.length() > 82) {
                    return tip;
                } else {
                    return null;
                }
            }
        };
        //headers
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Tahoma", Font.BOLD, 13));
        //Cell alignment
        DefaultTableCellRenderer Renderer = new DefaultTableCellRenderer();
        Renderer.setHorizontalAlignment(JLabel.CENTER);

        table.getColumnModel().getColumn(0).setCellRenderer(Renderer);
        table.setRowHeight(28);
        table.setColumnSelectionAllowed(true);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setFont(new Font("Tahoma", Font.PLAIN, 14));
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setMaxWidth(180);
        table.getColumnModel().getColumn(2).setPreferredWidth(490);
        table.getColumnModel().getColumn(3).setPreferredWidth(92);
        table.getColumnModel().getColumn(3).setMaxWidth(92);
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
        header_for_analysis.setFont(new Font("Tahoma", Font.BOLD, 13));
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
        tableForAnalysis.setFont(new Font("Tahoma", Font.PLAIN, 14));
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
        lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 15));
        lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
        getContentPane().add(lblNewLabel);

        //Keyword field
        topKeyword = new JTextField(findWord);
        topKeyword.setBounds(topLeftActionX + 42, topLeftActionY, 100, 22);
        topKeyword.setFont(new Font("Tahoma", Font.BOLD, 13));
        getContentPane().add(topKeyword);

        //Search
        searchBtnTop = new JButton();
        searchBtnTop.setIcon(Icons.SEARCH_KEYWORDS_ICON);
        searchBtnTop.setBackground(new Color(154, 237, 196));
        searchBtnTop.setFont(new Font("Tahoma", Font.BOLD, 10));
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
        excludedTitlesLabel.setBounds(topLeftActionX + 490, topLeftActionY + 3, 44, 14);
        getContentPane().add(excludedTitlesLabel);
        openDialog(excludedTitlesLabel, "exclTitlesDlg");

        // Диалоговое окно со списком избранных заголовков
        favoritesLabel = new JLabel("favorites");
        favoritesLabel.setEnabled(false);
        favoritesLabel.setHorizontalAlignment(SwingConstants.LEFT);
        favoritesLabel.setForeground(new Color(114, 237, 161));
        favoritesLabel.setFont(GUI_FONT);
        favoritesLabel.setBounds(topLeftActionX + 540, topLeftActionY + 3, 44, 14);
        getContentPane().add(favoritesLabel);
        openDialog(favoritesLabel, "favoritesDlg");

        // Диалоговое окно со списком избранных заголовков
        datesLabel = new JLabel("dates");
        datesLabel.setEnabled(false);
        datesLabel.setHorizontalAlignment(SwingConstants.LEFT);
        datesLabel.setForeground(new Color(237, 114, 114));
        datesLabel.setFont(GUI_FONT);
        datesLabel.setBounds(topLeftActionX + 590, topLeftActionY + 3, 27, 14);
        getContentPane().add(datesLabel);
        openDialog(datesLabel, "datesDlg");

        /* TOP-RIGHT ACTION PANEL */
        int topRightX = 740;
        int topRightY = 8;

        // Выбор цвета фона
        JButton backgroundColorBtn = new JButton();
        SetButton setButton = new SetButton(Icons.BACK_GROUND_COLOR_ICON, null, topRightX, topRightY);
        setButton.buttonSetting(backgroundColorBtn, "Background color");
        backgroundColorBtn.setContentAreaFilled(false);

        backgroundColorBtn.addActionListener(e -> {
            Color color = JColorChooser.showDialog(null, "Color", Color.black);
            if (color != null) {
                try {
                    table.setBackground(color);
                    Common.saveBackgroundColor(color);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        getContentPane().add(backgroundColorBtn);
        animation(backgroundColorBtn, Icons.BACK_GROUND_COLOR_ICON, Icons.WHEN_MOUSE_ON_BACK_GROUND_COLOR_ICON);

        // Выбор цвета шрифта в таблице
        JButton fontColorBtn = new JButton();
        setButton = new SetButton(Icons.FONT_COLOR_BUTTON_ICON, null, topRightX + 35, topRightY);
        setButton.buttonSetting(fontColorBtn, "Font color");
        fontColorBtn.setContentAreaFilled(false);

        fontColorBtn.addActionListener(e -> {
            Color color = JColorChooser.showDialog(null, "Color", Color.black);
            if (color != null) {
                try {
                    table.setForeground(color);
                    tableForAnalysis.setForeground(color);
                    Common.saveFontColor(color);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        getContentPane().add(fontColorBtn);
        animation(fontColorBtn, Icons.FONT_COLOR_BUTTON_ICON, Icons.WHEN_MOUSE_ON_FONT_COLOR_BUTTON_ICON);

        //Export to excel
        JButton exportBtn = new JButton();
        setButton = new SetButton(Icons.EXCEL_BUTTON_ICON, null, topRightX + 70, topRightY);
        setButton.buttonSetting(exportBtn, "Export news to excel");
        exportBtn.setContentAreaFilled(false);

        exportBtn.addActionListener(e -> {
            if (model.getRowCount() != 0) {
                new Thread(new ExportToExcel()::exportResultsToExcel).start();
            } else {
                Common.console("there is no data to export");
            }
        });
        getContentPane().add(exportBtn);
        animation(exportBtn, Icons.EXCEL_BUTTON_ICON, Icons.WHEN_MOUSE_ON_EXCEL_BUTTON_ICON);

        // Clear
        JButton clearBtnTop = new JButton();
        setButton = new SetButton(Icons.CLEAR_BUTTON_ICON, null, topRightX + 105, topRightY);
        setButton.buttonSetting(clearBtnTop, "Clear the list");
        clearBtnTop.setContentAreaFilled(false);

        clearBtnTop.addActionListener(e -> {
            try {
                if (model.getRowCount() == 0) {
                    Common.console("no data to clear");
                    return;
                }
                model.setRowCount(0);
                modelForAnalysis.setRowCount(0);
                newsCount = 0;
                labelSum.setText("" + newsCount);
            } catch (Exception t) {
                Common.console(t.getMessage());
                t.printStackTrace();
            }
        });
        getContentPane().add(clearBtnTop);
        animation(clearBtnTop, Icons.CLEAR_BUTTON_ICON, Icons.WHEN_MOUSE_ON_CLEAR_BUTTON_ICON);

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
        setButton = new SetButton(Icons.LIST_BUTTON_ICON, null, bottomLeftX - 3, bottomLeftY );
        setButton.buttonSetting(btnShowKeywordsList, null);
        btnShowKeywordsList.addActionListener(e -> new Dialogs("keywordsDlg"));
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
        consoleTextArea.setFont(new Font("Tahoma", Font.BOLD, 13));
        consoleTextArea.setBackground(new Color(222, 222, 222)); // 83, 82, 82
        getContentPane().add(consoleTextArea);

        //Console - scroll
        JScrollPane consoleScroll = new JScrollPane(consoleTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        consoleScroll.setBounds(879, 303, 290, 142);
        consoleScroll.setBorder(null);
        getContentPane().add(consoleScroll);

        //Console - label
        JLabel clearConsoleLabel = new JLabel();
        clearConsoleLabel.setEnabled(false);
        clearConsoleLabel.setText("clear");
        clearConsoleLabel.setForeground(new Color(255, 255, 153));
        clearConsoleLabel.setFont(GUI_FONT);
        clearConsoleLabel.setBounds(1145, 447, 24, 14);
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
        openDialog(excludedLabel, "exclDlg");

        /* BOTTOM RIGHT AREA */
        //Amount of news
        labelSum = new JLabel();
        labelSum.setBounds(880, 282, 120, 13);
        labelSum.setFont(GUI_FONT);
        labelSum.setForeground(new Color(255, 255, 153));
        labelSum.setBackground(new Color(240, 255, 240));
        getContentPane().add(labelSum);

        //Send current results e-mail
        sendEmailBtn = new JButton();
        sendEmailBtn.setVisible(false);
        setButton = new SetButton(Icons.SEND_EMAIL_ICON, new Color(255, 255, 153), 930, 277);
        setButton.buttonSetting(sendEmailBtn, "Send current search results");
        sendEmailBtn.setFocusable(false);
        sendEmailBtn.setContentAreaFilled(false);
        sendEmailBtn.setBorderPainted(false);
        sendEmailBtn.addActionListener(e -> {
            if (model.getRowCount() > 0 && Common.emailTo.contains("@")) {
                Common.console("sending e-mail");
                Common.IS_SENDING.set(false);
                new Thread(Common::fillProgressLine).start();
                EmailManager email = new EmailManager();
                new Thread(email::sendMessage).start();
            }
        });
        getContentPane().add(sendEmailBtn);
        animation(sendEmailBtn, Icons.SEND_EMAIL_ICON, Icons.WHEN_MOUSE_ON_SEND_ICON);

        // Диалоговое окно со списком источников "sources"
        smiBtn = new JButton();
        smiBtn.setFocusable(false);
        smiBtn.setContentAreaFilled(true);
        smiBtn.setBorderPainted(false);
        smiBtn.setFocusable(false);
        smiBtn.setBounds(883, 479, 14, 14);
        smiBtn.setBackground(new Color(221, 255, 221));
        getContentPane().add(smiBtn);
        smiBtn.addActionListener((e) -> new Dialogs("smiDlg"));
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

        //Открыть папку с настройками "files"
        JButton settingsDirectoryBtn = new JButton();
        settingsDirectoryBtn.setFocusable(false);
        settingsDirectoryBtn.setContentAreaFilled(true);
        settingsDirectoryBtn.setBorderPainted(false);
        settingsDirectoryBtn.setBackground(new Color(219, 229, 252));
        settingsDirectoryBtn.setBounds(940, 479, 14, 14);
        getContentPane().add(settingsDirectoryBtn);
        settingsDirectoryBtn.addActionListener(e -> {
//            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
//                try {
//                    Desktop.getDesktop().open(new File(Common.DIRECTORY_PATH));
//                } catch (IOException io) {
//                    io.printStackTrace();
//                }
//            }

            Common.getSettings();

            JTextField emailFrom = new JTextField(Common.emailFrom);
            JPasswordField emailFromPwd = new JPasswordField(Common.emailFromPwd);
            JTextField emailTo = new JTextField(Common.emailTo);
            JTextField transparency = new JTextField();
            transparency.setText(String.valueOf(Common.transparency));
            JTextField pathToDatabase = new JTextField();
            pathToDatabase.setText(Common.getPathToDatabase());

            Object[] newSource = {"Email from:", emailFrom,
                    "Email from password:", emailFromPwd,
                    "Email to:", emailTo,
                    "Transparency:", transparency,
                    "Path to database:", pathToDatabase
            };

            int result = JOptionPane.showConfirmDialog(null, newSource,
                    "Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                jdbcQueries.updateSettings("email_to", emailTo.getText());
                jdbcQueries.updateSettings("email_from", emailFrom.getText());
                jdbcQueries.updateSettings("from_pwd", String.valueOf(emailFromPwd.getPassword()));
                jdbcQueries.updateSettings("transparency", transparency.getText());

                Common.delSettings("db");
                Common.writeToConfigTxt("db", pathToDatabase.getText());
                jdbcQueries.updateSettings("db_path", pathToDatabase.getText());
            }
        });
        animation(settingsDirectoryBtn, "settings");

        // Источники, sqlite лейбл
        lblLogSourceSqlite = new JLabel("settings");
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

        /* WEB PAGES */
        int webX = 758;
        int webY = 545;
        int webHeight = 18;
        int webWidth = 18;

        String urlTradingView = "https://www.tradingview.com/";
        JLabel tradingView = new JLabel("trading");
        tradingView.setToolTipText(urlTradingView);
        tradingView.setIcon(Icons.TRADING_BUTTON_ICON);
        tradingView.setForeground(new Color(255, 160, 122));
        tradingView.setEnabled(false);
        tradingView.setFont(GUI_FONT_BOLD);
        tradingView.setBounds(webX, webY, webWidth, webHeight);
        getContentPane().add(tradingView);
        animation(tradingView, urlTradingView);

        String urlTranslator = "https://translate.google.com/";
        JLabel translator = new JLabel();
        translator.setIcon(Icons.TRANSLATOR_BUTTON_ICON);
        translator.setToolTipText(urlTranslator);
        translator.setForeground(new Color(255, 160, 122));
        translator.setEnabled(false);
        translator.setFont(GUI_FONT_BOLD);
        translator.setBounds(webX + 24, webY, webWidth, webHeight);
        getContentPane().add(translator);
        animation(translator, urlTranslator);

        String urlMaps = "https://yandex.ru/maps";
        JLabel maps = new JLabel("maps");
        maps.setIcon(Icons.MAPS_BUTTON_ICON);
        maps.setToolTipText(urlMaps);
        maps.setForeground(new Color(255, 160, 122));
        maps.setEnabled(false);
        maps.setFont(GUI_FONT_BOLD);
        maps.setBounds(webX + 48, webY, webWidth, webHeight);
        getContentPane().add(maps);
        animation(maps, urlMaps);

        String urlYandex = "https://ya.ru/";
        JLabel yandex = new JLabel();
        yandex.setIcon(Icons.YANDEX_BUTTON_ICON);
        yandex.setToolTipText(urlYandex);
        yandex.setForeground(new Color(255, 160, 122));
        yandex.setEnabled(false);
        yandex.setFont(GUI_FONT_BOLD);
        yandex.setBounds(webX + 72, webY, webWidth, webHeight);
        getContentPane().add(yandex);
        animation(yandex, urlYandex);

        String urlGithub = "https://github.com/mrprogre";
        JLabel github = new JLabel();
        github.setIcon(Icons.GITHUB_BUTTON_ICON);
        github.setToolTipText(urlGithub);
        github.setForeground(new Color(255, 160, 122));
        github.setEnabled(false);
        github.setFont(GUI_FONT_BOLD);
        github.setBounds(webX + 96, webY, webWidth, webHeight);
        getContentPane().add(github);
        animation(github, urlGithub);

        // Mouse right click menu
        final JPopupMenu popup = new JPopupMenu();

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
        JMenuItem menuDeleteRow = new JMenuItem("Delete");
        menuDeleteRow.addActionListener(e -> {
            int row = table.convertRowIndexToModel(table.getSelectedRow());
            if (row != -1) model.removeRow(row);
        });
        popup.add(menuDeleteRow);

        // Add to favorites (menu)
        JMenuItem menuFavorite = new JMenuItem("Favorite");
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
                    //int row = source.rowAtPoint(e.getPoint());
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

        this.setVisible(true);
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
                lblLogSourceSqlite.setText("settings");
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