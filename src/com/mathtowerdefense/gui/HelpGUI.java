package com.mathtowerdefense.gui;

import com.mathtowerdefense.engine.SoundManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Help screen: explains how to play (towers, enemies, math questions,
 * scoring, coins, waves, bosses, controls).
 */
public class HelpGUI extends JPanel {

    private static final String CONTENT = """
            HOW TO PLAY

            Goal: defend your base (the castle on the right) against waves of
            enemies that follow the winding road. If enemies reach the base,
            you lose base health. At 0 health the game is over - survive all
            10 waves to win!


            BUILDING TOWERS
            1. Pick a tower in the TOWER SHOP on the right.
            2. Click on the grass to place it (never on the road).
            3. Towers automatically attack enemies that enter their range.
            4. Click an existing tower to see its stats and UPGRADE it
               (levels 1-4: damage grows 10 -> 20 -> 35 -> 55 for the Basic
               Tower; upgrade prices increase with every level).

            Tower types:
            - Basic Tower:    cheap, balanced damage.
            - Rapid Tower:    low damage, very high rate of fire.
            - Power Tower:    expensive, slow, devastating hits.
            - Math Boost:     pays bonus coins + score for every correct answer!


            MATH CHALLENGES (the heart of the game)
            Every so often a MATH CHALLENGE appears and the battle pauses:
            - API MODE shows a picture from the Fish Game API - count the
              fish and type the number. The mode badge always tells you
              whether the question came from the API or from the offline
              fallback generator.
            - Correct answer: +50 score, +15 coins, combo increases, and your
              towers get MATH POWER (x1.5 damage) for 12 seconds!
            - Wrong answer or timeout (10 seconds): -25 score, -10 coins,
              -5 base HP, combo resets and towers are WEAKENED (x0.5 damage)
              for 8 seconds.
            - Combo bonuses: 3 in a row +100, 5 in a row +250, 10 in a row +600.

            If the question service is unreachable the game switches to
            OFFLINE FALLBACK MODE and generates arithmetic questions locally -
            it never pretends a local question came from the API.


            ENEMIES
            - Normal (N): medium health, normal speed.
            - Fast (F):   low health, high speed.
            - Tank (T):   very high health, slow.
            - Boss (B):   enormous health, huge reward - appears on waves 5
                          and 10 with escorts.


            SCORING & COINS
            - Enemy defeated:      +100 score (+ the enemy's coins)
            - Boss defeated:       +1000 score
            - Correct answer:      +50 score +15 coins
            - Wave completed:      +500 score +100 coins
            - Wrong answer:        -25 score, -10 coins
            Coins buy and upgrade towers - they can never drop below zero.


            CONTROLS
            - Mouse click:  place / select towers
            - Esc or P:     pause / resume
            - Buttons:      Pause, Resume, Restart, Main Menu


            SETTINGS
            Sound and music can be toggled in SETTINGS, and difficulty
            (Easy / Normal / Hard) changes enemy health, enemy speed and
            your starting coins and base health.
            """;

    public HelpGUI(MainGUI main) {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.PANEL);
        Theme.pad(header, 18, 30, 18, 30);

        JLabel title = new JLabel("HELP");
        title.setFont(Theme.titleFont());
        title.setForeground(Theme.ACCENT);

        JButton back = Theme.button("BACK", Theme.PANEL_LIGHT);
        back.setPreferredSize(new Dimension(130, 40));
        back.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            main.showCard(MainGUI.CARD_MENU);
        });

        header.add(title, BorderLayout.WEST);
        header.add(back, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        JTextArea text = new JTextArea(CONTENT);
        text.setEditable(false);
        text.setOpaque(false);
        text.setForeground(Theme.TEXT);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        text.setLineWrap(true);
        text.setWrapStyleWord(true);

        JScrollPane scroll = new JScrollPane(text);
        scroll.setBorder(BorderFactory.createEmptyBorder(16, 30, 20, 30));
        scroll.getViewport().setBackground(Theme.BG);
        add(scroll, BorderLayout.CENTER);
    }
}
