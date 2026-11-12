package com.mathtowerdefense.gui;

import com.mathtowerdefense.engine.SoundManager;
import com.mathtowerdefense.entities.Player;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * End-of-game results screen (GAME OVER / VICTORY) with the final statistics
 * and the buttons from the spec: Play Again / Leaderboard / Main Menu.
 */
public class GameOverGUI extends JPanel {

    /** Snapshot of everything the results screen needs. */
    public static final class GameResult {
        public final Player player;
        public final int waveReached;

        public GameResult(Player player, int waveReached) {
            this.player = player;
            this.waveReached = waveReached;
        }
    }

    private final MainGUI main;
    private final JLabel headline = new JLabel("GAME OVER", JLabel.CENTER);
    private final JLabel statsLabel = new JLabel(" ", JLabel.CENTER);
    private final JLabel saveLabel = new JLabel(" ", JLabel.CENTER);

    public GameOverGUI(MainGUI main) {
        this.main = main;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        Theme.pad(content, 60, 40, 50, 40);

        headline.setFont(Theme.titleFont());
        headline.setForeground(Theme.DANGER);
        headline.setAlignmentX(CENTER_ALIGNMENT);

        statsLabel.setFont(Theme.bodyFont());
        statsLabel.setForeground(Theme.TEXT);
        statsLabel.setAlignmentX(CENTER_ALIGNMENT);

        saveLabel.setFont(Theme.smallFont());
        saveLabel.setForeground(Theme.TEXT_DIM);
        saveLabel.setAlignmentX(CENTER_ALIGNMENT);

        content.add(Box.createVerticalGlue());
        content.add(headline);
        content.add(Box.createVerticalStrut(24));
        content.add(statsLabel);
        content.add(Box.createVerticalStrut(16));
        content.add(saveLabel);
        content.add(Box.createVerticalStrut(34));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        buttons.setOpaque(false);

        JButton again = Theme.primaryButton("PLAY AGAIN");
        again.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            main.startGame();
        });
        JButton leaderboard = Theme.button("LEADERBOARD", Theme.PANEL_LIGHT);
        leaderboard.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            main.showCard(MainGUI.CARD_LEADERBOARD);
        });
        JButton menu = Theme.button("MAIN MENU", new java.awt.Color(120, 40, 40));
        menu.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            main.showCard(MainGUI.CARD_MENU);
        });
        buttons.add(again);
        buttons.add(leaderboard);
        buttons.add(menu);
        buttons.setAlignmentX(CENTER_ALIGNMENT);
        content.add(buttons);
        content.add(Box.createVerticalGlue());

        add(content, BorderLayout.CENTER);
    }

    /** Fills the screen with the final statistics of the finished run. */
    public void setResult(boolean victory, GameResult result, String saveStatus) {
        Player p = result.player;
        headline.setText(victory ? "VICTORY!" : "GAME OVER");
        headline.setForeground(victory ? Theme.SUCCESS : Theme.DANGER);
        statsLabel.setText(String.format(
                "<html><center>"
                + "<b>Final score:</b> %,d<br><br>"
                + "Wave reached: %d &nbsp;|&nbsp; Level: %d<br>"
                + "Enemies defeated: %d<br>"
                + "Correct answers: %d &nbsp;|&nbsp; Incorrect: %d<br>"
                + "Accuracy: %.1f%%<br>"
                + "Coins: %d &nbsp;|&nbsp; Waves completed: %d"
                + "</center></html>",
                p.getScore(),
                result.waveReached,
                p.getLevel(),
                p.getEnemiesDefeated(),
                p.getCorrectAnswers(),
                p.getIncorrectAnswers(),
                p.getAccuracy(),
                p.getCoins(),
                p.getWavesCompleted()));

        saveLabel.setText(saveStatus);
    }
}
