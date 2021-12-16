package com.news;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.PasswordAuthentication;

public class EmailSender {
    private static final String from = "rps_project@mail.ru";
    private static final String from_pwd = "RN0kFwDRjEA2vZ91frNu";
    private static final String smtp = "smtp.mail.ru";
    private static final String subject = ("News (" + Search.today + ")");

    void sendMessage() {
        if (!Main.isConsoleSearch.get()) {
            StringBuilder text = new StringBuilder();
            for (String s : Search.dataForEmail) {
                text.append(s).append("\n\n");
            }
            sendMail(text.toString());
        } else {
            StringBuilder text = new StringBuilder();
            for (String s : Search.dataForEmail) {
                text.append(s).append("\n\n");
            }
            sendMailFromConsole(text.toString());
        }
    }

    void sendMail(String text) {
        String to = Gui.sendEmailTo.getText().trim();
        // чтобы не было задвоений в настройках - удаляем старую почту и записываем новую при отправке
        try {
            Common.delSettings("email,");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Common.writeToConfig(to, "email");

        Properties p = new Properties();
        p.put("mail.smtp.host", smtp);
        p.put("mail.smtp.socketFactory.port", 465);
        p.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.port", 465);

        Session session = Session.getInstance(p, new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(from, from_pwd);
                    }
                }
        );

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(Gui.send_to));
            message.setSubject(subject);

            //Mail body
            message.setText(text);

            //Send message
            Transport.send(message);
            Common.console("status: e-mail sent successfully");
            Gui.progressBar.setValue(100);
            Common.isSending.set(true);
            Main.LOGGER.log(Level.INFO, "Email has been sent");
            Gui.search_animation.setText("sended");
            Gui.sendEmailBtn.setIcon(Gui.send3);
        } catch (MessagingException mex) {
            mex.printStackTrace();
            Common.console("status: e-mail wasn't send");
            Gui.progressBar.setValue(100);
            Gui.search_animation.setText("not send");
            Common.isSending.set(true);
            Gui.passwordField.setText("");
        }
    }

    public static void sendMailFromConsole(String text) {
        try {
            Properties p = new Properties();
            p.put("mail.smtp.host", smtp);
            p.put("mail.smtp.socketFactory.port", 465);
            p.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            p.put("mail.smtp.auth", "true");
            p.put("mail.smtp.port", 465);

            Session session = Session.getInstance(p, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(from, from_pwd);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(Main.emailToFromConsole));
            message.setSubject(subject);
            //Mail body
            message.setText(text);
            //Send message
            Transport.send(message);

            Main.LOGGER.log(Level.INFO, "Email has been sent");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}