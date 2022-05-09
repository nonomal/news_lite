package main;

import com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme;
import database.SQLite;
import gui.Gui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import search.Search;
import utils.Common;
import utils.InternetAvailabilityChecker;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    public static String directoryPath = System.getProperty("user.home") + File.separator + "News" + File.separator;
    public static String settingsPath = directoryPath + "config.txt";
    public static Calendar minPubDate = Calendar.getInstance();
    public static int [] guiFont = new int[3];
    public static int [] guiBackground = new int[3];
    // Console search
    public static AtomicBoolean isConsoleSearch = new AtomicBoolean(false);
    public static String emailToFromConsole;
    public static int minutesIntervalForConsoleSearch;
    public static String[] keywordsFromConsole;

    // создание директорий и файлов
    static {
        // Минимальная дата публикации новости 01.01.2021
        minPubDate.set(Calendar.YEAR, 2022);
        minPubDate.set(Calendar.DAY_OF_YEAR, 1);

        File mainDirectory = new File(directoryPath);
        if (!mainDirectory.exists()) mainDirectory.mkdirs();

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
        if (args.length == 0) {
            log.info("Application started");

            // Применяем тему FlatLaf для GUI
            // https://github.com/JFormDesigner/FlatLaf
            // https://docs.oracle.com/javase/tutorial/uiswing/lookandfeel/_nimbusDefaults.html
            UIManager.put("Component.arc", 10);
            UIManager.put("ProgressBar.arc", 6);
            UIManager.put("Button.arc", 8);
            Common.getColorsSettingsFromFile();
            UIManager.put("Table.background", new Color(guiBackground[0], guiBackground[1], guiBackground[2]));
            UIManager.put("Table.alternateRowColor", new Color(59, 59, 59));
            UIManager.put("Table.foreground", new Color(guiFont[0], guiFont[1], guiFont[2]));
            UIManager.put("TextField.background", Color.GRAY);
            UIManager.put("TextField.foreground", Color.BLACK);
            FlatHiberbeeDarkIJTheme.setup();

            new Gui();
            // загрузка конфигураций из файла config.txt
            Common.getSettingsFromFile();
            System.out.println(Arrays.toString(guiFont) + "\n" + Arrays.toString(guiBackground));
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
