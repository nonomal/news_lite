package main;

import database.JdbcQueries;
import database.SQLite;
import search.ConsoleSearch;
import utils.Common;
import utils.Reminder;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static int userId;
    public static String username;

    public static void main(String[] args) {
        Common.createFiles();
        new SQLite().openConnection();
        login();

        if (args.length == 0) {
            Common.showGui();
            new Reminder().remind();
        } else {
            /*
              main.Main arguments for console search:
              args1 = email
              args2 = interval in minutes
              args3 = keyword1, keyword2 ... argsN = search keywords
             */
            new ConsoleSearch().searchByConsole(args);
        }
    }

    private static void login() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2, 0, 5));
        JLabel userLabel = new JLabel("user:");
        JTextField user = new JTextField("default"); //avandy
        //JLabel passwordLabel = new JLabel("password:");
        //JPasswordField password = new JPasswordField(10);
        panel.add(userLabel);
        panel.add(user);
        //panel.add(passwordLabel);
        //panel.add(password);

        String[] options = new String[]{"Ok", "Exit"};
        int option = JOptionPane.showOptionDialog(null, panel, "Login",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

        if(option == 0) {
            username = user.getText();

            if (new JdbcQueries().isUserExists(username)) {
                userId = new JdbcQueries().getUserIdByUsername(username);
                System.out.println("Your user: " + username + " user_id = " + userId);
                //char[] pwd = password.getPassword();
            } else {
                JOptionPane.showMessageDialog(null, "User not found, set to default settings");
                username = "default";
                userId = 0;
            }

        } else {
            System.exit(0);
        }

    }
}