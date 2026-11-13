package com.mathtowerdefense.gui;

import com.mathtowerdefense.api.Question;
import com.mathtowerdefense.authentication.SessionManager;
import com.mathtowerdefense.engine.Game;
import com.mathtowerdefense.engine.GameConfig;
import com.mathtowerdefense.engine.GameEngine;
import com.mathtowerdefense.engine.GameState;
import com.mathtowerdefense.engine.SoundManager;
import com.mathtowerdefense.entities.Player;
import com.mathtowerdefense.entities.Tower;
import com.mathtowerdefense.entities.TowerType;
import com.mathtowerdefense.managers.QuestionManager;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.EnumMap;
import java.util.Map;

/**
 * The gameplay screen: HUD (top), battlefield (centre), tower shop with the
 * selected-tower panel (right) and the toast strip (bottom).
 *
 * It implements {@link GameEngine.Listener} and drives
 * {@link MathChallengeOverlay}/{@link GameBoardPanel} purely from engine
 * state each frame - the GUI contains no game rules.
 */
public class GameGUI extends JPanel implements GameEngine.Listener {

    private static final String CARD_BOARD = "board";
    private static final String CARD_CHALLENGE = "challenge";
    static final String CARD_PAUSE = "pause";

    private final MainGUI main;

    // Active game (recreated on every "play"); null before the first run.
    private Game game;
    private TowerType shopSelection;
    private boolean fetchingQuestion;
    private Question lastShownQuestion;
    private boolean challengeWasOpen;
    private String currentCenterCard = CARD_BOARD;

    // ---- HUD ----
    private final JLabel scoreL = hudLabel();
    private final JLabel coinsL = hudLabel();
    private final JLabel waveL = hudLabel();
    private final JLabel levelL = hudLabel();
    private final JLabel comboL = hudLabel();
    private final JLabel statusL = hudLabel();
    private final JLabel modeL = hudLabel();
    private final HpBar hpBar = new HpBar();
    private final JButton pauseBtn = Theme.button("PAUSE", Theme.PANEL_LIGHT);

