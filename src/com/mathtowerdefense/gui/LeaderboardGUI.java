package com.mathtowerdefense.gui;

import com.mathtowerdefense.engine.SoundManager;
import com.mathtowerdefense.managers.ScoreService;
import com.mathtowerdefense.models.Score;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/**
 * Leaderboard screen: highest scores from the database (via
 * {@link ScoreService}), sorted by score - Rank / Username / Score / Level /
 * Wave / Enemies Defeated.
 */
public class LeaderboardGUI extends JPanel {

    private static final String[] COLUMNS = { "Rank", "Username", "Score", "Level", "Wave", "Enemies" };

    private final MainGUI main;
    private final ScoreService scoreService;
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false; // read-only table
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel messageLabel = new JLabel(" ", JLabel.CENTER);

    public LeaderboardGUI(MainGUI main, ScoreService scoreService) {
        this.main = main;
        this.scoreService = scoreService;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PANEL);
        Theme.pad(header, 18, 30, 18, 30);

        JLabel title = new JLabel("LEADERBOARD");
        title.setFont(Theme.titleFont());
        title.setForeground(Theme.GOLD);

        JButton back = Theme.button("BACK", Theme.PANEL_LIGHT);
        back.setPreferredSize(new Dimension(130, 40));
        back.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            main.showCard(MainGUI.CARD_MENU);
        });

        header.add(title, BorderLayout.WEST);
        header.add(back, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Table styling for the dark theme.
        table.setFont(Theme.bodyFont());
        table.setRowHeight(30);
        table.getTableHeader().setFont(Theme.bodyFont().deriveFont(Font.BOLD));
        table.getTableHeader().setBackground(Theme.PANEL_LIGHT);
        table.getTableHeader().setForeground(Theme.TEXT);
        table.setBackground(Theme.PANEL);
        table.setForeground(Theme.TEXT);
        table.setGridColor(new Color(50, 60, 80));
        table.setSelectionBackground(Theme.PANEL_LIGHT);
        table.setFillsViewportHeight(true);

        DefaultTableCellRenderer centred = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(CENTER);
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? Theme.PANEL : Theme.PANEL_LIGHT);
                }
                return c;
            }
        };
        for (int i = 0; i < COLUMNS.length; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centred);
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(Theme.BG);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 30, 10, 30));
        scroll.setPreferredSize(new Dimension(700, 360));

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(scroll, BorderLayout.CENTER);
        messageLabel.setFont(Theme.smallFont());
        messageLabel.setForeground(Theme.WARNING);
        Theme.pad(messageLabel, 6, 30, 16, 30);
        center.add(messageLabel, BorderLayout.SOUTH);

        add(center, BorderLayout.CENTER);
    }

    /** Reloads the top scores each time the screen is shown. */
    public void refresh() {
        List<Score> scores = scoreService.loadLeaderboard(10);
        model.setRowCount(0);

        if (scores.isEmpty()) {
            messageLabel.setText(scoreService.getLastError() != null
                    ? scoreService.getLastError()
                    : "No scores yet - be the first on the leaderboard!");
            return;
        }
        messageLabel.setText(scoreService.getLastError() != null
                ? scoreService.getLastError() : " ");

        int rank = 1;
        for (Score s : scores) {
            model.addRow(new Object[] {
                    "#" + rank++,
                    s.getUsername(),
                    String.format("%,d", s.getScore()),
                    s.getLevel(),
                    s.getWave(),
                    s.getEnemiesDefeated()
            });
        }
    }
}
