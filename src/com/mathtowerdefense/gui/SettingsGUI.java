package com.mathtowerdefense.gui;

import com.mathtowerdefense.engine.GameConfig;
import com.mathtowerdefense.engine.SoundManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Modern arcade-styled Settings screen:
 * - Sound effects toggle
 * - Background music toggle
 * - Game difficulty selection with dynamic economy & base HP breakdown
 * - Instant audio feedback and persistent settings storage via {@link GameConfig}
 */
public class SettingsGUI extends JPanel {

    private final MainGUI main;
    private final ModernToggle soundToggle = new ModernToggle(true);
    private final ModernToggle musicToggle = new ModernToggle(true);
    private final JComboBox<String> difficultyBox =
            new JComboBox<>(new String[] { "Easy", "Normal", "Hard" });
    private final JLabel diffDescLabel = new JLabel(" ");
    private final JLabel statusLabel = new JLabel(" ", JLabel.CENTER);

    public SettingsGUI(MainGUI main) {
        this.main = main;
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

                // Glowing accent border
                g2.setColor(new Color(64, 156, 255, 75));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 22, 22));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout(0, 16));
        card.setPreferredSize(new Dimension(660, 560));
        Theme.pad(card, 26, 34, 24, 34);

        // ---- Header (Title & Subtitle) ----
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("SETTINGS", JLabel.CENTER);
        titleLabel.setFont(Theme.titleFont().deriveFont(Font.BOLD, 30f));
        titleLabel.setForeground(Theme.GOLD);
        titleLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Audio & Game Preferences", JLabel.CENTER);
        subtitleLabel.setFont(Theme.bodyFont().deriveFont(14f));
        subtitleLabel.setForeground(Theme.TEXT_DIM);
        subtitleLabel.setAlignmentX(CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);
        card.add(headerPanel, BorderLayout.NORTH);

        // ---- Options container ----
        JPanel optionsPanel = new JPanel();
        optionsPanel.setOpaque(false);
        optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));

        // 1. Sound Effects Row
        soundToggle.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            statusLabel.setText(" ");
        });
        JPanel soundRow = createSettingRow(
                "Sound Effects",
                "Combat audio, tower shots & UI clicks",
                soundToggle,
                null
        );

        // 2. Background Music Row
        musicToggle.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            statusLabel.setText(" ");
        });
        JPanel musicRow = createSettingRow(
                "Background Music",
                "Atmospheric soundtrack & music loops",
                musicToggle,
                null
        );

        // 3. Difficulty Row
        styleDifficultyBox();
        difficultyBox.addActionListener(e -> {
            updateDifficultyDescription();
            statusLabel.setText(" ");
        });
        JPanel diffRow = createSettingRow(
                "Game Difficulty",
                "Starting resources & base defense level",
                difficultyBox,
                diffDescLabel
        );

        optionsPanel.add(Box.createVerticalGlue());
        optionsPanel.add(soundRow);
        optionsPanel.add(Box.createVerticalStrut(12));
        optionsPanel.add(musicRow);
        optionsPanel.add(Box.createVerticalStrut(12));
        optionsPanel.add(diffRow);
        optionsPanel.add(Box.createVerticalGlue());

        card.add(optionsPanel, BorderLayout.CENTER);

        // ---- Bottom Controls (Note + Status + Action Buttons) ----
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));

        JLabel noteLabel = new JLabel("Difficulty changes apply to the next new game.", JLabel.CENTER);
        noteLabel.setFont(Theme.smallFont());
        noteLabel.setForeground(Theme.TEXT_DIM);
        noteLabel.setAlignmentX(CENTER_ALIGNMENT);

        statusLabel.setFont(Theme.smallFont().deriveFont(Font.BOLD));
        statusLabel.setForeground(Theme.SUCCESS);
        statusLabel.setAlignmentX(CENTER_ALIGNMENT);
        Theme.pad(statusLabel, 2, 0, 6, 0);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        buttonRow.setOpaque(false);

        JButton saveBtn = Theme.primaryButton("SAVE SETTINGS");
        saveBtn.setPreferredSize(new Dimension(180, 44));
        saveBtn.addActionListener(e -> applyAndSave());

        JButton backBtn = Theme.button("BACK TO MENU", Theme.PANEL_LIGHT);
        backBtn.setPreferredSize(new Dimension(180, 44));
        backBtn.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            loadCurrent();
            main.showCard(MainGUI.CARD_MENU);
        });

        buttonRow.add(saveBtn);
        buttonRow.add(backBtn);
        buttonRow.setAlignmentX(CENTER_ALIGNMENT);

        bottomPanel.add(noteLabel);
        bottomPanel.add(Box.createVerticalStrut(4));
        bottomPanel.add(statusLabel);
        bottomPanel.add(buttonRow);
        card.add(bottomPanel, BorderLayout.SOUTH);

        add(card);
        loadCurrent();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        Theme.paintCoverBackground(g2, Theme.getMenuBackground(), getWidth(), getHeight(), 0.52f);
        g2.dispose();
    }

    /** Helper to create a consistent, sleek rounded settings row tile. */
    private JPanel createSettingRow(String title, String subtitle, JComponent control, JLabel dynamicSubtext) {
        int height = (dynamicSubtext != null) ? 80 : 66;

        JPanel row = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Tile background
                g2.setColor(new Color(24, 31, 46, 200));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 16, 16));
                // Subtle outline
                g2.setColor(new Color(50, 65, 95, 110));
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 16, 16));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        row.setOpaque(false);
        Theme.pad(row, 10, 18, 10, 18);
        row.setMaximumSize(new Dimension(580, height));
        row.setPreferredSize(new Dimension(580, height));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.bodyFont().deriveFont(Font.BOLD, 14.5f));
        titleLbl.setForeground(Theme.TEXT);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(Theme.smallFont());
        subLbl.setForeground(Theme.TEXT_DIM);

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(titleLbl);
        textPanel.add(Box.createVerticalStrut(2));
        textPanel.add(subLbl);

        if (dynamicSubtext != null) {
            dynamicSubtext.setFont(Theme.smallFont().deriveFont(Font.BOLD, 11.5f));
            dynamicSubtext.setForeground(Theme.ACCENT);
            textPanel.add(Box.createVerticalStrut(4));
            textPanel.add(dynamicSubtext);
        }
        textPanel.add(Box.createVerticalGlue());

        row.add(textPanel, BorderLayout.CENTER);

        JPanel controlWrap = new JPanel(new GridBagLayout());
        controlWrap.setOpaque(false);
        controlWrap.add(control);
        row.add(controlWrap, BorderLayout.EAST);

        return row;
    }

    /** Styles the JComboBox with the dark arcade theme. */
    private void styleDifficultyBox() {
        difficultyBox.setFont(Theme.bodyFont().deriveFont(Font.BOLD, 13.5f));
        difficultyBox.setPreferredSize(new Dimension(136, 36));
        difficultyBox.setMaximumSize(new Dimension(136, 36));
        difficultyBox.setFocusable(false);
        difficultyBox.setUI(new BasicComboBoxUI() {
            @Override
            public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(26, 34, 50));
                g2.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                g2.dispose();
            }

            @Override
            protected JButton createArrowButton() {
                JButton arrow = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(26, 34, 50));
                        g2.fillRect(0, 0, getWidth(), getHeight());
                        // Draw clean accent downward arrow
                        g2.setColor(Theme.ACCENT);
                        int cx = getWidth() / 2;
                        int cy = getHeight() / 2;
                        g2.fillPolygon(new int[] { cx - 5, cx + 5, cx }, new int[] { cy - 2, cy - 2, cy + 4 }, 3);
                        g2.dispose();
                    }
                };
                arrow.setBorder(BorderFactory.createEmptyBorder());
                arrow.setContentAreaFilled(false);
                arrow.setFocusPainted(false);
                return arrow;
            }
        });
        difficultyBox.setBackground(new Color(26, 34, 50));
        difficultyBox.setForeground(Theme.TEXT);
        difficultyBox.setBorder(BorderFactory.createLineBorder(new Color(64, 156, 255, 120), 1));
        difficultyBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                label.setFont(Theme.bodyFont().deriveFont(13f));
                label.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
                if (isSelected) {
                    label.setBackground(new Color(45, 95, 165));
                    label.setForeground(Color.WHITE);
                } else {
                    label.setBackground(new Color(26, 34, 50));
                    label.setForeground(Theme.TEXT);
                }
                return label;
            }
        });
    }

    private void updateDifficultyDescription() {
        int diff = difficultyBox.getSelectedIndex();
        switch (diff) {
            case GameConfig.DIFFICULTY_EASY ->
                diffDescLabel.setText("Coins: 350 | Base HP: 150 (Casual)");
            case GameConfig.DIFFICULTY_HARD ->
                diffDescLabel.setText("Coins: 250 | Base HP: 80 (Veteran)");
            default ->
                diffDescLabel.setText("Coins: 300 | Base HP: 100 (Balanced)");
        }
    }

    /** Reloads the current settings into the controls each time the screen is displayed. */
    public void refresh() {
        loadCurrent();
        statusLabel.setText(" ");
    }

    /** Loads the current settings into the controls. */
    private void loadCurrent() {
        GameConfig cfg = GameConfig.getInstance();
        soundToggle.setSelected(cfg.isSoundEnabled());
        musicToggle.setSelected(cfg.isMusicEnabled());
        difficultyBox.setSelectedIndex(cfg.getDifficulty());
        updateDifficultyDescription();
    }

    private void applyAndSave() {
        GameConfig cfg = GameConfig.getInstance();
        cfg.setSoundEnabled(soundToggle.isSelected());
        cfg.setMusicEnabled(musicToggle.isSelected());
        cfg.setDifficulty(difficultyBox.getSelectedIndex());

        // Apply music immediately so the toggle feels live.
        if (cfg.isMusicEnabled()) {
            SoundManager.getInstance().startMusic();
        } else {
            SoundManager.getInstance().stopMusic();
        }
        SoundManager.getInstance().play(SoundManager.Sound.CLICK);
        main.showCard(MainGUI.CARD_MENU);
    }

    /**
     * Modern pill toggle switch component with smooth circular knob.
     * Replaces standard clunky JCheckBox with an arcade aesthetic.
     */
    public static final class ModernToggle extends JToggleButton {
        private static final Color TRACK_ON = new Color(50, 140, 240);
        private static final Color TRACK_OFF = new Color(38, 46, 62);
        private static final Color BORDER_ON = new Color(100, 180, 255);
        private static final Color BORDER_OFF = new Color(60, 72, 98);

        public ModernToggle(boolean selected) {
            setSelected(selected);
            setPreferredSize(new Dimension(54, 28));
            setMinimumSize(new Dimension(54, 28));
            setMaximumSize(new Dimension(54, 28));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            boolean on = isSelected();

            // Pill background track
            g2.setColor(on ? TRACK_ON : TRACK_OFF);
            g2.fillRoundRect(2, 2, w - 4, h - 4, h - 4, h - 4);

            // Pill track border
            g2.setColor(on ? BORDER_ON : BORDER_OFF);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(2, 2, w - 4, h - 4, h - 4, h - 4);

            // Knob (circle)
            int knobSize = h - 10;
            int knobX = on ? (w - knobSize - 6) : 6;
            int knobY = 5;

            // Knob shadow
            g2.setColor(new Color(0, 0, 0, 50));
            g2.fillOval(knobX, knobY + 1, knobSize, knobSize);

            // Knob body
            g2.setColor(Color.WHITE);
            g2.fillOval(knobX, knobY, knobSize, knobSize);

            g2.dispose();
        }
    }
}
