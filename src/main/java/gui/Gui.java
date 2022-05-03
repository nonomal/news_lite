package gui;

import database.SQLite;
import search.Search;
import main.Main;
import utils.Common;
import email.EmailSender;
import utils.ExportToExcel;
import utils.MyTimerTask;

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
    private static final long AUTO_START_TIMER = 60000L; // 60 секунд
    public static ImageIcon logoIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/logo.png")));
    public static ImageIcon send = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/send.png")));
    public static ImageIcon send2 = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/send2.png")));
    public static ImageIcon send3 = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/send3.png")));
    public static ImageIcon searchIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/search.png")));
    public static ImageIcon stopIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/stop.png")));
    public static ImageIcon clearIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/clear.png")));
    public static ImageIcon excelIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/excel.png")));
    public static ImageIcon createIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/create.png")));
    public static ImageIcon deleteIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/delete.png")));
    public static ImageIcon fontIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/font.png")));
    public static ImageIcon bgIco = new ImageIcon(Toolkit.getDefaultToolkit().createImage(Gui.class.getResource("/icons/bg.png")));
    public static int q = 1;
    public static boolean isOnlyLastNews = false;
    public static boolean isInKeywords = false;
    public static String findWord = "";
    public static String sendTo;
    public static JScrollPane scrollPane;
    public static JTable table;
    public static JTable tableForAnalysis;
    public static DefaultTableModel model;
    public static DefaultTableModel modelForAnalysis;
    public static JTextField topKeyword;
    public static JTextField sendEmailTo;
    public static JTextField addKeywordToList;
    public static JTextArea consoleTextArea;
    public static JComboBox<String> keywordsCbox;
    public static JComboBox<String> newsIntervalCbox;
    public static JLabel labelSign;
    public static JLabel labelSum;
    public static JLabel labelInfo;
    public static JLabel timeLbl;
    public static JLabel searchAnimation;
    public static JLabel lblLogSourceSqlite;
    public static JButton searchBtnTop;
    public static JButton searchBtnBottom;
    public static JButton stopBtnTop;
    public static JButton stopBtnBottom;
    public static JButton sendEmailBtn;
    public static JButton smiBtn;
    public static JButton logBtn;
    public static JButton exclBtn;
    public static Checkbox todayOrNotCbx;
    public static Checkbox autoUpdateNewsTop;
    public static Checkbox autoUpdateNewsBottom;
    public static Checkbox autoSendMessage;
    public static Checkbox filterNewsChbx;
    public static JProgressBar progressBar;
    public static Timer timer;
    public static TimerTask timerTask;
    public static AtomicBoolean wasClickInTableForAnalysis = new AtomicBoolean(false);
    public static AtomicBoolean guiInTray = new AtomicBoolean(false);

    public Gui() {
        setResizable(false);
        getContentPane().setBackground(new Color(42, 42, 42));
        setTitle("Avandy News");
        setIconImage(logoIco.getImage());
        setFont(new Font("SansSerif", Font.PLAIN, 12));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(336, 170, 1195, 685);
        getContentPane().setLayout(null);

        //Action Listener for EXIT_ON_CLOSE
        addWindowListener(new WindowAdapter() {
            // закрытие окна
            @Override
            public void windowClosing(WindowEvent e) {
                Search.isSearchFinished.set(true);
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
                if (autoUpdateNewsBottom.getState()) consoleTextArea.setText("");
            }
            // разворачивание из трея
            public void windowDeiconified(WindowEvent pEvent) {
                guiInTray.set(false);
            }
        });

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

        //Input keyword
        JLabel lblNewLabel = new JLabel("Keyword:");
        lblNewLabel.setForeground(new Color(255, 179, 131));
        lblNewLabel.setBounds(10, 9, 71, 19);
        lblNewLabel.setFont(new Font("Tahoma", Font.BOLD, 15));
        lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
        getContentPane().add(lblNewLabel);

        //Table
        scrollPane = new JScrollPane();
        scrollPane.setBounds(10, 40, 1160, 400);
        getContentPane().add(scrollPane);
        Object[] columns = {"Num", "Source", "Title (double click to open the link)", "Date", "Link"};
        model = new DefaultTableModel(new Object[][]{
        }, columns) {
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
                if (tip.length() > 69) {
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
        table.setRowHeight(28);
        table.setColumnSelectionAllowed(true);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setFont(new Font("SansSerif", Font.PLAIN, 14));
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
        tableForAnalysisLabel.setText("word frequency");
        tableForAnalysisLabel.setToolTipText("matches more than " + sqlite.getWordFreqMatches());
        tableForAnalysisLabel.setForeground(new Color(255, 255, 153));
        tableForAnalysisLabel.setBounds(11, 443, 190, 14);
        getContentPane().add(tableForAnalysisLabel);

        //Table for analysis
        JScrollPane scrollForAnalysis = new JScrollPane();
        scrollForAnalysis.setBounds(10, 460, 300, 120);
        getContentPane().add(scrollForAnalysis);

        String[] columnsForAnalysis = {"freq.", "words", " "};
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
        //tableForAnalysis.getColumnModel().getColumn(1).setCellRenderer(rendererForAnalysis);
        tableForAnalysis.getColumn(" ").setCellRenderer(new ButtonColumn(tableForAnalysis, 2));
        tableForAnalysis.setRowHeight(20);
        tableForAnalysis.setColumnSelectionAllowed(true);
        tableForAnalysis.setCellSelectionEnabled(true);
        tableForAnalysis.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        tableForAnalysis.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tableForAnalysis.getColumnModel().getColumn(0).setPreferredWidth(80);
        tableForAnalysis.getColumnModel().getColumn(1).setPreferredWidth(140);
        tableForAnalysis.getColumnModel().getColumn(2).setMaxWidth(30);
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
        searchBtnTop.setToolTipText("Без заголовков со словами " + Search.excludeFromSearch);
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

        //Amount of news
        labelSum = new JLabel();
        labelSum.setBounds(70, 583, 115, 13);
        labelSum.setFont(new Font("Tahoma", Font.PLAIN, 11));
        labelSum.setForeground(new Color(255, 255, 153));
        labelSum.setBackground(new Color(240, 255, 240));
        getContentPane().add(labelSum);

        //Another info
        labelInfo = new JLabel();
        labelInfo.setBounds(125, 685, 300, 13);
        labelInfo.setFont(new Font("Tahoma", Font.PLAIN, 11));
        labelInfo.setForeground(new Color(149, 255, 118));
        labelInfo.setBackground(new Color(240, 255, 240));
        getContentPane().add(labelInfo);

        /* Top-Right bottoms */
        // Выбор цвета фона
        JButton backgroundColorBtn = new JButton();
        backgroundColorBtn.setToolTipText("Background color");
        backgroundColorBtn.setBackground(new Color(189, 189, 189));
        backgroundColorBtn.setIcon(bgIco);
        backgroundColorBtn.setBounds(1035, 9, 30, 22);
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
        fontColorBtn.setToolTipText("Font color");
        fontColorBtn.setBackground(new Color(190, 225, 255));
        fontColorBtn.setIcon(fontIco);
        fontColorBtn.setBounds(1070, 9, 30, 22);
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
        exportBtn.setIcon(excelIco);
        exportBtn.setToolTipText("Export news to excel");
        exportBtn.setBackground(new Color(255, 251, 183));
        exportBtn.setBounds(1105, 9, 30, 22);
        exportBtn.addActionListener(e -> {
            if (model.getRowCount() != 0) {
                new Thread(exp::export_from_RSS_to_excel).start();
                Common.console("status: export");
            } else {
                Common.console("status: there is no data to export");
            }
        });
        getContentPane().add(exportBtn);

        //Clear addNewSource
        JButton clearBtnTop = new JButton();
        clearBtnTop.setToolTipText("Clear the list");
        clearBtnTop.setBackground(new Color(250, 128, 114));
        clearBtnTop.setIcon(clearIco);
        clearBtnTop.setBounds(1140, 9, 30, 22);

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

        /* KEYWORDS SEARCH */
        int bottomSearchCoefficientX = 10;
        int bottomSearchCoefficientY = 0;

        // label
        JLabel lblKeywordsSearch = new JLabel();
        lblKeywordsSearch.setText("search by keywords");
        lblKeywordsSearch.setForeground(new Color(255, 255, 153));
        lblKeywordsSearch.setFont(new Font("Tahoma", Font.PLAIN, 11));
        lblKeywordsSearch.setBounds(621 + bottomSearchCoefficientX, 442 + bottomSearchCoefficientY, 160, 14);
        getContentPane().add(lblKeywordsSearch);

        //Add to combo box
        addKeywordToList = new JTextField();
        addKeywordToList.setFont(new Font("Serif", Font.PLAIN, 12));
        addKeywordToList.setBounds(621 + bottomSearchCoefficientX, 460 + bottomSearchCoefficientY, 80, 22);
        getContentPane().add(addKeywordToList);

        //Add to keywords combo box
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
        btnAddKeywordToList.setBounds(706 + bottomSearchCoefficientX, 460 + bottomSearchCoefficientY, 30, 22);

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
        btnDelFromList.setBounds(741 + bottomSearchCoefficientX, 460 + bottomSearchCoefficientY, 30, 22);
        getContentPane().add(btnDelFromList);

        //Keywords combo box
        keywordsCbox = new JComboBox<>();
        keywordsCbox.setFont(new Font("Arial", Font.PLAIN, 11));
        keywordsCbox.setModel(new DefaultComboBoxModel<>());
        keywordsCbox.setEditable(false);
        keywordsCbox.setBounds(776 + bottomSearchCoefficientX, 460 + bottomSearchCoefficientY, 90, 22);
        getContentPane().add(keywordsCbox);

        //Bottom search by keywords
        searchBtnBottom = new JButton("");
        searchBtnBottom.setIcon(searchIco);
        searchBtnBottom.setFont(new Font("Tahoma", Font.BOLD, 10));
        searchBtnBottom.setBackground(new Color(154, 237, 196));
        searchBtnBottom.setBounds(872 + bottomSearchCoefficientX, 460 + bottomSearchCoefficientY, 30, 22);
        //searchBtnBottom.addActionListener(e -> new Thread(Search::keywordsSearch).start());
        searchBtnBottom.addActionListener(e -> new Thread(() -> search.mainSearch("words")).start());
        getContentPane().add(searchBtnBottom);

        //Stop addNewSource (bottom)
        stopBtnBottom = new JButton("");
        stopBtnBottom.setIcon(stopIco);
        stopBtnBottom.setBackground(new Color(255, 208, 202));
        stopBtnBottom.setBounds(872 + bottomSearchCoefficientX, 460 + bottomSearchCoefficientY, 30, 22);
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
        autoUpdateNewsBottom = new Checkbox("auto update");
        autoUpdateNewsBottom.setState(false);
        autoUpdateNewsBottom.setFocusable(false);
        autoUpdateNewsBottom.setForeground(Color.WHITE);
        autoUpdateNewsBottom.setFont(new Font("Arial", Font.PLAIN, 11));
        autoUpdateNewsBottom.setBounds(908 + bottomSearchCoefficientX, 460 + bottomSearchCoefficientY, 75, 20);
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
                } catch (Exception ignored){

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
        consoleTextArea.setBounds(320, 460, 300, 120);
        consoleTextArea.setFont(new Font("Tahoma", Font.PLAIN, 11));
        consoleTextArea.setForeground(SystemColor.white);
        consoleTextArea.setBackground(new Color(83, 82, 82)); // 83, 82, 82
        getContentPane().add(consoleTextArea);

        //Console - scroll
        JScrollPane consoleScroll = new JScrollPane(consoleTextArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        consoleScroll.setBounds(320, 460, 300, 120);
        consoleScroll.setBorder(null);
        getContentPane().add(consoleScroll);
        //Console - label
        JLabel lblConsole = new JLabel();
        lblConsole.setText("console");
        lblConsole.setForeground(new Color(255, 255, 153));
        lblConsole.setFont(new Font("Tahoma", Font.PLAIN, 11));
        lblConsole.setBounds(321, 442, 83, 14);
        getContentPane().add(lblConsole);

        // Clear console
        JButton clearConsoleBtn = new JButton();
        //clearConsoleBtn.setIcon(clearIco);
        clearConsoleBtn.setToolTipText("Clear the console");
        clearConsoleBtn.setBackground(new Color(0, 52, 96));
        clearConsoleBtn.setBounds(606, 444, 14, 14);
        clearConsoleBtn.addActionListener(e -> consoleTextArea.setText(""));
        getContentPane().add(clearConsoleBtn);

        //Searching animation
        searchAnimation = new JLabel();
        searchAnimation.setForeground(new Color(255, 255, 153));
        searchAnimation.setFont(new Font("Tahoma", Font.PLAIN, 11));
        searchAnimation.setBackground(new Color(240, 255, 240));
        searchAnimation.setBounds(10, 583, 80, 13);
        getContentPane().add(searchAnimation);

        //Time label
        timeLbl = new JLabel();
        timeLbl.setForeground(new Color(255, 255, 153));
        timeLbl.setFont(new Font("Tahoma", Font.PLAIN, 11));
        timeLbl.setBounds(207, 580, 160, 20);
        getContentPane().add(timeLbl);

        // Шкала прогресса
        progressBar = new JProgressBar();
        progressBar.setFocusable(false);
        progressBar.setMaximum(100);
        progressBar.setBorderPainted(false);
        progressBar.setForeground(new Color(10, 255, 41));
        progressBar.setBackground(new Color(1, 1, 1));
        progressBar.setBounds(365, 449, 239, 3);
        getContentPane().add(progressBar);

        // Интервалы для поиска новостей
        newsIntervalCbox = new JComboBox<>();
        newsIntervalCbox.setFont(new Font("Arial", Font.PLAIN, 11));
        newsIntervalCbox.setBounds(378, 10, 75, 20);
        getContentPane().add(newsIntervalCbox);
        // запись интервалов в комбобокс
        Common.addIntervalsToComboBox(newsIntervalCbox);

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

        // Диалоговое окно со списком исключенных слов из анализа
        exclBtn = new JButton();
        exclBtn.setFocusable(false);
        exclBtn.setContentAreaFilled(true);
        exclBtn.setBorderPainted(false);
        exclBtn.setBackground(new Color(30, 27, 27));
        exclBtn.setBounds(297, 443, 14, 14);
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
        JLabel excludedLabel = new JLabel("excluded list");
        excludedLabel.setHorizontalAlignment(SwingConstants.LEFT);
        excludedLabel.setForeground(new Color(255, 255, 153));
        excludedLabel.setFont(new Font("Tahoma", Font.PLAIN, 11));
        excludedLabel.setBackground(new Color(240, 255, 240));
        excludedLabel.setBounds(231, 437, 130, 26);
        getContentPane().add(excludedLabel);

        /* BOTTOM RIGHT AREA */
        //send e-mail to - label
        JLabel lblSendToEmail = new JLabel();
        lblSendToEmail.setText("send to");
        lblSendToEmail.setForeground(new Color(255, 255, 153));
        lblSendToEmail.setFont(new Font("Tahoma", Font.PLAIN, 11));
        lblSendToEmail.setBounds(1011, 442, 83, 14);
        getContentPane().add(lblSendToEmail);

        //send e-mail to
        sendEmailTo = new JTextField("enter your email");
        sendEmailTo.setBounds(1016, 465, 126, 21);
        sendEmailTo.setFont(new Font("Serif", Font.PLAIN, 12));
        getContentPane().add(sendEmailTo);

        //Send current results e-mail
        sendEmailBtn = new JButton();
        sendEmailBtn.setIcon(send);
        sendEmailBtn.setToolTipText("send the current result");
        sendEmailBtn.setFocusable(false);
        sendEmailBtn.setContentAreaFilled(false);
        sendEmailBtn.setBorderPainted(false);
        sendEmailBtn.setBackground(new Color(255, 255, 153));
        sendEmailBtn.setBounds(1140, 464, 32, 23);
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
        autoSendMessage.setBounds(1016, 488, 135, 20);
        getContentPane().add(autoSendMessage);

        // Источники, лог, sqlite лейбл
        lblLogSourceSqlite = new JLabel();
        lblLogSourceSqlite.setForeground(Color.WHITE);
        lblLogSourceSqlite.setFont(new Font("Arial", Font.PLAIN, 11));
        lblLogSourceSqlite.setBounds(1110, 526, 60, 14);
        getContentPane().add(lblLogSourceSqlite);

        // Диалоговое окно со списком источников "sources"
        smiBtn = new JButton();
        smiBtn.setFocusable(false);
        smiBtn.setContentAreaFilled(true);
        smiBtn.setBorderPainted(false);
        smiBtn.setFocusable(false);
        smiBtn.setBounds(1016, 526, 14, 14);
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
        addNewSource.setBounds(1035, 526, 14, 14);
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

        // Диалоговое окно лога "log"
        logBtn = new JButton();
        logBtn.setContentAreaFilled(true);
        logBtn.setBorderPainted(false);
        logBtn.setFocusable(false);
        logBtn.setBackground(new Color(248, 206, 165));
        logBtn.setBounds(1054, 526, 14, 14);
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

        //SQLite
        JButton sqliteBtn = new JButton();
        sqliteBtn.setToolTipText("press CTRL+v in SQLite to open the database");
        sqliteBtn.setFocusable(false);
        sqliteBtn.setContentAreaFilled(true);
        sqliteBtn.setBorderPainted(false);
        sqliteBtn.setBackground(new Color(244, 181, 181));
        sqliteBtn.setBounds(1073, 526, 14, 14);
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

        //Открыть папку с настройками "files"
        JButton settingsDirectoryBtn = new JButton();
        settingsDirectoryBtn.setFocusable(false);
        settingsDirectoryBtn.setContentAreaFilled(true);
        settingsDirectoryBtn.setBorderPainted(false);
        settingsDirectoryBtn.setBackground(new Color(219, 229, 252));
        settingsDirectoryBtn.setBounds(1092, 526, 14, 14);
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
        verticalBox.setBounds(1010, 460, 161, 51);
        getContentPane().add(verticalBox);

        // Border different bottoms
        Box queryTableBox = Box.createVerticalBox();
        queryTableBox.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
        queryTableBox.setBounds(1011, 520, 161, 26);
        getContentPane().add(queryTableBox);

        // latest news
        filterNewsChbx = new Checkbox("only the latest news");
        filterNewsChbx.setState(false);
        filterNewsChbx.setFocusable(false);
        filterNewsChbx.setForeground(Color.WHITE);
        filterNewsChbx.setFont(new Font("Arial", Font.PLAIN, 11));
        filterNewsChbx.setBounds(1016, 557, 135, 20);
        getContentPane().add(filterNewsChbx);
        filterNewsChbx.addItemListener(e -> {
            isOnlyLastNews = filterNewsChbx.getState();
            if (!isOnlyLastNews){
                sqlite.deleteFrom256();
            }
        });

        // border latest news
        Box latestNewsBorder = Box.createVerticalBox();
        latestNewsBorder.setBorder(new BevelBorder(BevelBorder.RAISED, null, null, null, null));
        latestNewsBorder.setBounds(1011, 554, 161, 26);
        getContentPane().add(latestNewsBorder);

        //My sign
        labelSign = new JLabel(":mrprogre");
        labelSign.setForeground(new Color(255, 160, 122));
        labelSign.setEnabled(false);
        labelSign.setFont(new Font("Tahoma", Font.BOLD, 11));
        labelSign.setBounds(1115, 625, 57, 14);
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

        setVisible(true);
    }
}