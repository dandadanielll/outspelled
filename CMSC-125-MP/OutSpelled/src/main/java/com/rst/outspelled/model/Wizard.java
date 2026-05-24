package com.rst.outspelled.model;

public class Wizard extends Player {
    public enum WizardSkin {
        // 4 wizard skins in total
        ARCANE_WIZARD("Arcane Wizard", "Gandalf.png"),
        EMBER_MAGE("Ember Mage", "Alice.png"),
        PRISM_SAGE("Prism Sage", "EK.png"),
        GROVE_MAGUS("Grove Magus", "Peter.png");

        private final String displayName;
        private final String imagePath;

        // WizardSkin constructor
        WizardSkin(String displayName, String imagePath) {
            this.displayName = displayName;
            this.imagePath = imagePath;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getImagePath() {
            return imagePath;
        }
    }

    private WizardSkin skin;
    private int wins;
    private int losses;

    public Wizard(String name, int maxHp, WizardSkin skin) {
        super(name, maxHp);
        this.skin = skin;
        this.wins = 0;
        this.losses = 0;
    }

    // Wizard methods
    // recordWin method increases the wins of the wizard by 1
    public void recordWin() {
        wins++;
    }

    // recordLoss method increases the losses of the wizard by 1
    public void recordLoss() {
        losses++;
    }

    // getWinLossRecord method returns the win loss record of the wizard (X Wins / Y
    // Losses)
    public String getWinLossRecord() {
        return wins + "W / " + losses + "L";
    }

    // Getters and Setters
    public WizardSkin getSkin() {
        return skin;
    }

    public void setSkin(WizardSkin skin) {
        this.skin = skin;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    @Override
    public String toString() {
        return super.toString() + " | Skin: " + skin.getDisplayName();
    }
}
