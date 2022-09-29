import database.SQLite;
import lombok.extern.slf4j.Slf4j;
import search.ConsoleSearch;
import utils.Common;

@Slf4j
public class Main {

    public static void main(String[] args) {
        log.info("Application started");
        Common.createFiles();
        new SQLite().openConnection();

        if (args.length == 0) {
            Common.showGui();
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