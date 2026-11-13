package com.mathtowerdefense.gui;

import com.mathtowerdefense.engine.GameEngine;
import com.mathtowerdefense.engine.GameMap;
import com.mathtowerdefense.entities.Enemy;
import com.mathtowerdefense.entities.Projectile;
import com.mathtowerdefense.entities.Tower;
import com.mathtowerdefense.entities.TowerType;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Renders the battlefield (map, path, base, towers, enemies, projectiles,
 * effects) using pure Java2D - it works with ZERO image assets.
 *
 * The logical {@link GameMap#MAP_WIDTH x MAP_HEIGHT} coordinate system is
 * scaled uniformly into the panel; mouse events are converted back so the
 * engine only ever sees map coordinates.
 */
public class GameBoardPanel extends JPanel {

    private final Supplier<GameEngine> engineSource; // current engine (recreated per run)
    private final Supplier<TowerType> shopSelection; // currently chosen shop tower (or null)
    private final Consumer<Point2D.Double> mapClick; // click callback in map coords

    private double mouseX = -999;
    private double mouseY = -999;
    private boolean mouseInside;

    private double scale = 1;
    private double offsetX;
    private double offsetY;

    public GameBoardPanel(Supplier<GameEngine> engineSource,
                          Supplier<TowerType> shopSelection,
                          Consumer<Point2D.Double> mapClick) {
        this.engineSource = engineSource;
        this.shopSelection = shopSelection;
        this.mapClick = mapClick;
        setBackground(Theme.BG);
        setFocusable(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                Point2D.Double p = toMapCoords(e.getX(), e.getY());
                mapClick.accept(p);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouseInside = false;
                repaint();
            }
        });
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point2D.Double p = toMapCoords(e.getX(), e.getY());
                mouseX = p.x;
                mouseY = p.y;
                mouseInside = true;
                if (shopSelection.get() != null) {
                    repaint(); // live ghost preview
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                mouseMoved(e);
            }
        });
    }

    // ------------------------------------------------------------------
    // Coordinate transform
    // ------------------------------------------------------------------

    private void computeTransform() {
        double w = Math.max(1, getWidth());
        double h = Math.max(1, getHeight());
        scale = Math.min(w / GameMap.MAP_WIDTH, h / GameMap.MAP_HEIGHT);
        offsetX = (w - GameMap.MAP_WIDTH * scale) / 2;
        offsetY = (h - GameMap.MAP_HEIGHT * scale) / 2;
    }

    /** Converts a panel pixel to logical map coordinates. */
    private Point2D.Double toMapCoords(int px, int py) {
        computeTransform();
        return new Point2D.Double((px - offsetX) / scale, (py - offsetY) / scale);
    }
    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        GameEngine engine = engineSource.get();
        if (engine == null) {
            return; // no active game yet
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        computeTransform();
        g2.translate(offsetX, offsetY);
        g2.scale(scale, scale);

        GameMap map = engine.getMap();
        drawGround(g2, map);
        drawPath(g2, map);
        drawBase(g2, map, engine);
        drawTowers(g2, engine);
        drawEnemies(g2, engine);
        drawProjectiles(g2, engine);
        drawEffects(g2, engine);
        drawGhost(g2, engine);

        g2.dispose();
    }

    private void drawGround(Graphics2D g2, GameMap map) {
        g2.setColor(GameMap.GRASS_COLOR);
        g2.fillRect(0, 0, GameMap.MAP_WIDTH, GameMap.MAP_HEIGHT);

        // Subtle grid for depth (decorative background).
        g2.setColor(new Color(255, 255, 255, 12));
        for (int x = 0; x < GameMap.MAP_WIDTH; x += 40) {
            g2.drawLine(x, 0, x, GameMap.MAP_HEIGHT);
        }
        for (int y = 0; y < GameMap.MAP_HEIGHT; y += 40) {
            g2.drawLine(0, y, GameMap.MAP_WIDTH, y);
        }

        // Darker border vignette.
        g2.setColor(GameMap.GRASS_DARK);
        g2.setStroke(new BasicStroke(6));
        g2.drawRect(3, 3, GameMap.MAP_WIDTH - 6, GameMap.MAP_HEIGHT - 6);
        g2.setStroke(new BasicStroke(1));
    }

    private void drawPath(Graphics2D g2, GameMap map) {
        // Road: dark outline + lighter surface + dashed centre line.
        g2.setStroke(new BasicStroke(58, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(GameMap.PATH_EDGE);
        g2.draw(map.getPathOutline());

        g2.setStroke(new BasicStroke(48, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(GameMap.PATH_COLOR);
        g2.draw(map.getPathOutline());
    }

    private void drawBase(Graphics2D g2, GameMap map, GameEngine engine) {
        Point2D.Double bp = map.getBasePoint();
        int w = 74;
        int h = 64;
        double x = bp.x - w / 2.0;
        double y = bp.y - h / 2.0;

        // Castle body.
        g2.setColor(new Color(160, 50, 50));
        g2.fill(new RoundRectangle2D.Double(x, y, w, h, 10, 10));
        g2.setColor(new Color(220, 90, 90));
        g2.draw(new RoundRectangle2D.Double(x, y, w, h, 10, 10));

        // Battlements.
        g2.setColor(new Color(120, 36, 36));
        for (int i = 0; i < 4; i++) {
            g2.fillRect((int) x + 4 + i * 18, (int) y - 8, 12, 10);
        }

        // Flag.
        g2.setColor(Color.WHITE);
        g2.fillRect((int) (bp.x + 20), (int) y - 26, 3, 20);
        g2.setColor(Theme.WARNING);
        g2.fillPolygon(new int[] { (int) (bp.x + 23), (int) (bp.x + 40), (int) (bp.x + 23) },
                new int[] { (int) y - 26, (int) y - 20, (int) y - 14 }, 3);

        // HP bar above the castle.
        double hpW = 70;
        double hpX = bp.x - hpW / 2;
        double hpY = y - 36;
        g2.setColor(new Color(0, 0, 0, 160));
        g2.fill(new RoundRectangle2D.Double(hpX - 1, hpY - 1, hpW + 2, 10, 6, 6));
        double frac = engine.getBase().getHealthFraction();
        g2.setColor(frac > 0.5 ? Theme.HP_GREEN : frac > 0.25 ? Theme.WARNING : Theme.HP_RED);
        g2.fill(new RoundRectangle2D.Double(hpX, hpY, Math.max(4, hpW * frac), 8, 5, 5));

        g2.setFont(Theme.smallFont().deriveFont(Font.BOLD, 11f));
        g2.setColor(Color.WHITE);
        String hpText = "BASE " + engine.getBase().getHealth() + "/" + engine.getBase().getMaxHealth();
        int tw = g2.getFontMetrics().stringWidth(hpText);
        g2.drawString(hpText, (float) (bp.x - tw / 2), (float) (hpY - 5));
    }
    private void drawTowers(Graphics2D g2, GameEngine engine) {
        Tower selected = engine.getTowerManager().getSelected();
        g2.setFont(Theme.smallFont().deriveFont(Font.BOLD, 12f));

        for (Tower t : engine.getTowerManager().getTowers()) {
            // Range circle for the selected tower.
            if (t == selected) {
                g2.setColor(new Color(t.getColor().getRed(), t.getColor().getGreen(),
                        t.getColor().getBlue(), 40));
                g2.fill(new Ellipse2D.Double(t.getX() - t.getRange(), t.getY() - t.getRange(),
                        t.getRange() * 2, t.getRange() * 2));
                g2.setColor(new Color(t.getColor().getRGB(), true));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new Ellipse2D.Double(t.getX() - t.getRange(), t.getY() - t.getRange(),
                        t.getRange() * 2, t.getRange() * 2));
                g2.setStroke(new BasicStroke(1));
            }

            double r = 17;
            // Platform.
            g2.setColor(new Color(40, 46, 60));
            g2.fill(new Ellipse2D.Double(t.getX() - r - 3, t.getY() - r - 3,
                    (r + 3) * 2, (r + 3) * 2));
            // Body.
            g2.setColor(t.getColor());
            g2.fill(new Ellipse2D.Double(t.getX() - r, t.getY() - r, r * 2, r * 2));
            g2.setColor(t.getColor().darker());
            g2.draw(new Ellipse2D.Double(t.getX() - r, t.getY() - r, r * 2, r * 2));

            // Barrel toward the current target (or upward when idle).
            double dirX = 0;
            double dirY = -1;
            if (t.getTarget() != null && t.getTarget().isAlive()) {
                dirX = t.getTarget().getX() - t.getX();
                dirY = t.getTarget().getY() - t.getY();
                double len = Math.max(1, Math.hypot(dirX, dirY));
                dirX /= len;
                dirY /= len;
            }
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine((int) t.getX(), (int) t.getY(),
                    (int) (t.getX() + dirX * 20), (int) (t.getY() + dirY * 20));
            g2.setStroke(new BasicStroke(1));

            // Level pips under the tower.
            g2.setColor(Theme.GOLD);
            for (int i = 0; i < t.getLevel(); i++) {
                g2.fill(new Ellipse2D.Double(t.getX() - 10 + i * 7, t.getY() + r + 4, 5, 5));
            }

            // Level number.
            g2.setColor(Color.WHITE);
            String lvl = "L" + t.getLevel();
            int tw = g2.getFontMetrics().stringWidth(lvl);
            g2.drawString(lvl, (float) (t.getX() - tw / 2), (float) (t.getY() + 4));
        }
    }

    private void drawEnemies(Graphics2D g2, GameEngine engine) {
        g2.setFont(Theme.smallFont().deriveFont(Font.BOLD, 11f));
        for (Enemy e : engine.getEnemyManager().getEnemies()) {
            if (!e.isAlive() || e.hasReachedBase()) {
                continue;
            }
            int r = e.getRadius();

            // Shadow.
            g2.setColor(new Color(0, 0, 0, 90));
            g2.fill(new Ellipse2D.Double(e.getX() - r + 2, e.getY() - r + 4, r * 2, r * 2));

            // Body.
            g2.setColor(e.getType().getColor());
            g2.fill(new Ellipse2D.Double(e.getX() - r, e.getY() - r, r * 2, r * 2));
            g2.setColor(e.getType().getColor().darker());
            g2.draw(new Ellipse2D.Double(e.getX() - r, e.getY() - r, r * 2, r * 2));

            // Boss crown.
            if (e.isBoss()) {
                g2.setColor(Theme.GOLD);
                g2.fillPolygon(
                        new int[] { (int) (e.getX() - 12), (int) (e.getX() - 6),
                                    (int) e.getX(), (int) (e.getX() + 6), (int) (e.getX() + 12) },
                        new int[] { (int) (e.getY() - r + 4), (int) (e.getY() - r - 6),
                                    (int) (e.getY() - r + 2), (int) (e.getY() - r - 6),
                                    (int) (e.getY() - r + 4) }, 5);
            }

            // Type badge.
            g2.setColor(Color.WHITE);
            String badge = e.getType().getBadge();
            int tw = g2.getFontMetrics().stringWidth(badge);
            g2.drawString(badge, (float) (e.getX() - tw / 2), (float) (e.getY() + 4));

            // Health bar.
            double barW = r * 2.0;
            double barY = e.getY() - r - (e.isBoss() ? 14 : 8);
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fill(new RoundRectangle2D.Double(e.getX() - r, barY, barW, 5, 3, 3));
            double frac = e.getHealthFraction();
            g2.setColor(frac > 0.5 ? Theme.HP_GREEN : frac > 0.25 ? Theme.WARNING : Theme.HP_RED);
            g2.fill(new RoundRectangle2D.Double(e.getX() - r, barY, Math.max(2, barW * frac), 5, 3, 3));
        }
    }
    private void drawProjectiles(Graphics2D g2, GameEngine engine) {
        for (Projectile p : engine.getProjectiles()) {
            if (!p.isActive()) {
                continue;
            }
            g2.setColor(new Color(255, 236, 140, 70));
            g2.fill(new Ellipse2D.Double(p.getX() - 7, p.getY() - 7, 14, 14));
            g2.setColor(p.getColor());
            g2.fill(new Ellipse2D.Double(p.getX() - 4, p.getY() - 4, 8, 8));
        }
    }

    private void drawEffects(Graphics2D g2, GameEngine engine) {
        // Kill particles.
        for (GameEngine.Particle part : engine.getParticles()) {
            float alpha = (float) Math.min(1, part.life / 0.6);
            g2.setColor(new Color(part.color.getRed(), part.color.getGreen(),
                    part.color.getBlue(), (int) (alpha * 220)));
            g2.fill(new Ellipse2D.Double(part.x, part.y, part.size, part.size));
        }

        // Floating score texts (rise + fade).
        g2.setFont(Theme.smallFont().deriveFont(Font.BOLD, 14f));
        for (GameEngine.FloatingText ft : engine.getFloatingTexts()) {
            float alpha = (float) Math.min(1, ft.life / 1.2);
            double rise = (1.2 - ft.life) * 26;
            g2.setColor(new Color(ft.color.getRed(), ft.color.getGreen(),
                    ft.color.getBlue(), (int) (alpha * 255)));
            g2.drawString(ft.text, (float) ft.x, (float) (ft.y - rise));
        }
    }

    /** Ghost preview of the tower currently chosen in the shop. */
    private void drawGhost(Graphics2D g2, GameEngine engine) {
        TowerType type = shopSelection.get();
        if (!mouseInside || type == null) {
            return;
        }
        boolean valid = engine.canPlaceTower(type, mouseX, mouseY) == null;
        Color c = valid ? type.getColor() : Theme.DANGER;

        // Range preview.
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 50));
        g2.fill(new Ellipse2D.Double(mouseX - type.getRange(), mouseY - type.getRange(),
                type.getRange() * 2, type.getRange() * 2));
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 160));
        g2.draw(new Ellipse2D.Double(mouseX - type.getRange(), mouseY - type.getRange(),
                type.getRange() * 2, type.getRange() * 2));

        // Body preview.
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
        g2.fill(new Ellipse2D.Double(mouseX - 17, mouseY - 17, 34, 34));
        g2.setColor(c);
        g2.draw(new Ellipse2D.Double(mouseX - 17, mouseY - 17, 34, 34));

        // Cost label.
        g2.setFont(Theme.smallFont().deriveFont(Font.BOLD, 12f));
        String cost = String.valueOf(type.getCost());
        int tw = g2.getFontMetrics().stringWidth(cost);
        g2.setColor(valid ? Theme.GOLD : Theme.DANGER);
        g2.drawString(cost, (float) (mouseX - tw / 2), (float) (mouseY + 32));
    }
}
