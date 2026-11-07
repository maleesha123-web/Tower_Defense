package com.mathtowerdefense.gui;

import com.mathtowerdefense.authentication.SessionManager;
import com.mathtowerdefense.engine.SoundManager;
import com.mathtowerdefense.managers.ScoreService;
import com.mathtowerdefense.models.PlayerStats;
import com.mathtowerdefense.models.User;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Profile screen: lifetime statistics from the database via
 * {@link ScoreService} (never direct SQL in the GUI).
 */
public class ProfileGUI extends JPanel {

    private final MainGUI main;
    private final ScoreService scoreService;
    private final JLabel nameLabel = new JLabel(" ");
    private final JLabel statsLabel = new JLabel(" ");
    private final JLabel noteLabel = new JLabel(" ");

    public ProfileGUI(MainGUI main, ScoreService scoreService) {
        this.main = main;
        this.scoreService = scoreService;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        Theme.pad(content, 40, 50, 40, 50);

        JLabel title = new JLabel("PROFILE", JLabel.CENTER);
        title.setFont(Theme.titleFont());
        title.setForeground(Theme.ACCENT);
        title.setAlignmentX(CENTER_ALIGNMENT);

        nameLabel.setFont(Theme.headingFont());
        nameLabel.setForeground(Theme.TEXT);
        nameLabel.setAlignmentX(CENTER_ALIGNMENT);

        statsLabel.setFont(Theme.bodyFont());
        statsLabel.setForeground(Theme.TEXT);
        statsLabel.setAlignmentX(CENTER_ALIGNMENT);

        noteLabel.setFont(Theme.smallFont());
        noteLabel.setForeground(Theme.WARNING);
        noteLabel.setAlignmentX(CENTER_ALIGNMENT);

        JButton back = Theme.button("BACK", Theme.PANEL_LIGHT);
        back.setMaximumSize(new Dimension(200, 46));
        back.setAlignmentX(CENTER_ALIGNMENT);
        back.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            main.showCard(MainGUI.CARD_MENU);
        });

        content.add(Box.createVerticalGlue());
        content.add(title);
        content.add(Box.createVerticalStrut(24));
        content.add(nameLabel);
        content.add(Box.createVerticalStrut(14));
        content.add(statsLabel);
        content.add(Box.createVerticalStrut(12));
        content.add(noteLabel);
        content.add(Box.createVerticalStrut(28));
        content.add(back);
        content.add(Box.createVerticalGlue());

        add(content, BorderLayout.CENTER);
    }

    /** Reloads the statistics each time the screen is shown. */
    public void refresh() {
        SessionManager session = SessionManager.getInstance();
        User user = session.getCurrentUser();

        if (user == null) {
            nameLabel.setText("No active session");
            statsLabel.setText(" ");
            noteLabel.setText("Please log in first.");
            return;
        }
        if (session.isGuest()) {
            nameLabel.setText("Guest");
            statsLabel.setText(" ");
            noteLabel.setText("Guest session - create an account to track your statistics.");
            return;
        }

        PlayerStats stats = scoreService.loadProfile(user);
        noteLabel.setText(scoreService.getLastError() != null ? scoreService.getLastError() : " ");
        nameLabel.setText(user.getUsername() + "  (" + user.getEmail() + ")");

        statsLabel.setText(String.format(
                "<html><center>"
                + "Games played: %d<br>"
                + "Highest score: %,d<br>"
                + "Highest wave: %d<br>"
                + "Correct answers: %d<br>"
                + "Incorrect answers: %d<br>"
                + "Accuracy: %.1f%%<br>"
                + "Enemies defeated: %d"
                + "</center></html>",
                stats.getGamesPlayed(),
                stats.getHighestScore(),
                stats.getHighestWave(),
                stats.getCorrectAnswers(),
                stats.getIncorrectAnswers(),
                stats.getAccuracy(),
                stats.getTotalEnemiesDefeated()));
    }
}
