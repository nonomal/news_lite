package email;

import gui.Gui;
import gui.buttons.Icons;
import lombok.extern.slf4j.Slf4j;
import search.ConsoleSearch;
import search.Search;
import utils.Common;

import javax.mail.MessagingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
public class EmailManager {
    private String fromAdr;
    private String fromPwd;
    private final String subject = ("News (" + new Search().today + ")");
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
            for (String s : Search.dataForEmail) {
                text.append(s).append("\n\n");
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
                Common.console("status: e-mail sent successfully");
                log.info("Email has been sent");
                Gui.sendEmailBtn.setIcon(Icons.WHEN_SENT_ICON);
                Common.IS_SENDING.set(true);
                Search.isSearchFinished.set(true);
                Gui.progressBar.setValue(100);
            } catch (Exception mex) {
                mex.printStackTrace();
                Common.console("status: e-mail wasn't send: " + mex.getMessage() + "\n" + mex.getCause());
                Common.IS_SENDING.set(true);
                Search.isSearchFinished.set(true);
                Gui.progressBar.setValue(100);
                log.warn("E-mail wasn't send");
            }
        } else {
            // Отправка из консоли
            for (String s : ConsoleSearch.dataForEmail) {
                text.append(s).append("\n\n");
            }
            try {
                new Sender().send(subject, text.toString(), fromAdr, fromPwd, ConsoleSearch.sendEmailToFromConsole);
                log.info("Email has been sent");
            } catch (MessagingException e) {
                e.printStackTrace();
                log.warn("E-mail wasn't send");
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