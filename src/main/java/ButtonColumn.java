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
            int row = table.getSelectedRow();
            // для корректного определения строки после сортировки
            if (row != -1) {
                row = table.convertRowIndexToModel(row);
                String source = (String) Dialogs.model.getValueAt(row, 1);
                System.out.println(source);

                // удаление из диалогового окна
                Dialogs.model.removeRow(row);
                // удаление из файла sources.txt
                Common.delLine(source);
                // удаление из базы данных
                SQLite.deleteSource(source);
            }
        } catch (IOException io) {
            io.printStackTrace();
        }

    }
}