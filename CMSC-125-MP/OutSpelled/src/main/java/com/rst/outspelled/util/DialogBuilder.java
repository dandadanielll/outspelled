package com.rst.outspelled.util;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DialogBuilder {

    public static Stage buildDialogStage(String title, double width, double height) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setTitle(title);
        dialog.setWidth(width);
        dialog.setHeight(height);

        Scene scene = new Scene(new VBox(), width, height);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        return dialog;
    }

    public static Label styledDialogLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #e2b96f; -fx-font-size: 16px; -fx-font-weight: bold;");
        return label;
    }

    public static StackPane buildDialogButton(String text, boolean isPrimary) {
        StackPane btn = new StackPane();
        btn.setStyle("-fx-cursor: hand;");

        Rectangle bg = new Rectangle(120, 36);
        bg.setArcWidth(8);
        bg.setArcHeight(8);
        bg.setFill(Color.web(isPrimary ? "#e2b96f" : "#2a2a4a"));
        bg.setStroke(Color.web(isPrimary ? "#ffffff" : "#444466"));
        bg.setStrokeWidth(1.5);

        Label lbl = new Label(text);
        lbl.setStyle(isPrimary
                ? "-fx-text-fill: #1a1a2e; -fx-font-size: 14px; -fx-font-weight: bold;"
                : "-fx-text-fill: #e2b96f; -fx-font-size: 14px; -fx-font-weight: bold;");

        btn.getChildren().addAll(bg, lbl);

        btn.setOnMouseEntered(e -> bg.setFill(Color.web(isPrimary ? "#f3c77d" : "#333355")));
        btn.setOnMouseExited(e -> bg.setFill(Color.web(isPrimary ? "#e2b96f" : "#2a2a4a")));

        return btn;
    }

    public static VBox buildDialogRoot(String title, boolean withWarning, VBox contentBody, double width, double height) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #1a1a2e;" +
                "-fx-border-color: #e2b96f;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;");
        root.setPrefSize(width, height);
        root.setMaxSize(width, height);

        // Header
        HBox header = new HBox();
        header.setStyle("-fx-background-color: #12122a;" +
                "-fx-padding: 10 15;" +
                "-fx-background-radius: 8 8 0 0;");
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #e2b96f; -fx-font-size: 14px; -fx-font-weight: bold;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        header.getChildren().addAll(titleLabel, spacer);
        if (withWarning) {
            Polygon notch = dialogNotch();
            header.getChildren().add(notch);
        }

        root.getChildren().addAll(header, contentBody);
        return root;
    }

    public static VBox buildDialogRoot(String title, boolean withWarning, VBox contentBody, Stage dialogStage) {
        VBox root = new VBox();
        root.setStyle("-fx-background-color: #1a1a2e;" +
                "-fx-border-color: #e2b96f;" +
                "-fx-border-width: 2;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;");
        root.setPrefSize(dialogStage.getWidth(), dialogStage.getHeight());
        root.setMaxSize(dialogStage.getWidth(), dialogStage.getHeight());

        // Header
        HBox header = new HBox();
        header.setStyle("-fx-background-color: #12122a;" +
                "-fx-padding: 10 15;" +
                "-fx-background-radius: 8 8 0 0;");
        header.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #e2b96f; -fx-font-size: 14px; -fx-font-weight: bold;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        header.getChildren().addAll(titleLabel, spacer);
        if (withWarning) {
            Polygon notch = dialogNotch();
            header.getChildren().add(notch);
        }

        root.getChildren().addAll(header, contentBody);
        return root;
    }

    public static Polygon dialogNotch() {
        Polygon notch = new Polygon();
        notch.getPoints().addAll(
                0.0, 0.0,
                20.0, 0.0,
                10.0, 10.0
        );
        notch.setFill(Color.web("#ff6b6b"));
        return notch;
    }
}
