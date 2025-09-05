package ui;

import db.Db;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

import static costants_values.Costants.*;

public class AdminPanel extends JFrame {
    private final int userId;
    private final String fullName;

    private final JComboBox<String> tableCombo = new JComboBox<>();
    private final JButton loadBtn = new JButton("Load");
    private final JButton addShiftBtn = new JButton("Add shift");
    private final JButton deleteShiftBtn = new JButton("Delete shift");
    private final JButton addEmployeebtn = new JButton("Add employee");
    private final JButton deleteEmployeeBtn = new JButton("Delete employee");

    private final JTable table = new JTable();
    private final JLabel welcome = new JLabel("", SwingConstants.LEFT);


    public AdminPanel(int userId, String fullName) {
        super("Gardaland – Staff Portal");
        this.userId = userId;
        this.fullName = fullName;

        tableCombo.addActionListener(e -> {
            String sel = (String) tableCombo.getSelectedItem();
            addEmployeebtn.setEnabled("staff_users".equalsIgnoreCase(sel));
            deleteEmployeeBtn.setEnabled("staff_users".equalsIgnoreCase(sel));
        });
        String sel0 = (String) tableCombo.getSelectedItem();
        addEmployeebtn.setEnabled("staff_users".equalsIgnoreCase(sel0));
        deleteEmployeeBtn.setEnabled("staff_users".equalsIgnoreCase(sel0));


        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(true);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        welcome.setText("Welcome, " + fullName);
        welcome.setFont(welcome.getFont().deriveFont(Font.BOLD, 14f));

        JPanel bottom = new JPanel(new BorderLayout(8, 8));


        JPanel picker = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        picker.add(new JLabel("Table:"));
        picker.add(tableCombo);
        picker.add(loadBtn);

        JPanel picker2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));

        picker2.add(addShiftBtn);
        picker2.add(deleteShiftBtn);
        picker2.add(addEmployeebtn);
        picker2.add(deleteEmployeeBtn);

        bottom.add(picker2, BorderLayout.NORTH);
        top.add(welcome, BorderLayout.WEST);
        top.add(picker, BorderLayout.EAST);
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

        add(top, BorderLayout.NORTH);
        add(bottom, BorderLayout.SOUTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        loadBtn.addActionListener(ae -> loadSelectedTable());
        addShiftBtn.addActionListener(ae -> addShift());
        deleteShiftBtn.addActionListener(ae -> deleteShift());
        addEmployeebtn.addActionListener(ae -> addEmployee());
        deleteEmployeeBtn.addActionListener(ae -> deleteEmployee());

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
                    JOptionPane.showMessageDialog(AdminPanel.this, "Load failed: " + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    loadBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void addShift() {
        String sel = (String) tableCombo.getSelectedItem();
        if (sel == null || sel.equalsIgnoreCase("staff_users")) {
            JOptionPane.showMessageDialog(this,
                    "Pick a different role table (ride_operator, park_manager, food_beverage, cleanup_crew, wardrobe_assistant).",
                    "Invalid context", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String input = JOptionPane.showInputDialog(
                this,
                "Add shift for an employee (staff_id, text):\n" +
                        "Example: 1, monday (AM)",
                "Add shift",
                JOptionPane.QUESTION_MESSAGE
        );
        if (input == null) return;
        input = input.trim();
        if (input.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please type something.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int comma = input.indexOf(',');
        if (comma < 0) {
            JOptionPane.showMessageDialog(this, "Format must be: <staff_id>, <shift text>", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String idPart = input.substring(0, comma).trim();
        String textPart = input.substring(comma + 1).trim().toLowerCase();
        int staffId;
        try {
            staffId = Integer.parseInt(idPart);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid staff_id: " + idPart, "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (textPart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Shift text cannot be empty.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String fullNameDb = fetchFullName(staffId);
        if (fullNameDb == null) {
            JOptionPane.showMessageDialog(this, "No user with id=" + staffId + " in staff_users.",
                    "Not found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        saveShift(staffId, fullNameDb, textPart);
    }

    private String fetchFullName(int staffId) {
        String sql = "SELECT first_name, last_name FROM staff_users WHERE id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1) + " " + rs.getString(2);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Lookup failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private void saveShift(int staffId, String fullNameFromDb, String shiftText) {
        String tableName = (String) tableCombo.getSelectedItem();
        if (tableName == null || tableName.isBlank()) {
            JOptionPane.showMessageDialog(this, "Select a table first.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        String sql = "INSERT INTO `" + tableName + "` (staff_id, full_name, availability) VALUES (?, ?, ?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.setString(2, fullNameFromDb);
            ps.setString(3, shiftText);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Shift saved.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            loadSelectedTable();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteShift() {
        String sel = (String) tableCombo.getSelectedItem();
        if (sel == null || sel.equalsIgnoreCase("staff_users")) {
            JOptionPane.showMessageDialog(this,
                    "Pick a role table (ride_operator, park_manager, food_beverage, cleanup_crew, wardrobe_assistant).",
                    "Invalid context", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String input = JOptionPane.showInputDialog(
                this,
                "Delete a single shift (staff_id, availability_text)\n" +
                        "Example: 1, saturday (AM)",
                "Delete shift",
                JOptionPane.QUESTION_MESSAGE
        );
        if (input == null) return;
        input = input.trim();
        if (input.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please type something.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int comma = input.indexOf(',');
        if (comma < 0) {
            JOptionPane.showMessageDialog(this, "Format must be: <staff_id>, <shift text>", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String idPart = input.substring(0, comma).trim();
        String textPart = input.substring(comma + 1).trim().toLowerCase();
        int staffId;
        try {
            staffId = Integer.parseInt(idPart);
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Invalid staff_id: " + idPart, "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (textPart.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Shift text cannot be empty.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String tableName = (String) tableCombo.getSelectedItem();
        String sql = "DELETE FROM `" + tableName + "` WHERE staff_id = ? AND availability = ?";

        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, staffId);
            ps.setString(2, textPart);
            int n = ps.executeUpdate();
            JOptionPane.showMessageDialog(this,
                    (n > 0 ? "Deleted rows: " + n : "No matching row found."),
                    "Done", JOptionPane.INFORMATION_MESSAGE);
            loadSelectedTable();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addEmployee() {
        JTextField email = new JTextField(22);
        JPasswordField pwd = new JPasswordField(22);
        JTextField first = new JTextField(16);
        JTextField last = new JTextField(16);

        JPanel p = new JPanel(new GridLayout(0, 2, 6, 6));
        p.add(new JLabel("Email:"));
        p.add(email);
        p.add(new JLabel("Password:"));
        p.add(pwd);
        p.add(new JLabel("First name:"));
        p.add(first);
        p.add(new JLabel("Last name:"));
        p.add(last);

        int res = JOptionPane.showConfirmDialog(this, p, "Add employee",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String sql = "INSERT INTO staff_users (email, password, first_name, last_name) VALUES (?,?,?,?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.getText().trim());
            ps.setString(2, new String(pwd.getPassword()));
            ps.setString(3, first.getText().trim());
            ps.setString(4, last.getText().trim());
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Employee added.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            loadSelectedTable();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Insert failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteEmployee() {
        String val = JOptionPane.showInputDialog(
                this,
                "Delete employee by id OR email.\n" +
                        "Examples: 5  or ...@gmail.com",
                "Delete employee",
                JOptionPane.QUESTION_MESSAGE
        );
        if (val == null) return;
        val = val.trim();
        if (val.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please type an id or email.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String sqlId = "DELETE FROM staff_users WHERE id = ?";
        String sqlEm = "DELETE FROM staff_users WHERE email = ?";

        try (Connection conn = Db.getConnection()) {
            int deleted = 0;
            try {
                int id = Integer.parseInt(val);
                try (PreparedStatement ps = conn.prepareStatement(sqlId)) {
                    ps.setInt(1, id);
                    deleted = ps.executeUpdate();
                }
            } catch (NumberFormatException nfe) {
                try (PreparedStatement ps = conn.prepareStatement(sqlEm)) {
                    ps.setString(1, val);
                    deleted = ps.executeUpdate();
                }
            }
            JOptionPane.showMessageDialog(this, "Deleted employees: " + deleted,
                    "Done", JOptionPane.INFORMATION_MESSAGE);
            loadSelectedTable();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Delete failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

}