package com.rst.outspelled.network;

import com.rst.outspelled.model.Wizard;
import javafx.application.Platform;

/**
 * Holds the LAN client and session state so multiple screens can use it.
 */
public final class SessionManager {

    private static GameClient client;
    private static int myPlayerId;
    private static String myName;
    private static String player1Name = "";
    private static String player2Name = "";
    private static GameClient.Listener listener;

    public static void setClient(GameClient c) { client = c; }
    public static GameClient getClient() { return client; }
    public static int getMyPlayerId() { return myPlayerId; }
    public static void setMyPlayerId(int id) { myPlayerId = id; }
    public static String getMyName() { return myName; }
    public static void setMyName(String name) { myName = name; }
    public static String getPlayer1Name() { return player1Name; }
    public static String getPlayer2Name() { return player2Name; }
    public static void setPlayer1Name(String n) { player1Name = n; }
    public static void setPlayer2Name(String n) { player2Name = n; }

    public static void setListener(GameClient.Listener l) { listener = l; }
    public static GameClient.Listener getListener() { return listener; }

    private static int activeProfileSlot = -1;
    private static Wizard activeWizard = null;

    public static void clear() {
        if (client != null) client.disconnect();
        client = null;
        player1Name = "";
        player2Name = "";
        listener = null;
        activeProfileSlot = -1;
        activeWizard = null;
    }

    public static void setActiveProfile(int slot, Wizard w) {
        activeProfileSlot = slot;
        activeWizard = w;
    }
    public static Wizard getActiveWizard() { return activeWizard; }
    public static int getActiveProfileSlot() { return activeProfileSlot; }

    private SessionManager() {}
}
