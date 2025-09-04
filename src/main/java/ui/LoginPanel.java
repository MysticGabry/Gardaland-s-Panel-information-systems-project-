package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;

import static costants_values.Costants.*;


public class LoginPanel extends JPanel {
    private static final String EMAIL_REGEX =
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PASSWORD_REGEX =
            "^(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{6,}$";

    private final JTextField emailField;
    private final JPasswordField passwordField;
    private final JButton loginButton;


    public LoginPanel() {
        setLayout(new BorderLayout(8, 8));

        JLabel headerLabel = new JLabel("Gardaland Staff Login", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        add(headerLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridLayout(2, 2, 6, 6));
        JLabel emailLabel = new JLabel("Email:");
        JLabel passLabel = new JLabel("Password:");
        emailField = new JTextField(20);
        passwordField = new JPasswordField(20);

        centerPanel.add(emailLabel);
        centerPanel.add(emailField);
        centerPanel.add(passLabel);
        centerPanel.add(passwordField);

        add(centerPanel, BorderLayout.CENTER);

        loginButton = new JButton("Login");
        loginButton.addActionListener(this::onLogin);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(loginButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void onLogin(ActionEvent e) {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (!email.matches(EMAIL_REGEX)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Invalid email format.",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        if (!password.matches(PASSWORD_REGEX)) {
            JOptionPane.showMessageDialog(
                    this,
                    """
                            Password must be at least 6 characters and include:
                            - at least one uppercase letter
                            - at least one number
                            - at least one special symbol""",
                    "Validation Error",
                    JOptionPane.ERROR_MESSAGE
            );

        } else {
            try {
                if (!isAdmin(email, password)) {
                    if (authenticate(email, password)) {
                        ui.StaffPanel staffPanel = new ui.StaffPanel();
                        SwingUtilities.invokeLater(() ->{
                            staffPanel.setVisible(true);
                            this.setVisible(false);
                        });
                    } else {
                        JOptionPane.showMessageDialog(
                                this,
                                "Try entering the data correctly again, otherwise contact the admin for user registration.",
                                "User is not registered",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                } else {
                    //TODO open admin special frame

                }

            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }


    }

    private boolean authenticate(String email, String password) throws SQLException {
        String sql = "SELECT 1 FROM staff_users WHERE email = ? AND password = ? LIMIT 1";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean isAdmin(String email, String password) {
        return email.equalsIgnoreCase("admin@gmail.com") && password.equalsIgnoreCase("Admin.4dmin");
    }

    public static void main(String... args) {
        SwingUtilities.invokeLater(() -> {
            JFrame loginFrame = new JFrame("Gardaland Staff Portal");
            loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            loginFrame.setSize(1000, 460);
            loginFrame.setResizable(false);
            loginFrame.add(new LoginPanel());
            loginFrame.pack();
            loginFrame.setLocationRelativeTo(null);
            loginFrame.setVisible(true);

        });
    }
}
