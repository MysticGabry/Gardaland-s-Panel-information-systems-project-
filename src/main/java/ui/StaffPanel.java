package ui;

import db.Db;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

import static costants_values.Costants.*;

public class StaffPanel extends JFrame {
    private final int userId;
    private final String fullName;

    private final JComboBox<String> tableCombo = new JComboBox<>();
    private final JButton loadBtn = new JButton("Load");
    private final JButton addAvailBtn = new JButton("Add availability"); // NEW
    private final JTable table = new JTable();
    private final JLabel welcome = new JLabel("", SwingConstants.LEFT);

    public StaffPanel(int userId, String fullName) {
        super("Gardaland – Staff Portal");
        this.userId = userId;
        this.fullName = fullName;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(true);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        welcome.setText("Welcome, " + fullName);
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 14f));

        JPanel picker = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        picker.add(new JLabel("Table:"));
        picker.add(tableCombo);
        picker.add(loadBtn);
        picker.add(addAvailBtn);

        top.add(welcome, BorderLayout.WEST);
        top.add(picker, BorderLayout.EAST);
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
        add(top, BorderLayout.NORTH);

        add(new JScrollPane(table), BorderLayout.CENTER);

        loadBtn.addActionListener(ae -> loadSelectedTable());
        addAvailBtn.addActionListener(ae -> openAvailabilityDialog());
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
                    if (!rs.getString("TABLE_NAME").equals("staff_users"))
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

        String sql = "SELECT * FROM `" + tableName + "` LIMIT 100";

        loadBtn.setEnabled(false);
        new SwingWorker<DefaultTableModel, Void>() {
            @Override
            protected DefaultTableModel doInBackground() throws Exception {
                try (Connection conn = Db.getConnection();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(sql)) {
                    return util.TableModels.resultSetToTableModel(rs);
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

    private void openAvailabilityDialog() {
        String text = JOptionPane.showInputDialog(
                this,
                """
                        Type availability:
                        Example: monday (AM),tuesday (PM), Saturday (FULL) """,
                "Add availability",
                JOptionPane.QUESTION_MESSAGE
        );
        if (text == null) return;
        text = text.trim();
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please type something.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        saveAvailability(text);
    }

    private void saveAvailability(String availabilityText) {
        String tableName = (String) tableCombo.getSelectedItem();
        if (tableName == null || tableName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Select a table first.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sql = "INSERT INTO `" + tableName + "` (staff_id, full_name, availability) VALUES (?, ?, ?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, fullName);
            ps.setString(3, availabilityText);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Availability saved.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            loadSelectedTable();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

}