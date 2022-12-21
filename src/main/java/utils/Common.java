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
    public static final String CONFIG_FILE = DIRECTORY_PATH + "config.txt";
    public final AtomicBoolean IS_SENDING = new AtomicBoolean(true);
    public List<String> words;
    public Color guiColor;
    public Color fontColor;
    public Color tablesColor;
    public Color tablesAltColor;

    public void showGui() {
        setGuiTheme();

        Gui gui = new Gui();
        Runnable runnable = () -> {
            FrameDragListener frameDragListener = new FrameDragListener(gui);
            gui.addMouseListener(frameDragListener);
            gui.addMouseMotionListener(frameDragListener);
        };
        SwingUtilities.invokeLater(runnable);

        getSettingsAfterGui();
        getPathToDatabase();
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
            writeToConfigTxt("db_path", pathToDatabase);
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
        JdbcQueries jdbcQueries = new JdbcQueries();
        String[] guiColors = jdbcQueries.getSetting("gui_color").split(",");
        String[] fontColors = jdbcQueries.getSetting("font_color").split(",");
        String[] tablesColors = jdbcQueries.getSetting("tables_color").split(",");
        String[] tablesAltColors = jdbcQueries.getSetting("tables_alt_color").split(",");

        guiColor = new Color(Integer.parseInt(guiColors[0]), Integer.parseInt(guiColors[1]),
                Integer.parseInt(guiColors[2]));
        fontColor = new Color(Integer.parseInt(fontColors[0]), Integer.parseInt(fontColors[1]),
                Integer.parseInt(fontColors[2]));
        tablesColor = new Color(Integer.parseInt(tablesColors[0]), Integer.parseInt(tablesColors[1]),
                Integer.parseInt(tablesColors[2]));
        tablesAltColor = new Color(Integer.parseInt(tablesAltColors[0]), Integer.parseInt(tablesAltColors[1]),
                Integer.parseInt(tablesAltColors[2]));

        // FlatLaf theme
        UIManager.put("Component.arc", 10);
        UIManager.put("ProgressBar.arc", 6);
        UIManager.put("Button.arc", 8);
        UIManager.put("Table.background", tablesColor);
        UIManager.put("Table.foreground", fontColor);
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

    // Считывание конфигураций после запуска интерфейса
    public void getSettingsAfterGui() {
        JdbcQueries jdbcQueries = new JdbcQueries();
        words = jdbcQueries.getRandomWords();
        intervalMapper(jdbcQueries.getSetting("interval"));
        Gui.onlyNewNews.setState(Boolean.parseBoolean(jdbcQueries.getSetting("onlyNewNews")));
        Gui.autoSendMessage.setState(Boolean.parseBoolean(jdbcQueries.getSetting("autoSendMessage")));
        Gui.isOnlyLastNews = Gui.onlyNewNews.getState();
        Gui.consoleTextArea.setBackground(tablesColor);
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

    public void saveColor(String type, Color color) {
        new JdbcQueries().updateSettings(type, color.getRed() + "," + color.getGreen() + "," + color.getBlue());
    }

    public void setDefaultColors() {
        JdbcQueries jdbcQueries = new JdbcQueries();
        jdbcQueries.updateSettings("font_color", "0,0,0");
        jdbcQueries.updateSettings("gui_color", "47,47,47");
        jdbcQueries.updateSettings("tables_color", "255,255,255");
        jdbcQueries.updateSettings("tables_alt_color", "237,237,237");
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
    public int getXForEmailIcon(int length) {
        int x;
        if (length == 26) {
            x = 1003;
        } else if (length == 27) {
            x = 1009;
        } else if (length == 28) {
            x = 1015;
        } else if (length == 29) {
            x = 1021;
        } else if (length == 30) {
            x = 1027;
        } else if (length == 31) {
            x = 1033;
        } else if (length == 32) {
            x = 1039;
        } else if (length == 33) {
            x = 1045;
        } else if (length == 34) {
            x = 1051;
        } else if (length == 35) {
            x = 1057;
        } else if (length == 36) {
            x = 1063;
        } else if (length == 37) {
            x = 1069;
        } else {
            x = 1075;
        }
        return x;
    }

    public int getXForEmailIconKeywords(int count) {
        int x;
        if (count < 10) {
            x = 911;
        } else if (count < 100) {
            x = 911 + 6;
        } else if (count < 1000) {
            x = 911 + (6 * 2);
        } else {
            x = 911 + (6 * 3);
        }
        return x;
    }

}
