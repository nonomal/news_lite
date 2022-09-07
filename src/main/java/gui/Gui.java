package gui;

import database.DatabaseQueries;
import database.SQLite;
import email.EmailSender;
import gui.buttons.Icons;
import gui.buttons.SetButton;
import gui.checkboxes.SetCheckbox;
import lombok.extern.slf4j.Slf4j;
import main.Main;
import search.Search;
import utils.Common;
import utils.ExportToExcel;
import utils.MyTimerTask;
import utils.Translator;

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

@Slf4j
public class Gui extends JFrame {
    final SQLite sqLite = new SQLite();
    final DatabaseQueries databaseQueries = new DatabaseQueries();
    final Search search = new Search();

    private static final float OPACITY = Common.OPACITY;
    private static final Object[] MAIN_TABLE_HEADERS = {"Num", "Source", "Title", "Date", "Link"};
    private static final String[] TABLE_FOR_ANALYZE_HEADERS = {"top 10", "freq.", " "};
    private static final Font GUI_FONT = new Font("Tahoma", Font.PLAIN, 11);
    private static final String[] INTERVALS = {"1 min", "5 min", "15 min", "30 min", "45 min", "1 hour", "2 hours",
            "4 hours", "8 hours", "12 hours", "24 hours", "48 hours", "72 hours", "all"};
    private static final long AUTO_START_TIMER = 60000L; // 60 секунд
    public static final AtomicBoolean WAS_CLICK_IN_TABLE_FOR_ANALYSIS = new AtomicBoolean(false);
    public static final AtomicBoolean GUI_IN_TRAY = new AtomicBoolean(false);
    public static int newsCount = 1;
    public static boolean isOnlyLastNews = false;
    public static boolean isInKeywords = false;
    public static String findWord = "";
    public static JScrollPane scrollPane;
    public static JTable table;
    public static JTable tableForAnalysis;
    public static DefaultTableModel model;
    public static DefaultTableModel modelForAnalysis;
    public static JTextField topKeyword;
    public static JTextField sendEmailTo;
    public static JTextField addKeywordToList;
    public static JTextArea consoleTextArea;
    public static JComboBox<String> keywords;
    public static JComboBox<String> newsInterval;
    public static JLabel labelSign;
    public static JLabel labelSum;
    public static JLabel lblLogSourceSqlite;
    public static JButton searchBtnTop;
    public static JButton searchBtnBottom;
    public static JButton stopBtnTop;
    public static JButton stopBtnBottom;
    public static JButton sendEmailBtn;
    public static JButton smiBtn;
    public static JButton logBtn;
    public static JButton exclBtn;
    public static Checkbox autoUpdateNewsTop;
    public static Checkbox autoUpdateNewsBottom;
    public static Checkbox autoSendMessage;
    public static Checkbox onlyNewNews;
    public static JProgressBar progressBar;
    public static Timer timer;
    public static TimerTask timerTask;

