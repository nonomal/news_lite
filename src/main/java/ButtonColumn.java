import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

class ButtonColumn extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener {
    JTable table;
    JButton renderButton;
    JButton editButton;
    String text;

    public ButtonColumn(JTable table, int column) {
        super();
        this.table = table;
        renderButton = new JButton();

        editButton = new JButton();
        editButton.setFocusPainted(false);
        editButton.addActionListener(this);

        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(column).setCellRenderer(this);
        columnModel.getColumn(column).setCellEditor(this);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        if (hasFocus) {
            renderButton.setForeground(table.getForeground());
            renderButton.setBackground(UIManager.getColor("Button.background"));
            renderButton.setIcon(Gui.delete_ico);
        } else if (isSelected) {
            renderButton.setForeground(table.getSelectionForeground());
            //renderButton.setBackground(table.getSelectionBackground());
            renderButton.setIcon(Gui.delete_ico);
        } else {
            renderButton.setForeground(table.getForeground());
            renderButton.setBackground(UIManager.getColor("Button.background"));
            renderButton.setIcon(Gui.delete_ico);
        }
        //renderButton.setText((value == null) ? ";" : value.toString() );
        return renderButton;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        //text = (value == null) ? ";" : value.toString();
        //editButton.setText( text );
        return editButton;
    }

    public Object getCellEditorValue() {
        return text;
    }

    public void actionPerformed(ActionEvent e) {
        fireEditingStopped();
        try {
            int row_with_source = table.getSelectedRow();
            int row_with_exlude_word = Gui.table_for_analysis.getSelectedRow();
            int del_row_with_exlude_word = Dialogs.table.getSelectedRow();

            // определяем активное окно
            Window window = javax.swing.FocusManager.getCurrentManager().getActiveWindow();
            int activeWindow = 0;
            if (window.toString().contains("Avandy")) {
                activeWindow = 1;
            }
            if (window.toString().contains("Sources")) {
                activeWindow = 2;
            }
            if (window.toString().contains("Excluded")) {
                activeWindow = 3;
            }

            // окно таблицы с анализом частоты слов на основной панели (добавляем в базу)
            if (activeWindow == 1 && row_with_exlude_word != -1) {
                System.out.println(1);
                row_with_exlude_word = Gui.table_for_analysis.convertRowIndexToModel(row_with_exlude_word);
                String source = (String) Gui.model_for_analysis.getValueAt(row_with_exlude_word, 1);
                //System.out.println(source);

                // удаление из диалогового окна
                Gui.model_for_analysis.removeRow(row_with_exlude_word);
                // добавление в базу данных и файл excluded.txt
                SQLite.insertNewExcludedWord(source);
            }

            // окно источников RSS
            if (activeWindow == 2 && row_with_source != -1) {
                System.out.println(2);
                row_with_source = table.convertRowIndexToModel(row_with_source);
                String source = (String) Dialogs.model.getValueAt(row_with_source, 1);
                System.out.println(source);
                // удаление из диалогового окна
                Dialogs.model.removeRow(row_with_source);
                // удаление из файла sources.txt
                Common.delLine(source);
                // удаление из базы данных
                SQLite.deleteSource(source);
            }

            // окно с исключенными из анализа слов (удаляем из базы)
            if (activeWindow == 3 && del_row_with_exlude_word != -1) {
                System.out.println(3);
                del_row_with_exlude_word = Dialogs.table.convertRowIndexToModel(del_row_with_exlude_word);
                String source = (String) Dialogs.model.getValueAt(del_row_with_exlude_word, 1);
                System.out.println(source);

                // удаление из диалогового окна
                Dialogs.model.removeRow(del_row_with_exlude_word);

                // добавление в базу данных и файл excluded.txt
                //SQLite.insertNewExcludedWord(source);
            }
        } catch (IOException io) {
            io.printStackTrace();
        }

    }

}