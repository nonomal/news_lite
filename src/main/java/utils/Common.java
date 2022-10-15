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
import java.util.*;
import java.util.List;
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
    public float transparency;
    public String emailFrom;
    public String emailFromPwd;
    public String emailTo;
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
    }

    // создание файлов и директорий
    public static void createFiles() {
        // main directory create
        File mainDirectory = new File(DIRECTORY_PATH);
        if (!mainDirectory.exists()) mainDirectory.mkdirs();

        File configIsExists = new File(DIRECTORY_PATH + "config.txt");
        if (!configIsExists.exists()) {
            copyFiles(Common.class.getResource("/config.txt"), CONFIG_FILE);
        }

        String pathToDatabase = DIRECTORY_PATH + "news.db";
        File dbIsExists = new File(pathToDatabase);
        if (!dbIsExists.exists()) {
            copyFiles(Common.class.getResource("/news.db"), pathToDatabase);
            writeToConfigTxt("db", pathToDatabase);
        }

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
    public void writeToConfigTxt(String key, String value) {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(CONFIG_FILE, true),
                StandardCharsets.UTF_8)) {
            if ("db_path".equals(key)) {
                String text = "db_path=" + value + "\n";
                writer.write(text);
            }
            writer.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Считывание конфигураций до запуска интерфейса
    public void getSettingsBeforeGui() {
        for (Map.Entry<String, String> s : new JdbcQueries().getSettings().entrySet()) {
            switch (s.getKey()) {
                case "transparency":
                    transparency = Float.parseFloat(s.getValue());
                    break;
                case "fontColorRed":
                    GUI_FONT[0] = Integer.parseInt(s.getValue());
                    break;
                case "fontColorGreen":
                    GUI_FONT[1] = Integer.parseInt(s.getValue());
                    break;
                case "fontColorBlue":
                    GUI_FONT[2] = Integer.parseInt(s.getValue());
                    break;
                case "backgroundColorRed":
                    GUI_BACKGROUND[0] = Integer.parseInt(s.getValue());
                    break;
                case "backgroundColorGreen":
                    GUI_BACKGROUND[1] = Integer.parseInt(s.getValue());
                    break;
                case "backgroundColorBlue":
                    GUI_BACKGROUND[2] = Integer.parseInt(s.getValue());
                    break;
            }
        }
    }

    // Считывание конфигураций после запуска интерфейса
    public void getSettingsAfterGui() {
        words = new JdbcQueries().getRandomWords();

        for (Map.Entry<String, String> s : new JdbcQueries().getSettings().entrySet()) {
            switch (s.getKey()) {
                case "interval":
                    intervalMapper(s.getValue());
                    break;
                case "onlyNewNews":
                    Gui.onlyNewNews.setState(Boolean.parseBoolean(s.getValue()));
                    break;
                case "autoSendMessage":
                    Gui.autoSendMessage.setState(Boolean.parseBoolean(s.getValue()));
                    break;
                case "translate-url":
                    SCRIPT_URL = s.getValue();
                    break;
            }
        }
        getSettings();
        Gui.isOnlyLastNews = Gui.onlyNewNews.getState();
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

    // Считывание конфигураций после запуска интерфейса
    public void getSettings() {
        for (Map.Entry<String, String> s : new JdbcQueries().getSettings().entrySet()) {
            switch (s.getKey()) {
                case "email_from":
                    emailFrom = s.getValue();
                    break;
                case "from_pwd":
                    emailFromPwd = s.getValue();
                    break;
                case "email_to":
                    emailTo = s.getValue();
                    break;
                case "transparency":
                    transparency = Float.parseFloat(s.getValue());
                    break;
            }
            getPathToDatabase();
        }
    }

    // сохранение состояния окна в config.txt
    public void saveState() {
        JdbcQueries jdbcQueries = new JdbcQueries();
        String interval = Gui.newsInterval.getSelectedItem().toString()
                .replace(" hour", "h")
                .replace("s", "")
                .replace(" min", "m");

        jdbcQueries.updateSettings("interval", interval);
        jdbcQueries.updateSettings("onlyNewNews", String.valueOf(Gui.onlyNewNews.getState()));
        jdbcQueries.updateSettings("autoSendMessage", String.valueOf(Gui.autoSendMessage.getState()));
    }

    // Удаление ключевого слова из combo box
    public void delSettings(String s) {
        try {
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
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    // цвет шрифта
    public void saveFontColor(Color color) throws IOException {
        JdbcQueries jdbcQueries = new JdbcQueries();
        jdbcQueries.updateSettings("fontColorRed", String.valueOf(color.getRed()));
        jdbcQueries.updateSettings("fontColorGreen", String.valueOf(color.getGreen()));
        jdbcQueries.updateSettings("fontColorBlue", String.valueOf(color.getBlue()));
    }

    // цвет фона таблицы
    public void saveBackgroundColor(Color color) throws IOException {
        JdbcQueries jdbcQueries = new JdbcQueries();
        jdbcQueries.updateSettings("backgroundColorRed", String.valueOf(color.getRed()));
        jdbcQueries.updateSettings("backgroundColorGreen", String.valueOf(color.getGreen()));
        jdbcQueries.updateSettings("backgroundColorBlue", String.valueOf(color.getBlue()));
    }

    // преобразование строки в строку с хэш-кодом
    public String getHash(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5"); // MD2, MD5, SHA-1, SHA-256, SHA-384, SHA-512
            byte[] hash = digest.digest(base.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    // преобразование интервала
    private static void intervalMapper(String interval) {
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
    }

    // отсеивание описаний содержащих недопустимые символы
    public boolean isHref(String newsDescribe) {
        return newsDescribe.contains("<img")
                || newsDescribe.contains("href")
                || newsDescribe.contains("<div")
                || newsDescribe.contains("&#34")
                || newsDescribe.contains("<p lang")
                || newsDescribe.contains("&quot")
                || newsDescribe.contains("<span")
                || newsDescribe.contains("<ol")
                || newsDescribe.equals("");
    }

    // для красоты UI
    public int getXForEmailIcon(int length, int startValue, int initX, int offset) {
        int x;
        if (length == startValue) {
            x = initX;
        } else if (length == startValue + 1) {
            x = initX + offset;
        } else if (length == startValue + 2) {
            x = initX + (offset * 2);
        } else if (length == startValue + 3) {
            x = initX + (offset * 3);
        } else if (length == startValue + 4) {
            x = initX + (offset * 4);
        } else if (length == startValue + 5) {
            x = initX + (offset * 5);
        } else if (length == startValue + 6) {
            x = initX + (offset * 6);
        } else if (length == startValue + 7) {
            x = initX + (offset * 7);
        } else if (length == startValue + 8) {
            x = initX + (offset * 8);
        } else if (length == startValue + 9) {
            x = initX + (offset * 9);
        } else if (length == startValue + 10) {
            x = initX + (offset * 10);
        } else if (length == startValue + 11) {
            x = initX + (offset * 11);
        } else if (length == startValue + 12) {
            x = initX + (offset * 12);
        } else {
            x = initX + (offset * 13);
        }
        return x;
    }

}
