package main;

import database.SQLite;
import gui.FrameDragListener;
import gui.Gui;
import lombok.extern.slf4j.Slf4j;
import search.ConsoleSearch;
import utils.Common;
import utils.InternetAvailabilityChecker;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class Main {
    // Console search
    public static final AtomicBoolean IS_CONSOLE_SEARCH = new AtomicBoolean(false);
    public static String sendEmailToFromConsole;
    public static int minutesIntervalConsole;

    public static void main(String[] args) throws IOException {
        Common.createFiles();
        if (args.length == 0) {
            mainSearch();
        } else {
            consoleSearch(args);
        }
    }

    private static void mainSearch() throws IOException {
        log.info("Application started");
        Common.getSettingsBeforeGui();
        Common.setGuiTheme();

        Gui gui = new Gui();
        Runnable runnable = () -> {
            FrameDragListener frameDragListener = new FrameDragListener(gui);
            gui.addMouseListener(frameDragListener);
            gui.addMouseMotionListener(frameDragListener);
        };
        SwingUtilities.invokeLater(runnable);

        Common.getSettingsAfterGui();

        // check internet
        if (!InternetAvailabilityChecker.isInternetAvailable()) {
            Common.console("status: no internet connection");
        }
    }

    /**
     * Main arguments for console search:
     * args1 = email
     * args2 = interval in minutes
     * args3 = keyword1, keyword2 ... argsN = search keywords
     */
    private static void consoleSearch(String[] args) {
        String[] keywordsFromConsole = new String[args.length];
        IS_CONSOLE_SEARCH.set(true);
        sendEmailToFromConsole = args[0];
        minutesIntervalConsole = Integer.parseInt(args[1]);
        SQLite sqlite = new SQLite();
        sqlite.openConnection();
        System.arraycopy(args, 0, keywordsFromConsole, 0, args.length);
        System.out.println(Arrays.toString(keywordsFromConsole));
        new ConsoleSearch().searchByConsole(keywordsFromConsole);
        sqlite.closeConnection();
    }
}
