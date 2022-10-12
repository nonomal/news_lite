package email;

import gui.Gui;
import gui.buttons.Icons;
import model.TableRow;
import search.ConsoleSearch;
import search.Search;
import utils.Common;

import javax.mail.MessagingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EmailManager {
    private String fromAdr;
    private String fromPwd;
    private final String today = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(LocalDateTime.now());
    private final String subject = ("News (" + today + ")");
    private final StringBuilder text = new StringBuilder();

    // Отправка письма
    public void sendMessage() {
        getEmailSettingsFromFile();

        if (!ConsoleSearch.IS_CONSOLE_SEARCH.get()) {
            String to = Gui.sendEmailTo.getText();

            if (!fromAdr.contains("@")||!to.contains("@")) {
                Common.console("error: incorrect e-mail");
                return;
            }

            // Отправка из GUI
            int i = 1;
            for (TableRow s : Search.emailAndExcelData) {
                text.append(i++).append(") ").append(s).append("\n\n");
            }
            try {
                // чтобы не было задвоений в настройках - удаляем старую почту и записываем новую при отправке
                try {
                    Common.delSettings("email=");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Common.writeToConfig(to, "email");

                //отправка
                new Sender().send(subject, text.toString(), fromAdr, fromPwd, to);
                Common.console("e-mail sent successfully");
                Gui.sendEmailBtn.setIcon(Icons.WHEN_SENT_ICON);
                Common.IS_SENDING.set(true);
                Search.isSearchFinished.set(true);
                Gui.progressBar.setValue(100);
            } catch (Exception mex) {
                mex.printStackTrace();
                Common.console("e-mail wasn't send: " + mex.getMessage() + "\n" + mex.getCause());
                Common.IS_SENDING.set(true);
                Search.isSearchFinished.set(true);
                Gui.progressBar.setValue(100);
            }
        } else {
            // Отправка из консоли
            for (String s : ConsoleSearch.dataForEmail) {
                text.append(s).append("\n\n");
            }
            try {
                new Sender().send(subject, text.toString(), fromAdr, fromPwd, ConsoleSearch.sendEmailToFromConsole);
                System.out.println("e-mail sent successfully");
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }

    // Считывание настроек почты из файла
    private void getEmailSettingsFromFile() {
        try {
            for (String s : Files.readAllLines(Paths.get(Common.CONFIG_FILE))) {
                if (s.startsWith("from_pwd=")) {
                    fromPwd = s.replace("from_pwd=", "");

                } else if (s.startsWith("from_adr=")) {
                    fromAdr = s.replace("from_adr=", "");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}