package com.mathtowerdefense.gui;

import com.mathtowerdefense.api.Question;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * The MATH CHALLENGE dialog shown over the battlefield.
 *
 * Displays the Fish Game API question (image: "How many fish are in the
 * picture?") or a clearly-labelled local fallback question, with an answer
 * field, a submit button and the countdown timer. The panel only renders
 * state provided by {@link GameGUI}; all game rules stay in the engine.
 */
public class MathChallengeOverlay extends JPanel {

    private final JLabel modeBadge = new JLabel(" ");
    private final JLabel warningLabel = new JLabel(" ");
    private final JLabel promptLabel = new JLabel(" ");
    private final QuestionImagePanel imagePanel = new QuestionImagePanel();
    private final JTextField answerField = Theme.field(10);
    private final JButton submitButton = Theme.primaryButton("SUBMIT");
    private final JLabel timerLabel = new JLabel("Time: --");
    private final JLabel loadingLabel = new JLabel("Loading question...");

    private Consumer<String> submitHandler = input -> { };

    public MathChallengeOverlay() {
        setLayout(new BorderLayout());
        setBackground(new Color(8, 10, 16, 235));

        // ---- header: title + timer ----
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        Theme.pad(header, 24, 40, 8, 40);

        JLabel title = new JLabel("MATH CHALLENGE");
        title.setFont(Theme.titleFont().deriveFont(Font.BOLD, 34f));
        title.setForeground(Theme.ACCENT);

        timerLabel.setFont(Theme.headingFont().deriveFont(Font.BOLD, 28f));
        timerLabel.setForeground(Theme.TEXT);
        header.add(title, BorderLayout.WEST);
        header.add(timerLabel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ---- centre: badges + image/prompt + answer ----
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        Theme.pad(center, 0, 40, 30, 40);

        modeBadge.setFont(Theme.smallFont().deriveFont(Font.BOLD, 13f));
        modeBadge.setAlignmentX(CENTER_ALIGNMENT);
        center.add(modeBadge);
        center.add(Box.createVerticalStrut(4));

        warningLabel.setFont(Theme.smallFont());
        warningLabel.setForeground(Theme.WARNING);
        warningLabel.setAlignmentX(CENTER_ALIGNMENT);
        warningLabel.setVisible(false);
        center.add(warningLabel);
        center.add(Box.createVerticalStrut(10));

        promptLabel.setFont(Theme.headingFont());
        promptLabel.setForeground(Theme.TEXT);
        promptLabel.setAlignmentX(CENTER_ALIGNMENT);
        center.add(promptLabel);
        center.add(Box.createVerticalStrut(10));

        imagePanel.setAlignmentX(CENTER_ALIGNMENT);
        imagePanel.setMaximumSize(new Dimension(560, 330));
        center.add(imagePanel);
        center.add(Box.createVerticalStrut(14));

        loadingLabel.setFont(Theme.bodyFont());
        loadingLabel.setForeground(Theme.TEXT_DIM);
        loadingLabel.setAlignmentX(CENTER_ALIGNMENT);
        center.add(loadingLabel);
        center.add(Box.createVerticalStrut(8));

        JPanel answerRow = new JPanel();
        answerRow.setOpaque(false);
        answerRow.setLayout(new BoxLayout(answerRow, BoxLayout.X_AXIS));
        answerField.setMaximumSize(new Dimension(160, 44));
        answerField.setFont(Theme.headingFont());
        answerField.setAlignmentY(CENTER_ALIGNMENT);
        submitButton.setMaximumSize(new Dimension(150, 44));
        submitButton.setAlignmentY(CENTER_ALIGNMENT);
        submitButton.addActionListener(e -> submit());
        answerField.addActionListener(e -> submit());
        answerRow.add(Box.createHorizontalGlue());
        answerRow.add(answerField);
        answerRow.add(Box.createHorizontalStrut(12));
        answerRow.add(submitButton);
        answerRow.add(Box.createHorizontalGlue());
        answerRow.setAlignmentX(CENTER_ALIGNMENT);
        center.add(answerRow);

        add(center, BorderLayout.CENTER);

        setBorder(BorderFactory.createLineBorder(Theme.ACCENT, 2));
    }

    private void submit() {
        submitHandler.accept(answerField.getText());
    }

    /** Sets the callback GameGUI uses to route the input into the engine. */
    public void setSubmitHandler(Consumer<String> handler) {
        this.submitHandler = handler != null ? handler : input -> { };
    }

    /** Shows a loading state while the question is being fetched. */
    public void showLoading() {
        modeBadge.setText("FETCHING QUESTION...");
        modeBadge.setForeground(Theme.TEXT_DIM);
        warningLabel.setVisible(false);
        promptLabel.setText(" ");
        imagePanel.setImage(null);
        loadingLabel.setVisible(true);
        answerField.setText("");
        answerField.setEnabled(false);
        submitButton.setEnabled(false);
        timerLabel.setText("Time: --");
        timerLabel.setForeground(Theme.TEXT);
    }
    /**
     * Displays the fetched question.
     *
     * @param question the question to solve
     * @param warning  optional API-failure notice (shown when non-null)
     */
    public void showQuestion(Question question, String warning) {
        boolean api = !question.isFallback();
        modeBadge.setText(api ? "API MODE  -  Fish Game API (marcconrad.com)"
                : "OFFLINE FALLBACK MODE  -  local question, NOT from the API");
        modeBadge.setForeground(api ? Theme.SUCCESS : Theme.WARNING);

        if (warning != null && !warning.isBlank()) {
            warningLabel.setText(warning);
            warningLabel.setVisible(true);
        } else {
            warningLabel.setVisible(false);
        }

        promptLabel.setText(question.getPrompt());
        imagePanel.setImage(question.getImage());
        loadingLabel.setVisible(false);
        answerField.setText("");
        answerField.setEnabled(true);
        submitButton.setEnabled(true);
        answerField.requestFocusInWindow();
    }

    /** Updates the countdown label (called every frame). */
    public void setTimeSeconds(double seconds) {
        int whole = (int) Math.ceil(Math.max(0, seconds));
        timerLabel.setText("Time: " + String.format("%02d", whole));
        timerLabel.setForeground(whole <= 3 ? Theme.DANGER : Theme.TEXT);
    }

    /** Clears the form (called when the challenge closes). */
    public void reset() {
        answerField.setText("");
        answerField.setEnabled(false);
        submitButton.setEnabled(false);
    }

    // ------------------------------------------------------------------
    // Question image (or text-only fallback rendering)
    // ------------------------------------------------------------------

    /** Paints the API question image scaled to fit; empty state otherwise. */
    private static final class QuestionImagePanel extends JComponent {
        private BufferedImage image;

        QuestionImagePanel() {
            setPreferredSize(new Dimension(560, 320));
            setMinimumSize(new Dimension(320, 200));
        }

        void setImage(BufferedImage image) {
            this.image = image;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int w = getWidth();
            int h = getHeight();
            g2.setColor(Theme.PANEL_LIGHT);
            g2.fillRoundRect(0, 0, w, h, 16, 16);
            g2.setColor(new Color(70, 82, 108));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);

            if (image != null) {
                double scale = Math.min((w - 16) / (double) image.getWidth(),
                        (h - 16) / (double) image.getHeight());
                int iw = (int) (image.getWidth() * scale);
                int ih = (int) (image.getHeight() * scale);
                g2.drawImage(image, (w - iw) / 2, (h - ih) / 2, iw, ih, null);
            } else {
                g2.setColor(Theme.TEXT_DIM);
                g2.setFont(Theme.bodyFont());
                String hint = "Text question - solve it and type your answer below.";
                int tw = g2.getFontMetrics().stringWidth(hint);
                g2.drawString(hint, (w - tw) / 2, h / 2);
            }
            g2.dispose();
        }
    }
}
