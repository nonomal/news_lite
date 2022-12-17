package email;

import database.JdbcQueries;
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
    JdbcQueries jdbcQueries = new JdbcQueries();
    private final String today = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").format(LocalDateTime.now());
    private final String subject = ("News (" + today + ")");
    private final StringBuilder text = new StringBuilder();

    // Отправка письма
    public void sendMessage() {
        String emailFrom = jdbcQueries.getSetting("email_from");
        String emailFromPwd = jdbcQueries.getSetting("from_pwd");
        String emailTo = jdbcQueries.getSetting("email_to");

        if (!ConsoleSearch.IS_CONSOLE_SEARCH.get()) {
            if (!emailFrom.contains("@")||!emailTo.contains("@")) {
                Common.console("incorrect e-mail");
                return;
            }

            // Отправка из GUI
            int i = 1;
            for (TableRow s : Search.headlinesList) {
                text.append(i++).append(") ").append(s).append("\n\n");
            }
            try {
                //отправка
                new Sender().send(subject, text.toString(), emailFrom, emailFromPwd, emailTo);
                Common.console("e-mail sent successfully");
                Gui.sendCurrentResultsToEmail.setIcon(Icons.WHEN_SENT_ICON);
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
                new Sender().send(subject, text.toString(),
                        ConsoleSearch.sendEmailFromConsole,
                        ConsoleSearch.sendEmailFromPwdConsole,
                        ConsoleSearch.sendEmailToConsole);
                System.out.println("e-mail sent successfully");
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }
}