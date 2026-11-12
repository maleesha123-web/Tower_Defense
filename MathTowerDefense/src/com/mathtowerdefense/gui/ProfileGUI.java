package com.mathtowerdefense.gui;

import com.mathtowerdefense.authentication.SessionManager;
import com.mathtowerdefense.engine.SoundManager;
import com.mathtowerdefense.managers.ScoreService;
import com.mathtowerdefense.models.PlayerStats;
import com.mathtowerdefense.models.User;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.time.format.DateTimeFormatter;

/**
 * Modern arcade-styled Player Profile screen.
 * Displays user identity, combat rank, and lifetime statistics retrieved via
 * {@link ScoreService} inside a sleek, translucent, centered card.
 */
public class ProfileGUI extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM yyyy");

    private final MainGUI main;
    private final ScoreService scoreService;

    // Hero user banner components
    private final AvatarBadge avatarBadge = new AvatarBadge();
    private final JLabel usernameLabel = new JLabel("Player");
    private final JLabel emailLabel = new JLabel(" ");
    private final StatusPill statusPill = new StatusPill();
    private final RankBadge rankBadge = new RankBadge();

    // 6 Stat Tiles
    private final StatTile scoreTile;
    private final StatTile waveTile;
    private final StatTile gamesTile;
    private final StatTile killsTile;
    private final StatTile accuracyTile;
    private final StatTile challengesTile;

    private final AccuracyBar accuracyBar = new AccuracyBar();

    // Notification / Tip banner
    private final NoticeBanner noticeBanner = new NoticeBanner();

    // Action buttons
    private final JButton authButton;

    public ProfileGUI(MainGUI main, ScoreService scoreService) {
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
                g2.setColor(new Color(18, 23, 34, 238));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 22, 22));

                // Glowing cyan accent border
                g2.setColor(new Color(64, 156, 255, 75));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 22, 22));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0, 14));
        card.setPreferredSize(new Dimension(860, 615));
        Theme.pad(card, 20, 26, 18, 26);

        // ---- Header (Title & Subtitle) ----
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("PLAYER PROFILE", JLabel.CENTER);
        titleLabel.setFont(Theme.titleFont().deriveFont(Font.BOLD, 28f));
        titleLabel.setForeground(Theme.GOLD);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Combat Dossier & Lifetime Statistics", JLabel.CENTER);
        subtitleLabel.setFont(Theme.bodyFont().deriveFont(13.5f));
        subtitleLabel.setForeground(Theme.TEXT_DIM);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(3));
        headerPanel.add(subtitleLabel);
        card.add(headerPanel, BorderLayout.NORTH);

        // ---- Center Content (Hero Banner + Stats Grid + Notice Banner) ----
        JPanel centerPanel = new JPanel();
        centerPanel.setOpaque(false);
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // 1. Hero Identity Banner
        JPanel heroBanner = createHeroBanner();
        centerPanel.add(heroBanner);
        centerPanel.add(Box.createVerticalStrut(14));

        // 2. Stats Grid (2 rows x 3 columns)
        JPanel statsGrid = new JPanel(new GridLayout(2, 3, 14, 14));
        statsGrid.setOpaque(false);

        scoreTile = new StatTile("Highest Score", Theme.GOLD, null);
        waveTile = new StatTile("Deepest Wave", Theme.ACCENT, null);
        gamesTile = new StatTile("Games Played", new Color(175, 140, 255), null);
        killsTile = new StatTile("Enemies Defeated", Theme.SUCCESS, null);
        accuracyTile = new StatTile("Math Accuracy", Theme.SUCCESS, accuracyBar);
        challengesTile = new StatTile("Challenges Solved", new Color(90, 190, 255), null);

        statsGrid.add(scoreTile);
        statsGrid.add(waveTile);
        statsGrid.add(gamesTile);
        statsGrid.add(killsTile);
        statsGrid.add(accuracyTile);
        statsGrid.add(challengesTile);

        centerPanel.add(statsGrid);
        centerPanel.add(Box.createVerticalStrut(12));

        // 3. Notice / Tip Banner
        centerPanel.add(noticeBanner);

        card.add(centerPanel, BorderLayout.CENTER);

        // ---- Bottom Action Bar ----
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        buttonRow.setOpaque(false);

        JButton backBtn = Theme.button("BACK TO MENU", Theme.PANEL_LIGHT);
        backBtn.setPreferredSize(new Dimension(180, 44));
        backBtn.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            main.showCard(MainGUI.CARD_MENU);
        });

        JButton refreshBtn = Theme.button("REFRESH", Theme.ACCENT_DARK);
        refreshBtn.setPreferredSize(new Dimension(170, 44));
        refreshBtn.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            refresh();
        });

        authButton = Theme.button("CREATE ACCOUNT", new Color(40, 140, 85));
        authButton.setPreferredSize(new Dimension(190, 44));
        authButton.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            main.showCard(MainGUI.CARD_REGISTER);
        });

        buttonRow.add(backBtn);
        buttonRow.add(refreshBtn);
        buttonRow.add(authButton);
        buttonRow.setAlignmentX(CENTER_ALIGNMENT);

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

    /** Reloads the player identity and statistics each time the screen is displayed. */
    public void refresh() {
        SessionManager session = SessionManager.getInstance();
        User user = session.getCurrentUser();

        if (user == null || session.isGuest()) {
            // Guest or unauthenticated state
            avatarBadge.setInitial("G");
            usernameLabel.setText(user != null ? user.getUsername() : "Guest Player");
            emailLabel.setText("Guest Session (Stats not saved)");
            statusPill.setStatus("GUEST SESSION", new Color(160, 110, 20), new Color(255, 230, 150));
            rankBadge.setRank("NOVICE", new Color(150, 160, 180), "Unranked Defender");

            scoreTile.setValue("0");
            scoreTile.setSubtext("Personal Best Record");

            waveTile.setValue("Wave 0");
            waveTile.setSubtext("Highest Wave Cleared");

            gamesTile.setValue("0");
            gamesTile.setSubtext("Total Battles Launched");

            killsTile.setValue("0");
            killsTile.setSubtext("Invaders Obliterated");

            accuracyTile.setValue("0.0%");
            accuracyTile.setValueColor(Theme.TEXT_DIM);
            accuracyTile.setSubtext("Combat Problem Accuracy");
            accuracyBar.setPercent(0.0);

            challengesTile.setValue("0 / 0");
            challengesTile.setSubtext("0 Correct  |  0 Mistakes");

            noticeBanner.showWarning("Guest Mode: Scores & stats are not saved permanently. "
                    + "Create a free account to rank on the leaderboard!");
            authButton.setText("CREATE ACCOUNT");
            authButton.setVisible(true);
            return;
        }

        // Registered user state
        String username = user.getUsername() != null ? user.getUsername() : "Player";
        String initial = username.length() > 0 ? username.substring(0, 1).toUpperCase() : "P";
        avatarBadge.setInitial(initial);

        usernameLabel.setText(username);
        emailLabel.setText(user.getEmail() != null ? user.getEmail() : "Registered Defender");
        statusPill.setStatus("REGISTERED", new Color(30, 130, 70), new Color(180, 255, 200));

        PlayerStats stats = scoreService.loadProfile(user);
        String lastError = scoreService.getLastError();

        // Calculate Combat Rank
        String rank = computeRank(stats.getHighestScore(), stats.getHighestWave());
        Color rankColor = computeRankColor(rank);
        String memberSubtext = "Defender #" + user.getId();
        if (user.getCreatedAt() != null) {
            memberSubtext += "  |  Joined " + user.getCreatedAt().format(DATE_FMT);
        }
        rankBadge.setRank(rank, rankColor, memberSubtext);

        // Populate Stat Tiles
        scoreTile.setValue(String.format("%,d", stats.getHighestScore()));
        scoreTile.setSubtext("Personal Best Record");

        waveTile.setValue("Wave " + stats.getHighestWave());
        waveTile.setSubtext("Highest Wave Cleared");

        gamesTile.setValue(String.valueOf(stats.getGamesPlayed()));
        gamesTile.setSubtext("Total Battles Launched");

        killsTile.setValue(String.format("%,d", stats.getTotalEnemiesDefeated()));
        killsTile.setSubtext("Invaders Obliterated");

        double accuracy = stats.getAccuracy();
        accuracyTile.setValue(String.format("%.1f%%", accuracy));
        Color accColor = accuracy >= 80.0 ? Theme.SUCCESS : (accuracy >= 60.0 ? Theme.ACCENT : Theme.WARNING);
        accuracyTile.setValueColor(accColor);
        accuracyTile.setSubtext(stats.getTotalQuestions() > 0 ? "Combat Problem Accuracy" : "No questions answered yet");
        accuracyBar.setPercent(accuracy);

        challengesTile.setValue(stats.getCorrectAnswers() + " / " + stats.getTotalQuestions());
        challengesTile.setSubtext(stats.getCorrectAnswers() + " Correct  |  " + stats.getIncorrectAnswers() + " Mistakes");

        authButton.setVisible(false);

        if (lastError != null) {
            noticeBanner.showWarning(lastError);
        } else if (stats.getGamesPlayed() == 0) {
            noticeBanner.showInfo("Welcome to Math Tower Defense! Play your first campaign to unlock combat records and climb the leaderboard.");
        } else {
            noticeBanner.showTip("Tactical Tip: Solve math challenges quickly to grant your towers bonus attack power and score multipliers!");
        }
    }

    /** Creates the top hero identity panel displaying avatar, user details, and rank badge. */
    private JPanel createHeroBanner() {
        JPanel banner = new JPanel(new BorderLayout(18, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Dark tile background
                g2.setColor(new Color(24, 31, 46, 215));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));

                // Glowing border
                g2.setColor(new Color(50, 70, 105, 130));
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        banner.setOpaque(false);
        Theme.pad(banner, 12, 18, 12, 18);
        banner.setMaximumSize(new Dimension(840, 84));
        banner.setPreferredSize(new Dimension(840, 84));

        // Left: Avatar
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPanel.setOpaque(false);
        leftPanel.add(avatarBadge);
        banner.add(leftPanel, BorderLayout.WEST);

        // Center: User Details (Name, Email, Status Pill)
        JPanel detailsPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        detailsPanel.setOpaque(false);

        JPanel nameRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        nameRow.setOpaque(false);

        usernameLabel.setFont(Theme.headingFont().deriveFont(Font.BOLD, 20f));
        usernameLabel.setForeground(Theme.TEXT);
        nameRow.add(usernameLabel);
        nameRow.add(statusPill);

        JPanel emailRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        emailRow.setOpaque(false);

        emailLabel.setFont(Theme.smallFont().deriveFont(13f));
        emailLabel.setForeground(Theme.TEXT_DIM);
        emailRow.add(emailLabel);

        detailsPanel.add(nameRow);
        detailsPanel.add(emailRow);
        banner.add(detailsPanel, BorderLayout.CENTER);

        // Right: Combat Rank Badge
        banner.add(rankBadge, BorderLayout.EAST);

        return banner;
    }

    private static String computeRank(int highestScore, int highestWave) {
        if (highestScore >= 20000 || highestWave >= 10) {
            return "GRANDMASTER";
        } else if (highestScore >= 12000 || highestWave >= 8) {
            return "VETERAN TACTICIAN";
        } else if (highestScore >= 8000 || highestWave >= 6) {
            return "ELITE GUARDIAN";
        } else if (highestScore >= 4000 || highestWave >= 4) {
            return "DEFENDER";
        } else if (highestScore > 0 || highestWave >= 1) {
            return "RECRUIT";
        } else {
            return "NOVICE";
        }
    }

    private static Color computeRankColor(String rank) {
        return switch (rank) {
            case "GRANDMASTER" -> new Color(255, 215, 0); // Gold
            case "VETERAN TACTICIAN" -> new Color(185, 115, 255); // Epic Purple
            case "ELITE GUARDIAN" -> new Color(64, 165, 255); // Rare Cyan
            case "DEFENDER" -> new Color(46, 204, 113); // Green
            case "RECRUIT" -> new Color(205, 215, 230); // Silver
            default -> new Color(140, 150, 170); // Dim
        };
    }

    // -------------------------------------------------------------------------
    // Styled Inner UI Components
    // -------------------------------------------------------------------------

    /** Circular avatar badge showing the initial of the user. */
    private static class AvatarBadge extends JComponent {
        private String initial = "P";

        AvatarBadge() {
            setPreferredSize(new Dimension(56, 56));
            setMinimumSize(new Dimension(56, 56));
        }

        void setInitial(String initial) {
            this.initial = (initial != null && !initial.isEmpty()) ? initial : "P";
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int size = Math.min(getWidth(), getHeight()) - 4;
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;

            // Gradient background
            GradientPaint gp = new GradientPaint(x, y, new Color(45, 125, 235),
                    x + size, y + size, new Color(20, 60, 140));
            g2.setPaint(gp);
            g2.fill(new Ellipse2D.Double(x, y, size, size));

            // Glowing border
            g2.setColor(new Color(110, 190, 255, 200));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(new Ellipse2D.Double(x, y, size, size));

            // Letter
            g2.setColor(Color.WHITE);
            g2.setFont(Theme.headingFont().deriveFont(Font.BOLD, 24f));
            java.awt.FontMetrics fm = g2.getFontMetrics();
            int tx = x + (size - fm.stringWidth(initial)) / 2;
            int ty = y + (size - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(initial, tx, ty);

            g2.dispose();
        }
    }

    /** Rounded status badge pill (e.g., "REGISTERED" or "GUEST"). */
    private static class StatusPill extends JComponent {
        private String text = "REGISTERED";
        private Color bgColor = new Color(30, 130, 70);
        private Color fgColor = new Color(180, 255, 200);

        StatusPill() {
            setPreferredSize(new Dimension(135, 24));
        }

        void setStatus(String text, Color bg, Color fg) {
            this.text = text;
            this.bgColor = bg;
            this.fgColor = fg;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), bgColor.getBlue(), 180));
            g2.fill(new RoundRectangle2D.Double(0, 1, getWidth(), getHeight() - 2, 12, 12));

            g2.setColor(bgColor.brighter());
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Double(0, 1, getWidth() - 1, getHeight() - 3, 12, 12));

            g2.setColor(fgColor);
            g2.setFont(Theme.smallFont().deriveFont(Font.BOLD, 10.5f));
            java.awt.FontMetrics fm = g2.getFontMetrics();
            String display = "\u25CF  " + text;
            int tx = (getWidth() - fm.stringWidth(display)) / 2;
            int ty = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(display, tx, ty);

            g2.dispose();
        }
    }

    /** Combat Rank box in the upper right corner of the hero banner. */
    private static class RankBadge extends JPanel {
        private final JLabel categoryLabel = new JLabel("COMBAT RANK", JLabel.RIGHT);
        private final JLabel rankLabel = new JLabel("NOVICE", JLabel.RIGHT);
        private final JLabel subtextLabel = new JLabel("Unranked Defender", JLabel.RIGHT);

        RankBadge() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            categoryLabel.setFont(Theme.smallFont().deriveFont(Font.BOLD, 10f));
            categoryLabel.setForeground(Theme.TEXT_DIM);
            categoryLabel.setAlignmentX(RIGHT_ALIGNMENT);

            rankLabel.setFont(Theme.bodyFont().deriveFont(Font.BOLD, 15f));
            rankLabel.setForeground(Theme.GOLD);
            rankLabel.setAlignmentX(RIGHT_ALIGNMENT);

            subtextLabel.setFont(Theme.smallFont().deriveFont(11.5f));
            subtextLabel.setForeground(Theme.TEXT_DIM);
            subtextLabel.setAlignmentX(RIGHT_ALIGNMENT);

            add(Box.createVerticalGlue());
            add(categoryLabel);
            add(Box.createVerticalStrut(2));
            add(rankLabel);
            add(Box.createVerticalStrut(2));
            add(subtextLabel);
            add(Box.createVerticalGlue());
        }

        void setRank(String rank, Color color, String subtext) {
            rankLabel.setText(rank);
            rankLabel.setForeground(color);
            subtextLabel.setText(subtext);
        }
    }

    /** Individual styled metric card tile. */
    private static class StatTile extends JPanel {
        private final JLabel titleLabel = new JLabel();
        private final JLabel valueLabel = new JLabel("0");
        private final JLabel subtextLabel = new JLabel(" ");
        private final Color accentColor;

        StatTile(String title, Color accentColor, JComponent extra) {
            this.accentColor = accentColor;
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

            titleLabel.setText(title.toUpperCase());
            titleLabel.setFont(Theme.smallFont().deriveFont(Font.BOLD, 11f));
            titleLabel.setForeground(Theme.TEXT_DIM);
            titleLabel.setAlignmentX(LEFT_ALIGNMENT);

            valueLabel.setFont(Theme.headingFont().deriveFont(Font.BOLD, 22f));
            valueLabel.setForeground(accentColor);
            valueLabel.setAlignmentX(LEFT_ALIGNMENT);

            subtextLabel.setFont(Theme.smallFont().deriveFont(11.5f));
            subtextLabel.setForeground(Theme.TEXT_DIM);
            subtextLabel.setAlignmentX(LEFT_ALIGNMENT);

            add(titleLabel);
            add(Box.createVerticalStrut(4));
            add(valueLabel);
            if (extra != null) {
                add(Box.createVerticalStrut(6));
                extra.setAlignmentX(LEFT_ALIGNMENT);
                add(extra);
                add(Box.createVerticalStrut(4));
            } else {
                add(Box.createVerticalStrut(4));
            }
            add(subtextLabel);
        }

        void setValue(String value) {
            valueLabel.setText(value);
        }

        void setValueColor(Color color) {
            valueLabel.setForeground(color);
        }

        void setSubtext(String subtext) {
            subtextLabel.setText(subtext);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Tile background
            g2.setColor(new Color(24, 31, 46, 215));
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));

            // Top accent color bar
            g2.setColor(accentColor);
            g2.fill(new RoundRectangle2D.Double(12, 0, Math.max(1, getWidth() - 24), 3, 3, 3));

            // Subtle outline
            g2.setColor(new Color(50, 65, 95, 115));
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));

            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Smooth rounded progress bar for visual accuracy presentation. */
    private static class AccuracyBar extends JComponent {
        private double percent = 0.0;

        AccuracyBar() {
            setPreferredSize(new Dimension(160, 7));
            setMaximumSize(new Dimension(240, 7));
        }

        void setPercent(double percent) {
            this.percent = Math.max(0.0, Math.min(100.0, percent));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            // Track background
            g2.setColor(new Color(36, 46, 68));
            g2.fill(new RoundRectangle2D.Double(0, 0, w, h, h, h));

            // Filled progress
            if (percent > 0) {
                int fillW = (int) Math.round((w * percent) / 100.0);
                fillW = Math.max(h, fillW);
                Color fillCol = percent >= 80.0 ? Theme.SUCCESS : (percent >= 60.0 ? Theme.ACCENT : Theme.WARNING);
                g2.setColor(fillCol);
                g2.fill(new RoundRectangle2D.Double(0, 0, fillW, h, h, h));
            }
            g2.dispose();
        }
    }

    /** Informational, tip, or warning banner strip. */
    private static class NoticeBanner extends JPanel {
        private final JLabel label = new JLabel(" ", JLabel.CENTER);
        private Color borderColor = new Color(64, 156, 255, 80);
        private Color bgColor = new Color(20, 28, 42, 190);

        NoticeBanner() {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
            setMaximumSize(new Dimension(840, 42));
            setPreferredSize(new Dimension(840, 42));

            label.setFont(Theme.smallFont().deriveFont(12.5f));
            label.setForeground(Theme.TEXT);
            label.setHorizontalAlignment(JLabel.CENTER);
            add(label, BorderLayout.CENTER);
        }

        void showInfo(String text) {
            label.setText(text);
            label.setForeground(Theme.TEXT);
            borderColor = new Color(64, 156, 255, 90);
            bgColor = new Color(20, 28, 44, 200);
            repaint();
        }

        void showTip(String text) {
            label.setText(text);
            label.setForeground(new Color(200, 220, 250));
            borderColor = new Color(75, 140, 225, 90);
            bgColor = new Color(20, 28, 44, 200);
            repaint();
        }

        void showWarning(String text) {
            label.setText("[!]  " + text);
            label.setForeground(Theme.GOLD);
            borderColor = new Color(241, 196, 15, 120);
            bgColor = new Color(38, 30, 20, 210);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(bgColor);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 12, 12));

            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 12, 12));

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
