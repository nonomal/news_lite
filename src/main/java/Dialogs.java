import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class Dialogs extends JDialog implements KeyListener {
    static JTextArea textAreaForDialogs;
    static JTable table;
    static DefaultTableModel model;

    public Dialogs(String p_file) {
        textAreaForDialogs = new JTextArea();
        textAreaForDialogs.setFont(new Font("Dialog", Font.PLAIN, 13));
        textAreaForDialogs.setTabSize(10);
        textAreaForDialogs.setEditable(false);
        textAreaForDialogs.setLineWrap(true);
        textAreaForDialogs.setWrapStyleWord(true);
        textAreaForDialogs.setBounds(12, 27, 22, 233);

        if (p_file.equals("smiDlg")) {
            this.setResizable(false);
            this.setFont(new Font("Tahoma", Font.PLAIN, 14));
            this.setBounds(600, 200, 250, 300);
            this.getContentPane().setLayout(new BorderLayout(0, 0));
            this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            this.addKeyListener(this);
            this.setVisible(true);
            this.setTitle("Sources");
            this.setLocationRelativeTo(Gui.smiBtn);
            final JScrollPane scrollPane = new JScrollPane();
            Object[] columns = {"Num", "Source", "Del"};
            model = new DefaultTableModel(new Object[][]{
            }, columns) {final boolean[] columnEditables = new boolean[]{false, false, true};
                public boolean isCellEditable(int row, int column) {
                    return columnEditables[column];
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
            //table.setColumnSelectionAllowed(true);
            //table.setCellSelectionEnabled(true);
            table.setFont(new Font("SansSerif", Font.PLAIN, 13));
            JTableHeader header = table.getTableHeader();
            header.setFont(new Font("Tahoma", Font.BOLD, 13));
            table.getColumnModel().getColumn(0).setCellRenderer(renderer);
            table.getColumnModel().getColumn(0).setMaxWidth(40);
            table.getColumnModel().getColumn(2).setMaxWidth(40);
            table.setForeground(Color.black);
            table.setSelectionForeground(new Color(26, 79, 164));
            table.setSelectionBackground(new Color(255, 255, 160));
            getContentPane().add(table, BorderLayout.CENTER);

            scrollPane.setBounds(10, 27, 324, 233);
            this.getContentPane().add(scrollPane);
            scrollPane.setViewportView(table);

            Common.showDialog("smi");
        } else if (p_file.equals("logDlg")){
            this.setResizable(false);
            this.setFont(new Font("Tahoma", Font.PLAIN, 14));
            this.setBounds(600, 200, 350, 300);
            this.getContentPane().setLayout(new BorderLayout(0, 0));
            this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            this.addKeyListener(this);
            this.setVisible(true);
            this.setTitle("Log");
            this.setLocationRelativeTo(Gui.logBtn);
            final JScrollPane scrollPane = new JScrollPane();
            scrollPane.setBounds(10, 27, 324, 233);
            this.getContentPane().add(scrollPane);
            scrollPane.setViewportView(textAreaForDialogs);
            Common.showDialog("log");
        } else if (p_file.equals("exclDlg")){
            this.setResizable(false);
            this.setFont(new Font("Tahoma", Font.PLAIN, 14));
            this.setBounds(600, 200, 250, 300);
            this.getContentPane().setLayout(new BorderLayout(0, 0));
            this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            this.addKeyListener(this);
            this.setVisible(true);
            this.setTitle("Excluded words");
            this.setLocationRelativeTo(Gui.exclBtn);
            final JScrollPane scrollPane = new JScrollPane();
            Object[] columns = {"Num", "Word", "Del"};
            model = new DefaultTableModel(new Object[][]{
            }, columns) {final boolean[] columnEditables = new boolean[]{false, false, true};
                public boolean isCellEditable(int row, int column) {
                    return columnEditables[column];
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
            //table.setColumnSelectionAllowed(true);
            //table.setCellSelectionEnabled(true);
            table.setFont(new Font("SansSerif", Font.PLAIN, 13));
            JTableHeader header = table.getTableHeader();
            header.setFont(new Font("Tahoma", Font.BOLD, 13));
            table.getColumnModel().getColumn(0).setCellRenderer(renderer);
            table.getColumnModel().getColumn(0).setMaxWidth(40);
            table.getColumnModel().getColumn(2).setMaxWidth(40);
            table.setForeground(Color.black);
            table.setSelectionForeground(new Color(26, 79, 164));
            table.setSelectionBackground(new Color(255, 255, 160));
            getContentPane().add(table, BorderLayout.CENTER);

            scrollPane.setBounds(10, 27, 324, 233);
            this.getContentPane().add(scrollPane);
            scrollPane.setViewportView(table);

            Common.showDialog("excl");
        }
        // делаем фокус на окно, чтобы работал захват клавиш
        this.requestFocusInWindow();
        this.setVisible(true);
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