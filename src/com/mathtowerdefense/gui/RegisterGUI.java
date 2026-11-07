package com.mathtowerdefense.gui;

import com.mathtowerdefense.authentication.RegisterService;
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
 * Registration screen with full client-side validation
 * (empty fields, username rules, email format, password length,
 * confirmation) backed by {@link RegisterService} - the same validation
 * code is unit tested, the GUI only displays its messages.
 */
public class RegisterGUI extends JPanel {

    private final MainGUI main;
    private final JTextField usernameField = Theme.field(18);
    private final JTextField emailField = Theme.field(18);
    private final JPasswordField passwordField = new JPasswordField(18);
    private final JPasswordField confirmField = new JPasswordField(18);
    private final JLabel messageLabel = new JLabel(" ");
    private final JButton registerButton = Theme.primaryButton("CREATE ACCOUNT");

    public RegisterGUI(MainGUI main) {
        this.main = main;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);

        JPanel form = Theme.panel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        Theme.pad(form, 30, 60, 30, 60);

        JLabel title = new JLabel("CREATE YOUR ACCOUNT");
        title.setFont(Theme.headingFont());
        title.setForeground(Theme.ACCENT);
        title.setAlignmentX(CENTER_ALIGNMENT);

        messageLabel.setFont(Theme.bodyFont());
        messageLabel.setForeground(Theme.DANGER);
        messageLabel.setAlignmentX(CENTER_ALIGNMENT);
        messageLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        form.add(Box.createVerticalGlue());
        form.add(title);
        form.add(Box.createVerticalStrut(20));
        form.add(labeled("Username (3-20 chars, letters/digits/_)", usernameField));
        form.add(Box.createVerticalStrut(10));
        form.add(labeled("Email", emailField));
        form.add(Box.createVerticalStrut(10));
        form.add(labeled("Password (min 6 characters)", passwordField));
        form.add(Box.createVerticalStrut(10));
        form.add(labeled("Confirm password", confirmField));
        form.add(Box.createVerticalStrut(10));
        form.add(messageLabel);
        form.add(Box.createVerticalStrut(14));

        registerButton.setAlignmentX(CENTER_ALIGNMENT);
        registerButton.setMaximumSize(new Dimension(280, 46));
        registerButton.addActionListener(e -> attemptRegister());
        form.add(registerButton);
        form.add(Box.createVerticalStrut(14));

        JPanel links = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        links.setOpaque(false);
        JButton backBtn = Theme.button("BACK TO LOGIN", Theme.PANEL_LIGHT);
        backBtn.setPreferredSize(new Dimension(190, 40));
        backBtn.addActionListener(e -> {
            SoundManager.getInstance().play(SoundManager.Sound.CLICK);
            clearForm();
            main.showCard(MainGUI.CARD_LOGIN);
        });
        links.add(backBtn);
        links.setAlignmentX(CENTER_ALIGNMENT);
        form.add(links);
        form.add(Box.createVerticalGlue());

        add(form, BorderLayout.CENTER);

        confirmField.addActionListener(e -> attemptRegister());
    }

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
    private void clearForm() {
        usernameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        confirmField.setText("");
        messageLabel.setText(" ");
    }

    /** Runs registration on a background worker (database may be offline). */
    private void attemptRegister() {
        String username = usernameField.getText();
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        // Fast local validation first - no worker needed for form errors.
        String validationError = RegisterService.validate(username, email, password, confirm);
        if (validationError != null) {
            showMessage(validationError);
            return;
        }

        registerButton.setEnabled(false);
        messageLabel.setForeground(Theme.TEXT_DIM);
        messageLabel.setText("Creating account...");

        new SwingWorker<RegisterService.Result, Void>() {
            @Override
            protected RegisterService.Result doInBackground() {
                return new RegisterService().register(username, email, password, confirm);
            }

            @Override
            protected void done() {
                try {
                    RegisterService.Result result = get();
                    if (result.isSuccess()) {
                        SoundManager.getInstance().play(SoundManager.Sound.CORRECT);
                        clearForm();
                        main.showCard(MainGUI.CARD_LOGIN);
                    } else {
                        showMessage(result.getMessage());
                    }
                } catch (Exception e) {
                    showMessage("Unexpected problem while creating the account.");
                } finally {
                    registerButton.setEnabled(true);
                }
            }
        }.execute();
    }

    private void showMessage(String text) {
        SoundManager.getInstance().play(SoundManager.Sound.WRONG);
        messageLabel.setForeground(Theme.DANGER);
        messageLabel.setText("<html><center>" + text + "</center></html>");
    }
}
