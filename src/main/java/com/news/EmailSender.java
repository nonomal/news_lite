package com.news;

import java.io.IOException;
import java.util.logging.Level;

public class EmailSender {
    static String from;
    static String from_pwd;
    static String smtp;
    private final String subject = ("News (" + Search.today + ")");

    // Отправка письма
    void sendMessage() {
        String to = Gui.sendEmailTo.getText();
        Common.getEmailSettingsFromFile();
        smtp = Common.getSmtp();

        if (!Main.isConsoleSearch.get()) {
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
                Common.console("status: e-mail wasn't send: " + mex.getMessage());
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
            //sendMailFromConsole(text.toString());
            Sender.send(subject, text.toString(), from, from_pwd, Main.emailToFromConsole);
            Main.LOGGER.log(Level.INFO, "Email has been sent");
        }
    }
}

