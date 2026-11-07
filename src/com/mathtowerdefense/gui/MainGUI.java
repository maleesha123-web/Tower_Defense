package com.mathtowerdefense.gui;

import com.mathtowerdefense.authentication.SessionManager;
import com.mathtowerdefense.engine.SoundManager;
import com.mathtowerdefense.managers.ScoreService;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Application entry point and screen coordinator.
 *
 * <strong>This class contains {@code public static void main(String[] args)}.</strong>
 * It owns the window and a CardLayout where every screen is a card; screens
 * navigate by calling back into this class (no screen talks to another
 * screen directly).
 */
public class MainGUI extends JFrame {

    // Card identifiers
    public static final String CARD_LOGIN = "login";
    public static final String CARD_REGISTER = "register";
    public static final String CARD_MENU = "menu";
    public static final String CARD_GAME = "game";
    public static final String CARD_PROFILE = "profile";
    public static final String CARD_LEADERBOARD = "leaderboard";
    public static final String CARD_HELP = "help";
    public static final String CARD_SETTINGS = "settings";
    public static final String CARD_GAMEOVER = "gameover";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cards = new JPanel(cardLayout);

    private final LoginGUI loginGUI;
    private final RegisterGUI registerGUI;
    private final MainMenuGUI menuGUI;
    private final GameGUI gameGUI;
    private final ProfileGUI profileGUI;
    private final LeaderboardGUI leaderboardGUI;
    private final HelpGUI helpGUI;
    private final SettingsGUI settingsGUI;
    private final GameOverGUI gameOverGUI;

    private final ScoreService scoreService = new ScoreService();

    public MainGUI() {
        super("Math Tower Defense 2D");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 640));
        setSize(1280, 720);
        setLocationRelativeTo(null);

        loginGUI = new LoginGUI(this);
        registerGUI = new RegisterGUI(this);
        menuGUI = new MainMenuGUI(this);
        gameGUI = new GameGUI(this);
        profileGUI = new ProfileGUI(this, scoreService);
        leaderboardGUI = new LeaderboardGUI(this, scoreService);
        helpGUI = new HelpGUI(this);
        settingsGUI = new SettingsGUI(this);
        gameOverGUI = new GameOverGUI(this);

        cards.add(loginGUI, CARD_LOGIN);
        cards.add(registerGUI, CARD_REGISTER);
        cards.add(menuGUI, CARD_MENU);
        cards.add(gameGUI, CARD_GAME);
        cards.add(profileGUI, CARD_PROFILE);
        cards.add(leaderboardGUI, CARD_LEADERBOARD);
        cards.add(helpGUI, CARD_HELP);
        cards.add(settingsGUI, CARD_SETTINGS);
        cards.add(gameOverGUI, CARD_GAMEOVER);

        setLayout(new BorderLayout());
        add(cards, BorderLayout.CENTER);

        // Stop audio/loop cleanly when the window closes.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                gameGUI.shutdown();
                SoundManager.getInstance().stopMusic();
            }
        });
    }
    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    /** Shows a card and refreshes screens that need fresh data. */
    public void showCard(String id) {
        switch (id) {
            case CARD_MENU -> menuGUI.refresh();
            case CARD_PROFILE -> profileGUI.refresh();
            case CARD_LEADERBOARD -> leaderboardGUI.refresh();
            case CARD_GAME -> gameGUI.prepare();
            default -> { /* screens without refresh logic */ }
        }
        cardLayout.show(cards, id);
        cards.revalidate();
        cards.repaint();
    }

    /** Called by Login/Register on success. */
    public void goToMenu() {
        showCard(CARD_MENU);
    }

    /** Called by the menu: start a fresh game. */
    public void startGame() {
        showCard(CARD_GAME);
        gameGUI.startNewGame();
    }

    /**
     * Called by GameGUI when the engine reached GAME_OVER/VICTORY.
     * Saves the score (logged-in users) and shows the results screen.
     */
    public void showGameOver(boolean victory, GameOverGUI.GameResult result) {
        String saveStatus;
        if (SessionManager.getInstance().isGuest()) {
            saveStatus = "Guest session - score not saved. Log in to appear on the leaderboard.";
        } else {
            boolean saved = scoreService.saveGameResult(
                    SessionManager.getInstance().getCurrentUser(),
                    result.player,
                    result.waveReached);
            saveStatus = saved ? "Score saved to the leaderboard."
                    : (scoreService.getLastError() != null ? scoreService.getLastError()
                       : "Score could not be saved.");
        }
        gameOverGUI.setResult(victory, result, saveStatus);
        showCard(CARD_GAMEOVER);
    }

    /** Logout: clears the session and returns to the login screen. */
    public void logout() {
        gameGUI.shutdown();
        SessionManager.getInstance().logout();
        SoundManager.getInstance().stopMusic();
        showCard(CARD_LOGIN);
    }

    public GameGUI getGameGUI() {
        return gameGUI;
    }

    // ------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        // Use the platform look-and-feel where available; the custom dark
        // Theme paints on top of it, so a missing LAF never matters.
        try {
            javax.swing.UIManager.setLookAndFeel(
                    javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fall back to the default LAF.
        }
        javax.swing.SwingUtilities.invokeLater(() -> {
            MainGUI gui = new MainGUI();
            gui.setVisible(true);
            gui.showCard(CARD_LOGIN);
        });
    }
}
