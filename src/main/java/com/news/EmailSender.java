package com.news;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.logging.Level;

public class EmailSender {
    static String from;
    static String from_pwd;
    private final String subject = ("News (" + Search.today + ")");

    // Отправка письма
    void sendMessage(){
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
                Sender.send(subject, text.toString(), from, from_pwd, to);
                Common.console("status: e-mail sent successfully");
                Gui.progressBar.setValue(100);
                Common.isSending.set(true);
                Main.LOGGER.log(Level.INFO, "Email has been sent");
                Gui.searchAnimation.setText("sended");
                Gui.sendEmailBtn.setIcon(Gui.send3);
            } catch (Exception mex) {
                mex.printStackTrace();
                Common.console("status: e-mail wasn't send: " + mex.getMessage() + "\n" + mex.getCause());
                Gui.progressBar.setValue(100);
                Gui.searchAnimation.setText("not send");
                Common.isSending.set(true);
            }
        } else {
            // Отправка из консоли
            StringBuilder text = new StringBuilder();
            for (String s : Search.dataForEmail) {
                text.append(s).append("\n\n");
            }
            try {
                Sender.send(subject, text.toString(), from, from_pwd, Main.emailToFromConsole);
            } catch (MessagingException me) {
                Common.console("status: e-mail wasn't send: " + me.getMessage());
            }
            Main.LOGGER.log(Level.INFO, "Email has been sent");
        }
    }
}

