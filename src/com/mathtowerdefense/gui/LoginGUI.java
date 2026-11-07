package com.mathtowerdefense.gui;

import com.mathtowerdefense.authentication.LoginService;
import com.mathtowerdefense.authentication.SessionManager;
import com.mathtowerdefense.engine.SoundManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * Login screen: username/password form, register link and guest play.
 * The database call runs on a background worker so the UI never freezes,
 * even when MySQL is unreachable (the user just sees a friendly message).
 */
public class LoginGUI extends JPanel {

    private final MainGUI main;
    private final JTextField usernameField = Theme.field(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JLabel messageLabel = new JLabel(" ");
    private final JButton loginButton = Theme.primaryButton("LOGIN");

    public LoginGUI(MainGUI main) {
        this.main = main;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel form = Theme.panel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        Theme.pad(form, 40, 60, 40, 60);

        JLabel title = new JLabel("MATH TOWER DEFENSE 2D");
        title.setFont(Theme.titleFont());
        title.setForeground(Theme.ACCENT);
        title.setAlignmentX(CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Defend your base with numbers!");
        subtitle.setFont(Theme.bodyFont());
        subtitle.setForeground(Theme.TEXT_DIM);
        subtitle.setAlignmentX(CENTER_ALIGNMENT);

        messageLabel.setFont(Theme.bodyFont());
        messageLabel.setForeground(Theme.DANGER);
        messageLabel.setAlignmentX(CENTER_ALIGNMENT);

        form.add(Box.createVerticalGlue());
        form.add(title);
        form.add(Box.createVerticalStrut(6));
        form.add(subtitle);
        form.add(Box.createVerticalStrut(30));

        form.add(labeled("Username", usernameField));
        form.add(Box.createVerticalStrut(14));
        form.add(labeled("Password", passwordField));
        form.add(Box.createVerticalStrut(10));
        form.add(messageLabel);
        form.add(Box.createVerticalStrut(14));

        loginButton.setAlignmentX(CENTER_ALIGNMENT);
        loginButton.setMaximumSize(new Dimension(280, 46));
        loginButton.addActionListener(e -> attemptLogin());
        form.add(loginButton);

        form.add(Box.createVerticalStrut(18));

        JPanel links = new JPanel(new FlowLayout(FlowLayout.CENTER, 18, 0));
        links.setOpaque(false);
        JButton registerBtn = Theme.button("CREATE ACCOUNT", Theme.PANEL_LIGHT);
        registerBtn.setPreferredSize(new Dimension(170, 40));
        registerBtn.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            main.showCard(MainGUI.CARD_REGISTER);
        });
        JButton guestBtn = Theme.button("PLAY AS GUEST", Theme.PANEL_LIGHT);
        guestBtn.setPreferredSize(new Dimension(170, 40));
        guestBtn.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            SessionManager.getInstance().startGuestSession();
            main.goToMenu();
        });
        links.add(registerBtn);
        links.add(guestBtn);
        links.setAlignmentX(CENTER_ALIGNMENT);
        form.add(links);
        form.add(Box.createVerticalGlue());

        add(form, BorderLayout.CENTER);

        // Enter key submits.
        passwordField.addActionListener(e -> attemptLogin());
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow());
    }

    /** Builds a vertical "label over field" group. */
    private JPanel labeled(String labelText, javax.swing.JComponent field) {
        JPanel group = new JPanel();
        group.setOpaque(false);
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setAlignmentX(CENTER_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(Theme.smallFont());
        label.setForeground(Theme.TEXT_DIM);
        label.setAlignmentX(CENTER_ALIGNMENT);

        field.setMaximumSize(new Dimension(320, 40));
        field.setAlignmentX(CENTER_ALIGNMENT);

        group.add(label);
        group.add(Box.createVerticalStrut(4));
        group.add(field);
        return group;
    }

    /** Runs the login on a background worker (database may be slow/offline). */
    private void attemptLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        messageLabel.setForeground(Theme.TEXT_DIM);
        messageLabel.setText("Signing in...");
        loginButton.setEnabled(false);

        new SwingWorker<LoginService.Result, Void>() {
            @Override
            protected LoginService.Result doInBackground() {
                return new LoginService().login(username, password);
            }

            @Override
            protected void done() {
                try {
                    LoginService.Result result = get();
                    if (result.isSuccess()) {
                        SoundManager.getInstance().play(SoundManager.Sound.CORRECT);
                        passwordField.setText("");
                        messageLabel.setText(" ");
                        main.goToMenu();
                    } else {
                        SoundManager.getInstance().play(SoundManager.Sound.WRONG);
                        messageLabel.setForeground(Theme.DANGER);
                        messageLabel.setText("<html><center>" + result.getMessage() + "</center></html>");
                    }
                } catch (Exception e) {
                    messageLabel.setForeground(Theme.DANGER);
                    messageLabel.setText("Unexpected problem while signing in.");
                } finally {
                    loginButton.setEnabled(true);
                }
            }
        }.execute();
    }
}
