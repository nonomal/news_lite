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
              main arguments for console search:
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

        if (option == 0) { //create user
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(2, 2, 0, 5));
            JLabel userLabel = new JLabel("Username");
            JTextField user = new JTextField(); // avandy default
            JLabel passwordLabel = new JLabel("Password");
            JPasswordField passwordField = new JPasswordField();
            panel.add(userLabel);
            panel.add(user);
            panel.add(passwordLabel);
            panel.add(passwordField);

            String[] menu = new String[]{"add", "cancel"};
            int action = JOptionPane.showOptionDialog(null, panel, "Login",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                    Icons.LOGO_ICON, menu, menu[0]);

            String pwd = Common.getHash(new String(passwordField.getPassword()));
            if (action == 0 && user.getText().length() >= 3) {
                jdbcQueries.addUser(user.getText(), pwd);
                login();
            } else if (action == 0 && user.getText().length() < 3) {
                JOptionPane.showMessageDialog(null, "The minimum username length is 3 chars");
                login();
            } else {
                login();
            }

        } else if (option == 1) {
            username = "default";
            userId = 0;
        } else if (option == 2) {
            username = loginParams[1];

            if (jdbcQueries.isUserExists(username)) {
                userId = jdbcQueries.getUserIdByUsername(username);
                String password = loginParams[2];
                String userHashPassword = jdbcQueries.getUserHashPassword(username);

                // Password check
                if (!password.equals(userHashPassword)) {
                    JOptionPane.showMessageDialog(null, "Incorrect password");
                    login();
                }
            } else {
                JOptionPane.showMessageDialog(null, "User not found");
                login();
            }
        } else {
            System.exit(0);
        }
    }

    /*
     login[0] = Create user, Default user, Ok
     login[1] = username
     login[2] = password
    */
    private static String[] showLoginDialog() {
        String[] options = new String[3];

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2, 0, 5));
        JLabel userLabel = new JLabel("Username");
        JTextField user = new JTextField("avandy", 3); // avandy default
        JLabel passwordLabel = new JLabel("Password");
        JPasswordField passwordField = new JPasswordField(3);
        panel.add(userLabel);
        panel.add(user);
        panel.add(passwordLabel);
        panel.add(passwordField);

        String[] menu = new String[]{"add user", "default", "ok"};
        int option = JOptionPane.showOptionDialog(null, panel, "Login",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                Icons.LOGO_ICON, menu, menu[2]);

        options[0] = String.valueOf(option);
        options[1] = user.getText();
        options[2] = Common.getHash(new String(passwordField.getPassword()));

        return options;
    }
}