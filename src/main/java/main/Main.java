package main;

import com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme;
import database.SQLite;
import search.ConsoleSearch;
import utils.Common;
import utils.Reminder;

public class Main {

    public static void main(String[] args) {
        Common.createFiles();
        new SQLite().openConnection();

        if (args.length == 0) {
            FlatHiberbeeDarkIJTheme.setup();
            new Login().login();
            Common.showGui();
            new Reminder().remind();
        } else {
            new ConsoleSearch().searchByConsole(args);
        }
    }

}