package gui;

import database.DatabaseQueries2;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class CheckBoxEditor extends DefaultCellEditor implements ItemListener {
    private final JCheckBox checkBox;
    private int row;

    public CheckBoxEditor(JCheckBox checkBox) {
        super(checkBox);
        this.checkBox = checkBox;
        this.checkBox.addItemListener(this);
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.row = row;
        checkBox.setSelected((Boolean) value);
        return super.getTableCellEditorComponent(table, value, isSelected, row, column);
    }

    public void itemStateChanged(ItemEvent e) {
        DatabaseQueries2 databaseQueries2 = new DatabaseQueries2();
        this.fireEditingStopped();
        String source = (String) Dialogs.model.getValueAt(row, 1);
        databaseQueries2.updateIsActiveStatus(checkBox.isSelected(), source);
    }

}