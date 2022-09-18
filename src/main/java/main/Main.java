package main;

import gui.FrameDragListener;
import gui.Gui;
import lombok.extern.slf4j.Slf4j;
import search.ConsoleSearch;
import utils.Common;
import utils.InternetAvailabilityChecker;

import javax.swing.*;
import java.io.IOException;

@Slf4j
public class Main {

    public static void main(String[] args) throws IOException {
        Common.createFiles();

        if (args.length == 0) {
            new Main().mainSearch();
        } else {
            /*
              Main arguments for console search:
              args1 = email
              args2 = interval in minutes
              args3 = keyword1, keyword2 ... argsN = search keywords
             */
            new ConsoleSearch().searchByConsole(args);
        }
    }

    private void mainSearch() throws IOException {
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
}