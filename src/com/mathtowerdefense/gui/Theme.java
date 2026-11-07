package com.mathtowerdefense.gui;

import com.mathtowerdefense.util.ResourceLoader;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.io.InputStream;

/**
 * Central look & feel: dark colour palette, fonts, rounded buttons/panels.
 * All assets are OPTIONAL - missing images/fonts simply fall back to
 * built-in values so the game never crashes over a resource.
 */
public final class Theme {

    // ---- palette (dark modern game style) ----
    public static final Color BG = new Color(14, 17, 24);
    public static final Color PANEL = new Color(24, 29, 40);
    public static final Color PANEL_LIGHT = new Color(34, 41, 56);
    public static final Color ACCENT = new Color(64, 156, 255);
    public static final Color ACCENT_DARK = new Color(36, 100, 180);
    public static final Color SUCCESS = new Color(46, 204, 113);
    public static final Color DANGER = new Color(231, 76, 60);
    public static final Color WARNING = new Color(241, 196, 15);
    public static final Color TEXT = new Color(236, 240, 248);
    public static final Color TEXT_DIM = new Color(150, 158, 175);
    public static final Color GOLD = new Color(255, 200, 60);
    public static final Color HP_GREEN = new Color(46, 204, 113);
    public static final Color HP_RED = new Color(231, 76, 60);

    private static Font titleFont;
    private static Font headingFont;
    private static Font bodyFont;
    private static Font smallFont;

    private Theme() {}

    // ------------------------------------------------------------------
    // Fonts (optional custom font -> logical fallback)
    // ------------------------------------------------------------------

    private static synchronized void ensureFonts() {
        if (bodyFont != null) {
            return;
        }
        Font base = loadCustomFont("fonts/gamefont.ttf");
        if (base == null) {
            base = new Font("Segoe UI", Font.PLAIN, 14);
        }
        titleFont = base.deriveFont(Font.BOLD, 42f);
        headingFont = base.deriveFont(Font.BOLD, 22f);
        bodyFont = base.deriveFont(Font.PLAIN, 15f);
        smallFont = base.deriveFont(Font.PLAIN, 12f);
    }

    public static Font titleFont() { ensureFonts(); return titleFont; }

    public static Font headingFont() { ensureFonts(); return headingFont; }

    public static Font bodyFont() { ensureFonts(); return bodyFont; }

    public static Font smallFont() { ensureFonts(); return smallFont; }

    private static Font loadCustomFont(String path) {
        try (InputStream in = ResourceLoader.openStream(path)) {
            return in == null ? null : Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (Throwable t) {
            return null; // missing/broken font -> logical fallback
        }
    }

    // ------------------------------------------------------------------
    // Optional images (null when missing - callers must null-check)
    // ------------------------------------------------------------------

    /** Loads an optional image via {@link ResourceLoader}; null when unavailable. */
    public static Image loadImage(String path) {
        return ResourceLoader.loadImage(path);
    }
    // ------------------------------------------------------------------
    // Reusable components
    // ------------------------------------------------------------------

    /** Rounded, colour-shifting button used across all screens. */
    public static JButton button(String text, Color bg) {
        HoverButton b = new HoverButton(text, bg);
        b.setOpaque(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setForeground(TEXT);
        b.setFont(bodyFont());
        b.setFocusPainted(false);
        b.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(240, 46));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setHover(true);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setHover(false);
            }
        });
        return b;
    }

    /** JButton that paints a rounded background with a hover highlight. */
    private static final class HoverButton extends JButton {
        private final Color base;
        private boolean hover;

        HoverButton(String text, Color base) {
            super(text);
            this.base = base;
        }

        void setHover(boolean hover) {
            this.hover = hover;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hover ? base.brighter() : base);
            g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 14, 14));
            g2.setColor(base.darker());
            g2.draw(new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 14, 14));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Convenience: accent-coloured primary button. */
    public static JButton primaryButton(String text) {
        return button(text, ACCENT_DARK);
    }

    /** Dark rounded panel (paints its own background). */
    public static JPanel panel() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PANEL);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 18, 18));
                g2.dispose();
                super.paintComponent(g);
            }
        };
    }

    /** Styled text field for the dark theme. */
    public static JTextField field(int columns) {
        JTextField f = new JTextField(columns);
        f.setBackground(PANEL_LIGHT);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setFont(bodyFont());
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 82, 108)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        return f;
    }

    /** Adds consistent padding to a component. */
    public static void pad(JComponent c, int top, int left, int bottom, int right) {
        c.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
    }
}
