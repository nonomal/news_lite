import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.logging.*;

public class Main {
    static SimpleDateFormat date_format = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    static String directoryPath = "C:\\Users\\Public\\Documents\\News\\";
    static String settingsPath = directoryPath + "config.txt";
    static String logPath = directoryPath + "log.txt";
    static String sourcesPath = directoryPath + "sources.txt";
    public static final Logger LOGGER = Logger.getLogger("");

    // создание директории, файла настроек, лога
    static {
        File directory = new File(directoryPath);
        File fav_file = new File(settingsPath);
        File log_file = new File(logPath);

        try {
            if (!directory.exists()) directory.mkdirs();
            if (!fav_file.exists()) fav_file.createNewFile();
            if (!log_file.exists()) log_file.createNewFile();
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

        // создание файлов SQLite
        File sqliteIsExists = new File(directoryPath + "sqlite3.exe");
        if (!sqliteIsExists.exists()) {
            Common.copyFiles(Main.class.getResource("sqlite3.exe"), directoryPath + "sqlite3.exe");
            Common.copyFiles(Main.class.getResource("sqlite3.dll"), directoryPath + "sqlite3.dll");
            Common.copyFiles(Main.class.getResource("sqlite3.def"), directoryPath + "sqlite3.def");
        }

        // создание первоначального файла источников новостей
        File sourcesIsExists = new File(sourcesPath);
        if (!sourcesIsExists.exists()) {
            Common.copyFiles(Main.class.getResource("sources.txt"), sourcesPath);
            //SQLite.deleteFromSources();
            SQLite.initialInsertSources();
            SQLite.selectSources();
        }
    }

    public static void main(String[] args) {
        LOGGER.log(Level.INFO, "Application started");
        new Gui();
        Common.getSettingsFromFile();
        SQLite.open();
        Gui.newsIntervalCbox.setEnabled(Gui.todayOrNotChbx.getState());
    }
}