    // ---- centre cards ----
    private final CardLayout centerLayout = new CardLayout();
    private final JPanel centerCards = new JPanel(centerLayout);
    private final GameBoardPanel board;
    private final MathChallengeOverlay challenge = new MathChallengeOverlay();
    private final JPanel pausePanel = new JPanel(new GridBagLayout()) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Theme.paintCoverBackground(g2, Theme.getMenuBackground(), getWidth(), getHeight(), 0.50f);
            g2.dispose();
        }
    };

    // ---- right-hand shop ----
    private final Map<TowerType, JButton> shopButtons = new EnumMap<>(TowerType.class);
    private final JLabel towerInfoL = new JLabel(" ");
    private final JButton upgradeBtn = Theme.button("UPGRADE", Theme.ACCENT_DARK);

    // ---- toast strip ----
    private final JLabel toastL = new JLabel(" ");
    private final javax.swing.Timer toastTimer;

    public GameGUI(MainGUI main) {
        this.main = main;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        board = new GameBoardPanel(this::currentEngine, this::getShopSelection, this::onMapClick);

        buildCenter();
        add(centerCards, BorderLayout.CENTER);
        add(buildHud(), BorderLayout.NORTH);
        add(buildShop(), BorderLayout.EAST);
        add(buildToastStrip(), BorderLayout.SOUTH);

        challenge.setSubmitHandler(input -> {
            if (game != null) {
                game.getEngine().submitAnswer(input);
            }
        });

        toastTimer = new javax.swing.Timer(3600, e -> {
            toastL.setText(" ");
            toastL.setForeground(Theme.TEXT_DIM);
        });
        toastTimer.setRepeats(false);

        installKeyBindings();
    }
    // ------------------------------------------------------------------
    // UI construction
    // ------------------------------------------------------------------

    private void buildCenter() {
        centerCards.add(board, CARD_BOARD);
        centerCards.add(challenge, CARD_CHALLENGE);

        // Pause overlay: centered card with background image and centered action buttons.
        pausePanel.setOpaque(false);

        JPanel pauseBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Semi-translucent dark panel background
                g2.setColor(new Color(18, 23, 34, 238));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 22, 22));

                // Subtle glowing accent border
                g2.setColor(new Color(64, 156, 255, 80));
                g2.setStroke(new BasicStroke(1.5f));
                g2.draw(new RoundRectangle2D.Double(1, 1, getWidth() - 2, getHeight() - 2, 22, 22));

                g2.dispose();
                super.paintComponent(g);
            }
        };
        pauseBox.setOpaque(false);
        pauseBox.setLayout(new javax.swing.BoxLayout(pauseBox, javax.swing.BoxLayout.Y_AXIS));
        pauseBox.setPreferredSize(new Dimension(420, 360));
        Theme.pad(pauseBox, 32, 40, 32, 40);

        JLabel pausedTitle = new JLabel("PAUSED");
        pausedTitle.setFont(Theme.titleFont().deriveFont(Font.BOLD, 36f));
        pausedTitle.setForeground(Theme.GOLD);
        pausedTitle.setAlignmentX(CENTER_ALIGNMENT);
        pauseBox.add(pausedTitle);
        pauseBox.add(strut(6));

        JLabel pauseSubtitle = new JLabel("Press ESC or P to Resume");
        pauseSubtitle.setFont(Theme.bodyFont().deriveFont(Font.BOLD, 13.5f));
        pauseSubtitle.setForeground(Theme.TEXT_DIM);
        pauseSubtitle.setAlignmentX(CENTER_ALIGNMENT);
        pauseBox.add(pauseSubtitle);
        pauseBox.add(strut(28));

        JButton resume = Theme.primaryButton("RESUME (Esc / P)");
        resume.setMaximumSize(new Dimension(280, 48));
        resume.setAlignmentX(CENTER_ALIGNMENT);
        resume.addActionListener(e -> resumeGame());
        pauseBox.add(resume);
        pauseBox.add(strut(14));

        JButton restart = Theme.button("RESTART", Theme.PANEL_LIGHT);
        restart.setMaximumSize(new Dimension(280, 48));
        restart.setAlignmentX(CENTER_ALIGNMENT);
        restart.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            startNewGame();
        });
        pauseBox.add(restart);
        pauseBox.add(strut(14));

        JButton toMenu = Theme.button("MAIN MENU", new Color(130, 42, 42));
        toMenu.setMaximumSize(new Dimension(280, 48));
        toMenu.setAlignmentX(CENTER_ALIGNMENT);
        toMenu.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            shutdown();
            main.showCard(MainGUI.CARD_MENU);
        });
        pauseBox.add(toMenu);

        pausePanel.add(pauseBox);
        centerCards.add(pausePanel, CARD_PAUSE);
    }

    private static java.awt.Component strut(int height) {
        return javax.swing.Box.createVerticalStrut(height);
    }

    private static JLabel hudLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(Theme.bodyFont().deriveFont(Font.BOLD, 15f));
        l.setForeground(Theme.TEXT);
        return l;
    }
    private JPanel buildHud() {
        JPanel hud = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 8));
        hud.setBackground(Theme.PANEL);
        hud.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(50, 60, 80)));

        scoreL.setForeground(Theme.GOLD);
        coinsL.setForeground(Theme.WARNING);
        waveL.setForeground(Theme.ACCENT);
        comboL.setForeground(Theme.SUCCESS);
        modeL.setForeground(Theme.SUCCESS);

        pauseBtn.setPreferredSize(new Dimension(108, 34));
        pauseBtn.setToolTipText("Pause or resume gameplay (Esc / P)");
        pauseBtn.addActionListener(e -> togglePause());

        hud.add(scoreL);
        hud.add(coinsL);
        hud.add(waveL);
        hud.add(levelL);
        hud.add(comboL);
        hud.add(hpBar);
        hud.add(statusL);
        hud.add(modeL);
        hud.add(pauseBtn);
        return hud;
    }

    private JPanel buildShop() {
        JPanel shop = new JPanel();
        shop.setBackground(Theme.PANEL);
        shop.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(50, 60, 80)));
        shop.setLayout(new javax.swing.BoxLayout(shop, javax.swing.BoxLayout.Y_AXIS));
        Theme.pad(shop, 14, 12, 14, 12);
        shop.setPreferredSize(new Dimension(250, 100));

        JLabel title = new JLabel("TOWER SHOP");
        title.setFont(Theme.headingFont().deriveFont(Font.BOLD, 17f));
        title.setForeground(Theme.ACCENT);
        title.setAlignmentX(CENTER_ALIGNMENT);
        shop.add(title);

        JLabel hint = new JLabel("<html><center>Select a tower, then click<br>the map to build it</center></html>");
        hint.setFont(Theme.smallFont());
        hint.setForeground(Theme.TEXT_DIM);
        hint.setAlignmentX(CENTER_ALIGNMENT);
        shop.add(hint);
        shop.add(strut(10));

        for (TowerType type : TowerType.values()) {
            JButton btn = shopButton(type);
            shopButtons.put(type, btn);
            shop.add(btn);
            shop.add(strut(8));
        }

        shop.add(separator());
        shop.add(strut(8));

        towerInfoL.setFont(Theme.smallFont());
        towerInfoL.setForeground(Theme.TEXT);
        towerInfoL.setAlignmentX(CENTER_ALIGNMENT);
        towerInfoL.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        shop.add(towerInfoL);
        shop.add(strut(8));

        upgradeBtn.setAlignmentX(CENTER_ALIGNMENT);
        upgradeBtn.setMaximumSize(new Dimension(210, 40));
        upgradeBtn.setEnabled(false);
        upgradeBtn.addActionListener(e -> {
            if (game != null) {
                String error = game.getEngine().upgradeSelectedTower();
                if (error != null) {
                    showToast(error, false);
                }
            }
        });
        shop.add(upgradeBtn);
        refreshShopSelection();
        return shop;
    }

    private JButton shopButton(TowerType type) {
        JButton btn = Theme.button(" ", Theme.PANEL_LIGHT);
        btn.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(215, 74));
        btn.setPreferredSize(new Dimension(215, 74));
        btn.setFont(Theme.smallFont());
        btn.setText("<html><b>" + type.getDisplayName() + "</b> &nbsp; " + type.getCost()
                + " coins<br>DMG " + type.getBaseDamage() + " | RNG " + (int) type.getRange()
                + " | " + String.format("%.1f", 1.0 / type.getAttackInterval()) + "/s</html>");
        btn.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            shopSelection = shopSelection == type ? null : type;
            refreshShopSelection();
        });
        return btn;
    }

    private void refreshShopSelection() {
        for (Map.Entry<TowerType, JButton> entry : shopButtons.entrySet()) {
            boolean selected = entry.getKey() == shopSelection;
            entry.getValue().setBorder(selected
                    ? BorderFactory.createLineBorder(Theme.GOLD, 2)
                    : BorderFactory.createLineBorder(new Color(70, 82, 108)));
        }
    }

    private javax.swing.JSeparator separator() {
        javax.swing.JSeparator sep = new javax.swing.JSeparator();
        sep.setMaximumSize(new Dimension(215, 2));
        return sep;
    }

    private JPanel buildToastStrip() {
        JPanel strip = new JPanel(new BorderLayout());
        strip.setBackground(Theme.PANEL);
        strip.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(50, 60, 80)));
        Theme.pad(strip, 6, 14, 6, 14);
        toastL.setFont(Theme.bodyFont());
        toastL.setForeground(Theme.TEXT_DIM);
        strip.add(toastL, BorderLayout.CENTER);
        return strip;
    }

    /**
     * Toggles between PLAYING and PAUSED states with immediate UI update
     * and audio feedback. Can be called via Esc, P, or the HUD Pause button.
     */
    public void togglePause() {
        if (game == null || game.getEngine().isEnded()) {
            return;
        }
        if (game.getEngine().isChallengeOpen()) {
            return; // don't toggle pause if user is solving math challenge
        }
        SoundManager.getInstance().play(SoundManager.Sound.CLICK);
        game.getEngine().togglePause();
        onFrame();
    }

    /** Resumes from PAUSED to PLAYING. */
    public void resumeGame() {
        if (game == null || game.getEngine().isEnded()) {
            return;
        }
        SoundManager.getInstance().play(SoundManager.Sound.CLICK);
        game.getEngine().resume();
        onFrame();
    }

    private void installKeyBindings() {
        Action toggleAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isShowing()) {
                    togglePause();
                }
            }
        };

        // Esc / P toggle pause while the game screen is showing.
        for (int condition : new int[] {
                JComponent.WHEN_IN_FOCUSED_WINDOW,
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                JComponent.WHEN_FOCUSED }) {
            InputMap im = getInputMap(condition);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "togglePause");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, 0), "togglePause");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_P, KeyEvent.SHIFT_DOWN_MASK), "togglePause");
            im.put(KeyStroke.getKeyStroke('p'), "togglePause");
            im.put(KeyStroke.getKeyStroke('P'), "togglePause");
        }
        getActionMap().put("togglePause", toggleAction);

        // Also register a window-wide KeyEventDispatcher so Esc / P triggers reliably
        // regardless of which component inside the game window currently has focus.
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() != KeyEvent.KEY_PRESSED) {
                return false;
            }
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_P) {
                if (isShowing() && game != null && !game.getEngine().isEnded()) {
                    if (game.getEngine().isChallengeOpen()) {
                        return false;
                    }
                    togglePause();
                    return true;
                }
            }
            return false;
        });
    }
    // ------------------------------------------------------------------
    // Game lifecycle
    // ------------------------------------------------------------------

    /** Called by MainGUI right before the game card is shown. */
    public void prepare() {
        setFocusable(true);
        requestFocusInWindow();
    }

    /** Creates a fresh game for the current session/difficulty and starts it. */
    public void startNewGame() {
        if (game != null) {
            game.stop(); // stop the previous run (also stops the previous engine)
        }
        String username = SessionManager.getInstance().getDisplayUsername();
        int difficulty = GameConfig.getInstance().getDifficulty();
        game = new Game(username, difficulty, this, this::onFrame);
        lastShownQuestion = null;
        challengeWasOpen = false;
        challenge.reset();
        challenge.showLoading();
        shopSelection = null;
        refreshShopSelection();
        currentCenterCard = CARD_BOARD;
        centerLayout.show(centerCards, CARD_BOARD);
        game.start();
        startQuestionFetch(); // pre-fetch so the first challenge appears instantly
        requestFocusInWindow();
    }

    /** Stops the loop + music (window close, logout, leaving the screen). */
    public void shutdown() {
        if (game != null) {
            game.stop();
        }
        SoundManager.getInstance().stopMusic();
    }

    private GameEngine currentEngine() {
        return game == null ? null : game.getEngine();
    }

    private TowerType getShopSelection() {
        return shopSelection;
    }

    /** Board click: place the selected shop tower, else select an existing one. */
    private void onMapClick(Point2D.Double p) {
        GameEngine engine = currentEngine();
        if (engine == null || engine.isEnded()) {
            return;
        }
        if (shopSelection != null) {
            String error = engine.placeTower(shopSelection, p.x, p.y);
            if (error == null) {
                shopSelection = null; // stay in selection mode after one build
                refreshShopSelection();
            } else {
                showToast(error, false);
            }
        } else {
            if (engine.selectTowerAt(p.x, p.y) == null) {
                engine.clearTowerSelection();
            }
        }
    }

    // ------------------------------------------------------------------
    // GameEngine.Listener callbacks (always on the EDT)
    // ------------------------------------------------------------------

    @Override
    public void onNotice(String message, boolean positive) {
        showToast(message, positive);
    }

    @Override
    public void onMathChallengeNeeded() {
        startQuestionFetch();
    }

    @Override
    public void onGameEnded(boolean victory) {
        if (game == null) {
            return;
        }
        game.stop();
        GameEngine engine = game.getEngine();
        Player player = engine.getPlayer();
        int wave = engine.getWaveNumber();
        // Give the world one last render pass before leaving the screen.
        board.repaint();
        main.showGameOver(victory, new GameOverGUI.GameResult(player, wave));
    }

    /**
     * Fetches the next question on a background worker so the network call
     * never blocks the EDT. The result is delivered to the engine either as
     * the open challenge's question or as a preloaded one.
     */
    private void startQuestionFetch() {
        if (fetchingQuestion || game == null) {
            return;
        }
        fetchingQuestion = true;
        GameEngine engine = game.getEngine();
        int level = Math.max(1, engine.getWaveNumber());

        new SwingWorker<QuestionManager.FetchResult, Void>() {
            @Override
            protected QuestionManager.FetchResult doInBackground() {
                return engine.getQuestionManager().fetchQuestion(level);
            }

            @Override
            protected void done() {
                try {
                    QuestionManager.FetchResult result = get();
                    if (result != null && result.getQuestion() != null) {
                        deliverIfCurrent(engine, result.getQuestion(), result.getWarning());
                    } else if (challenge != null && engine.isChallengeOpen()) {
                        String warn = (result != null && result.getWarning() != null)
                                ? result.getWarning()
                                : "Connecting to Fish Game API...";
                        challenge.showError(warn);
                    }
                } catch (Exception problem) {
                    deliverIfCurrent(engine, null, null);
                } finally {
                    fetchingQuestion = false;
                }
            }
        }.execute();
    }

    /** Delivers only if the game was not restarted while fetching. */
    private void deliverIfCurrent(GameEngine engine, Question question, String warning) {
        if (game != null && game.getEngine() == engine && question != null) {
            engine.deliverQuestion(question, warning);
        }
    }
    // ------------------------------------------------------------------
    // Per-frame sync (called by GameLoop on the EDT)
    // ------------------------------------------------------------------

    private void onFrame() {
        GameEngine engine = currentEngine();
        if (engine == null) {
            return;
        }
        updateHud(engine);
        updateCenter(engine);
        updateTowerInfo(engine);
        board.repaint();
    }

    private void updateHud(GameEngine engine) {
        Player p = engine.getPlayer();
        scoreL.setText("SCORE  " + String.format("%,d", p.getScore()));
        coinsL.setText("COINS  " + p.getCoins());
        waveL.setText("WAVE  " + engine.getWaveNumber() + "/" + engine.getTotalWaves());
        levelL.setText("LV  " + p.getLevel());
        comboL.setText(p.getCombo() > 0 ? "COMBO  x" + p.getCombo() : "COMBO  -");

        hpBar.setValues(p.getHealthFraction(),
                "BASE  " + p.getHealth() + "/" + p.getMaxHealth());

        if (engine.isMathPowerActive()) {
            statusL.setText(String.format("MATH POWER x1.5  %.0fs", engine.getPowerTimeLeft()));
            statusL.setForeground(Theme.SUCCESS);
        } else if (engine.isWeakened()) {
            statusL.setText(String.format("WEAKENED x0.5  %.0fs", engine.getWeakenTimeLeft()));
            statusL.setForeground(Theme.DANGER);
        } else if (engine.getIntermission() > 0) {
            statusL.setText("Next wave in " + (int) Math.ceil(engine.getIntermission()) + "s");
            statusL.setForeground(Theme.ACCENT);
        } else if (!engine.isEnded()) {
            statusL.setText("Next challenge in "
                    + (int) Math.ceil(engine.getChallengeCountdown()) + "s");
            statusL.setForeground(Theme.TEXT_DIM);
        }

        modeL.setText(engine.getQuestionManager().getModeLabel());
        modeL.setForeground(Theme.SUCCESS);

        boolean paused = engine.getState() == GameState.PAUSED;
        pauseBtn.setText(paused ? "RESUME" : "PAUSE");
    }

    /** Drives the board/challenge/pause card switching from engine state. */
    private void updateCenter(GameEngine engine) {
        boolean open = engine.isChallengeOpen();
        boolean paused = engine.getState() == GameState.PAUSED;

        if (open) {
            Question q = engine.getCurrentQuestion();
            if (q == null) {
                if (lastShownQuestion != null) {
                    challenge.reset();
                    challenge.showLoading();
                    lastShownQuestion = null;
                }
            } else if (q != lastShownQuestion) {
                challenge.showQuestion(q, engine.getCurrentWarning());
                lastShownQuestion = q;
            }
            challenge.setTimeSeconds(engine.getChallengeTimeLeft());
            showCenterCard(CARD_CHALLENGE);
            challengeWasOpen = true;
        } else {
            if (lastShownQuestion != null) {
                challenge.reset();
                lastShownQuestion = null;
            }
            if (challengeWasOpen) {
                // Challenge just closed -> pre-fetch the next question.
                challengeWasOpen = false;
                startQuestionFetch();
            }
            showCenterCard(paused ? CARD_PAUSE : CARD_BOARD);
        }
    }

    private void showCenterCard(String card) {
        if (!card.equals(currentCenterCard)) {
            currentCenterCard = card;
            centerLayout.show(centerCards, card);
        }
    }
    private void updateTowerInfo(GameEngine engine) {
        Tower t = engine.getTowerManager().getSelected();
        if (t == null) {
            towerInfoL.setText("<html><center>No tower selected.<br>"
                    + "Build one from the shop, or click a tower.</center></html>");
            upgradeBtn.setEnabled(false);
            upgradeBtn.setText("UPGRADE");
            return;
        }
        int nextCost = t.getUpgradeCost();
        int nextDmg = t.isMaxLevel() ? t.getDamage()
                : (int) Math.round(t.getType().getBaseDamage()
                    * TowerType.damageMultiplierForLevel(t.getLevel() + 1));
        towerInfoL.setText("<html><center><b>" + t.getDisplayName() + "</b> &nbsp; Lv "
                + t.getLevel() + "<br>DMG " + t.getDamage()
                + " &nbsp; RNG " + (int) t.getRange() + "<br>"
                + (t.isMaxLevel() ? "MAX LEVEL" : "Next: " + nextDmg + " dmg")
                + "</center></html>");
        upgradeBtn.setEnabled(!t.isMaxLevel());
        upgradeBtn.setText(t.isMaxLevel() ? "MAX LEVEL" : "UPGRADE (" + nextCost + ")");
    }

    /** Shows a toast message in the bottom strip for a few seconds. */
    public void showToast(String message, boolean positive) {
        toastL.setText(message);
        toastL.setForeground(positive ? Theme.SUCCESS : Theme.DANGER);
        toastTimer.restart();
    }

    // ------------------------------------------------------------------
    // HUD helpers
    // ------------------------------------------------------------------

    /** Small health-bar component used in the HUD. */
    private static final class HpBar extends JComponent {
        private double fraction = 1;
        private String text = "BASE";

        HpBar() {
            setPreferredSize(new Dimension(170, 22));
            setMinimumSize(new Dimension(120, 20));
        }

        void setValues(double fraction, String text) {
            this.fraction = Math.max(0, Math.min(1, fraction));
            this.text = text;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRoundRect(0, 0, w, h, 10, 10);
            g2.setColor(fraction > 0.5 ? Theme.HP_GREEN
                    : fraction > 0.25 ? Theme.WARNING : Theme.HP_RED);
            g2.fillRoundRect(0, 0, (int) (w * fraction), h, 10, 10);
            g2.setFont(Theme.smallFont().deriveFont(Font.BOLD, 12f));
            g2.setColor(Color.WHITE);
            int tw = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, Math.max(4, (w - tw) / 2), h - 6);
            g2.dispose();
        }
    }
}
