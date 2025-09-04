import ui.LoginPanel;

import javax.swing.*;

public class App {
    public static void main(String... args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Gardaland Staff Portal");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setResizable(false);
            f.add(new LoginPanel());
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
