package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.network.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Login / Sign up screen. For testing, no database: any credentials "work".
 */
public class LoginController {

    @FXML private TextField signInUsername;
    @FXML private PasswordField signInPassword;
    @FXML private TextField signUpUsername;
    @FXML private PasswordField signUpPassword;
    @FXML private PasswordField signUpConfirm;
    @FXML private Label loginErrorLabel;
    @FXML private TabPane loginTabPane;

    @FXML
    private void onSignInClicked() {
        loginErrorLabel.setText("");
        String user = signInUsername.getText() != null ? signInUsername.getText().trim() : "";
        String pass = signInPassword.getText() != null ? signInPassword.getText() : "";
        if (user.isEmpty()) {
            loginErrorLabel.setText("Enter a username.");
            return;
        }
        // Testing: no DB — accept any sign in
        SessionManager.setMyName(user);
        Main.navigateTo("menu-view.fxml");
    }

    @FXML
    private void onSignUpClicked() {
        loginErrorLabel.setText("");
        String user = signUpUsername.getText() != null ? signUpUsername.getText().trim() : "";
        String pass = signUpPassword.getText() != null ? signUpPassword.getText() : "";
        String confirm = signUpConfirm.getText() != null ? signUpConfirm.getText() : "";
        if (user.isEmpty()) {
            loginErrorLabel.setText("Enter a username.");
            return;
        }
        if (pass.isEmpty()) {
            loginErrorLabel.setText("Enter a password.");
            return;
        }
        if (!pass.equals(confirm)) {
            loginErrorLabel.setText("Passwords do not match.");
            return;
        }
        // Testing: no DB — just "create" and sign in
        SessionManager.setMyName(user);
        Main.navigateTo("menu-view.fxml");
    }
}
