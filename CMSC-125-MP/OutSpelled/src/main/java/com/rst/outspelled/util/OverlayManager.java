package com.rst.outspelled.util;

import com.rst.outspelled.Main;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class OverlayManager {

    private static StackPane currentOverlay = null;

    public static void showOverlay(Node content) {
        Scene scene = Main.getPrimaryStage().getScene();
        if (scene == null) return;

        Parent root = scene.getRoot();
        StackPane baseStackPane;

        if (root instanceof StackPane && "ROOT_STACK_PANE".equals(root.getId())) {
            baseStackPane = (StackPane) root;
        } else {
            // Fallback: wrap the current root if it's not our dedicated stack pane
            baseStackPane = new StackPane(root);
            baseStackPane.setId("ROOT_STACK_PANE");
            scene.setRoot(baseStackPane);
        }

        // Create overlay container
        StackPane overlayContainer = new StackPane();
        overlayContainer.setAlignment(Pos.CENTER);

        // Dark background
        Region darkBg = new Region();
        darkBg.setStyle("-fx-background-color: rgba(0, 0, 0, 0.7);");
        
        // Prevent clicks from passing through to the game
        darkBg.setOnMouseClicked(e -> e.consume());

        overlayContainer.getChildren().addAll(darkBg, content);
        
        // Remove existing overlay if present
        if (currentOverlay != null) {
            baseStackPane.getChildren().remove(currentOverlay);
        }
        
        baseStackPane.getChildren().add(overlayContainer);
        currentOverlay = overlayContainer;
    }

    public static void hideOverlay() {
        if (currentOverlay != null) {
            Scene scene = Main.getPrimaryStage().getScene();
            if (scene != null && scene.getRoot() instanceof StackPane) {
                ((StackPane) scene.getRoot()).getChildren().remove(currentOverlay);
            }
            currentOverlay = null;
        }
    }
}
