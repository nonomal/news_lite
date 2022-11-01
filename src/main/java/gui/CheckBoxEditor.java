package gui;

import database.JdbcQueries;

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
        JdbcQueries jdbcQueries = new JdbcQueries();
        this.fireEditingStopped();
        String item = (String) Dialogs.model.getValueAt(row, 1);
        String columnName = Dialogs.model.getColumnName(1);

        switch (columnName) {
            case "Source":
                jdbcQueries.updateIsActiveCheckboxes(checkBox.isSelected(), item, "rss");
                break;
            case "Keyword":
                jdbcQueries.updateIsActiveCheckboxes(checkBox.isSelected(), item, "keywords");
                break;
            case "Description":
                String type = (String) Dialogs.model.getValueAt(row, 0);
                jdbcQueries.updateIsActiveDates(checkBox.isSelected(), type, item);
                break;
        }
    }

}