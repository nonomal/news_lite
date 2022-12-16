package main;

import database.JdbcQueries;
import database.SQLite;
import gui.buttons.Icons;
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

        if (args.length == 0) {
            Common.showGui();
            new Reminder().remind();
            login();
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
        JdbcQueries jdbcQueries = new JdbcQueries();
        String[] loginParams = showLoginDialog();

        int option = Integer.parseInt(loginParams[0]);

        if (option == 0) {
            username = loginParams[1];

            if (jdbcQueries.isUserExists(username)) {
                userId = jdbcQueries.getUserIdByUsername(username);
                String password = loginParams[2];
                String userHashPassword = jdbcQueries.getUserHashPassword(username);

                // Correct password
                if (password.equals(userHashPassword) || username.equals("default")) {
                    System.out.println("Ok");
                } else {
                    JOptionPane.showMessageDialog(null, "Incorrect password");
                    login();
                }
            } else {
                JOptionPane.showMessageDialog(null, "User not found");
                login();
            }
        } else if (option == 1) {
            username = "default";
            userId = 0;
        } else if (option == 2) {
            //jdbcQueries.createUser();
        } else if (option == 3) {
            System.exit(0);
        }
    }

    /*
     login[0] = Ok, Default user, Create user, Exit from dialog
     login[1] = username
     login[2] = password
    */
    private static String[] showLoginDialog() {
        String[] options = new String[3];

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

        String[] menu = new String[]{"Ok", "Default user", "Create user", "Exit"};
        int option = JOptionPane.showOptionDialog(null, panel, "Login",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                Icons.LOGO_ICON, menu, menu[0]);

        options[0] = String.valueOf(option);
        options[1] = user.getText();
        options[2] = Common.getHash(new String(passwordField.getPassword()));

        return options;
    }
}