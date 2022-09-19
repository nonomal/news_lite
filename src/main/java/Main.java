import lombok.extern.slf4j.Slf4j;
import search.ConsoleSearch;
import utils.Common;

import java.io.IOException;

@Slf4j
public class Main {

    public static void main(String[] args) throws IOException {
        log.info("Application started");
        Common.createFiles();

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