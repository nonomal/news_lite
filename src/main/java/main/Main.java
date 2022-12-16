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
        login(args);
    }

    private static void login(String[] args) {
        String[] loginParams = showLoginDialog();

        if (Integer.parseInt(loginParams[0]) == 0) {
            username = loginParams[1];
            JdbcQueries jdbcQueries = new JdbcQueries();

            if (jdbcQueries.isUserExists(username)) {
                userId = jdbcQueries.getUserIdByUsername(username);
                String password = loginParams[2];
                String userHashPassword = jdbcQueries.getUserHashPassword(username);

                // Correct password
                if (password.equals(userHashPassword)) {
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
                } else {
                    JOptionPane.showMessageDialog(null, "Incorrect password");
                    login(args);
                }
            } else {
                JOptionPane.showMessageDialog(null, "User not found, set to default settings");
                username = "default";
                userId = 0;
            }

        } else {
            System.exit(0);
        }
    }

    /*
     login[0] = Ok or Exit from dialog
     login[1] = username
     login[2] = password
    */
    private static String[] showLoginDialog() {
        String[] login = new String[3];

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(4, 2, 0, 5));
        JLabel userLabel = new JLabel("Username");
        JTextField user = new JTextField("avandy"); // avandy default
        JLabel passwordLabel = new JLabel("Password");
        JPasswordField passwordField = new JPasswordField();
        panel.add(userLabel);
        panel.add(user);
        panel.add(passwordLabel);
        panel.add(passwordField);

        String[] options = new String[]{"Ok", "Exit"};
        int option = JOptionPane.showOptionDialog(null, panel, "Login",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

        login[0] = String.valueOf(option);
        login[1] = user.getText();
        login[2] = Common.getHash(new String(passwordField.getPassword()));

        return login;
    }
}