    public Gui() {
        this.setResizable(false);
        this.getContentPane().setBackground(new Color(42, 42, 42));
        this.setTitle("Avandy News");
        this.setIconImage(Icons.LOGO_ICON.getImage());
        this.setFont(GUI_FONT);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setBounds(360, 180, 1181, 592);
        this.getContentPane().setLayout(null);

        // Прозрачность и оформление окна
        this.setUndecorated(true);
        // Проверка поддерживает ли операционная система прозрачность окон
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        GraphicsDevice gd = ge.getDefaultScreenDevice();
        boolean isUniformTranslucencySupported = gd.isWindowTranslucencySupported(TRANSLUCENT);
        if (isUniformTranslucencySupported) {
            this.setOpacity(OPACITY);
        }

        //Input keyword
        JLabel lblNewLabel = new JLabel("Keyword:");
        lblNewLabel.setForeground(new Color(255, 179, 131));
        lblNewLabel.setBounds(10, 9, 71, 19);
        lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 15));
        lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
        getContentPane().add(lblNewLabel);

        //Table
        scrollPane = new JScrollPane();
        scrollPane.setBounds(10, 40, 860, 500);
        getContentPane().add(scrollPane);
        model = new DefaultTableModel(new Object[][]{
        }, MAIN_TABLE_HEADERS) {
            final boolean[] columnEditable = new boolean[]{
                    false, false, false, false, false
            };

            public boolean isCellEditable(int row, int column) {
                return columnEditable[column];
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
                }
//                else if (!tip.contains("а-я")) {
//                    return Translator.translate("en", "ru", tip);
//                }
                else {
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
        table.getColumnModel().getColumn(3).setPreferredWidth(100);
        table.getColumnModel().getColumn(3).setMaxWidth(100);
        table.removeColumn(table.getColumnModel().getColumn(4)); // Скрыть 4 колонку со ссылкой на новость
        scrollPane.setViewportView(table);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.convertRowIndexToModel(table.rowAtPoint(new Point(e.getX(), e.getY()))); // при сортировке строк оставляет верные данные
                    int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
                    if (col == 2 || col == 4) {
                        String url = (String) table.getModel().getValueAt(row, 4);
                        URI uri = null;
                        try {
                            uri = new URI(url);
                        } catch (URISyntaxException ex) {
                            ex.printStackTrace();
                            log.warn(ex.getMessage());
                        }
                        Desktop desktop = Desktop.getDesktop();
                        assert uri != null;
                        try {
                            desktop.browse(uri);
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            log.warn(ex.getMessage());
                        }
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
                        searchBtnTop.doClick();
                        WAS_CLICK_IN_TABLE_FOR_ANALYSIS.set(true);
                    }
                }
            }
        });

        /* TOP-LEFT ACTION PANEL */
        int topLeftActionY = 9;

        //Keyword field
        topKeyword = new JTextField(findWord);
        topKeyword.setBounds(87, topLeftActionY, 100, 22);
        topKeyword.setFont(new Font("Tahoma", Font.BOLD, 13));
        getContentPane().add(topKeyword);

        //Search addNewSource
        searchBtnTop = new JButton();
        searchBtnTop.setIcon(Icons.SEARCH_KEYWORDS_ICON);
        searchBtnTop.setToolTipText("Без заголовков со словами " + Search.excludeFromSearch);
        searchBtnTop.setBackground(new Color(154, 237, 196));
        searchBtnTop.setFont(new Font("Tahoma", Font.BOLD, 10));
        searchBtnTop.setBounds(192, topLeftActionY, 30, 22);
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
        stopBtnTop.setBounds(192, topLeftActionY, 30, 22);
        stopBtnTop.addActionListener(e -> {
            try {
                Search.isSearchFinished.set(true);
                Search.isStop.set(true);
                Common.console("status: search stopped");
                searchBtnTop.setVisible(true);
                stopBtnTop.setVisible(false);
                Search.isSearchNow.set(false);
                try {
                    sqLite.transactionCommand("ROLLBACK");
                } catch (SQLException ignored) {
                }
            } catch (Exception t) {
                Common.console("status: there is no threads to stop");
            }
        });
        getContentPane().add(stopBtnTop);

        // Интервалы для поиска новостей
        newsInterval = new JComboBox<>(INTERVALS);
        newsInterval.setFont(GUI_FONT);
        newsInterval.setBounds(230, topLeftActionY, 75, 20); //516
        getContentPane().add(newsInterval);

        // latest news
        onlyNewNews = new Checkbox("only new");
        SetCheckbox setCheckbox = new SetCheckbox(315, topLeftActionY, 65);
        setCheckbox.checkBoxSetting(onlyNewNews);
        getContentPane().add(onlyNewNews);
        onlyNewNews.addItemListener(e -> {
            isOnlyLastNews = onlyNewNews.getState();
            if (!isOnlyLastNews) {
                databaseQueries.deleteFrom256(SQLite.connection);
            }
        });

        // Автоматическая отправка письма с результатами
        autoSendMessage = new Checkbox("auto send");
        setCheckbox = new SetCheckbox(383, topLeftActionY, 66);
        setCheckbox.checkBoxSetting(autoSendMessage);
        getContentPane().add(autoSendMessage);

        // Автозапуск поиска по слову каждые 60 секунд
        autoUpdateNewsTop = new Checkbox("auto update");
        SetCheckbox setCheckbox1 = new SetCheckbox(455, topLeftActionY, 75);
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

        /* Top-Right action panel */
        int topRightX = 965;
        int topRightY = 9;

        // Выбор цвета фона
        JButton backgroundColorBtn = new JButton();
        SetButton setButton = new SetButton(Icons.BACK_GROUND_COLOR_ICON, new Color(189, 189, 189), topRightX, topRightY);
        setButton.buttonSetting(backgroundColorBtn, "Background color");

        backgroundColorBtn.addActionListener(e -> {
            Color color = JColorChooser.showDialog(null, "Color", Color.black);
            if (color != null) {
                try {
                    table.setBackground(color);
                    Common.delSettings("backgroundColorRed");
                    Common.delSettings("backgroundColorGreen");
                    Common.delSettings("backgroundColorBlue");
                    Common.writeToConfig(String.valueOf(color.getRed()), "backgroundColorRed");
                    Common.writeToConfig(String.valueOf(color.getGreen()), "backgroundColorGreen");
                    Common.writeToConfig(String.valueOf(color.getBlue()), "backgroundColorBlue");
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        getContentPane().add(backgroundColorBtn);

        // Выбор цвета шрифта в таблице
        JButton fontColorBtn = new JButton();
        setButton = new SetButton(Icons.FONT_COLOR_BUTTON_ICON, new Color(190, 225, 255), topRightX + 35, topRightY);
        setButton.buttonSetting(fontColorBtn, "Font color");

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

        //Export to excel
        JButton exportBtn = new JButton();
        setButton = new SetButton(Icons.EXCEL_BUTTON_ICON, new Color(255, 251, 183), topRightX + 70, topRightY);
        setButton.buttonSetting(exportBtn, "Export news to excel");
        exportBtn.addActionListener(e -> {
            if (model.getRowCount() != 0) {
                new Thread(new ExportToExcel()::exportResultsToExcel).start();
                Common.console("status: export");
            } else {
                Common.console("status: there is no data to export");
            }
        });
        getContentPane().add(exportBtn);

        // Clear
        JButton clearBtnTop = new JButton();
        setButton = new SetButton(Icons.CLEAR_BUTTON_ICON, new Color(250, 128, 114), topRightX + 105, topRightY);
        setButton.buttonSetting(clearBtnTop, "Clear the list");
        clearBtnTop.addActionListener(e -> {
            try {
                if (model.getRowCount() == 0) {
                    Common.console("status: no data to clear");
                    return;
                }
                Search.j = 1;
                model.setRowCount(0);
                modelForAnalysis.setRowCount(0);
                newsCount = 0;
                labelSum.setText("" + newsCount);
                Common.console("status: clear");
            } catch (Exception t) {
                Common.console(t.getMessage());
                t.printStackTrace();
                log.warn(t.getMessage());
            }
        });
        getContentPane().add(clearBtnTop);

        /* Сворачивание в трей */
        JButton toTrayBtn = new JButton(Icons.TRAY_BUTTON_ICON);
        toTrayBtn.setToolTipText("to tray");
        toTrayBtn.setFocusable(false);
        toTrayBtn.setContentAreaFilled(false);
        toTrayBtn.setBorderPainted(false);
        toTrayBtn.setBounds(1126, 3, 24, 22);
        if (SystemTray.isSupported()) {
            getContentPane().add(toTrayBtn);
        }
        toTrayBtn.addActionListener(e -> {
            GUI_IN_TRAY.set(true);
            setVisible(false);
            if (autoUpdateNewsBottom.getState()) consoleTextArea.setText("");
        });
        animation(toTrayBtn, Icons.TRAY_BUTTON_ICON, Icons.WHEN_MOUSE_ON_TRAY_BUTTON_ICON);

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
        exitBtn.setToolTipText("exit");
        exitBtn.setBounds(1151, 3, 24, 22);
        exitBtn.setContentAreaFilled(false);
        exitBtn.setBorderPainted(false);
        exitBtn.setFocusable(false);
        getContentPane().add(exitBtn);
        exitBtn.addActionListener((e) -> {
            Search.isSearchFinished.set(true);
            SQLite.isConnectionToSQLite = false;
            Common.saveState();
            log.info("Application closed");
            if (SQLite.isConnectionToSQLite) sqLite.closeConnection();
            System.exit(0);
        });
        animation(exitBtn, Icons.EXIT_BUTTON_ICON, Icons.WHEN_MOUSE_ON_EXIT_BUTTON_ICON);

        /* KEYWORDS BOTTOM SEARCH */
        // label
        JLabel lblKeywordsSearch = new JLabel();
        lblKeywordsSearch.setText("search by keywords");
        lblKeywordsSearch.setForeground(new Color(255, 255, 153));
        lblKeywordsSearch.setFont(GUI_FONT);
        lblKeywordsSearch.setBounds(10, 545, 160, 14);
        getContentPane().add(lblKeywordsSearch);

        //Add to combo box
        addKeywordToList = new JTextField();
        addKeywordToList.setFont(GUI_FONT);
        addKeywordToList.setBounds(9, 561, 80, 22);
        getContentPane().add(addKeywordToList);

        //Add to keywords combo box
        JButton btnAddKeywordToList = new JButton();
        setButton = new SetButton(Icons.ADD_KEYWORD_ICON, null, 95, 561);
        setButton.buttonSetting(btnAddKeywordToList, "Add keyword");
        getContentPane().add(btnAddKeywordToList);
        btnAddKeywordToList.addActionListener(e -> {
            if (addKeywordToList.getText().length() > 0) {
                String word = addKeywordToList.getText();
                for (int i = 0; i < keywords.getItemCount(); i++) {
                    if (word.equals(keywords.getItemAt(i))) {
                        Common.console("info: список ключевых слов уже содержит: " + word);
                        isInKeywords = true;
                    } else {
                        isInKeywords = false;
                    }
                }
                if (!isInKeywords) {
                    Common.writeToConfig(word, "keyword");
                    keywords.addItem(word);
                    isInKeywords = false;
                }
                addKeywordToList.setText("");
            }
        });

        //Delete from combo box
        JButton btnDelFromList = new JButton();
        setButton = new SetButton(Icons.DELETE_FROM_KEYWORDS_ICON, null, 130, 561);
        setButton.buttonSetting(btnDelFromList, "Delete word from list");
        btnDelFromList.addActionListener(e -> {
            if (keywords.getItemCount() > 0) {
                try {
                    String item = (String) keywords.getSelectedItem();
                    keywords.removeItem(item);
                    Common.delSettings("keyword=" + Objects.requireNonNull(item));
                } catch (IOException io) {
                    io.printStackTrace();
                    log.warn(io.getMessage());
                }
            }
        });

        getContentPane().add(btnDelFromList);

        //Keywords combo box
        keywords = new JComboBox<>();
        keywords.setFont(GUI_FONT);
        keywords.setModel(new DefaultComboBoxModel<>());
        keywords.setEditable(false);
        keywords.setBounds(165, 561, 90, 22);
        getContentPane().add(keywords);

        //Bottom search by keywords
        searchBtnBottom = new JButton();
        setButton = new SetButton(Icons.SEARCH_KEYWORDS_ICON, new Color(154, 237, 196), 261, 561);
        setButton.buttonSetting(searchBtnBottom, "Search by keywords");
        searchBtnBottom.addActionListener(e -> new Thread(() -> search.mainSearch("words")).start());
        getContentPane().add(searchBtnBottom);

        //Stop addNewSource (bottom)
        stopBtnBottom = new JButton();
        setButton = new SetButton(Icons.STOP_SEARCH_ICON, new Color(255, 208, 202), 261, 561);
        setButton.buttonSetting(stopBtnBottom, null);
        stopBtnBottom.addActionListener(e -> {
            try {
                Search.isSearchFinished.set(true);
                Search.isStop.set(true);
                Common.console("status: search stopped");
                searchBtnBottom.setVisible(true);
                stopBtnBottom.setVisible(false);
                Search.isSearchNow.set(false);
                try {
                    sqLite.transactionCommand("ROLLBACK");
                } catch (SQLException ignored) {
                }
            } catch (Exception t) {
                Common.console("status: there is no threads to stop");
            }
        });
        getContentPane().add(stopBtnBottom);

        // Автозапуск поиска по ключевым словам каждые 60 секунд
        autoUpdateNewsBottom = new Checkbox("auto update");
        setCheckbox1 = new SetCheckbox(297, 561, 75);
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
        // авто скроллинг
        DefaultCaret caret = (DefaultCaret) consoleTextArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        consoleTextArea.setCaretPosition(consoleTextArea.getDocument().getLength());
        consoleTextArea.setAutoscrolls(true);
        consoleTextArea.setLineWrap(true);
        consoleTextArea.setEditable(false);
        consoleTextArea.setBounds(20, 11, 145, 51);
        consoleTextArea.setFont(GUI_FONT);
        consoleTextArea.setForeground(SystemColor.white);
        consoleTextArea.setBackground(new Color(83, 82, 82)); // 83, 82, 82
        getContentPane().add(consoleTextArea);

        //Console - scroll
        JScrollPane consoleScroll = new JScrollPane(consoleTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        consoleScroll.setBounds(879, 303, 290, 142);
        consoleScroll.setBorder(null);
        getContentPane().add(consoleScroll);
        //Console - label
        JLabel lblConsole = new JLabel();
        lblConsole.setText("clear console");
        lblConsole.setForeground(new Color(255, 255, 153));
        lblConsole.setFont(GUI_FONT);
        lblConsole.setBounds(1089, 448, 64, 14);
        getContentPane().add(lblConsole);

        // Clear console
        JButton clearConsoleBtn = new JButton();
        clearConsoleBtn.setToolTipText("Clear the console");
        clearConsoleBtn.setBackground(new Color(0, 52, 96));
        clearConsoleBtn.setBounds(1155, 448, 14, 14);
        clearConsoleBtn.addActionListener(e -> consoleTextArea.setText(""));
        getContentPane().add(clearConsoleBtn);

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
        exclBtn = new JButton();
        exclBtn.setFocusable(false);
        exclBtn.setContentAreaFilled(true);
        //exclBtn.setBorderPainted(false);
        exclBtn.setBackground(new Color(0, 52, 96));
        exclBtn.setBounds(1157, 278, 14, 14);
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
                exclBtn.setBackground(new Color(0, 52, 96));
            }
        });
        //label
        JLabel excludedLabel = new JLabel("excluded list");
        excludedLabel.setHorizontalAlignment(SwingConstants.LEFT);
        excludedLabel.setForeground(new Color(255, 255, 153));
        excludedLabel.setFont(GUI_FONT);
        excludedLabel.setBackground(new Color(240, 255, 240));
        excludedLabel.setBounds(1092, 278, 64, 14);
        getContentPane().add(excludedLabel);

        /* BOTTOM RIGHT AREA */
        //send e-mail to - label
        JLabel lblSendToEmail = new JLabel();
        lblSendToEmail.setText("send to");
        lblSendToEmail.setForeground(new Color(255, 255, 153));
        lblSendToEmail.setFont(GUI_FONT);
        lblSendToEmail.setBounds(880, 504, 36, 14);
        getContentPane().add(lblSendToEmail);

        //send e-mail to
        sendEmailTo = new JTextField("enter your email");
        sendEmailTo.setBounds(879, 519, 142, 21);
        sendEmailTo.setFont(GUI_FONT);
        getContentPane().add(sendEmailTo);

        //Send current results e-mail
        sendEmailBtn = new JButton();
        setButton = new SetButton(Icons.SEND_EMAIL_ICON, new Color(255, 255, 153), 1020, 518);
        setButton.buttonSetting(sendEmailBtn, "Send current search results");
        sendEmailBtn.setFocusable(false);
        sendEmailBtn.setContentAreaFilled(false);
        sendEmailBtn.setBorderPainted(false);
        sendEmailBtn.addActionListener(e -> {
            if (model.getRowCount() > 0 && sendEmailTo.getText().contains("@")) {
                Common.console("status: sending e-mail");
                //sendTo = sendEmailTo.getText();
                Common.IS_SENDING.set(false);
                new Thread(Common::fill).start();
                EmailSender email = new EmailSender();
                new Thread(email::sendMessage).start();
            }
        });
        sendEmailBtn.addMouseListener(new MouseAdapter() {
            // наведение мышки на письмо
            @Override
            public void mouseEntered(MouseEvent e) {
                if (sendEmailBtn.getIcon() == Icons.SEND_EMAIL_ICON) {
                    sendEmailBtn.setIcon(Icons.WHEN_MOUSE_ON_SEND_ICON);
                }
            }

            @Override
            // убрали мышку с письма
            public void mouseExited(MouseEvent e) {
                if (sendEmailBtn.getIcon() == Icons.WHEN_MOUSE_ON_SEND_ICON) {
                    sendEmailBtn.setIcon(Icons.SEND_EMAIL_ICON);
                }
            }

        });
        getContentPane().add(sendEmailBtn);

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

        // добавить новый RSS источник "add source"
        JButton addNewSource = new JButton();
        addNewSource.setFocusable(false);
        addNewSource.setContentAreaFilled(true);
        addNewSource.setBorderPainted(false);
        addNewSource.setBackground(new Color(243, 229, 255));
        addNewSource.setBounds(902, 479, 14, 14);
        getContentPane().add(addNewSource);
        addNewSource.addActionListener(e -> databaseQueries.insertNewSource(SQLite.connection));
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

        // Диалоговое окно лога "log"
        logBtn = new JButton();
        logBtn.setContentAreaFilled(true);
        logBtn.setBorderPainted(false);
        logBtn.setFocusable(false);
        logBtn.setBackground(new Color(248, 206, 165));
        logBtn.setBounds(921, 479, 14, 14);
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

        // SQLite
        JButton sqliteBtn = new JButton();
        sqliteBtn.setToolTipText("Press CTRL+V in SQLite to open the database");
        sqliteBtn.setFocusable(false);
        sqliteBtn.setContentAreaFilled(true);
        sqliteBtn.setBorderPainted(false);
        sqliteBtn.setBackground(new Color(244, 181, 181));
        sqliteBtn.setBounds(940, 479, 14, 14);
        getContentPane().add(sqliteBtn);
        sqliteBtn.addActionListener(e -> {
            // запуск DatabaseQueries
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                try {
                    Desktop.getDesktop().open(new File(Main.DIRECTORY_PATH + "sqlite3.exe"));
                } catch (IOException io) {
                    io.printStackTrace();
                }
            }

            // копируем адрес базы в DatabaseQueries в системный буфер для быстрого доступа
            String pathToBase = (".open " + Main.DIRECTORY_PATH + "news.db").replace("\\", "/");
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

        //Открыть папку с настройками "files"
        JButton settingsDirectoryBtn = new JButton();
        settingsDirectoryBtn.setFocusable(false);
        settingsDirectoryBtn.setContentAreaFilled(true);
        settingsDirectoryBtn.setBorderPainted(false);
        settingsDirectoryBtn.setBackground(new Color(219, 229, 252));
        settingsDirectoryBtn.setBounds(959, 479, 14, 14);
        getContentPane().add(settingsDirectoryBtn);
        settingsDirectoryBtn.addActionListener(e -> {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                try {
                    Desktop.getDesktop().open(new File(Main.DIRECTORY_PATH));
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

        // Источники, лог, sqlite лейбл
        lblLogSourceSqlite = new JLabel();
        lblLogSourceSqlite.setForeground(Color.WHITE);
        lblLogSourceSqlite.setFont(GUI_FONT);
        lblLogSourceSqlite.setBounds(979, 479, 60, 14);
        getContentPane().add(lblLogSourceSqlite);

        // Border different bottoms
        Box queryTableBox = Box.createVerticalBox();
        queryTableBox.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
        queryTableBox.setBounds(879, 473, 290, 26);
        getContentPane().add(queryTableBox);

        //Amount of news
        labelSum = new JLabel();
        labelSum.setBounds(880, 278, 120, 13);
        labelSum.setFont(GUI_FONT);
        labelSum.setForeground(new Color(255, 255, 153));
        labelSum.setBackground(new Color(240, 255, 240));
        getContentPane().add(labelSum);

        //My sign
        labelSign = new JLabel("mrprogre");
        labelSign.setForeground(new Color(255, 160, 122));
        labelSign.setEnabled(false);
        labelSign.setFont(new Font("Tahoma", Font.BOLD, 11));
        labelSign.setBounds(1110, 567, 57, 14);
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
                        log.warn(ex.getMessage());
                    }
                }
            }
        });

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

        // Delete rows (menu)
        JMenuItem menuDeleteRow = new JMenuItem("Delete");
        menuDeleteRow.addActionListener((e) -> {
            int[] rows = table.getSelectedRows();
            for (int i = rows.length - 1; i >= 0; --i) {
                model.removeRow(rows[i]);
            }
        });
        popup.add(menuDeleteRow);

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
                    if (!source.isRowSelected(row)) {
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
}