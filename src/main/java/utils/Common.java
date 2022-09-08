package utils;

import database.DatabaseQueries2;
import email.EmailSender;
import gui.Dialogs;
import gui.Gui;
import lombok.experimental.UtilityClass;
import main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import search.Search;

import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@UtilityClass
public class Common {
    public final String SETTINGS_PATH = Main.DIRECTORY_PATH + "config.txt";
    private final Logger log = LoggerFactory.getLogger(Common.class);
    public final AtomicBoolean IS_SENDING = new AtomicBoolean(true);
    public final ArrayList<String> KEYWORDS_LIST = new ArrayList<>();
    public int SMI_ID = 0;
    public final ArrayList<String> SMI_LINK = new ArrayList<>();
    public final ArrayList<String> SMI_SOURCE = new ArrayList<>();
    public final ArrayList<Boolean> SMI_IS_ACTIVE = new ArrayList<>();
    public final ArrayList<String> EXCLUDED_WORDS = new ArrayList<>();
    public String SCRIPT_URL = null;
    public float OPACITY;

    // Запись конфигураций приложения
    public void writeToConfig(String p_word, String p_type) {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(SETTINGS_PATH, true), StandardCharsets.UTF_8)) {
            switch (p_type) {
                case "keyword": {
                    String text = "keyword=" + p_word + "\n";
                    writer.write(text);
                    writer.flush();
                    writer.close();
                    break;
                }
                case "fontColorRed": {
                    String text = "fontColorRed=" + p_word + "\n";
                    writer.write(text);
                    writer.flush();
                    writer.close();
                    break;
                }
                case "fontColorGreen": {
                    String text = "fontColorGreen=" + p_word + "\n";
                    writer.write(text);
                    writer.flush();
                    writer.close();
                    break;
                }
                case "fontColorBlue": {
                    String text = "fontColorBlue=" + p_word + "\n";
                    writer.write(text);
                    writer.flush();
                    writer.close();
                    break;
                }
                case "backgroundColorRed": {
                    String text = "backgroundColorRed=" + p_word + "\n";
                    writer.write(text);
                    writer.flush();
                    writer.close();
                    break;
                }
                case "backgroundColorGreen": {
                    String text = "backgroundColorGreen=" + p_word + "\n";
                    writer.write(text);
                    writer.flush();
                    writer.close();
                    break;
                }
                case "backgroundColorBlue": {
                    String text = "backgroundColorBlue=" + p_word + "\n";
                    writer.write(text);
                    writer.flush();
                    writer.close();
                    break;
                }
                case "email": {
                    String text = "email=" + p_word;
                    writer.write(text.trim() + "\n");
                    writer.flush();
                    writer.close();
                    break;
                }
                case "interval": {
                    String text = "interval=" + p_word.replace(" hour", "h")
                            .replace("s", "")
                            .replace(" min", "m");
                    writer.write(text + "\n");
                    writer.flush();
                    writer.close();
                    break;
                }
                case "checkbox": {
                    String text = null;
                    switch (p_word) {
                        case "filterNewsChbx":
                            text = "checkbox:" + p_word + "=" + Gui.onlyNewNews.getState() + "\n";
                            break;
                        case "autoSendChbx":
                            text = "checkbox:" + p_word + "=" + Gui.autoSendMessage.getState() + "\n";
                            break;
                    }
                    if (text != null) writer.write(text);
                    writer.flush();
                    writer.close();
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Считывание конфигураций
    public void getSettingsFromFile() {
        int linesAmount = Common.countLines();
        String[][] lines = new String[linesAmount][];

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(Paths.get(SETTINGS_PATH)), StandardCharsets.UTF_8))) {
            String line;
            int i = 0;

            while ((line = reader.readLine()) != null && i < linesAmount) {
                lines[i++] = line.split("=");
            }

            for (String[] f : lines) {
                switch (f[0]) {
                    case "interval":
                        switch (f[1]) {
                            case "1h":
                                Gui.newsInterval.setSelectedItem(f[1].replace("h", "") + " hour");
                                break;
                            case "1m":
                            case "5m":
                            case "15m":
                            case "30m":
                            case "45m":
                                Gui.newsInterval.setSelectedItem(f[1].replace("m", "") + " min");
                                break;
                            case "all":
                                Gui.newsInterval.setSelectedItem("all");
                                break;
                            default:
                                Gui.newsInterval.setSelectedItem(f[1].replace("h", "") + " hours");
                                break;
                        }
                        break;
                    case "email":
                        Gui.sendEmailTo.setText(f[1].trim());
                        break;
                    case "keyword":
                        Gui.keywords.addItem(f[1]);
                        KEYWORDS_LIST.add(f[1]);
                        break;
                    case "checkbox:filterNewsChbx":
                        Gui.onlyNewNews.setState(Boolean.parseBoolean(f[1]));
                        break;
                    case "checkbox:autoSendChbx":
                        Gui.autoSendMessage.setState(Boolean.parseBoolean(f[1]));
                        break;
                    case "translate-url":
                        SCRIPT_URL = f[1];
                        break;
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Считывание ключевых слов при добавлении/удалении в комбобоксе
    public List<String> getKeywordsFromFile() {
        List<String> keywords = new ArrayList<>();
        try {
            for (String s : Files.readAllLines(Paths.get(SETTINGS_PATH))) {
                if (s.startsWith("keyword="))
                    keywords.add(s.replace("keyword=", ""));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return keywords;
    }

    // Считывание слов исключений для поиска по одному слову
    public List<String> getExcludeWordsFromFile() {
        List<String> excludeWords = new ArrayList<>();
        try {
            for (String s : Files.readAllLines(Paths.get(SETTINGS_PATH))) {
                if (s.startsWith("exclude="))
                    excludeWords.add(s.replace("exclude=", ""));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return excludeWords;
    }

    // Считывание настройки прозрачности окна
    public void getOpacity() {
        try {
            for (String s : Files.readAllLines(Paths.get(SETTINGS_PATH))) {
                if (s.startsWith("opacity="))
                    OPACITY = Float.parseFloat(s.replace("opacity=", ""));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Считывание сохранённого цвета шрифта из файла
    public void getColorsSettingsFromFile() {
        int linesAmount = Common.countLines();
        String[][] lines = new String[linesAmount][];

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(Paths.get(SETTINGS_PATH)), StandardCharsets.UTF_8))) {
            String line;
            int i = 0;

            while ((line = reader.readLine()) != null && i < linesAmount) {
                lines[i++] = line.split("=");
            }

            for (String[] f : lines) {
                switch (f[0]) {
                    case "fontColorRed":
                        Main.GUI_FONT[0] = Integer.parseInt(f[1].trim());
                        break;
                    case "fontColorGreen":
                        Main.GUI_FONT[1] = Integer.parseInt(f[1].trim());
                        break;
                    case "fontColorBlue":
                        Main.GUI_FONT[2] = Integer.parseInt(f[1].trim());
                        break;
                    case "backgroundColorRed":
                        Main.GUI_BACKGROUND[0] = Integer.parseInt(f[1].trim());
                        break;
                    case "backgroundColorGreen":
                        Main.GUI_BACKGROUND[1] = Integer.parseInt(f[1].trim());
                        break;
                    case "backgroundColorBlue":
                        Main.GUI_BACKGROUND[2] = Integer.parseInt(f[1].trim());
                        break;
                }
            }
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
            log.warn(io.getMessage());
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

    // определение SMTP исходящей почты
    public String getSmtp() {
        String smtp = "";
        String serviceName = EmailSender.from.substring(EmailSender.from.indexOf(64) + 1);
        switch (serviceName) {
            case "mail.ru":
            case "internet.ru":
            case "inbox.ru":
            case "list.ru":
            case "bk.ru":
                smtp = "smtp.mail.ru";
                break;
            case "gmail.com":
                smtp = "smtp.gmail.com";
                break;
            case "yahoo.com":
                smtp = "smtp.mail.yahoo.com";
                break;
            case "yandex.ru":
                smtp = "smtp.yandex.ru";
                break;
            case "rambler.ru":
                smtp = "smtp.rambler.ru";
                break;
            default:
                Common.console("info: mail is sent only from Mail.ru, Gmail, Yandex, Yahoo, Rambler");
        }
        return smtp;
    }

    // Считывание настроек почты из файла
    public void getEmailSettingsFromFile() {
        int linesAmount = Common.countLines();
        String[][] lines = new String[linesAmount][];
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(Paths.get(SETTINGS_PATH)), StandardCharsets.UTF_8))) {
            String line;
            int i = 0;

            while ((line = reader.readLine()) != null && i < linesAmount) {
                lines[i++] = line.split("=");
            }

            for (String[] f : lines) {
                for (int j = 0; j < 1; j++) {
                    switch (f[0]) {
                        case "from_pwd":
                            EmailSender.from_pwd = f[1].trim();
                            break;
                        case "from_adr":
                            EmailSender.from = f[1].trim();
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Подсчет количества строк в файле
    int countLines() {
        try {
            LineNumberReader reader = new LineNumberReader(new FileReader(SETTINGS_PATH));
            int cnt;
            while (true) {
                if (reader.readLine() == null) break;
            }
            cnt = reader.getLineNumber();
            reader.close();
            return cnt;
        } catch (IOException io) {
            io.printStackTrace();
        }
        return 0;
    }

    // Удаление ключевого слова из combo box
    public void delSettings(String s) throws IOException {
        Path input = Paths.get(SETTINGS_PATH);
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
    public void console(String p_console) {
        try {
            Thread.sleep(100);
            Gui.consoleTextArea.setText(Gui.consoleTextArea.getText() + p_console + "\n");
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Шкала прогресса
    public void fill() {
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
    public int compareDatesOnly(Date p_now, Date p_in) {
        int minutes;
        if (Main.IS_CONSOLE_SEARCH.get()) minutes = Main.minutesIntervalForConsoleSearch;
        else minutes = Common.getInterval();

        Calendar minus = Calendar.getInstance();
        minus.setTime(new Date());
        minus.add(Calendar.MINUTE, -minutes);
        Calendar now_cal = Calendar.getInstance();
        now_cal.setTime(p_now);

        if (p_in.after(minus.getTime()) && p_in.before(now_cal.getTime())) {
            return 1;
        } else
            return 0;
    }

    // Заполнение диалоговых окон лога и СМИ
    public void showDialog(String p_file) {
        DatabaseQueries2 databaseQueries2 = new DatabaseQueries2();
        switch (p_file) {
            case "smi": {
                databaseQueries2.selectSources("active_smi");
                int i = 1;
                for (String s : Common.SMI_SOURCE) {
                    Object[] row = new Object[]{i, s, Common.SMI_IS_ACTIVE.get(i - 1)};
                    Dialogs.model.addRow(row);
                    i++;
                }
                break;
            }
            case "log":
                String path = "C:/Users/Public/Documents/app.log"; // TODO dynamic path

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(Files.newInputStream(Paths.get(path)), StandardCharsets.UTF_8))) {
                    String line;
                    StringBuilder allTab = new StringBuilder();

                    while ((line = reader.readLine()) != null) {
                        allTab.append(line).append("\n");
                    }
                    Dialogs.textAreaForDialogs.setText(allTab.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "excl": {
                databaseQueries2.selectSources("excl");
                int i = 1;
                for (String s : Common.EXCLUDED_WORDS) {
                    Object[] row = new Object[]{i, s};
                    Dialogs.model.addRow(row);
                    i++;
                }
                break;
            }
        }
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

    // Оставляет только буквы
    public String delNoLetter(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i)))
                sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    // преобразование строки в строку с хэш кодом
    public String sha256(String base) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
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

}
