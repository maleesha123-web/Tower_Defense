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
import java.awt.GradientPaint;
import java.awt.RadialGradientPaint;
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

    /** Styled password field for the dark theme. */
    public static javax.swing.JPasswordField passwordField(int columns) {
        javax.swing.JPasswordField f = new javax.swing.JPasswordField(columns);
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
    /**
     * Paints a soft, game-styled auth background (gradient, centre glow, vignette
     * and drifting decorative dots) for the login and create-account screens.
     * Fully Java2D - no image assets required, so the screens never depend on
     * resources/images/ being populated.
     *
     * @param g2         graphics context to paint into
     * @param w          width of the surface
     * @param h          height of the surface
     * @param timeMillis current wall-clock time, used to drift the dots slowly
     */
    public static void paintAuthBackground(Graphics2D g2, int w, int h, long timeMillis) {
        // ---- base gradient (dark navy, slightly lighter at the top) ----
        GradientPaint grad = new GradientPaint(0, 0, new Color(20, 24, 36),
                0, h, new Color(8, 10, 16));
        g2.setPaint(grad);
        g2.fillRect(0, 0, w, h);

        // ---- centre accent glow (brand blue, very subtle) ----
        float radius = Math.min(w, h) * 0.55f;
        RadialGradientPaint glow = new RadialGradientPaint(w * 0.5f, h * 0.42f, radius,
                new float[] { 0f, 0.45f, 1f },
                new Color[] { new Color(64, 156, 255, 30),
                        new Color(64, 156, 255, 8), new Color(64, 156, 255, 0) });
        g2.setPaint(glow);
        g2.fillRect(0, 0, w, h);

        // ---- vignette (darker edges to frame the central form) ----
        RadialGradientPaint vignette = new RadialGradientPaint(w * 0.5f, h * 0.5f,
                Math.min(w, h) * 0.9f,
                new float[] { 0f, 0.55f, 0.9f, 1f },
                new Color[] { new Color(0, 0, 0, 0), new Color(0, 0, 0, 20),
                        new Color(0, 0, 0, 70), new Color(0, 0, 0, 150) });
        g2.setPaint(vignette);
        g2.fillRect(0, 0, w, h);

        // ---- decorative drifting dots / distant stars (stable layout, slow drift) ----
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        java.util.Random r = new java.util.Random(20260811L); // fixed seed -> stable layout
        int dotCount = Math.max(12, (w * h) / 90000);
        double t = timeMillis / 3000.0; // slow drift cycle
        for (int i = 0; i < dotCount; i++) {
            float x = r.nextFloat() * w;
            float y = r.nextFloat() * h;
            float drift = (float) (Math.sin(t + i) * 14f + Math.cos(t * 0.7f + i) * 10f);
            float size = 2f + r.nextFloat() * 3f;
            float alpha = 14f + r.nextFloat() * 26f;
            g2.setColor(new Color(Theme.TEXT_DIM.getRed(), Theme.TEXT_DIM.getGreen(),
                    Theme.TEXT_DIM.getBlue(), (int) alpha));
            g2.fillOval((int) (x + drift), (int) (y + drift * 0.5f), (int) size, (int) size);
        }

        // ---- faint horizontal grid lines (subtle tech feel) ----
        g2.setColor(new Color(Theme.TEXT_DIM.getRed(), Theme.TEXT_DIM.getGreen(),
                Theme.TEXT_DIM.getBlue(), 6));
        int lineSpacing = 48;
        int offset = (int) (t * 24f) % lineSpacing;
        for (int y = -lineSpacing + offset; y < h; y += lineSpacing) {
            g2.drawLine(0, y, w, y);
        }
    }

    // ---- auth / menu background images (lazy, optional) ----
    private static Image loginBgImage;
    private static Image menuBgImage;
    private static boolean bgImagesResolved;

    /** Lazily loads the login/create-account background image (null when missing). */
    public static Image getLoginBackground() {
        ensureBgImages();
        return loginBgImage;
    }

    /** Lazily loads the main-menu background image (null when missing). */
    public static Image getMenuBackground() {
        ensureBgImages();
        return menuBgImage;
    }

    private static void ensureBgImages() {
        if (bgImagesResolved) { return; }
        bgImagesResolved = true;
        loginBgImage = loadImage("images/Login_Create_Account_Background.png");
        if (loginBgImage == null) {
            loginBgImage = loadImage("resources/images/Login_Create_Account_Background.png");
        }
        menuBgImage = loadImage("images/Main_Menu_Background.png");
        if (menuBgImage == null) {
            menuBgImage = loadImage("resources/images/Main_Menu_Background.png");
        }
    }

    /**
     * Paints an image as a full-window cover background (aspect-ratio preserved,
     * scaled to fill, cropped if needed - like CSS background-size: cover) followed
     * by a dark transparent overlay so UI elements stay readable.
     *
     * @param img           background image, or null for a solid dark fallback
     * @param w               surface width
     * @param h               surface height
     * @param overlayAlpha    overlay darkness as a fraction (0.0 = none, 1.0 = black)
     */
    public static void paintCoverBackground(Graphics2D g2, Image img, int w, int h, float overlayAlpha) {
        if (w <= 0 || h <= 0) { return; }
        if (img == null) {
            paintAuthBackground(g2, w, h, System.currentTimeMillis());
            return;
        }
        double scale = Math.max((double) w / img.getWidth(null),
                (double) h / img.getHeight(null));
        int iw = Math.max(1, (int) (img.getWidth(null) * scale));
        int ih = Math.max(1, (int) (img.getHeight(null) * scale));
        int x = (w - iw) / 2;
        int y = (h - ih) / 2;
        g2.drawImage(img, x, y, iw, ih, null);

        // dark transparent overlay behind UI elements (30-45% recommended)
        float a = Math.min(1f, Math.max(0f, overlayAlpha));
        g2.setColor(new Color(0, 0, 0, (int) (255 * a)));
        g2.fillRect(0, 0, w, h);
    }
}
