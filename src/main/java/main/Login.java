package main;

import database.JdbcQueries;
import gui.buttons.Icons;
import utils.Common;

import javax.swing.*;
import java.awt.*;

public class Login {
    public static int userId;
    public static String username;
    private String newUser;
    private final JdbcQueries jdbcQueries = new JdbcQueries();
    JComboBox<Object> usersCombobox = new JComboBox<>(jdbcQueries.getAllUsers().toArray());

    public void login() {
        String[] loginParams = showLoginDialog();
        int option = Integer.parseInt(loginParams[0]);

        // remove user
        if (option == 0) {
            if (usersCombobox.getSelectedItem() != null){
                String[] opt = new String[]{"yes", "no"};
                int action = JOptionPane.showOptionDialog(null, "Are you confirming user deletion?",
                        "Remove user", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                        Icons.TRANSLATOR_BUTTON_ICON, opt, opt[1]);
                if (action == 0) {
                    String user = usersCombobox.getSelectedItem().toString();
                    jdbcQueries.removeFromUsers(user);
                    usersCombobox = new JComboBox<>(jdbcQueries.getAllUsers().toArray());
                    login();
                }
            } else {
                JOptionPane.showMessageDialog(null, "There is no user to delete");
                login();
            }
            // create user
        } else if (option == 1) {
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
            int length = user.getText().length();
             if (!jdbcQueries.isUserExists(user.getText())) {
                 if (action == 0 && length >= 3 && length <= 10) {
                     jdbcQueries.addUser(user.getText(), pwd);
                     usersCombobox = new JComboBox<>(jdbcQueries.getAllUsers().toArray());
                     newUser = user.getText();
                     login();
                     if (newUser != null) jdbcQueries.initUser();
                 } else if (action == 0 && (user.getText().length() < 3 || user.getText().length() > 10)) {
                     JOptionPane.showMessageDialog(null, "The username length between 3 and 10 chars");
                     login();
                 } else {
                     login();
                 }
             } else {
                 JOptionPane.showMessageDialog(null, "User exists!");
                 login();
             }
            // Enter
        } else if (option == 2) {
            //if (newUser != null) jdbcQueries.initUser();

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
     login[0] = "Remove", "Create", "Enter"
     login[1] = username
     login[2] = password
    */
    private String[] showLoginDialog() {
        String[] options = new String[3];

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(2, 2, 5, 5));
        JLabel userLabel = new JLabel("Username");
        JLabel passwordLabel = new JLabel("Password");
        JPasswordField passwordField = new JPasswordField(3);
        panel.add(userLabel);
        panel.add(usersCombobox);
        panel.add(passwordLabel);
        panel.add(passwordField);

        // after create user
        if (newUser != null) usersCombobox.setSelectedItem(newUser);

        String[] menu = new String[]{"Remove", "Create", "Enter"};
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
