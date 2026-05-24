package com.rst.outspelled.ui;

import com.rst.outspelled.Main;
import com.rst.outspelled.model.Wizard;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;

public class CosmeticsController {

    @FXML
    private ComboBox<String> player1SkinCombo;
    @FXML
    private ComboBox<String> player2SkinCombo;
    @FXML
    private ComboBox<String> arenaCombo;

    private static Wizard.WizardSkin selectedPlayer1Skin = Wizard.WizardSkin.EMBER_MAGE;
    private static Wizard.WizardSkin selectedPlayer2Skin = Wizard.WizardSkin.ARCANE_WIZARD;

    @FXML
    public void initialize() {
        for (Wizard.WizardSkin s : Wizard.WizardSkin.values()) {
            player1SkinCombo.getItems().add(s.getDisplayName());
            player2SkinCombo.getItems().add(s.getDisplayName());
        }

        player1SkinCombo.setValue(selectedPlayer1Skin.getDisplayName());
        player2SkinCombo.setValue(selectedPlayer2Skin.getDisplayName());

        player1SkinCombo.setOnAction(e -> applySelections());
        player2SkinCombo.setOnAction(e -> applySelections());
        arenaCombo.setOnAction(e -> applySelections());
    }

    private void applySelections() {
        if (player1SkinCombo.getValue() != null) {
            selectedPlayer1Skin = skinFromDisplayName(player1SkinCombo.getValue());
        }
        if (player2SkinCombo.getValue() != null) {
            selectedPlayer2Skin = skinFromDisplayName(player2SkinCombo.getValue());
        }
    }

    private static Wizard.WizardSkin skinFromDisplayName(String name) {
        for (Wizard.WizardSkin s : Wizard.WizardSkin.values()) {
            if (s.getDisplayName().equals(name))
                return s;
        }
        return Wizard.WizardSkin.EMBER_MAGE;
    }

    @FXML
    private void onBackClicked() {
        applySelections();
        Main.navigateTo("menu-view.fxml");
    }

    // Getters for MenuController to use when creating wizards
    public static Wizard.WizardSkin getSelectedPlayer1Skin() {
        return selectedPlayer1Skin;
    }

    public static Wizard.WizardSkin getSelectedPlayer2Skin() {
        return selectedPlayer2Skin;
    }
}
