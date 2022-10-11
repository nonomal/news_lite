package utils;

import com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme;
import database.JdbcQueries;
import gui.FrameDragListener;
import gui.Gui;
import lombok.experimental.UtilityClass;
import search.ConsoleSearch;
import search.Search;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@UtilityClass
public class Common {
    public static final String DIRECTORY_PATH = System.getProperty("user.home") +
            File.separator + "News" + File.separator;
    private final int[] GUI_FONT = new int[3];
    private final int[] GUI_BACKGROUND = new int[3];
    public static final String CONFIG_FILE = DIRECTORY_PATH + "config.txt";
    public final AtomicBoolean IS_SENDING = new AtomicBoolean(true);
    public String SCRIPT_URL = null;
    public float TRANSPARENCY;
    public List<String> words;

    public void showGui() {
        getSettingsBeforeGui();
        setGuiTheme();

        Gui gui = new Gui();
        Runnable runnable = () -> {
            FrameDragListener frameDragListener = new FrameDragListener(gui);
            gui.addMouseListener(frameDragListener);
            gui.addMouseMotionListener(frameDragListener);
        };
        SwingUtilities.invokeLater(runnable);

        getSettingsAfterGui();

        // check internet
        try {
            if (!InternetAvailabilityChecker.isInternetAvailable()) {
                console("no internet connection");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // создание файлов и директорий
    public static void createFiles() {
        // main directory create
        File mainDirectory = new File(DIRECTORY_PATH);
        if (!mainDirectory.exists()) mainDirectory.mkdirs();

        // создание файлов программы
        File sqliteExeIsExists = new File(DIRECTORY_PATH + "sqlite3.exe");
        if (!sqliteExeIsExists.exists()) {
            copyFiles(Common.class.getResource("/sqlite3.exe"), DIRECTORY_PATH + "sqlite3.exe");
        }
        File sqliteDllIsExists = new File(DIRECTORY_PATH + "sqlite3.dll");
        if (!sqliteDllIsExists.exists()) {
            copyFiles(Common.class.getResource("/sqlite3.dll"), DIRECTORY_PATH + "sqlite3.dll");
        }
        File sqliteDefIsExists = new File(DIRECTORY_PATH + "sqlite3.def");
        if (!sqliteDefIsExists.exists()) {
            copyFiles(Common.class.getResource("/sqlite3.def"), DIRECTORY_PATH + "sqlite3.def");
        }

        String pathToDatabase = DIRECTORY_PATH + "news.db";
        File dbIsExists = new File(pathToDatabase);
        if (!dbIsExists.exists()) {
            copyFiles(Common.class.getResource("/news.db"), pathToDatabase);
            writeToConfig(pathToDatabase, "db");
        }
        File configIsExists = new File(DIRECTORY_PATH + "config.txt");
        if (!configIsExists.exists()) {
            copyFiles(Common.class.getResource("/config.txt"), CONFIG_FILE);
        }
    }

    // установка темы интерфейса
    public static void setGuiTheme() {
        // FlatLaf theme
        UIManager.put("Component.arc", 10);
        UIManager.put("ProgressBar.arc", 6);
        UIManager.put("Button.arc", 8);
        UIManager.put("Table.background", new Color(GUI_BACKGROUND[0], GUI_BACKGROUND[1], GUI_BACKGROUND[2]));
        UIManager.put("Table.alternateRowColor", new Color(59, 59, 59));
        UIManager.put("Table.foreground", new Color(GUI_FONT[0], GUI_FONT[1], GUI_FONT[2]));
        UIManager.put("TextField.background", Color.GRAY);
        UIManager.put("TextField.foreground", Color.BLACK);
        FlatHiberbeeDarkIJTheme.setup();
    }

    // Запись конфигураций приложения
    public void writeToConfig(String value, String type) {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(CONFIG_FILE, true), StandardCharsets.UTF_8)) {
            switch (type) {
                case "db": {
                    String text = "db_path=" + value + "\n";
                    writer.write(text);
                    break;
                }
                case "keyword": {
                    String text = "keyword=" + value + "\n";
                    writer.write(text);
                    break;
                }
                case "fontColorRed": {
                    String text = "fontColorRed=" + value + "\n";
                    writer.write(text);
                    break;
                }
                case "fontColorGreen": {
                    String text = "fontColorGreen=" + value + "\n";
                    writer.write(text);
                    break;
                }
                case "fontColorBlue": {
                    String text = "fontColorBlue=" + value + "\n";
                    writer.write(text);
                    break;
                }
                case "backgroundColorRed": {
                    String text = "backgroundColorRed=" + value + "\n";
                    writer.write(text);
                    break;
                }
                case "backgroundColorGreen": {
                    String text = "backgroundColorGreen=" + value + "\n";
                    writer.write(text);
                    break;
                }
                case "backgroundColorBlue": {
                    String text = "backgroundColorBlue=" + value + "\n";
                    writer.write(text);
                    break;
                }
                case "email": {
                    String text = "email=" + value;
                    writer.write(text.trim() + "\n");
                    break;
                }
                case "interval": {
                    String text = "interval=" + value.replace(" hour", "h")
                            .replace("s", "")
                            .replace(" min", "m");
                    writer.write(text + "\n");
                    break;
                }
                case "checkbox": {
                    String text = null;
                    switch (value) {
                        case "filterNewsChbx":
                            text = "checkbox:" + value + "=" + Gui.onlyNewNews.getState() + "\n";
                            break;
                        case "autoSendChbx":
                            text = "checkbox:" + value + "=" + Gui.autoSendMessage.getState() + "\n";
                            break;
                    }
                    if (text != null) writer.write(text);
                    break;
                }
            }
            writer.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Получить путь к базе данных (в config.txt можно поменять путь к БД)
    public String getPathToDatabase() {
        File file = null;
        try {
            for (String s : Files.readAllLines(Paths.get(CONFIG_FILE))) {
                if (s.startsWith("db_path="))
                    file = new File(s.replace("db_path=", ""));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert file != null;
        return file.getPath();
    }

    // Считывание конфигураций до запуска интерфейса
    public void getSettingsBeforeGui() {
        try {
            for (String s : Files.readAllLines(Paths.get(CONFIG_FILE))) {
                if (s.startsWith("transparency="))
                    TRANSPARENCY = Float.parseFloat(s.replace("transparency=", ""));
                else if (s.startsWith("fontColorRed="))
                    GUI_FONT[0] = Integer.parseInt(s.replace("fontColorRed=", ""));
                else if (s.startsWith("fontColorGreen="))
                    GUI_FONT[1] = Integer.parseInt(s.replace("fontColorGreen=", ""));
                else if (s.startsWith("fontColorBlue="))
                    GUI_FONT[2] = Integer.parseInt(s.replace("fontColorBlue=", ""));
                else if (s.startsWith("backgroundColorRed="))
                    GUI_BACKGROUND[0] = Integer.parseInt(s.replace("backgroundColorRed=", ""));
                else if (s.startsWith("backgroundColorGreen="))
                    GUI_BACKGROUND[1] = Integer.parseInt(s.replace("backgroundColorGreen=", ""));
                else if (s.startsWith("backgroundColorBlue="))
                    GUI_BACKGROUND[2] = Integer.parseInt(s.replace("backgroundColorBlue=", ""));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Считывание конфигураций после запуска интерфейса
    public void getSettingsAfterGui() {
        try {
            words = new JdbcQueries().getRandomWords();

            for (String s : Files.readAllLines(Paths.get(CONFIG_FILE))) {
                // Интервал поиска interval=1m
                if (s.startsWith("interval=")) {
                    String interval = s.replace("interval=", "");
                    switch (interval) {
                        case "1h":
                            Gui.newsInterval.setSelectedItem(interval.replace("h", "") + " hour");
                            break;
                        case "1m":
                        case "5m":
                        case "15m":
                        case "30m":
                        case "45m":
                            Gui.newsInterval.setSelectedItem(interval.replace("m", "") + " min");
                            break;
                        case "all":
                            Gui.newsInterval.setSelectedItem("all");
                            break;
                        default:
                            Gui.newsInterval.setSelectedItem(interval.replace("h", "") + " hours");
                            break;
                    }
                } else if (s.startsWith("email=")) {
                    Gui.sendEmailTo.setText(s.replace("email=", ""));
                } else if (s.startsWith("checkbox:filterNewsChbx=")) {
                    Gui.onlyNewNews.setState(Boolean.parseBoolean(s.replace("checkbox:filterNewsChbx=", "")));
                } else if (s.startsWith("checkbox:autoSendChbx=")) {
                    Gui.autoSendMessage.setState(Boolean.parseBoolean(s.replace("checkbox:autoSendChbx=", "")));
                } else if (s.startsWith("translate-url=")) {
                    SCRIPT_URL = s.replace("translate-url=", "");
                }
            }
            Gui.isOnlyLastNews = Gui.onlyNewNews.getState();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // сохранение состояния окна в config.txt
    public void saveState() {
        // delete old values
        try {
            Common.delSettings("interval");
            Common.delSettings("checkbox");
            Common.delSettings("email");
        } catch (IOException io) {
            io.printStackTrace();
        }
        // write new values
        Common.writeToConfig(Gui.sendEmailTo.getText(), "email");
        Common.writeToConfig(String.valueOf(Gui.newsInterval.getSelectedItem()), "interval");
        Common.writeToConfig("todayOrNotChbx", "checkbox");
        Common.writeToConfig("checkTitle", "checkbox");
        Common.writeToConfig("checkLink", "checkbox");
        Common.writeToConfig("filterNewsChbx", "checkbox");
        Common.writeToConfig("autoSendChbx", "checkbox");
    }

    // Удаление ключевого слова из combo box
    public void delSettings(String s) throws IOException {
        Path input = Paths.get(CONFIG_FILE);
        Path temp = Files.createTempFile("temp", ".txt");
        try (Stream<String> lines = Files.lines(input)) {
            try (BufferedWriter writer = Files.newBufferedWriter(temp)) {
                lines
                        .filter(line -> {
                            assert s != null;
                            return !line.startsWith(s);
                        })
                        .forEach(line -> {
                            try {
                                writer.write(line);
                                writer.newLine();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        }
        Files.move(temp, input, StandardCopyOption.REPLACE_EXISTING);
    }

    //Console
    public void console(String text) {
        Gui.consoleTextArea.setText(Gui.consoleTextArea.getText() +
                "- - - - - - - - - - - - - - - - - - - - - - - - - - -\n" + text + "\n");
    }

    // Шкала прогресса
    public void fillProgressLine() {
        int counter = 0;
        while (!Search.isSearchFinished.get() || !IS_SENDING.get()) {
            if (!IS_SENDING.get()) Gui.progressBar.setForeground(new Color(255, 115, 0));
            else Gui.progressBar.setForeground(new Color(10, 255, 41));
            if (counter == 99) {
                counter = 0;
            }
            Gui.progressBar.setValue(counter);
            try {
                Thread.sleep(7);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            counter++;
        }
    }

    // Интервал поиска/таймера в секундах
    int getInterval() {
        int minutes;
        if (Objects.requireNonNull(Gui.newsInterval.getSelectedItem()).toString().contains(" min")) {
            minutes = Integer.parseInt(Objects.requireNonNull(Gui.newsInterval
                            .getSelectedItem())
                    .toString()
                    .replace(" min", ""));
        } else if (Objects.requireNonNull(Gui.newsInterval.getSelectedItem()).toString().contains("all")) {
            minutes = 240000;
        } else {
            minutes = Integer.parseInt(Objects.requireNonNull(Gui.newsInterval
                            .getSelectedItem())
                    .toString()
                    .replace(" hour", "")
                    .replace("s", "")) * 60;
        }
        return minutes;
    }

    // Сравнение дат для отображения новостей по интервалу (Gui.newsInterval)
    public int compareDatesOnly(Date now, Date in) {
        int minutes;
        if (ConsoleSearch.IS_CONSOLE_SEARCH.get()) minutes = ConsoleSearch.minutesIntervalConsole;
        else minutes = Common.getInterval();

        Calendar minus = Calendar.getInstance();
        minus.setTime(new Date());
        minus.add(Calendar.MINUTE, -minutes);
        Calendar now_cal = Calendar.getInstance();
        now_cal.setTime(now);

        if (in.after(minus.getTime()) && in.before(now_cal.getTime())) {
            return 1;
        } else
            return 0;
    }

    // Копирование файлов из jar
    public void copyFiles(URL p_file, String copy_to) {
        File copied = new File(copy_to);
        try (InputStream in = p_file.openStream();
             OutputStream out = new BufferedOutputStream(Files.newOutputStream(copied.toPath()))) {
            byte[] buffer = new byte[1024];
            int lengthRead;
            while ((lengthRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, lengthRead);
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Уведомление в трее
    public void trayMessage(String pMessage) {
        if (SystemTray.isSupported()) {
            PopupMenu popup = new PopupMenu();
            MenuItem exitItem = new MenuItem("Close");

            SystemTray systemTray = SystemTray.getSystemTray();
            Image image = Toolkit.getDefaultToolkit().createImage(Common.class.getResource("/icons/message.png"));
            TrayIcon trayIcon = new TrayIcon(image, pMessage, popup);
            trayIcon.setImageAutoSize(true);
            try {
                systemTray.add(trayIcon);
            } catch (AWTException e) {
                e.printStackTrace();
            }
            trayIcon.displayMessage("Avandy News", pMessage, TrayIcon.MessageType.INFO);
            //systemTray.remove(trayIcon);
            exitItem.addActionListener(e -> systemTray.remove(trayIcon));
            popup.add(exitItem);
        }
    }

    public void saveFontColor(Color color) throws IOException {
        delSettings("fontColorRed");
        delSettings("fontColorGreen");
        delSettings("fontColorBlue");
        writeToConfig(String.valueOf(color.getRed()), "fontColorRed");
        writeToConfig(String.valueOf(color.getGreen()), "fontColorGreen");
        writeToConfig(String.valueOf(color.getBlue()), "fontColorBlue");
    }

    public void saveBackgroundColor(Color color) throws IOException {
        delSettings("backgroundColorRed");
        delSettings("backgroundColorGreen");
        delSettings("backgroundColorBlue");
        writeToConfig(String.valueOf(color.getRed()), "backgroundColorRed");
        writeToConfig(String.valueOf(color.getGreen()), "backgroundColorGreen");
        writeToConfig(String.valueOf(color.getBlue()), "backgroundColorBlue");
    }

    // преобразование строки в строку с хэш-кодом
    public String getHash(String base) {
        try{
            MessageDigest digest = MessageDigest.getInstance("MD5"); // MD2, MD5, SHA-1, SHA-256, SHA-384, SHA-512
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch(Exception ex){
            throw new RuntimeException(ex);
        }
    }

}
