package com.mathtowerdefense.gui;

import com.mathtowerdefense.engine.GameConfig;
import com.mathtowerdefense.engine.SoundManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Settings screen: sound on/off, music on/off and difficulty.
 * Values are stored in the {@link GameConfig} singleton, which the engine
 * reads when a new game starts.
 */
public class SettingsGUI extends JPanel {

    private final MainGUI main;
    private final JCheckBox soundBox = new JCheckBox("Sound effects");
    private final JCheckBox musicBox = new JCheckBox("Background music");
    private final JComboBox<String> difficultyBox =
            new JComboBox<>(new String[] { "Easy", "Normal", "Hard" });

    public SettingsGUI(MainGUI main) {
        this.main = main;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel content = Theme.panel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        Theme.pad(content, 40, 70, 40, 70);

        JLabel title = new JLabel("SETTINGS");
        title.setFont(Theme.titleFont());
        title.setForeground(Theme.ACCENT);
        title.setAlignmentX(CENTER_ALIGNMENT);

        styleRow(soundBox);
        styleRow(musicBox);

        JPanel diffRow = new JPanel();
        diffRow.setOpaque(false);
        diffRow.setLayout(new BoxLayout(diffRow, BoxLayout.X_AXIS));
        JLabel diffLabel = new JLabel("Difficulty  ");
        diffLabel.setFont(Theme.bodyFont());
        diffLabel.setForeground(Theme.TEXT);
        difficultyBox.setFont(Theme.bodyFont());
        diffRow.add(diffLabel);
        diffRow.add(difficultyBox);
        diffRow.setAlignmentX(CENTER_ALIGNMENT);

        JLabel note = new JLabel("<html><center>Changes apply to the next game.<br>"
                + "Music plays when resources/sounds/music.wav exists.</center></html>");
        note.setFont(Theme.smallFont());
        note.setForeground(Theme.TEXT_DIM);
        note.setAlignmentX(CENTER_ALIGNMENT);

        JButton save = Theme.primaryButton("SAVE");
        save.setMaximumSize(new Dimension(240, 46));
        save.setAlignmentX(CENTER_ALIGNMENT);
        save.addActionListener(e -> applyAndGoBack());

        JButton back = Theme.button("BACK", Theme.PANEL_LIGHT);
        back.setMaximumSize(new Dimension(240, 46));
        back.setAlignmentX(CENTER_ALIGNMENT);
        back.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            main.showCard(MainGUI.CARD_MENU);
        });

        content.add(Box.createVerticalGlue());
        content.add(title);
        content.add(Box.createVerticalStrut(30));
        content.add(soundBox);
        content.add(Box.createVerticalStrut(12));
        content.add(musicBox);
        content.add(Box.createVerticalStrut(12));
        content.add(diffRow);
        content.add(Box.createVerticalStrut(16));
        content.add(note);
        content.add(Box.createVerticalStrut(24));
        content.add(save);
        content.add(Box.createVerticalStrut(12));
        content.add(back);
        content.add(Box.createVerticalGlue());

        add(content, BorderLayout.CENTER);
        loadCurrent();
    }

    private void styleRow(JCheckBox box) {
        box.setFont(Theme.bodyFont());
        box.setForeground(Theme.TEXT);
        box.setAlignmentX(CENTER_ALIGNMENT);
    }

    /** Loads the current settings into the controls. */
    private void loadCurrent() {
        GameConfig cfg = GameConfig.getInstance();
        soundBox.setSelected(cfg.isSoundEnabled());
        musicBox.setSelected(cfg.isMusicEnabled());
        difficultyBox.setSelectedIndex(cfg.getDifficulty());
    }

    private void applyAndGoBack() {
        GameConfig cfg = GameConfig.getInstance();
        cfg.setSoundEnabled(soundBox.isSelected());
        cfg.setMusicEnabled(musicBox.isSelected());
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
}
