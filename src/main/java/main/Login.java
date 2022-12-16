package main;

import database.JdbcQueries;
import gui.Gui;
import gui.buttons.Icons;
import utils.Common;

import javax.swing.*;
import java.awt.*;

public class Login {
    public static int userId;
    public static String username;

    protected void login() {
        JdbcQueries jdbcQueries = new JdbcQueries();
        String[] loginParams = showLoginDialog();

        int option = Integer.parseInt(loginParams[0]);

        // create user
        if (option == 0) {
            JPanel panel = new JPanel();
            panel.setLayout(new GridLayout(2, 2, 0, 5));
            JLabel userLabel = new JLabel("Username");
            JTextField user = new JTextField();
            JLabel passwordLabel = new JLabel("Password");
            JPasswordField passwordField = new JPasswordField();
            panel.add(userLabel);
            panel.add(user);
            panel.add(passwordLabel);
            panel.add(passwordField);

            String[] menu = new String[]{"add", "cancel"};
            int action = JOptionPane.showOptionDialog(null, panel, "Add user",
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                    Icons.LIST_BUTTON_ICON, menu, menu[0]);

            String pwd = Common.getHash(new String(passwordField.getPassword()));
            if (action == 0 && user.getText().length() >= 3) {
                jdbcQueries.addUser(user.getText(), pwd);
                login();
                jdbcQueries.initUser(userId);
            } else if (action == 0 && user.getText().length() < 3) {
                JOptionPane.showMessageDialog(null, "The minimum username length is 3 chars");
                login();
            } else {
                login();
            }
            // default user
        } else if (option == 1) {
            username = "default";
            userId = 0;
            Gui.loginLabel.setText("user: default");
            // ok
        } else if (option == 2) {
            username = loginParams[1];
            if (jdbcQueries.isUserExists(username)) {
                userId = jdbcQueries.getUserIdByUsername(username);
                String password = loginParams[2];
                String userHashPassword = jdbcQueries.getUserHashPassword(username);
                Gui.loginLabel.setText("user: " + username);

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
    private String[] showLoginDialog() {
        JdbcQueries jdbcQueries = new JdbcQueries();
        String[] options = new String[3];

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2, 5, 5));
        JLabel userLabel = new JLabel("Username");
        JComboBox<Object> usersCombobox = new JComboBox<>(jdbcQueries.getAllUsers().toArray());

        // Удаление пользователя
        JButton removeUserButton = new JButton(Icons.DELETE_UNIT);
        removeUserButton.setHorizontalAlignment(SwingConstants.LEFT);
        removeUserButton.setContentAreaFilled(false);
        removeUserButton.addActionListener(x -> {
            String[] opt = new String[]{"yes", "no"};
            int action = JOptionPane.showOptionDialog(null, "Are you confirming user deletion?",
                    "Remove user", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    Icons.TRANSLATOR_BUTTON_ICON, opt, opt[1]);

            if (action == 0) {
                jdbcQueries.removeFromUsers(usersCombobox.getSelectedItem().toString());
            }

        });
        Gui.animation(removeUserButton, Icons.EXIT_BUTTON_ICON, Icons.WHEN_MOUSE_ON_EXIT_BUTTON_ICON);

        JLabel passwordLabel = new JLabel("Password");
        JPasswordField passwordField = new JPasswordField(3);
        panel.add(userLabel);
        panel.add(usersCombobox);
        panel.add(removeUserButton);
        panel.add(passwordLabel);
        panel.add(passwordField);

        String[] menu = new String[]{"add user", "default", "ok"};
        int option = JOptionPane.showOptionDialog(null, panel, "Login",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                Icons.LOGO_ICON, menu, menu[2]);

        options[0] = String.valueOf(option);
        if (usersCombobox.getSelectedItem() != null) {
            options[1] = usersCombobox.getSelectedItem().toString();
        }
        options[2] = Common.getHash(new String(passwordField.getPassword()));

        return options;
    }

}
