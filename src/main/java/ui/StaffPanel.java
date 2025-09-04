package ui;

import db.Db;
import util.TableModels;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

import static costants_values.Costants.*;

public class StaffPanel extends JFrame {
    private final JComboBox<String> tableCombo = new JComboBox<>();
    private final JButton loadBtn = new JButton("Load");
    private final JTable table = new JTable();
    private final JLabel welcome = new JLabel("", SwingConstants.LEFT);

    public StaffPanel() {
        super("Gardaland – Staff Portal");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(true);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        welcome.setText("Welcome");
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 14f));

        JPanel picker = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        picker.add(new JLabel("Table:"));
        picker.add(tableCombo);
        picker.add(loadBtn);

        top.add(welcome, BorderLayout.WEST);
        top.add(picker, BorderLayout.EAST);
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(top, BorderLayout.NORTH);

        // center
        add(new JScrollPane(table), BorderLayout.CENTER);

        // events
        loadBtn.addActionListener(ae -> loadSelectedTable());

        // fill table list
        loadTablesIntoCombo();

        setSize(900, 520);
        setLocationRelativeTo(null);
    }

    private void loadTablesIntoCombo() {
        tableCombo.removeAllItems();
        try (Connection conn = Db.getConnection()) {
            DatabaseMetaData md = conn.getMetaData();

            try (ResultSet rs = md.getTables(DB_NAME, null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tableCombo.addItem(rs.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Failed to list tables: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSelectedTable() {
        String tableName = (String) tableCombo.getSelectedItem();
        if (tableName == null || tableName.isEmpty()) return;

        String sql = "SELECT * " +
                    "FROM " + tableName;

        loadBtn.setEnabled(false);
        new SwingWorker<DefaultTableModel, Void>() {
            @Override
            protected DefaultTableModel doInBackground() throws Exception {
                try (Connection conn = Db.getConnection();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(sql)) {
                    return TableModels.resultSetToTableModel(rs);
                }
            }

            @Override
            protected void done() {
                try {
                    table.setModel(get());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StaffPanel.this, "Load failed: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    loadBtn.setEnabled(true);
                }
            }
        }.execute();
    }
}
