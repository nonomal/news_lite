package com.news;

import com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;

public class Main {
    static SimpleDateFormat date_format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    static String directoryPath = System.getProperty("user.home") + File.separator + "News" + File.separator;
    static String settingsPath = directoryPath + "config.txt";
    static String logPath = directoryPath + "log.txt";
    public static final Logger LOGGER = Logger.getLogger("");
    static Calendar minPubDate = Calendar.getInstance();
    static int red;
    static int green;
    static int blue;
    // Console search
    static AtomicBoolean isConsoleSearch = new AtomicBoolean(false);
    static String emailToFromConsole;
    static int minutesIntervalForConsoleSearch;
    static String[] keywordsFromConsole;

    // создание директорий и файлов
    static {
        // Минимальная дата публикации новости 01.01.2021
        minPubDate.set(Calendar.YEAR, 2021);
        minPubDate.set(Calendar.DAY_OF_YEAR, 1);

        File mainDirectory = new File(directoryPath);
        File logFile = new File(logPath);

        try {
            if (!mainDirectory.exists()) mainDirectory.mkdirs();
            if (!logFile.exists()) logFile.createNewFile();
            // запись лога в файл
            Handler handler = new FileHandler(logPath, true);
            handler.setLevel(Level.ALL);
            handler.setEncoding("UTF-8");
            handler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(handler);
            LOGGER.getHandlers()[0].setFormatter(new Formatter() {
                @Override
                public String format(LogRecord record) {
                    return record.getLevel() + " " + record.getMessage() + " " + date_format.format(record.getMillis()) + "\n";
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // создание файлов программы
        File sqliteExeIsExists = new File(directoryPath + "sqlite3.exe");
        if (!sqliteExeIsExists.exists()) {
            Common.copyFiles(Main.class.getResource("/sqlite3.exe"), directoryPath + "sqlite3.exe");
        }
        File sqliteDllIsExists = new File(directoryPath + "sqlite3.dll");
        if (!sqliteDllIsExists.exists()) {
            Common.copyFiles(Main.class.getResource("/sqlite3.dll"), directoryPath + "sqlite3.dll");
        }
        File sqliteDefIsExists = new File(directoryPath + "sqlite3.def");
        if (!sqliteDefIsExists.exists()) {
            Common.copyFiles(Main.class.getResource("/sqlite3.def"), directoryPath + "sqlite3.def");
        }
        File dbIsExists = new File(directoryPath + "news.db");
        if (!dbIsExists.exists()) {
            Common.copyFiles(Main.class.getResource("/news.db"), directoryPath + "news.db");
        }
        File configIsExists = new File(directoryPath + "config.txt");
        if (!configIsExists.exists()) {
            Common.copyFiles(Main.class.getResource("/config.txt"), directoryPath + "config.txt");
        }
    }

    public static void main(String[] args) throws IOException { //args1 = email, args2 = interval, args3 = keyword1,.., argsN = keywordN
        keywordsFromConsole = new String[args.length];
        SQLite sqlite = new SQLite();
        Common.getEmailSettingsFromFile();
        if (args.length == 0 ) {
            LOGGER.log(Level.INFO, "Application started");

            // Применяем тему FlatLaf для GUI
            // https://github.com/JFormDesigner/FlatLaf
            // https://docs.oracle.com/javase/tutorial/uiswing/lookandfeel/_nimbusDefaults.html
            UIManager.put("Component.arc", 10);
            UIManager.put("ProgressBar.arc", 6);
            UIManager.put("Button.arc", 8);
            UIManager.put("Table.alternateRowColor", new Color(59, 59, 59));
            Common.getColorsSettingsFromFile();
            UIManager.put("Table.foreground", new Color(red, green, blue));
            UIManager.put("TextField.background", Color.GRAY);
            UIManager.put("TextField.foreground", Color.BLACK);
            FlatHiberbeeDarkIJTheme.setup();

            new Gui();
            Common.getSettingsFromFile();
            Gui.newsIntervalCbox.setVisible(Gui.todayOrNotCbx.getState());
            Gui.isOnlyLastNews = Gui.filterNewsChbx.getState();
            sqlite.openSQLiteConnection();
        } else {
            // Console search
            isConsoleSearch.set(true);
            emailToFromConsole = args[0];
            minutesIntervalForConsoleSearch = Integer.parseInt(args[1]);
            sqlite.openSQLiteConnection();
            System.arraycopy(args, 0, keywordsFromConsole, 0, args.length);
            System.out.println(Arrays.toString(keywordsFromConsole)); //***
            Search search = new Search();
            search.searchByConsole();
            sqlite.closeSQLiteConnection();
        }

        // проверка подключения к интернету
        if (!InternetAvailabilityChecker.isInternetAvailable()) {
            Common.console("status: no internet connection");
        }
    }
}
