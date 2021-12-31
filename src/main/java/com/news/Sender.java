package com.news;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class Sender {
    private static Properties p = new Properties();

    public static void send(String subject, String text, String fromEmail, String pwd, String toEmail) {
        String host = "smtp.gmail.com";
        p.put("mail.smtp.starttls.enable", "true");
        p.put("mail.smtp.host", host);
        p.put("mail.smtp.user", fromEmail);
        p.put("mail.smtp.password", pwd);
        p.put("mail.smtp.port", "587");
        p.put("mail.smtp.auth", "true");

        Session session = Session.getDefaultInstance(p, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, pwd);
            }
        });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(text);
            Transport.send(message);
        } catch (MessagingException e) {
            Common.console(e.getMessage());
        }
    }
}
