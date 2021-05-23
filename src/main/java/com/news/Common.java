package com.news;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class Common {
    static AtomicBoolean isSending = new AtomicBoolean(true);
    static ArrayList<String> keywordsList = new ArrayList<>();
    static String text = "";
    //данные из SQLite
    public static int smi_number = 0;
    static ArrayList<String> smi_link = new ArrayList<>();
    static ArrayList<String> smi_source = new ArrayList<>();
    static ArrayList<String> excludedWords = new ArrayList<>();

    // формирование теста письма
    public static void concatText(String p_in) {
        text += (Gui.q + 1) + ") " + p_in + "\nИсточник: " + smi_source.get(smi_number) + "\n" + "\n";
        Gui.q++;
        Gui.labelSum.setText(String.valueOf(Gui.q));
    }

    // Запись конфигураций приложения
    static void writeToConfig(String p_word, String p_type) {
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(Main.settingsPath, true), StandardCharsets.UTF_8)) {
            switch (p_type) {
                case "keyword": {
                    String text = "keyword," + p_word + "\n";
                    writer.write(text);
                    writer.flush();
                    break;
                }
                case "email": {
                    String text = "email," + p_word;
                    writer.write(text.trim() + "\n");
                    writer.flush();
                    break;
                }
                case "interval": {
                    String text = "interval," + p_word.replace(" hour", "h")
                            .replace("s", "")
                            .replace(" min", "m");
                    writer.write(text + "\n");
                    writer.flush();
                    break;
                }
                case "checkbox": {
                    String text = null;
                    switch (p_word) {
                        case "todayOrNotChbx":
                            text = "checkbox:" + p_word + "," + Gui.todayOrNotChbx.getState() + "\n";
                            break;
                        case "checkTitle":
                            text = "checkbox:" + p_word + "," + Gui.searchInTitleCbx.getState() + "\n";
                            break;
                        case "checkLink":
                            text = "checkbox:" + p_word + "," + Gui.searchInLinkCbx.getState() + "\n";
                            break;
                    }
                    if (text != null) writer.write(text);
                    writer.flush();
                    break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Запись интервалов в комбобокс
    static void addIntervalsToCombobox(JComboBox<String> p_cbx_name, String[] p_items) {
        for (String p_item : p_items) {
            p_cbx_name.addItem(p_item);
        }
    }

    // Подсчет количества строк в файле
    static int countLines(String p_path) {
        try {
            LineNumberReader reader = new LineNumberReader(new FileReader(p_path));
            int cnt;
            //String lineRead = "";
            while (true) {
                //if ((lineRead = reader.readLine()) == null) break;
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

    // Удаление ключевого слова из комбобокса
    static void delSettings(String s) throws IOException {
        Path input = Paths.get(Main.settingsPath);
        Path temp = Files.createTempFile("temp", ".txt");
        Stream<String> lines = Files.lines(input);
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
        Files.move(temp, input, StandardCopyOption.REPLACE_EXISTING);
    }

    // Search animation
    public static void statusLabel(AtomicBoolean p_isSending, String p_text) {
        Thread thr = new Thread(() -> {
            for (smi_number = 0; smi_number < smi_link.size(); ) {
                try {
                    if (p_isSending.get()) return;
                    Gui.search_animation.setText(p_text + "");
                    Thread.sleep(500);
                    if (p_isSending.get()) return;
                    Gui.search_animation.setText(p_text + ".");
                    Thread.sleep(500);
                    if (p_isSending.get()) return;
                    Gui.search_animation.setText(p_text + "..");
                    Thread.sleep(500);
                    if (p_isSending.get()) return;
                    Gui.search_animation.setText(p_text + ".");
                    Thread.sleep(500);
                    if (p_isSending.get()) return;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        thr.start();
    }

    //Console
    public static void console(String p_console) {
        try {
            Thread.sleep(100);
            Gui.animation_status.setText(Gui.animation_status.getText() + p_console + "\n");
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Шкала прогресса
    static void fill() {
        int counter = 0;
        while (!Search.isSearchFinished.get() || !isSending.get()) {
            if (!isSending.get()) Gui.progressBar.setForeground(new Color(255, 115, 0));
            else if (!Search.isSearchFinished.get()) Gui.progressBar.setForeground(new Color(10, 255, 41));
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

    // Считывание настроек из файла в массив строк
    static void getSettingsFromFile() {
        int linesAmount = Common.countLines(Main.settingsPath);
        String[][] lines = new String[linesAmount][];

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(Main.settingsPath), StandardCharsets.UTF_8))) {
            String line;
            int i = 0;

            while ((line = reader.readLine()) != null && i < linesAmount) {
                lines[i++] = line.split(",");
            }

            for (String[] f : lines) {
                for (int j = 0; j < 1; j++) {
                    switch (f[0]) {
                        case "interval":
                            if (f[1].equals("1h")) {
                                Gui.newsIntervalCbox.setSelectedItem(f[1].replace("h", "") + " hour");
                            } else if (f[1].equals("1m") || f[1].equals("5m") || f[1].equals("15m")
                                    || f[1].equals("30m") || f[1].equals("45m")) {
                                Gui.newsIntervalCbox.setSelectedItem(f[1].replace("m", "") + " min");
                            } else {
                                Gui.newsIntervalCbox.setSelectedItem(f[1].replace("h", "") + " hours");
                            }
                            break;
                        case "email":
                            Gui.sendEmailTo.setText(f[1].trim());
                            break;
                        case "keyword":
                            Gui.keywordsCbox.addItem(f[1]);
                            keywordsList.add(f[1]);
                            break;
                        case "checkbox:todayOrNotChbx":
                            Gui.todayOrNotChbx.setState(Boolean.parseBoolean(f[1]));
                            break;
                        case "checkbox:checkTitle":
                            Gui.searchInTitleCbx.setState(Boolean.parseBoolean(f[1]));
                            break;
                        case "checkbox:checkLink":
                            Gui.searchInLinkCbx.setState(Boolean.parseBoolean(f[1]));
                            break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Считывание ключевых слов при добавлении/удалении в комбобоксе
    static String[] getKeywordsFromFile() {
        ArrayList<String> lines = new ArrayList<>();
        String[] listOfKeywords = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(Main.settingsPath), StandardCharsets.UTF_8))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("keyword,"))
                    lines.add(line.replace("keyword,", ""));
            }
            listOfKeywords = lines.toArray(new String[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return listOfKeywords;
    }

    // Интервал поиска/таймера в секундах
    static int getInterval() {
        int minutes;
        if (Objects.requireNonNull(Gui.newsIntervalCbox.getSelectedItem()).toString().contains(" min")) {
            minutes = Integer.parseInt(Objects.requireNonNull(Gui.newsIntervalCbox
                    .getSelectedItem())
                    .toString()
                    .replace(" min", ""));
        } else {
            minutes = Integer.parseInt(Objects.requireNonNull(Gui.newsIntervalCbox
                    .getSelectedItem())
                    .toString()
                    .replace(" hour", "")
                    .replace("s", "")) * 60;
        }
        return minutes;
    }

    // Сравнение дат
    public static int compareDatesOnly(Date p_now, Date p_in) {
        int minutes = Common.getInterval();
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
    static void showDialog(String p_file) {
        if (p_file.equals("smi")) {
            SQLite.selectSources("smi");
            int i = 1;
            for (String s: Common.smi_source) {
                Object[] row = new Object[]{i, s};
                Dialogs.model.addRow(row);
                i++;
            }
        } else if (p_file.equals("log")) {
            String path = Main.logPath;

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
                String line;
                StringBuilder allTab = new StringBuilder();

                while ((line = reader.readLine()) != null) {
                    allTab.append(line).append("\n");
                }
                Dialogs.textAreaForDialogs.setText(allTab.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (p_file.equals("excl")) {
            SQLite.selectSources("excl");
            int i = 1;
            for (String s: Common.excludedWords) {
                Object[] row = new Object[]{i, s};
                Dialogs.model.addRow(row);
                i++;
            }
        }

    }

    // Копирование файлов из jar
    static void copyFiles(URL p_file, String copy_to) {
        File copied = new File(copy_to);
        try (InputStream in = p_file.openStream();
             OutputStream out = new BufferedOutputStream(new FileOutputStream(copied))) {
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

    // Удаление строки с источником новостей из файла sources.txt
    static void delLine(String s, String pPath) throws IOException {
        Path input = Paths.get(pPath);
        Path temp = Files.createTempFile("temp", ".txt");
        Stream<String> lines = Files.lines(input);
        try (BufferedWriter writer = Files.newBufferedWriter(temp)) {
            lines
                    .filter(line -> {
                        assert s != null;
                        return !line.contains(s);
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
        Files.move(temp, input, StandardCopyOption.REPLACE_EXISTING);
    }

    // Оставляет только буквы
    static String delNoLetter(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i)))
                sb.append(s.charAt(i));
        }
        return sb.toString();
    }

}
