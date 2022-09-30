package gui;

import database.JdbcQueries;
import gui.buttons.Icons;

import javax.swing.FocusManager;
import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ButtonColumn extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener {
    final JTable table;
    final JButton renderButton;
    final JButton editButton;
    //String text;

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
            renderButton.setIcon(Icons.DELETE_UNIT);
        } else if (isSelected) {
            renderButton.setForeground(table.getSelectionForeground());
            //renderButton.setBackground(table.getSelectionBackground());
            renderButton.setIcon(Icons.DELETE_UNIT);
        } else {
            renderButton.setForeground(table.getForeground());
            renderButton.setBackground(UIManager.getColor("Button.background"));
            renderButton.setIcon(Icons.DELETE_UNIT);
        }
        //renderButton.setText((value == null) ? ";" : value.toString() );
        return renderButton;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        return editButton;
    }

    public Object getCellEditorValue() {
        try {
            return ButtonColumn.class.getMethod("getCellEditorValue");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public void actionPerformed(ActionEvent e) {
        JdbcQueries jdbcQueries = new JdbcQueries();
        fireEditingStopped();
        int rowWithSource = table.getSelectedRow();
        int rowWithExcludeWord = Gui.tableForAnalysis.getSelectedRow();
        int delRowWithExcludeWord = 0;
        int delRowWithExcludeTitlesWord = 0;
        int delKeyword = 0;

        // определяем активное окно
        Window window = FocusManager.getCurrentManager().getActiveWindow();
        int activeWindow = 0;
        if (window.toString().contains("Avandy")) {
            activeWindow = 1;
        }
        if (window.toString().contains("Sources")) {
            activeWindow = 2;
        }
        if (window.toString().contains("analysis")) {
            activeWindow = 3;
            delRowWithExcludeWord = Dialogs.table.getSelectedRow();
        }
        if (window.toString().contains("search")) {
            activeWindow = 4;
            delRowWithExcludeTitlesWord = Dialogs.table.getSelectedRow();
        }
        if (window.toString().contains("Keywords")) {
            activeWindow = 5;
            delKeyword = Dialogs.table.getSelectedRow();
        }

        // окно таблицы с анализом частоты слов на основной панели (добавляем в базу)
        if (activeWindow == 1 && rowWithExcludeWord != -1) {
            rowWithExcludeWord = Gui.tableForAnalysis.convertRowIndexToModel(rowWithExcludeWord);
            String source = (String) Gui.modelForAnalysis.getValueAt(rowWithExcludeWord, 0);
            // удаление из диалогового окна
            Gui.modelForAnalysis.removeRow(rowWithExcludeWord);
            // добавление в базу данных и файл excluded.txt
            jdbcQueries.addExcludedWord(source);
        }

        // окно источников RSS
        if (activeWindow == 2 && rowWithSource != -1) {
            rowWithSource = table.convertRowIndexToModel(rowWithSource);
            String source = (String) Dialogs.model.getValueAt(rowWithSource, 1);
            // удаление из диалогового окна
            Dialogs.model.removeRow(rowWithSource);
            jdbcQueries.deleteSource(source);
        }

        // окно с исключенными из анализа слов (удаляем из базы)
        if (activeWindow == 3 && delRowWithExcludeWord != -1) {
            delRowWithExcludeWord = Dialogs.table.convertRowIndexToModel(delRowWithExcludeWord);
            String source = (String) Dialogs.model.getValueAt(delRowWithExcludeWord, 1);
            // удаление из диалогового окна
            Dialogs.model.removeRow(delRowWithExcludeWord);
            jdbcQueries.deleteExcluded(source, 3);
        }

        // окно с исключенными из поиска слов (удаляем из базы)
        if (activeWindow == 4 && delRowWithExcludeTitlesWord != -1) {
            delRowWithExcludeTitlesWord = Dialogs.table.convertRowIndexToModel(delRowWithExcludeTitlesWord);
            String word = (String) Dialogs.model.getValueAt(delRowWithExcludeTitlesWord, 1);
            // удаление из диалогового окна
            Dialogs.model.removeRow(delRowWithExcludeTitlesWord);
            jdbcQueries.deleteExcluded(word, 4);
        }

        // окно с ключевыми словами (удаляем из базы)
        if (activeWindow == 5 && delKeyword != -1) {
            delKeyword = Dialogs.table.convertRowIndexToModel(delKeyword);
            String word = (String) Dialogs.model.getValueAt(delKeyword, 1);
            // удаление из диалогового окна
            Dialogs.model.removeRow(delKeyword);
            jdbcQueries.deleteExcluded(word, 5);
        }

    }

}