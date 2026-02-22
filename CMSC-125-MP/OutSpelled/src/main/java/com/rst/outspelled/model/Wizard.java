package com.rst.outspelled.model;

public class Wizard extends Player {
    public enum WizardSkin {
        EMBER_MAGE("Ember Mage", "ember_mage.png"),
        FROST_WITCH("Frost Witch", "frost_witch.png"),
        STORM_SAGE("Storm Sage", "storm_sage.png"),
        SHADOW_SCRIBE("Shadow Scribe", "shadow_scribe.png");

        private final String displayName;
        private final String imagePath;

        WizardSkin(String displayName, String imagePath) {
            this.displayName = displayName;
            this.imagePath = imagePath;
        }

        public String getDisplayName() { return displayName; }
        public String getImagePath() { return imagePath; }
    }

    public enum ArenaBackground {
        DARK_TOWER("Dark Tower", "dark_tower.png"),
        ENCHANTED_FOREST("Enchanted Forest", "enchanted_forest.png"),
        VOLCANIC_CRATER("Volcanic Crater", "volcanic_crater.png"),
        ANCIENT_LIBRARY("Ancient Library", "ancient_library.png");

        private final String displayName;
        private final String imagePath;

        ArenaBackground(String displayName, String imagePath) {
            this.displayName = displayName;
            this.imagePath = imagePath;
        }

        public String getDisplayName() { return displayName; }
        public String getImagePath() { return imagePath; }
    }

    private WizardSkin skin;
    private ArenaBackground preferredArena;
    private int wins;
    private int losses;

    public Wizard(String name, int maxHp, WizardSkin skin, ArenaBackground preferredArena) {
        super(name, maxHp);
        this.skin = skin;
        this.preferredArena = preferredArena;
        this.wins = 0;
        this.losses = 0;
    }

    public void recordWin() { wins++; }
    public void recordLoss() { losses++; }

    public String getWinLossRecord() {
        return wins + "W / " + losses + "L";
    }

    // Getters and Setters
    public WizardSkin getSkin() { return skin; }
    public void setSkin(WizardSkin skin) { this.skin = skin; }

    public ArenaBackground getPreferredArena() { return preferredArena; }
    public void setPreferredArena(ArenaBackground arena) { this.preferredArena = arena; }

    public int getWins() { return wins; }
    public int getLosses() { return losses; }

    @Override
    public String toString() {
        return super.toString() + " | Skin: " + skin.getDisplayName();
    }
}
