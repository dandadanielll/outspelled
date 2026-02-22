package com.rst.outspelled.network;

import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * LAN game client: connects to server, sends JOIN/READY/WHEEL_SCORE/WORD,
 * receives state and dispatches to listener on the FX thread.
 */
public class GameClient {

    public interface Listener {
        void onJoinOk(int myPlayerId);
        void onPlayerJoined(int playerId, String name);
        void onReadyAck(int playerId);
        void onBothReady();
        void onWheelStart();
        void onWheelMana(int playerId, int mana);
        void onWheelOvertime(int seconds);
        void onWheelResult(int firstPlayerId);
        void onGameStart(String name1, String name2, int firstPlayerId, long gridSeed);
        void onGridSeed(long seed);
        void onShuffleGrid(String letters);
        void onState(int p1Hp, int p2Hp, int currentTurn, String lastSpellDesc);
        void onInvalidWord(String word);
        void onTurnStart(int currentTurn);
        void onTurnTick(int secondsLeft);
        void onTurnExpired();
        void onGameOver(int winnerId);
        void onError(String message);
        void onHalfHpPrompt(int playerId);
        void onHalfHpStart(long gridSeed);
        void onHalfHpResult(int initiatorWon, int damageOrHeal, int p1Hp, int p2Hp);
        void onLastStandStart(String scrambledWord);
        void onLastStandResult(int winnerId, int hpGain, int p1Hp, int p2Hp);
    }

    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Listener listener;
    private volatile int myPlayerId = 0;
    private final ExecutorService readExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setName("GameClient-Read");
        t.setDaemon(true);
        return t;
    });

    public GameClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public boolean connect() {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            readExecutor.submit(this::readLoop);
            return true;
        } catch (IOException e) {
            if (listener != null) Platform.runLater(() -> listener.onError(e.getMessage()));
            return false;
        }
    }

    public void sendJoin(String playerName) {
        send(Protocol.JOIN + " " + Protocol.quote(playerName));
    }

    public void sendReady() {
        send(Protocol.READY);
    }

    public void sendWheelMana(int mana) {
        send(Protocol.WHEEL_MANA + " " + mana);
    }

    public void sendWheelScore(int mana) {
        send(Protocol.WHEEL_SCORE + " " + mana);
    }

    public void sendWord(String word) {
        send(Protocol.WORD + " " + Protocol.quote(word != null ? word : ""));
    }

    public void sendShuffle(String letters) {
        send(Protocol.SHUFFLE + " " + (letters != null && letters.length() >= 16 ? letters.substring(0, 16) : ""));
    }

    public void sendHalfHpInitiate() {
        send(Protocol.HALF_HP_INITIATE);
    }

    public void sendHalfHpSkip() {
        send(Protocol.HALF_HP_SKIP);
    }

    public void sendHalfHpWord(String word) {
        send(Protocol.HALF_HP_WORD + " " + Protocol.quote(word != null ? word : ""));
    }

    public void sendLastStandWord(String word) {
        send(Protocol.LAST_STAND_WORD + " " + Protocol.quote(word != null ? word : ""));
    }

    private void send(String line) {
        try {
            if (out != null && socket != null && !socket.isClosed()) {
                out.println(line);
            }
        } catch (Exception e) {
            if (listener != null) Platform.runLater(() -> listener.onError(e.getMessage()));
        }
    }

    private void readLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                String finalLine = line;
                dispatch(finalLine);
            }
        } catch (IOException e) {
            if (!socket.isClosed() && listener != null)
                Platform.runLater(() -> listener.onError("Connection closed: " + e.getMessage()));
        }
    }

    private void dispatch(String line) {
        String[] parts = line.trim().split("\\s+", 2);
        String cmd = parts.length > 0 ? parts[0] : "";
        String arg = parts.length > 1 ? parts[1].trim() : "";
        Listener L = listener;
        if (L == null) return;
        switch (cmd) {
            case Protocol.JOIN_OK:
                try {
                    int id = Integer.parseInt(arg.trim());
                    myPlayerId = id;
                    Platform.runLater(() -> L.onJoinOk(id));
                } catch (NumberFormatException ignored) {}
                break;
            case Protocol.PLAYER_JOINED:
                try {
                    String[] a = arg.split("\\s+", 2);
                    int id = Integer.parseInt(a[0]);
                    String name = Protocol.unquote(a.length > 1 ? a[1] : "");
                    Platform.runLater(() -> L.onPlayerJoined(id, name));
                } catch (Exception ignored) {}
                break;
            case Protocol.READY_ACK:
                try {
                    int id = Integer.parseInt(arg.trim());
                    Platform.runLater(() -> L.onReadyAck(id));
                } catch (NumberFormatException ignored) {}
                break;
            case Protocol.BOTH_READY:
                Platform.runLater(L::onBothReady);
                break;
            case Protocol.WHEEL_START:
                Platform.runLater(L::onWheelStart);
                break;
            case Protocol.WHEEL_MANA:
                try {
                    String[] a = arg.split("\\s+");
                    int pid = Integer.parseInt(a[0]);
                    int m = Integer.parseInt(a[1]);
                    Platform.runLater(() -> L.onWheelMana(pid, m));
                } catch (Exception ignored) {}
                break;
            case Protocol.WHEEL_OVERTIME:
                try {
                    int sec = Integer.parseInt(arg.trim());
                    Platform.runLater(() -> L.onWheelOvertime(sec));
                } catch (NumberFormatException ignored) {}
                break;
            case Protocol.WHEEL_RESULT:
                try {
                    int first = Integer.parseInt(arg.trim());
                    Platform.runLater(() -> L.onWheelResult(first));
                } catch (NumberFormatException ignored) {}
                break;
            case Protocol.GAME_START:
                parseGameStart(arg, L);
                break;
            case Protocol.GRID_SEED:
                try {
                    long seed = Long.parseLong(arg.trim());
                    Platform.runLater(() -> L.onGridSeed(seed));
                } catch (NumberFormatException ignored) {}
                break;
            case Protocol.SHUFFLE_GRID:
                String letters = arg != null ? arg.replaceAll("\\s+", "") : "";
                if (letters.length() >= 16) {
                    Platform.runLater(() -> L.onShuffleGrid(letters.substring(0, 16)));
                }
                break;
            case Protocol.STATE:
                parseState(arg, L);
                break;
            case Protocol.INVALID_WORD:
                Platform.runLater(() -> L.onInvalidWord(Protocol.unquote(arg)));
                break;
            case Protocol.TURN_START:
                try {
                    int t = Integer.parseInt(arg.trim());
                    Platform.runLater(() -> L.onTurnStart(t));
                } catch (NumberFormatException ignored) {}
                break;
            case Protocol.TURN_TICK:
                try {
                    int sec = Integer.parseInt(arg.trim());
                    Platform.runLater(() -> L.onTurnTick(sec));
                } catch (NumberFormatException ignored) {}
                break;
            case Protocol.TURN_EXPIRED:
                Platform.runLater(L::onTurnExpired);
                break;
            case Protocol.GAME_OVER:
                try {
                    int winner = Integer.parseInt(arg.trim());
                    Platform.runLater(() -> L.onGameOver(winner));
                } catch (NumberFormatException ignored) {}
                break;
            case Protocol.ERROR:
                Platform.runLater(() -> L.onError(Protocol.unquote(arg)));
                break;
            case Protocol.HALF_HP_PROMPT:
                try {
                    int pid = Integer.parseInt(arg.trim());
                    Platform.runLater(() -> L.onHalfHpPrompt(pid));
                } catch (NumberFormatException ignored) {}
                break;
            case Protocol.HALF_HP_START:
                try {
                    long seed = Long.parseLong(arg.trim());
                    Platform.runLater(() -> L.onHalfHpStart(seed));
                } catch (NumberFormatException ignored) {}
                break;
            case Protocol.HALF_HP_RESULT:
                try {
                    String[] a = arg.split("\\s+");
                    int won = a.length > 0 ? Integer.parseInt(a[0]) : 0;
                    int dmg = a.length > 1 ? Integer.parseInt(a[1]) : 0;
                    int hp1 = a.length > 2 ? Integer.parseInt(a[2]) : 0;
                    int hp2 = a.length > 3 ? Integer.parseInt(a[3]) : 0;
                    Platform.runLater(() -> L.onHalfHpResult(won, dmg, hp1, hp2));
                } catch (Exception ignored) {}
                break;
            case Protocol.LAST_STAND_START:
                Platform.runLater(() -> L.onLastStandStart(Protocol.unquote(arg)));
                break;
            case Protocol.LAST_STAND_RESULT:
                try {
                    String[] a = arg.split("\\s+");
                    int winner = a.length > 0 ? Integer.parseInt(a[0]) : 0;
                    int gain = a.length > 1 ? Integer.parseInt(a[1]) : 0;
                    int hp1 = a.length > 2 ? Integer.parseInt(a[2]) : 0;
                    int hp2 = a.length > 3 ? Integer.parseInt(a[3]) : 0;
                    Platform.runLater(() -> L.onLastStandResult(winner, gain, hp1, hp2));
                } catch (Exception ignored) {}
                break;
            default:
                break;
        }
    }

    private void parseGameStart(String arg, Listener L) {
        // GAME_START "name1" "name2" firstPlayerId [gridSeed]
        String[] tokens = splitQuoted(arg);
        if (tokens.length >= 3) {
            String n1 = tokens[0];
            String n2 = tokens[1];
            int first = Integer.parseInt(tokens[2].trim());
            long seed = tokens.length >= 4 ? Long.parseLong(tokens[3].trim()) : System.currentTimeMillis();
            Platform.runLater(() -> L.onGameStart(n1, n2, first, seed));
        }
    }

    private void parseState(String arg, Listener L) {
        // STATE p1hp p2hp currentTurn "lastSpellDesc"
        String[] tokens = splitQuoted(arg);
        if (tokens.length >= 4) {
            try {
                int hp1 = Integer.parseInt(tokens[0].trim());
                int hp2 = Integer.parseInt(tokens[1].trim());
                int turn = Integer.parseInt(tokens[2].trim());
                String last = tokens[3];
                Platform.runLater(() -> L.onState(hp1, hp2, turn, last));
            } catch (NumberFormatException ignored) {}
        }
    }

    private static String[] splitQuoted(String s) {
        java.util.List<String> list = new java.util.ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
            if (i >= s.length()) break;
            if (s.charAt(i) == '"') {
                int start = i + 1;
                i = start;
                while (i < s.length() && s.charAt(i) != '"') {
                    if (s.charAt(i) == '\\') i++;
                    i++;
                }
                list.add(s.substring(start, i).replace("\\\"", "\""));
                i++;
            } else {
                int start = i;
                while (i < s.length() && !Character.isWhitespace(s.charAt(i))) i++;
                list.add(s.substring(start, i));
            }
        }
        return list.toArray(new String[0]);
    }

    public int getMyPlayerId() { return myPlayerId; }

    public void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) { /* ignore */ }
        readExecutor.shutdown();
    }
}
