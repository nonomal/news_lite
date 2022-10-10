import database.SQLite;
import search.ConsoleSearch;
import utils.Common;
import utils.Reminder;

public class Main {

    public static void main(String[] args) {
        Common.createFiles();
        new SQLite().openConnection();

        if (args.length == 0) {
            Common.showGui();
            new Reminder().remind();
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
}