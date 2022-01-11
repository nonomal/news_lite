package com.news;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class Sender {
    private final Properties p = new Properties();

    public void send(String subject, String text, String fromEmail, String pwd, String toEmail) throws MessagingException {
        String host = Common.getSmtp();
        p.put("mail.store.protocol", "imaps");
        p.put("mail.smtp.auth", "true");
        p.put("mail.smtp.starttls.enable", "true");
        p.put("mail.smtp.host", host);
        p.put("mail.smtp.port", "587");


        Session session = Session.getDefaultInstance(p, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, pwd);
            }
        });
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(fromEmail));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setText(text);
        Transport.send(message);
    }
}
