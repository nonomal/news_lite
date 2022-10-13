package email;

import gui.Gui;
import gui.buttons.Icons;
import model.TableRow;
import search.ConsoleSearch;
import search.Search;
import utils.Common;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EmailManager {
    private final String today = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(LocalDateTime.now());
    private final String subject = ("News (" + today + ")");
    private final StringBuilder text = new StringBuilder();

    // Отправка письма
    public void sendMessage() {
        Common.getSettings();

        if (!ConsoleSearch.IS_CONSOLE_SEARCH.get()) {
            if (!Common.emailFrom.contains("@")||!Common.emailTo.contains("@")) {
                Common.console("incorrect e-mail");
                return;
            }

            // Отправка из GUI
            int i = 1;
            for (TableRow s : Search.emailAndExcelData) {
                text.append(i++).append(") ").append(s).append("\n\n");
            }
            try {
                //отправка
                new Sender().send(subject, text.toString(), Common.emailFrom, Common.emailFromPwd, Common.emailTo);
                Common.console("e-mail sent successfully");
                Gui.sendEmailBtn.setIcon(Icons.WHEN_SENT_ICON);
                Common.IS_SENDING.set(true);
                Search.isSearchFinished.set(true);
                Gui.progressBar.setValue(100);
            } catch (AuthenticationFailedException e) {
                Common.console("Неверный пароль или почта");
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
                new Sender().send(subject, text.toString(), Common.emailFrom, Common.emailFromPwd,
                        ConsoleSearch.sendEmailToFromConsole);
                System.out.println("e-mail sent successfully");
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }
}