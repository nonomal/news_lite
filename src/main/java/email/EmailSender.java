package email;

import gui.Gui;
import main.Main;
import search.Search;
import utils.Common;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.logging.Level;

public class EmailSender {
    public static String from;
    public static String from_pwd;
    private final String subject = ("News (" + new Search().today + ")");

    // Отправка письма
    public void sendMessage(){
        Common.getEmailSettingsFromFile();

        if (!Main.isConsoleSearch.get()) {
            String to = Gui.sendEmailTo.getText();
            // Отправка из GUI
            StringBuilder text = new StringBuilder();
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
                new Sender().send(subject, text.toString(), from, from_pwd, to);
                Common.console("status: e-mail sent successfully");
                Main.LOGGER.log(Level.INFO, "Email has been sent");
                Gui.sendEmailBtn.setIcon(Gui.send3);
                Common.isSending.set(true);
                Search.isSearchFinished.set(true);
                Gui.progressBar.setValue(100);
            } catch (Exception mex) {
                mex.printStackTrace();
                Common.console("status: e-mail wasn't send: " + mex.getMessage() + "\n" + mex.getCause());
                Common.isSending.set(true);
                Search.isSearchFinished.set(true);
                Gui.progressBar.setValue(100);
            }
        } else {
            // Отправка из консоли
            StringBuilder text = new StringBuilder();
            for (String s : Search.dataForEmail) {
                text.append(s).append("\n\n");
            }
            try {
                Sender sender = new Sender();
                sender.send(subject, text.toString(), from, from_pwd, Main.emailToFromConsole);
            } catch (MessagingException me) {
                Common.console("status: e-mail wasn't send: " + me.getMessage());
            }
            Main.LOGGER.log(Level.INFO, "Email has been sent");
        }
    }
}

