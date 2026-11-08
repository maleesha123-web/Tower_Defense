package com.mathtowerdefense.gui;

import com.mathtowerdefense.authentication.SessionManager;
import com.mathtowerdefense.engine.GameConfig;
import com.mathtowerdefense.engine.SoundManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Main menu: title, welcome line and the navigation buttons from the spec
 * (Play / Profile / Leaderboard / Help / Settings / Logout).
 */
public class MainMenuGUI extends JPanel {

    private final MainGUI main;
    private final JLabel welcomeLabel = new JLabel(" ");

    public MainMenuGUI(MainGUI main) {
        this.main = main;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setOpaque(false);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        Theme.pad(content, 50, 40, 40, 40);

        JLabel title = new JLabel("MATH TOWER DEFENSE 2D");
        title.setFont(Theme.titleFont());
        title.setForeground(Theme.ACCENT);
        title.setAlignmentX(CENTER_ALIGNMENT);

        welcomeLabel.setFont(Theme.bodyFont());
        welcomeLabel.setForeground(Theme.TEXT_DIM);
        welcomeLabel.setAlignmentX(CENTER_ALIGNMENT);

        JLabel hint = new JLabel("Answer math challenges to power up your towers!");
        hint.setFont(Theme.smallFont());
        hint.setForeground(Theme.TEXT_DIM);
        hint.setAlignmentX(CENTER_ALIGNMENT);

        content.add(Box.createVerticalGlue());
        content.add(title);
        content.add(Box.createVerticalStrut(8));
        content.add(welcomeLabel);
        content.add(Box.createVerticalStrut(4));
        content.add(hint);
        content.add(Box.createVerticalStrut(32));

        content.add(menuButton("PLAY GAME", Theme.ACCENT_DARK, main::startGame));
        content.add(Box.createVerticalStrut(12));
        content.add(menuButton("PROFILE", Theme.PANEL_LIGHT,
                () -> main.showCard(MainGUI.CARD_PROFILE)));
        content.add(Box.createVerticalStrut(12));
        content.add(menuButton("LEADERBOARD", Theme.PANEL_LIGHT,
                () -> main.showCard(MainGUI.CARD_LEADERBOARD)));
        content.add(Box.createVerticalStrut(12));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 14, 0));
        row.setOpaque(false);
        JButton help = Theme.button("HELP", Theme.PANEL_LIGHT);
        help.setPreferredSize(new Dimension(170, 46));
        help.addActionListener(e -> main.showCard(MainGUI.CARD_HELP));
        JButton settings = Theme.button("SETTINGS", Theme.PANEL_LIGHT);
        settings.setPreferredSize(new Dimension(170, 46));
        settings.addActionListener(e -> main.showCard(MainGUI.CARD_SETTINGS));
        JButton logout = Theme.button("LOGOUT", new Color(120, 40, 40));
        logout.setPreferredSize(new Dimension(170, 46));
        logout.addActionListener(e -> main.logout());
        row.add(help);
        row.add(settings);
        row.add(logout);
        row.setAlignmentX(CENTER_ALIGNMENT);
        content.add(row);
        content.add(Box.createVerticalGlue());

        add(content, BorderLayout.CENTER);
    }

    private JButton menuButton(String text, Color color, Runnable action) {
        JButton b = Theme.button(text, color);
        b.setMaximumSize(new Dimension(300, 50));
        b.setAlignmentX(CENTER_ALIGNMENT);
        b.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            action.run();
        });
        return b;
    }

    /** Refreshes the welcome line each time the menu is shown. */
    public void refresh() {
        SessionManager session = SessionManager.getInstance();
        String who = session.isGuest() ? "Guest" : session.getDisplayUsername();
        welcomeLabel.setText("Welcome, " + who + "   |   Difficulty: "
                + GameConfig.getInstance().getDifficultyLabel());
    }
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        Theme.paintCoverBackground(g2, Theme.getMenuBackground(), getWidth(), getHeight(), 0.40f);
        g2.dispose();
        super.paintComponent(g);
    }
}