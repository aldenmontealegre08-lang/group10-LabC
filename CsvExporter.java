package com.pos;
import javax.swing.*;
import java.awt.*;
import java.io.*;
public final class CsvExporter {
    public static void exportTable(Component parent, JTable table, String fileName) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(fileName));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter writer = new PrintWriter(chooser.getSelectedFile(), "UTF-8")) {
            for (int c = 0; c < table.getColumnCount(); c++) {
                if (c > 0) writer.print(",");
                writer.print(quote(table.getColumnName(c)));
            }
            writer.println();
            for (int r = 0; r < table.getRowCount(); r++) {
                for (int c = 0; c < table.getColumnCount(); c++) {
                    if (c > 0) writer.print(",");
                    writer.print(quote(String.valueOf(table.getValueAt(r, c))));
                }
                writer.println();
            }
            UIUtils.info(parent, "CSV exported successfully.");
        } catch (Exception ex) { UIUtils.error(parent, ex); }
    }
    private static String quote(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
    private CsvExporter() { }
}
