package com.rst.outspelled.ui;

import javafx.animation.Transition;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

public class CustomSpriteTransition extends Transition {
    private final ImageView imageView;
    private final int totalFrames;
    private final int columns;
    private final double frameWidth;
    private final double frameHeight;
    private int lastIndex = -1;

    public CustomSpriteTransition(ImageView imageView, Duration duration, int totalFrames, int columns,
            double frameWidth, double frameHeight) {
        this.imageView = imageView;
        this.totalFrames = totalFrames;
        this.columns = columns;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        setCycleDuration(duration);
    }

    @Override
    protected void interpolate(double frac) {
        int index = Math.min((int) Math.floor(frac * totalFrames), totalFrames - 1);
        if (index != lastIndex) {
            int x = (index % columns) * (int) frameWidth;
            int y = (index / columns) * (int) frameHeight;
            imageView.setViewport(new Rectangle2D(x, y, frameWidth, frameHeight));
            lastIndex = index;
        }
    }
}
