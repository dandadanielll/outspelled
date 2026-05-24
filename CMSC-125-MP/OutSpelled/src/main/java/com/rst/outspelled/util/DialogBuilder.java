package com.rst.outspelled.util;

import javafx.geometry.Insets;
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
        label.setStyle(
                "-fx-font-family: 'Pixelify Sans'; -fx-text-fill: #e2b96f; -fx-font-size: 16px; -fx-font-weight: bold;");
        return label;
    }

    public static StackPane buildDialogButton(String text, boolean isPrimary) {
        return buildDialogButton(text, isPrimary, false);
    }

    public static StackPane buildDialogButton(String text, boolean isPrimary, boolean danger) {
        StackPane outerBevel = new StackPane();
        outerBevel.setPrefSize(130, 36);
        outerBevel.setMinSize(130, 36);
        outerBevel.setMaxSize(130, 36);

        String outerBg = danger ? "#5a0f0f" : (isPrimary ? "#7a5a18" : "#1f2430");
        outerBevel.setStyle(
                "-fx-background-color: " + outerBg + ";" +
                        "-fx-background-radius: 0;" +
                        "-fx-cursor: hand;" +
                        "-fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.5), 0, 0.0, 4, 4);");

        String bgIdle, borderIdle, textStyle;
        String bgHover, borderHover, effectHover;
        String bgPressed, borderPressed;

        if (danger) {
            bgIdle = "linear-gradient(to bottom, #ff9494 0%, #ff9494 3px, #cc3333 3px, #b32424 100%)";
            borderIdle = "#ffc4c4 #8c1c1c #8c1c1c #ffc4c4";
            bgHover = "linear-gradient(to bottom, #ffadad 0%, #ffadad 3px, #e63939 3px, #cc2929 100%)";
            borderHover = "#ffe0e0 #a62222 #a62222 #ffe0e0";
            effectHover = "dropshadow(three-pass-box, rgba(255,100,100,0.22), 6, 0.1, 0, 0)";
            bgPressed = "linear-gradient(to bottom, #8a1515 0%, #8a1515 3px, #cc3333 3px, #d94141 100%)";
            borderPressed = "#5c0c0c #ffc4c4 #ffc4c4 #5c0c0c";
            textStyle = "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #200505; -fx-effect: dropshadow(one-pass-box, rgba(255,180,180,0.6), 0, 0.0, 0, 1);";
        } else if (isPrimary) {
            bgIdle = "linear-gradient(to bottom, #f5d080 0%, #f5d080 3px, #d4a035 3px, #c8922a 100%)";
            borderIdle = "#fce89a #a07020 #a07020 #fce89a";
            bgHover = "linear-gradient(to bottom, #ffe9a0 0%, #ffe9a0 3px, #f0c040 3px, #e8a828 100%)";
            borderHover = "#fff5c0 #c09030 #c09030 #fff5c0";
            effectHover = "dropshadow(three-pass-box, rgba(255,200,60,0.18), 6, 0.1, 0, 0)";
            bgPressed = "linear-gradient(to bottom, #b07820 0%, #b07820 3px, #c8922a 3px, #d4a035 100%)";
            borderPressed = "#7a5010 #fce89a #fce89a #7a5010";
            textStyle = "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1a0d00; -fx-effect: dropshadow(one-pass-box, rgba(255,220,130,0.6), 0, 0.0, 0, 1);";
        } else {
            bgIdle = "linear-gradient(to bottom, #728198 0%, #728198 3px, #424e61 3px, #364050 100%)";
            borderIdle = "#9bb0cc #262e3b #262e3b #9bb0cc";
            bgHover = "linear-gradient(to bottom, #8496b0 0%, #8496b0 3px, #4f5d75 3px, #414d61 100%)";
            borderHover = "#adccf5 #303b4d #303b4d #adccf5";
            effectHover = "dropshadow(three-pass-box, rgba(150,180,220,0.15), 6, 0.1, 0, 0)";
            bgPressed = "linear-gradient(to bottom, #2b3340 0%, #2b3340 3px, #424e61 3px, #4d5b70 100%)";
            borderPressed = "#1a2029 #9bb0cc #9bb0cc #1a2029";
            textStyle = "-fx-font-family: 'Pixelify Sans'; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #0a0d14; -fx-effect: dropshadow(one-pass-box, rgba(200,220,255,0.4), 0, 0.0, 0, 1);";
        }

        Label label = new Label(text);
        label.setStyle(textStyle);
        label.setMouseTransparent(true);

        StackPane inner = new StackPane(label);
        inner.setPrefSize(126, 32);
        inner.setMinSize(126, 32);
        inner.setMaxSize(126, 32);

        String idleStyle = "-fx-background-color: " + bgIdle
                + "; -fx-background-insets: 0; -fx-background-radius: 0; -fx-border-color: " + borderIdle
                + "; -fx-border-width: 2px; -fx-cursor: hand;";
        String hoverStyle = "-fx-background-color: " + bgHover
                + "; -fx-background-insets: 0; -fx-background-radius: 0; -fx-border-color: " + borderHover
                + "; -fx-border-width: 2px; -fx-cursor: hand; -fx-effect: " + effectHover + ";";
        String pressedStyle = "-fx-background-color: " + bgPressed
                + "; -fx-background-insets: 0; -fx-background-radius: 0; -fx-border-color: " + borderPressed
                + "; -fx-border-width: 2px; -fx-cursor: hand;";

        inner.setStyle(idleStyle);

        StackPane.setAlignment(inner, Pos.TOP_LEFT);
        inner.setTranslateX(2);
        inner.setTranslateY(2);

        outerBevel.getChildren().add(inner);

        outerBevel.setOnMouseEntered(e -> inner.setStyle(hoverStyle));
        outerBevel.setOnMouseExited(e -> inner.setStyle(idleStyle));
        outerBevel.setOnMousePressed(e -> {
            inner.setStyle(pressedStyle);
            inner.setTranslateX(4);
            inner.setTranslateY(4);
        });
        outerBevel.setOnMouseReleased(e -> {
            inner.setTranslateX(2);
            inner.setTranslateY(2);
            if (outerBevel.isHover()) {
                inner.setStyle(hoverStyle);
            } else {
                inner.setStyle(idleStyle);
            }
        });

        return outerBevel;
    }

    public static VBox buildDialogRoot(String title, boolean withWarning, VBox contentBody, double width,
            double height) {
        applyPixelFont(contentBody);
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-font-family: 'Pixelify Sans';" +
                        "-fx-font-size: 15px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + (withWarning ? "#ff6b6b" : "#e2b96f") + ";");

        HBox titleBar = new HBox(titleLabel);
        titleBar.setAlignment(Pos.CENTER);
        titleBar.setStyle(
                "-fx-background-color: #0f1220;" +
                        "-fx-padding: 12 20;" +
                        "-fx-border-color: transparent transparent " +
                        (withWarning ? "#cc3333" : "#e8c97a") + " transparent;" +
                        "-fx-border-width: 0 0 1 0;" +
                        "-fx-border-opacity: 0.5;");

        VBox inner = new VBox(0, titleBar, contentBody);
        inner.setStyle(
                "-fx-background-color: #1a1e2e;" +
                        "-fx-border-color: " + (withWarning ? "#cc3333" : "#e8c97a") + ";" +
                        "-fx-border-width: 2px;");

        Rectangle notchTL = dialogNotch(0, 0);
        Rectangle notchTR = dialogNotch(-4, 0);
        Rectangle notchBL = dialogNotch(0, -4);
        Rectangle notchBR = dialogNotch(-4, -4);
        StackPane.setAlignment(notchTR, Pos.TOP_RIGHT);
        StackPane.setAlignment(notchBL, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(notchBR, Pos.BOTTOM_RIGHT);

        StackPane root = new StackPane(inner, notchTL, notchTR, notchBL, notchBR);
        root.setPrefSize(width, height);
        root.setMaxSize(width, height);
        root.setStyle("-fx-background-color: transparent;");

        VBox outerShell = new VBox(root);
        outerShell.setStyle(
                "-fx-background-color: transparent; -fx-effect: dropshadow(one-pass-box, rgba(0,0,0,0.6), 0, 0.0, 6, 6);");
        outerShell.setAlignment(Pos.CENTER);
        return outerShell;
    }

    public static VBox buildDialogRoot(String title, boolean withWarning, VBox contentBody, Stage dialogStage) {
        return buildDialogRoot(title, withWarning, contentBody, dialogStage.getWidth(), dialogStage.getHeight());
    }

    private static void applyPixelFont(javafx.scene.Node node) {
        if (node instanceof Label) {
            Label label = (Label) node;
            String currentStyle = label.getStyle();
            if (currentStyle == null) {
                label.setStyle("-fx-font-family: 'Pixelify Sans';");
            } else if (!currentStyle.contains("Pixelify Sans")) {
                label.setStyle(currentStyle + "; -fx-font-family: 'Pixelify Sans';");
            }
        } else if (node instanceof javafx.scene.Parent) {
            for (javafx.scene.Node child : ((javafx.scene.Parent) node).getChildrenUnmodifiable()) {
                applyPixelFont(child);
            }
        }
    }

    private static Rectangle dialogNotch(double x, double y) {
        Rectangle r = new Rectangle(4, 4);
        r.setFill(Color.web("#12151e"));
        if (x != 0)
            StackPane.setMargin(r, new Insets(0, Math.abs(x), 0, 0));
        if (y != 0)
            StackPane.setMargin(r, new Insets(0, 0, Math.abs(y), 0));
        return r;
    }

    public static Polygon dialogNotch() {
        Polygon notch = new Polygon();
        notch.getPoints().addAll(
                0.0, 0.0,
                20.0, 0.0,
                10.0, 10.0);
        notch.setFill(Color.web("#ff6b6b"));
        return notch;
    }
}
