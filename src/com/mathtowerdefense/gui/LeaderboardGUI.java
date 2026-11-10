package com.mathtowerdefense.gui;

import com.mathtowerdefense.authentication.SessionManager;
import com.mathtowerdefense.engine.SoundManager;
import com.mathtowerdefense.managers.ScoreService;
import com.mathtowerdefense.models.Score;
import com.mathtowerdefense.models.User;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Modern arcade-styled Leaderboard screen.
 * Displays the top 10 high scores retrieved via {@link ScoreService} inside a
 * sleek, translucent, centered card with customized table rendering, gold/silver/bronze
 * rank badges, and responsive controls.
 */
public class LeaderboardGUI extends JPanel {

    private static final String[] COLUMNS = { "Rank", "Player", "Score", "Level", "Wave", "Enemies" };

    private final MainGUI main;
    private final ScoreService scoreService;
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JLabel messageLabel = new JLabel(" ", JLabel.CENTER);

    public LeaderboardGUI(MainGUI main, ScoreService scoreService) {
        this.main = main;
        this.scoreService = scoreService;

        setOpaque(false);
        setLayout(new GridBagLayout()); // Naturally centers the card panel

        // ---- Central translucent card panel ----
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Semi-translucent dark panel background
                g2.setColor(new Color(18, 23, 34, 235));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 22, 22));

                // Subtle glowing accent border
                g2.setColor(new Color(64, 156, 255, 75));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 22, 22));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0, 14));
        card.setPreferredSize(new Dimension(920, 610));
        Theme.pad(card, 20, 26, 18, 26);

        // ---- Card Header (Title & Subtitle) ----
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("LEADERBOARD", JLabel.CENTER);
        titleLabel.setFont(Theme.titleFont().deriveFont(Font.BOLD, 30f));
        titleLabel.setForeground(Theme.GOLD);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Hall of Fame - Top 10 High Scores & Defenses", JLabel.CENTER);
        subtitleLabel.setFont(Theme.bodyFont().deriveFont(14f));
        subtitleLabel.setForeground(Theme.TEXT_DIM);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);
        card.add(headerPanel, BorderLayout.NORTH);

        // ---- Table Styling ----
        table.setOpaque(false);
        table.setFont(Theme.bodyFont().deriveFont(13.5f));
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(40, 52, 75, 120));
        table.setSelectionBackground(new Color(45, 95, 165));
        table.setSelectionForeground(Color.WHITE);
        table.setFocusable(false);
        table.setRowSelectionAllowed(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setFillsViewportHeight(true);

        // Custom Table Header (bypasses Windows LAF default gray button renderer)
        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setReorderingAllowed(false);
        tableHeader.setResizingAllowed(false);
        tableHeader.setPreferredSize(new Dimension(0, 36));
        tableHeader.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, column);
                l.setOpaque(true);
                l.setBackground(new Color(26, 34, 50));
                l.setForeground(Theme.ACCENT);
                l.setFont(Theme.bodyFont().deriveFont(Font.BOLD, 13f));
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(64, 156, 255, 130)),
                        BorderFactory.createEmptyBorder(0, column == 1 ? 16 : 8, 0, column == 2 ? 16 : 8)
                ));
                if (column == 1) {
                    l.setHorizontalAlignment(LEFT);
                } else if (column == 2) {
                    l.setHorizontalAlignment(RIGHT);
                } else {
                    l.setHorizontalAlignment(CENTER);
                }
                return l;
            }
        });

        // Set explicit proportional column widths (sum = ~860px)
        int[] columnWidths = { 90, 250, 150, 95, 95, 140 };
        for (int i = 0; i < columnWidths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
        }

        // Custom Cell Renderer for all columns
        LeaderboardCellRenderer cellRenderer = new LeaderboardCellRenderer();
        for (int i = 0; i < COLUMNS.length; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getViewport().setBackground(new Color(18, 23, 34));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(45, 58, 82), 1));
        scrollPane.setPreferredSize(new Dimension(860, 385));

        // Modern sleek dark scrollbar UI
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(55, 72, 102);
                this.thumbHighlightColor = new Color(75, 100, 142);
                this.trackColor = new Color(18, 23, 34);
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(0, 0));
                b.setMinimumSize(new Dimension(0, 0));
                b.setMaximumSize(new Dimension(0, 0));
                return b;
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isDragging ? thumbHighlightColor : thumbColor);
                g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 1, thumbBounds.width - 2, thumbBounds.height - 2, 6, 6);
                g2.dispose();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                g.setColor(trackColor);
                g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            }
        });

        card.add(scrollPane, BorderLayout.CENTER);

        // ---- Bottom Controls (Status message + Action Buttons) ----
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        messageLabel.setFont(Theme.bodyFont().deriveFont(13f));
        messageLabel.setForeground(Theme.GOLD);
        messageLabel.setAlignmentX(CENTER_ALIGNMENT);
        Theme.pad(messageLabel, 6, 10, 8, 10);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonRow.setOpaque(false);

        JButton backBtn = Theme.button("BACK TO MENU", Theme.PANEL_LIGHT);
        backBtn.setPreferredSize(new Dimension(180, 44));
        backBtn.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            main.showCard(MainGUI.CARD_MENU);
        });

        JButton refreshBtn = Theme.button("REFRESH", Theme.ACCENT_DARK);
        refreshBtn.setPreferredSize(new Dimension(180, 44));
        refreshBtn.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            refresh();
        });

        buttonRow.add(backBtn);
        buttonRow.add(refreshBtn);
        buttonRow.setAlignmentX(CENTER_ALIGNMENT);

        bottomPanel.add(messageLabel);
        bottomPanel.add(buttonRow);
        card.add(bottomPanel, BorderLayout.SOUTH);

        add(card);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.paintCoverBackground(g2, Theme.getMenuBackground(), getWidth(), getHeight(), 0.52f);
        g2.dispose();
    }

    /** Reloads the top scores each time the screen is shown. */
    public void refresh() {
        List<Score> scores = scoreService.loadLeaderboard(10);
        model.setRowCount(0);

        String error = scoreService.getLastError();
        if (scores == null || scores.isEmpty()) {
            if (error != null) {
                messageLabel.setForeground(Theme.WARNING);
                messageLabel.setText("[!] " + error);
            } else {
                messageLabel.setForeground(Theme.GOLD);
                messageLabel.setText("No scores recorded yet - play a game to claim the #1 spot!");
            }
            return;
        }

        if (error != null) {
            messageLabel.setForeground(Theme.WARNING);
            messageLabel.setText("[!] " + error);
        } else {
            messageLabel.setText(" ");
        }

        int rank = 1;
        for (Score s : scores) {
            String username = s.getUsername() != null ? s.getUsername() : "Anonymous";
            model.addRow(new Object[] {
                    "#" + rank++,
                    username,
                    String.format("%,d", s.getScore()),
                    "Lv " + s.getLevel(),
                    "Wave " + s.getWave(),
                    s.getEnemiesDefeated() + " Kills"
            });
        }
    }

    /**
     * Custom cell renderer for the leaderboard table.
     * Handles column-specific alignments, bold podium colors (gold/silver/bronze),
     * and subtle alternating row backgrounds.
     */
    private static final class LeaderboardCellRenderer extends DefaultTableCellRenderer {
        private static final Color COLOR_GOLD = new Color(255, 215, 0);
        private static final Color COLOR_SILVER = new Color(225, 235, 250);
        private static final Color COLOR_BRONZE = new Color(245, 175, 110);
        private static final Color COLOR_GREEN = new Color(90, 220, 150);
        private static final Color ROW_EVEN = new Color(22, 28, 42, 220);
        private static final Color ROW_ODD = new Color(28, 36, 54, 220);
        private static final Color ROW_SELECTED = new Color(45, 95, 165);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            // Alternating row background
            if (!isSelected) {
                label.setBackground(row % 2 == 0 ? ROW_EVEN : ROW_ODD);
            } else {
                label.setBackground(ROW_SELECTED);
            }

            label.setFont(Theme.bodyFont().deriveFont(14f));
            label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

            // Check if this row belongs to the active logged-in user
            User currentUser = SessionManager.getInstance().getCurrentUser();
            boolean isCurrentUser = false;
            if (currentUser != null && table.getValueAt(row, 1) != null) {
                String playerInRow = table.getValueAt(row, 1).toString();
                isCurrentUser = currentUser.getUsername().equalsIgnoreCase(playerInRow);
            }

            switch (column) {
                case 0: // Rank
                    label.setHorizontalAlignment(CENTER);
                    label.setFont(Theme.bodyFont().deriveFont(Font.BOLD, 14f));
                    if (row == 0) {
                        label.setForeground(COLOR_GOLD);
                    } else if (row == 1) {
                        label.setForeground(COLOR_SILVER);
                    } else if (row == 2) {
                        label.setForeground(COLOR_BRONZE);
                    } else {
                        label.setForeground(Theme.TEXT_DIM);
                    }
                    break;

                case 1: // Player / Username
                    label.setHorizontalAlignment(LEFT);
                    label.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 10));
                    label.setFont(Theme.bodyFont().deriveFont(Font.BOLD, 14f));
                    if (isSelected) {
                        label.setForeground(Color.WHITE);
                    } else if (isCurrentUser) {
                        label.setForeground(Theme.ACCENT);
                    } else {
                        label.setForeground(Theme.TEXT);
                    }
                    break;

                case 2: // Score
                    label.setHorizontalAlignment(RIGHT);
                    label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 16));
                    label.setFont(Theme.bodyFont().deriveFont(Font.BOLD, 15f));
                    label.setForeground(isSelected ? Color.WHITE : Theme.GOLD);
                    break;

                case 3: // Level
                    label.setHorizontalAlignment(CENTER);
                    label.setFont(Theme.bodyFont());
                    label.setForeground(isSelected ? Color.WHITE : Theme.ACCENT);
                    break;

                case 4: // Wave
                    label.setHorizontalAlignment(CENTER);
                    label.setFont(Theme.bodyFont());
                    label.setForeground(isSelected ? Color.WHITE : Theme.TEXT);
                    break;

                case 5: // Enemies Defeated
                    label.setHorizontalAlignment(CENTER);
                    label.setFont(Theme.bodyFont());
                    label.setForeground(isSelected ? Color.WHITE : COLOR_GREEN);
                    break;

                default:
                    label.setHorizontalAlignment(CENTER);
                    label.setForeground(Theme.TEXT);
                    break;
            }

            return label;
        }
    }
}

