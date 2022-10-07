package gui;

import database.JdbcQueries;
import model.Excluded;
import model.Favorite;
import model.Keyword;
import model.Source;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class Dialogs extends JDialog implements KeyListener {
    static JTable table;
    public static DefaultTableModel model;
    public static JTextArea textAreaForDialogs;

    public Dialogs(String name) {
        textAreaForDialogs = new JTextArea();
        textAreaForDialogs.setFont(new Font("Dialog", Font.PLAIN, 13));
        textAreaForDialogs.setTabSize(10);
        textAreaForDialogs.setEditable(false);
        textAreaForDialogs.setLineWrap(true);
        textAreaForDialogs.setWrapStyleWord(true);
        textAreaForDialogs.setBounds(12, 27, 22, 233);
        this.setResizable(true);
        this.setFont(new Font("Tahoma", Font.PLAIN, 14));
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        this.setVisible(true);
        JScrollPane scrollPane = new JScrollPane();
        this.getContentPane().setLayout(new BorderLayout(0, 0));
        this.addKeyListener(this);

        switch (name) {
            case "smiDlg": {
                this.setBounds(600, 200, 250, 306);
                this.setTitle("Sources");
                this.setLocationRelativeTo(Gui.smiBtn);
                Object[] columns = {"Pos", "Source", "", " "};
                model = new DefaultTableModel(new Object[][]{
                }, columns) {
                    final boolean[] columnEditable = new boolean[]{false, false, true, true};

                    public boolean isCellEditable(int row, int column) {
                        return columnEditable[column];
                    }

                    // Сортировка
                    final Class[] types_unique = {Integer.class, String.class, Boolean.class, Button.class};

                    @Override
                    public Class getColumnClass(int columnIndex) {
                        return this.types_unique[columnIndex];
                    }
                };
                table = new JTable(model);
                table.getColumnModel().getColumn(2).setCellEditor(new CheckBoxEditor(new JCheckBox()));
                table.getColumn(" ").setCellRenderer(new ButtonColumn(table, 3));
                table.setAutoCreateRowSorter(true);
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(JLabel.CENTER);
                table.setRowHeight(20);
                table.setFont(new Font("SansSerif", Font.PLAIN, 13));
                JTableHeader header = table.getTableHeader();
                header.setFont(new Font("Tahoma", Font.BOLD, 13));
                table.getColumnModel().getColumn(0).setCellRenderer(renderer);
                table.getColumnModel().getColumn(0).setMaxWidth(30);
                table.getColumnModel().getColumn(2).setMaxWidth(30);
                table.getColumnModel().getColumn(3).setMaxWidth(30);
                getContentPane().add(table, BorderLayout.CENTER);

                scrollPane.setBounds(10, 27, 324, 233);
                this.getContentPane().add(scrollPane);
                scrollPane.setViewportView(table);

                showDialogs("smi");
                break;
            }
            case "exclDlg": {
                this.setBounds(600, 200, 250, 306);
                this.setTitle("Excluded from analysis");
                this.setLocationRelativeTo(Gui.exclBtn);
                Object[] columns = {"Num", "Word", "Del"};
                model = new DefaultTableModel(new Object[][]{
                }, columns) {
                    final boolean[] columnEditable = new boolean[]{false, false, true};

                    public boolean isCellEditable(int row, int column) {
                        return columnEditable[column];
                    }

                    // Сортировка
                    final Class[] types_unique = {Integer.class, String.class, Button.class};

                    @Override
                    public Class getColumnClass(int columnIndex) {
                        return this.types_unique[columnIndex];
                    }
                };
                table = new JTable(model);
                table.getColumn("Del").setCellRenderer(new ButtonColumn(table, 2));
                table.setAutoCreateRowSorter(true);
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(JLabel.CENTER);
                table.setRowHeight(20);
                table.setFont(new Font("SansSerif", Font.PLAIN, 13));
                JTableHeader header = table.getTableHeader();
                header.setFont(new Font("Tahoma", Font.BOLD, 13));
                table.getColumnModel().getColumn(0).setCellRenderer(renderer);
                table.getColumnModel().getColumn(0).setMaxWidth(40);
                table.getColumnModel().getColumn(2).setMaxWidth(40);
                getContentPane().add(table, BorderLayout.CENTER);

                scrollPane.setBounds(10, 27, 324, 233);
                this.getContentPane().add(scrollPane);
                scrollPane.setViewportView(table);

                showDialogs("excl");
                break;
            }
            case "exclTitlesDlg": {
                this.setBounds(600, 200, 250, 306);
                this.setTitle("Excluded headlines");
                this.setLocationRelativeTo(Gui.exclTitlesBtn);

                Object[] columns = {"Num", "Word", "Del"};
                model = new DefaultTableModel(new Object[][]{
                }, columns) {
                    final boolean[] columnEditable = new boolean[]{false, false, true};

                    public boolean isCellEditable(int row, int column) {
                        return columnEditable[column];
                    }

                    // Сортировка
                    final Class[] types_unique = {Integer.class, String.class, Button.class};

                    @Override
                    public Class getColumnClass(int columnIndex) {
                        return this.types_unique[columnIndex];
                    }
                };
                table = new JTable(model);
                table.getColumn("Del").setCellRenderer(new ButtonColumn(table, 2));
                table.setAutoCreateRowSorter(true);
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(JLabel.CENTER);
                table.setRowHeight(20);
                table.setFont(new Font("SansSerif", Font.PLAIN, 13));
                JTableHeader header = table.getTableHeader();
                header.setFont(new Font("Tahoma", Font.BOLD, 13));
                table.getColumnModel().getColumn(0).setCellRenderer(renderer);
                table.getColumnModel().getColumn(0).setMaxWidth(40);
                table.getColumnModel().getColumn(2).setMaxWidth(40);
                getContentPane().add(table, BorderLayout.CENTER);

                scrollPane.setBounds(10, 27, 324, 233);
                this.getContentPane().add(scrollPane);
                scrollPane.setViewportView(table);

                showDialogs("title-excl");
                break;
            }
            case "keywordsDlg": {
                this.setBounds(600, 200, 250, 306);
                this.setTitle("Keywords");
                this.setLocationRelativeTo(Gui.addKeywordToList);

                Object[] columns = {"Pos", "Keyword", "", " "};
                model = new DefaultTableModel(new Object[][]{
                }, columns) {
                    final boolean[] columnEditable = new boolean[]{false, false, true, true};

                    public boolean isCellEditable(int row, int column) {
                        return columnEditable[column];
                    }

                    // Сортировка
                    final Class[] types_unique = {Integer.class, String.class, Boolean.class, Button.class};

                    @Override
                    public Class getColumnClass(int columnIndex) {
                        return this.types_unique[columnIndex];
                    }
                };
                table = new JTable(model);
                table.getColumnModel().getColumn(2).setCellEditor(new CheckBoxEditor(new JCheckBox()));
                table.getColumn(" ").setCellRenderer(new ButtonColumn(table, 3));
                table.setAutoCreateRowSorter(true);
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(JLabel.CENTER);
                table.setRowHeight(20);
                table.setFont(new Font("SansSerif", Font.PLAIN, 13));
                JTableHeader header = table.getTableHeader();
                header.setFont(new Font("Tahoma", Font.BOLD, 13));
                table.getColumnModel().getColumn(0).setCellRenderer(renderer);
                table.getColumnModel().getColumn(0).setMaxWidth(30);
                table.getColumnModel().getColumn(2).setMaxWidth(30);
                table.getColumnModel().getColumn(3).setMaxWidth(30);
                getContentPane().add(table, BorderLayout.CENTER);

                scrollPane.setBounds(10, 27, 324, 233);
                this.getContentPane().add(scrollPane);
                scrollPane.setViewportView(table);

                showDialogs("keywords");
                break;
            }
            case "favoritesDlg": {
                this.setBounds(640, 215, 800, 400);
                this.setTitle("Favorites");
                this.setLocationRelativeTo(Gui.favoritesBtn);

                Object[] columns = {"", "title", "added", "link", " "};
                model = new DefaultTableModel(new Object[][]{
                }, columns) {
                    final boolean[] columnEditable = new boolean[]{false, false, false, false, true};

                    public boolean isCellEditable(int row, int column) {
                        return columnEditable[column];
                    }

                    // Сортировка
                    final Class[] types_unique = {Integer.class, String.class, String.class, String.class, Button.class};

                    @Override
                    public Class getColumnClass(int columnIndex) {
                        return this.types_unique[columnIndex];
                    }
                };
                table = new JTable(model);
                table.getColumn(" ").setCellRenderer(new ButtonColumn(table, 4));
                table.setAutoCreateRowSorter(true);
                DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
                renderer.setHorizontalAlignment(JLabel.CENTER);
                table.setRowHeight(20);
                table.setFont(new Font("SansSerif", Font.PLAIN, 13));
                JTableHeader header = table.getTableHeader();
                header.setFont(new Font("Tahoma", Font.BOLD, 13));
                table.getColumnModel().getColumn(0).setCellRenderer(renderer);
                table.getColumnModel().getColumn(0).setMaxWidth(30);
                table.getColumnModel().getColumn(2).setPreferredWidth(130);
                table.getColumnModel().getColumn(2).setMaxWidth(130);
                table.getColumnModel().getColumn(3).setMaxWidth(70);
                table.getColumnModel().getColumn(4).setMaxWidth(30);
                table.setColumnSelectionAllowed(true);
                table.setCellSelectionEnabled(true);
                table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                getContentPane().add(table, BorderLayout.CENTER);

                // открытие вкладки двойным кликом
                table.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            int row = table.convertRowIndexToModel(table.rowAtPoint(new Point(e.getX(), e.getY()))); // при сортировке строк оставляет верные данные
                            int col = table.columnAtPoint(new Point(e.getX(), e.getY()));
                            if (col == 1 || col == 3) {
                                String url = (String) table.getModel().getValueAt(row, 3);
                                Gui.openPage(url);
                            }
                        }
                    }
                });

                scrollPane.setBounds(10, 27, 324, 233);
                this.getContentPane().add(scrollPane);
                scrollPane.setViewportView(table);

                showDialogs("favorites");
                break;
            }
        }

        // делаем фокус на окно, чтобы работал захват клавиш
        this.requestFocusInWindow();

    }

    // Заполнение диалоговых окон данными
    private void showDialogs(String name) {
        int id = 0;
        JdbcQueries jdbcQueries = new JdbcQueries();
        switch (name) {
            case "smi": {
                java.util.List<Source> sources = jdbcQueries.getSources("all");
                for (Source s : sources) {
                    Dialogs.model.addRow(new Object[]{++id, s.getSource(), s.getIsActive()});
                }
                break;
            }
            case "excl": {
                java.util.List<Excluded> excludes = jdbcQueries.getExcludedWords();

                for (Excluded excluded : excludes) {
                    Object[] row = new Object[]{++id, excluded.getWord()};
                    Dialogs.model.addRow(row);
                }
                break;
            }
            case "title-excl": {
                java.util.List<Excluded> excludes = jdbcQueries.getExcludedTitlesWords();

                for (Excluded excluded : excludes) {
                    Object[] row = new Object[]{++id, excluded.getWord()};
                    Dialogs.model.addRow(row);
                }
                break;
            }
            case "keywords": {
                List<Keyword> keywords = jdbcQueries.getKeywords(2);
                for (Keyword keyword : keywords) {
                    Dialogs.model.addRow(new Object[]{++id, keyword.getKeyword(), keyword.getIsActive()});
                }
                break;
            }
            case "favorites": {
                List<Favorite> favorites = jdbcQueries.getFavorites();
                for (Favorite favorite : favorites) {
                    Dialogs.model.addRow(new Object[]{++id, favorite.getTitle(), favorite.getDate(), favorite.getLink()});
                }
                break;
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    // Закрываем диалоговые окна клавишей ESC
    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            setVisible(false);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}