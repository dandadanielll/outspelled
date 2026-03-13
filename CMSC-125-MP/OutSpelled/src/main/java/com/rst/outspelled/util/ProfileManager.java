package com.rst.outspelled.util;

import com.rst.outspelled.model.Wizard;
import java.util.prefs.Preferences;
import java.util.ArrayList;
import java.util.List;

public final class ProfileManager {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(ProfileManager.class);

    private static final int MAX_SLOTS = 3;

    // Key format: slot0_name, slot0_wins, slot0_losses, slot0_skin, slot0_arena
    private static String key(int slot, String field) {
        return "slot" + slot + "_" + field;
    }

    public static int getMaxSlots() { return MAX_SLOTS; }

    /** Returns null if the slot is empty. */
    public static Wizard loadSlot(int slot) {
        String name = PREFS.get(key(slot, "name"), null);
        if (name == null || name.isBlank()) return null;

        int wins   = PREFS.getInt(key(slot, "wins"), 0);
        int losses = PREFS.getInt(key(slot, "losses"), 0);

        String skinName  = PREFS.get(key(slot, "skin"),
                Wizard.WizardSkin.EMBER_MAGE.name());
        String arenaName = PREFS.get(key(slot, "arena"),
                Wizard.ArenaBackground.DARK_TOWER.name());

        Wizard.WizardSkin skin;
        Wizard.ArenaBackground arena;
        try { skin  = Wizard.WizardSkin.valueOf(skinName); }
        catch (Exception e) { skin = Wizard.WizardSkin.EMBER_MAGE; }
        try { arena = Wizard.ArenaBackground.valueOf(arenaName); }
        catch (Exception e) { arena = Wizard.ArenaBackground.DARK_TOWER; }

        Wizard w = new Wizard(name, 200, skin, arena);
        // restore stats without exposing setters
        for (int i = 0; i < wins; i++) w.recordWin();
        for (int i = 0; i < losses; i++) w.recordLoss();
        return w;
    }

    public static void saveSlot(int slot, Wizard wizard) {
        PREFS.put(key(slot, "name"),   wizard.getName());
        PREFS.putInt(key(slot, "wins"),   wizard.getWins());
        PREFS.putInt(key(slot, "losses"), wizard.getLosses());
        PREFS.put(key(slot, "skin"),   wizard.getSkin().name());
        PREFS.put(key(slot, "arena"),  wizard.getPreferredArena().name());
    }

    public static void deleteSlot(int slot) {
        PREFS.remove(key(slot, "name"));
        PREFS.remove(key(slot, "wins"));
        PREFS.remove(key(slot, "losses"));
        PREFS.remove(key(slot, "skin"));
        PREFS.remove(key(slot, "arena"));
    }

    /** Returns a list of all loaded profiles (null entries = empty slots). */
    public static List<Wizard> loadAll() {
        List<Wizard> list = new ArrayList<>();
        for (int i = 0; i < MAX_SLOTS; i++) list.add(loadSlot(i));
        return list;
    }

    private ProfileManager() {}
}