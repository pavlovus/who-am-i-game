package com.whoami.server.admin;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

/**
 * Swing (Java SE) admin panel for the server. All real logic lives in
 * {@link AdminService}; this class is only the view/controller wiring and
 * auto-refreshes the tables every two seconds.
 */
public class AdminConsole extends JFrame {

    private final AdminService adminService;

    private final DefaultTableModel connectionsModel =
            new DefaultTableModel(new Object[]{"Session", "User", "Auth", "Admin"}, 0);
    private final DefaultTableModel usersModel =
            new DefaultTableModel(new Object[]{"Id", "Username", "Played", "Won", "Blocked"}, 0);
    private final DefaultTableModel charactersModel =
            new DefaultTableModel(new Object[]{"Id", "Name", "Category", "Status"}, 0);

    private final JLabel statsLabel = new JLabel("Stats: -");

    public AdminConsole(AdminService adminService) {
        super("WhoAmI - Admin Console");
        this.adminService = adminService;

        // Closing the panel must not kill the server JVM / network listener.
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(720, 480);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Connections", buildConnectionsTab());
        tabs.addTab("Users", buildUsersTab());
        tabs.addTab("Characters", buildCharactersTab());

        add(tabs, BorderLayout.CENTER);
        add(statsLabel, BorderLayout.SOUTH);

        Timer timer = new Timer(2000, e -> refresh());
        timer.start();
        refresh();
    }

    private JPanel buildConnectionsTab() {
        JPanel panel = new JPanel(new BorderLayout());
        JTable table = new JTable(connectionsModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton kick = new JButton("Kick selected");
        kick.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                int sessionId = (int) connectionsModel.getValueAt(row, 0);
                adminService.kickConnection(sessionId);
                refresh();
            }
        });
        panel.add(buttonBar(kick), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildUsersTab() {
        JPanel panel = new JPanel(new BorderLayout());
        JTable table = new JTable(usersModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton block = new JButton("Block");
        block.addActionListener(e -> toggleBlock(table, true));
        JButton unblock = new JButton("Unblock");
        unblock.addActionListener(e -> toggleBlock(table, false));
        panel.add(buttonBar(block, unblock), BorderLayout.SOUTH);
        return panel;
    }

    private void toggleBlock(JTable table, boolean blocked) {
        int row = table.getSelectedRow();
        if (row >= 0) {
            int userId = (int) usersModel.getValueAt(row, 0);
            if (blocked) {
                adminService.blockUser(userId);
            } else {
                adminService.unblockUser(userId);
            }
            refresh();
        }
    }

    private JPanel buildCharactersTab() {
        JPanel panel = new JPanel(new BorderLayout());
        JTable table = new JTable(charactersModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JButton add = new JButton("Add");
        add.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Character name:");
            if (name != null && !name.isBlank()) {
                String category = JOptionPane.showInputDialog(this, "Category:");
                adminService.addCharacter(name.trim(), category == null ? "" : category.trim());
                refresh();
            }
        });
        JButton approve = new JButton("Approve");
        approve.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                adminService.approveCharacter((int) charactersModel.getValueAt(row, 0));
                refresh();
            }
        });
        JButton delete = new JButton("Delete");
        delete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                adminService.deleteCharacter((int) charactersModel.getValueAt(row, 0));
                refresh();
            }
        });
        panel.add(buttonBar(add, approve, delete), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buttonBar(JButton... buttons) {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JButton button : buttons) {
            bar.add(button);
        }
        return bar;
    }

    private void refresh() {
        SwingUtilities.invokeLater(() -> {
            connectionsModel.setRowCount(0);
            adminService.listConnections().forEach(c -> connectionsModel.addRow(
                    new Object[]{c.sessionId(), c.username() == null ? "-" : c.username(),
                            c.authenticated(), c.admin()}));

            usersModel.setRowCount(0);
            adminService.listUsers().forEach(u -> usersModel.addRow(
                    new Object[]{u.id(), u.username(), u.gamesPlayed(), u.gamesWon(), u.blocked()}));

            charactersModel.setRowCount(0);
            adminService.listCharacters().forEach(ch -> charactersModel.addRow(
                    new Object[]{ch.id(), ch.name(), ch.category(), ch.status()}));

            SystemStats s = adminService.systemStats();
            statsLabel.setText(String.format(
                    " Connections: %d | Users: %d (blocked %d) | Characters: %d (approved %d)",
                    s.activeConnections(), s.totalUsers(), s.blockedUsers(),
                    s.totalCharacters(), s.approvedCharacters()));
        });
    }
}
