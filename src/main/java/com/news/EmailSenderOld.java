package com.news;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Level;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.PasswordAuthentication;

public class EmailSenderOld {
    private String from;
    private String from_pwd;
    private String smtp;
    private final String subject = ("News (" + Search.today + ")");


    // Считывание настроек почты из файла
    void getEmailSettingsFromFile() {
        int linesAmount = Common.countLines(Main.settingsPath);
        String[][] lines = new String[linesAmount][];

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(Main.settingsPath), StandardCharsets.UTF_8))) {
            String line;
            int i = 0;

            while ((line = reader.readLine()) != null && i < linesAmount) {
                lines[i++] = line.split("=");
            }

            for (String[] f : lines) {
                for (int j = 0; j < 1; j++) {
                    switch (f[0]) {
                        case "from_pwd":
                            from_pwd = f[1].trim();
                            break;
                        case "from_adr":
                            from = f[1].trim();
                            break;
//                        case "smtp":
//                            smtp = f[1].trim();
//                            break;
                    }
                }
            }
            //System.out.println(from + " " + from_pwd + " " + smtp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Отправка письма
    void sendMessage() {
        String to = Gui.sendTo;

        if (!Main.isConsoleSearch.get()) {
            StringBuilder text = new StringBuilder();
            for (String s : Search.dataForEmail) {
                text.append(s).append("\n\n");
            }
            //sendMail(text.toString());

            Common.console(from+ " " + from_pwd + " to=" + to + " " + getSmtp());
            Sender.send(subject, text.toString(), from, from_pwd, to);
        } else {
            StringBuilder text = new StringBuilder();
            for (String s : Search.dataForEmail) {
                text.append(s).append("\n\n");
            }
            //sendMailFromConsole(text.toString());
            Sender.send(subject, text.toString(), from, from_pwd, Main.emailToFromConsole);
        }
    }

    // определение названия почтового сервиса
    public String getMailServiceName(String email) {
        return email.substring(email.indexOf(64) + 1);
    }

    public String getSmtp() {
        String serviceName = getMailServiceName(from);
        switch(serviceName) {
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

    void sendMail(String text) {
        String to = Gui.sendTo;
        // чтобы не было задвоений в настройках - удаляем старую почту и записываем новую при отправке
        try {
            Common.delSettings("email=");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Common.writeToConfig(to, "email");

        Properties p = new Properties();
        smtp = getSmtp();
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
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);

            //Mail body
            message.setText(text);

            //Send message
            Transport.send(message);
            Common.console("status: e-mail sent successfully");
            Gui.progressBar.setValue(100);
            Common.isSending.set(true);
            Main.LOGGER.log(Level.INFO, "Email has been sent");
            Gui.searchAnimation.setText("sended");
            Gui.sendEmailBtn.setIcon(Gui.send3);
        } catch (MessagingException mex) {
            mex.printStackTrace();
            Common.console("status: e-mail wasn't send");
            Gui.progressBar.setValue(100);
            Gui.searchAnimation.setText("not send");
            Common.isSending.set(true);
        }
    }

    public void sendMailFromConsole(String text) {
        try {
            Properties p = new Properties();
            smtp = getSmtp();
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
