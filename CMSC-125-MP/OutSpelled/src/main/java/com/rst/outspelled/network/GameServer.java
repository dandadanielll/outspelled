package com.rst.outspelled.network;

import com.rst.outspelled.dictionary.DictionaryLoader;
import com.rst.outspelled.model.Spell;
import com.rst.outspelled.model.Wizard;
import com.rst.outspelled.util.LetterValues;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LAN game server: accepts 2 clients, ready-check, wheel, then duel.
 * Server is authority for game state; clients send words and receive state updates.
 */
public class GameServer {

    private static final int TURN_SECONDS = 30;
    private static final int WHEEL_TIMEOUT_MS = 20_000;

    private final int port;
    private final ServerSocket serverSocket;
    private final AtomicInteger connected = new AtomicInteger(0);
    private ClientHandler client1;
    private ClientHandler client2;
    private volatile boolean bothReady = false;
    private volatile int wheelScore1 = -1;
    private volatile int wheelScore2 = -1;
    private ScheduledExecutorService turnTimer;
    private ScheduledFuture<?> turnTask;
    private volatile int currentTurn = 1; // 1 or 2
    private volatile int p1Hp = 200;
    private volatile int p2Hp = 200;
    private static final int MAX_HP = 200;
    private volatile String p1Name = "Player 1";
    private volatile String p2Name = "Player 2";
    private volatile String lastSpellDesc = "";

    // Round tracking for half-HP (end of round = both have gone)
    private volatile boolean roundP1Gone = false;
    private volatile boolean roundP2Gone = false;

    // Half HP challenge
    private static final int SKILL_CHECK_DURATION_SEC = 15;
    private static final int SPEED_BONUS_MAX = 30;
    private static final String[] WORD_POOL = {
            "WIZARD", "ARCANE", "MYSTIC", "CIPHER", "BLAZE",
            "QUARTZ", "VORTEX", "SPECTER", "DRAGON", "RUNE",
            "SPELL", "CURSE", "EMBER", "STORM", "FROST"
    };
    private volatile boolean halfHpCheckUsed = false;
    private volatile boolean halfHpActive = false;
    private volatile int halfHpInitiator = 0; // 1 or 2
    private volatile String halfHpWord1 = "";
    private volatile String halfHpWord2 = "";
    private volatile long halfHpTime1 = 0;
    private volatile long halfHpTime2 = 0;
    private volatile long halfHpStartTime = 0;
    private volatile long halfHpGridSeed = 0;
    private volatile int halfHpPromptedPlayerId = 0; // who can initiate or skip
    private ScheduledFuture<?> halfHpTimeoutTask;

    // Last Stand
    private static final int LAST_STAND_MAX_HP_GAIN = 50;
    private final Set<Integer> lastStandUsed = new HashSet<>();
    private volatile boolean lastStandActive = false;
    private volatile String lastStandScrambled = "";
    private volatile String lastStandUnscrambled = "";
    private volatile long lastStandStartTime = 0;
    private volatile Integer lastStandWinner = null; // first correct
    private volatile int lastStandHpGain = 0;
    private ScheduledFuture<?> lastStandTimeoutTask;

    public GameServer(int port) throws IOException {
        this.port = port;
        this.serverSocket = new ServerSocket(port);
    }

    public int getPort() { return port; }

    public void start() {
        new Thread(() -> {
            try {
                System.out.println("Server listening on port " + port);
                loadDictionary();
                acceptClients();
            } catch (Exception e) {
                System.err.println("Server error: " + e.getMessage());
            }
        }, "GameServer-Accept").start();
    }

    private void loadDictionary() {
        DictionaryLoader.loadAsync(() -> System.out.println("Dictionary loaded for server."));
        while (!DictionaryLoader.isLoaded()) {
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    private void acceptClients() throws IOException {
        Socket s1 = serverSocket.accept();
        client1 = new ClientHandler(1, s1, this);
        client1.start();
        connected.set(1);
        System.out.println("Player 1 connected.");

        Socket s2 = serverSocket.accept();
        client2 = new ClientHandler(2, s2, this);
        client2.start();
        connected.set(2);
        System.out.println("Player 2 connected.");
    }

    public void onJoin(int playerId, String name) {
        if (playerId == 1) p1Name = name;
        else p2Name = name;
        sendTo(client(playerId), Protocol.JOIN_OK + " " + playerId);
        broadcast(client1, client2, Protocol.PLAYER_JOINED + " " + playerId + " " + Protocol.quote(playerId == 1 ? p1Name : p2Name));
        // Tell the other client who the joining player is (if already connected)
        if (playerId == 1 && client2 != null)
            sendTo(client2, Protocol.PLAYER_JOINED + " 1 " + Protocol.quote(p1Name));
        if (playerId == 2 && client1 != null) {
            sendTo(client1, Protocol.PLAYER_JOINED + " 2 " + Protocol.quote(p2Name));
            sendTo(client2, Protocol.PLAYER_JOINED + " 1 " + Protocol.quote(p1Name));
        }
    }

    /** Forward opponent's mana update during wheel (so each client sees the other's mana). */
    public void forwardWheelMana(int playerId, int mana) {
        ClientHandler other = playerId == 1 ? client2 : client1;
        if (other != null) sendTo(other, Protocol.WHEEL_MANA + " " + playerId + " " + mana);
    }

    public void onReady(int playerId) {
        if (playerId == 1) client1.setReady(true);
        else client2.setReady(true);
        broadcast(client1, client2, Protocol.READY_ACK + " " + playerId);
        if (client1.isReady() && client2.isReady()) {
            bothReady = true;
            broadcast(client1, client2, Protocol.BOTH_READY);
            broadcast(client1, client2, Protocol.WHEEL_START);
        }
    }

    public void onWheelScore(int playerId, int mana) {
        if (playerId == 1) wheelScore1 = mana;
        else wheelScore2 = mana;
        if (wheelScore1 >= 0 && wheelScore2 >= 0) {
            if (wheelScore1 == wheelScore2) {
                broadcast(client1, client2, Protocol.WHEEL_OVERTIME + " 5");
                wheelScore1 = -1;
                wheelScore2 = -1;
            } else {
                int first = wheelScore1 > wheelScore2 ? 1 : 2;
                broadcast(client1, client2, Protocol.WHEEL_RESULT + " " + first);
                startGame(first);
            }
        }
    }

    private final java.util.Random gridRandom = new java.util.Random();

    private void startGame(int firstPlayerId) {
        currentTurn = firstPlayerId;
        p1Hp = MAX_HP;
        p2Hp = MAX_HP;
        lastSpellDesc = "";
        long seed = gridRandom.nextLong();
        String msg = Protocol.GAME_START + " " + Protocol.quote(p1Name) + " " + Protocol.quote(p2Name) + " " + firstPlayerId + " " + seed;
        broadcast(client1, client2, msg);
        turnTimer = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("ServerTurnTimer");
            t.setDaemon(true);
            return t;
        });
        scheduleTurnTick();
    }

    private void scheduleTurnTick() {
        final int[] secondsLeft = { TURN_SECONDS };
        turnTask = turnTimer.scheduleAtFixedRate(() -> {
            secondsLeft[0]--;
            broadcast(client1, client2, Protocol.TURN_TICK + " " + secondsLeft[0]);
            if (secondsLeft[0] <= 0) {
                turnTask.cancel(false);
                currentTurn = currentTurn == 1 ? 2 : 1;
                broadcast(client1, client2, Protocol.TURN_EXPIRED);
                broadcastState();
                scheduleTurnTick();
            }
        }, 1, 1, TimeUnit.SECONDS);
        broadcast(client1, client2, Protocol.TURN_START + " " + currentTurn);
        broadcastState();
    }

    public void onWord(int playerId, String word) {
        if (playerId != currentTurn) return;
        if (halfHpActive || lastStandActive) return;
        if (turnTask != null) {
            turnTask.cancel(false);
            turnTask = null;
        }
        word = word != null ? word.trim().toLowerCase() : "";
        if (word.length() < 3) {
            sendTo(client(playerId), Protocol.INVALID_WORD + " " + Protocol.quote(word));
            scheduleTurnTick();
            return;
        }
        if (!DictionaryLoader.getWords().contains(word)) {
            sendTo(client(playerId), Protocol.INVALID_WORD + " " + Protocol.quote(word));
            scheduleTurnTick();
            return;
        }
        String casterName = playerId == 1 ? p1Name : p2Name;
        Spell spell = new Spell(word, casterName);
        int damage = spell.getTotalDamage();
        if (playerId == 1) {
            p2Hp = Math.max(0, p2Hp - damage);
        } else {
            p1Hp = Math.max(0, p1Hp - damage);
        }
        lastSpellDesc = spell.getSpellDescription();
        if (p1Hp <= 0 || p2Hp <= 0) {
            int winner = p1Hp <= 0 ? 2 : 1;
            broadcast(client1, client2, Protocol.GAME_OVER + " " + winner);
            if (turnTimer != null) turnTimer.shutdown();
            return;
        }

        int damagedPlayerId = playerId == 1 ? 2 : 1;
        int damagedHp = damagedPlayerId == 1 ? p1Hp : p2Hp;

        // Last Stand: damaged player at 10% or below
        if (!lastStandActive && !halfHpActive && !lastStandUsed.contains(damagedPlayerId)
                && damagedHp <= MAX_HP / 10 && damagedHp > 0) {
            startLastStand(damagedPlayerId);
            broadcastState();
            return;
        }

        // Mark round
        if (playerId == 1) roundP1Gone = true;
        else roundP2Gone = true;
        boolean endOfRound = roundP1Gone && roundP2Gone;

        // Half HP prompt at end of round (whoever is at 50% can be prompted)
        if (!halfHpCheckUsed && !lastStandActive && endOfRound) {
            if (p1Hp <= MAX_HP / 2 && p1Hp > 0) {
                roundP1Gone = false;
                roundP2Gone = false;
                halfHpPromptedPlayerId = 1;
                broadcast(client1, client2, Protocol.HALF_HP_PROMPT + " 1");
                broadcastState();
                return;
            }
            if (p2Hp <= MAX_HP / 2 && p2Hp > 0) {
                roundP1Gone = false;
                roundP2Gone = false;
                halfHpPromptedPlayerId = 2;
                broadcast(client1, client2, Protocol.HALF_HP_PROMPT + " 2");
                broadcastState();
                return;
            }
        }

        if (endOfRound) {
            roundP1Gone = false;
            roundP2Gone = false;
        }

        currentTurn = currentTurn == 1 ? 2 : 1;
        long seed = gridRandom.nextLong();
        broadcast(client1, client2, Protocol.GRID_SEED + " " + seed);
        broadcastState();
        scheduleTurnTick();
    }

    private void startLastStand(int damagedPlayerId) {
        lastStandUsed.add(damagedPlayerId);
        lastStandActive = true;
        lastStandWinner = null;
        lastStandUnscrambled = WORD_POOL[gridRandom.nextInt(WORD_POOL.length)];
        lastStandScrambled = scramble(lastStandUnscrambled);
        while (lastStandScrambled.equals(lastStandUnscrambled)) {
            lastStandScrambled = scramble(lastStandUnscrambled);
        }
        lastStandStartTime = System.currentTimeMillis();
        broadcast(client1, client2, Protocol.LAST_STAND_START + " " + Protocol.quote(lastStandScrambled));
        lastStandTimeoutTask = turnTimer.schedule(() -> {
            resolveLastStand();
        }, SKILL_CHECK_DURATION_SEC, TimeUnit.SECONDS);
    }

    private static String scramble(String word) {
        List<Character> chars = new ArrayList<>();
        for (char c : word.toCharArray()) chars.add(c);
        Collections.shuffle(chars);
        StringBuilder sb = new StringBuilder();
        for (char c : chars) sb.append(c);
        return sb.toString();
    }

    public void onLastStandWord(int playerId, String word) {
        if (!lastStandActive || word == null) return;
        word = word.trim();
        if (!word.equalsIgnoreCase(lastStandUnscrambled)) return;
        if (lastStandWinner != null) return; // already resolved
        lastStandWinner = playerId;
        long timeMs = System.currentTimeMillis() - lastStandStartTime;
        lastStandHpGain = Math.max(1, (int) Math.round(LAST_STAND_MAX_HP_GAIN * Math.max(0, 1.0 - timeMs / 1000.0 / SKILL_CHECK_DURATION_SEC)));
        if (lastStandTimeoutTask != null) {
            lastStandTimeoutTask.cancel(false);
            lastStandTimeoutTask = null;
        }
        resolveLastStand();
    }

    private void resolveLastStand() {
        if (lastStandTimeoutTask != null) {
            lastStandTimeoutTask.cancel(false);
            lastStandTimeoutTask = null;
        }
        lastStandActive = false;
        int winner = lastStandWinner != null ? lastStandWinner : 0;
        int hpGain = lastStandWinner != null ? lastStandHpGain : 0;
        if (winner == 1) {
            p1Hp = Math.min(MAX_HP, p1Hp + hpGain);
        } else if (winner == 2) {
            p2Hp = Math.min(MAX_HP, p2Hp + hpGain);
        }
        broadcast(client1, client2, Protocol.LAST_STAND_RESULT + " " + winner + " " + hpGain + " " + p1Hp + " " + p2Hp);
        broadcastState();
        currentTurn = currentTurn == 1 ? 2 : 1;
        scheduleTurnTick();
    }

    public void onHalfHpInitiate(int playerId) {
        if (halfHpCheckUsed || halfHpActive) return;
        if (playerId != halfHpPromptedPlayerId) return;
        halfHpCheckUsed = true;
        halfHpActive = true;
        halfHpInitiator = playerId;
        halfHpWord1 = "";
        halfHpWord2 = "";
        halfHpTime1 = 0;
        halfHpTime2 = 0;
        halfHpStartTime = System.currentTimeMillis();
        halfHpGridSeed = gridRandom.nextLong();
        broadcast(client1, client2, Protocol.HALF_HP_START + " " + halfHpGridSeed);
        halfHpTimeoutTask = turnTimer.schedule(this::resolveHalfHp, SKILL_CHECK_DURATION_SEC, TimeUnit.SECONDS);
    }

    public void onHalfHpSkip(int playerId) {
        if (halfHpActive) return; // already started challenge
        if (playerId != halfHpPromptedPlayerId) return;
        halfHpCheckUsed = true;
        halfHpPromptedPlayerId = 0;
        broadcast(client1, client2, Protocol.HALF_HP_RESULT + " 0 0 " + p1Hp + " " + p2Hp);
        broadcastState();
        currentTurn = currentTurn == 1 ? 2 : 1;
        scheduleTurnTick();
    }

    public void onHalfHpWord(int playerId, String word) {
        if (!halfHpActive || word == null || word.isBlank()) return;
        word = word.trim().toLowerCase();
        if (word.length() < 3 || !DictionaryLoader.getWords().contains(word)) return;
        long timeMs = System.currentTimeMillis() - halfHpStartTime;
        if (playerId == 1) {
            if (!halfHpWord1.isEmpty()) return;
            halfHpWord1 = word;
            halfHpTime1 = timeMs;
        } else {
            if (!halfHpWord2.isEmpty()) return;
            halfHpWord2 = word;
            halfHpTime2 = timeMs;
        }
        if (!halfHpWord1.isEmpty() && !halfHpWord2.isEmpty()) {
            if (halfHpTimeoutTask != null) {
                halfHpTimeoutTask.cancel(false);
                halfHpTimeoutTask = null;
            }
            resolveHalfHp();
        }
    }

    private int scoreWithSpeed(String word, long timeMs) {
        int wordScore = LetterValues.getWordValue(word) + word.length();
        int speedBonus = (int) Math.max(0, SPEED_BONUS_MAX - (timeMs / 1000.0));
        return wordScore + speedBonus;
    }

    private void resolveHalfHp() {
        if (halfHpTimeoutTask != null) {
            halfHpTimeoutTask.cancel(false);
            halfHpTimeoutTask = null;
        }
        halfHpActive = false;

        int initiator = halfHpInitiator;
        int opponent = initiator == 1 ? 2 : 1;
        int initiatorScore = initiator == 1 ? scoreWithSpeed(halfHpWord1, halfHpTime1) : scoreWithSpeed(halfHpWord2, halfHpTime2);
        int opponentScore = initiator == 1 ? scoreWithSpeed(halfHpWord2, halfHpTime2) : scoreWithSpeed(halfHpWord1, halfHpTime1);
        String initiatorWord = initiator == 1 ? halfHpWord1 : halfHpWord2;

        int damageOrHeal;
        if (initiatorScore >= opponentScore) {
            // Initiator wins: opponent drops to initiator's HP
            int initiatorHp = initiator == 1 ? p1Hp : p2Hp;
            int opponentHp = opponent == 1 ? p1Hp : p2Hp;
            damageOrHeal = Math.max(0, opponentHp - initiatorHp);
            if (opponent == 1) p1Hp = Math.max(0, p1Hp - damageOrHeal);
            else p2Hp = Math.max(0, p2Hp - damageOrHeal);
        } else {
            // Initiator loses: takes word damage
            damageOrHeal = LetterValues.getWordValue(initiatorWord) + initiatorWord.length();
            if (initiator == 1) p1Hp = Math.max(0, p1Hp - damageOrHeal);
            else p2Hp = Math.max(0, p2Hp - damageOrHeal);
        }

        broadcast(client1, client2, Protocol.HALF_HP_RESULT + " " + (initiatorScore >= opponentScore ? 1 : 0) + " " + damageOrHeal + " " + p1Hp + " " + p2Hp);
        if (p1Hp <= 0 || p2Hp <= 0) {
            int winner = p1Hp <= 0 ? 2 : 1;
            broadcast(client1, client2, Protocol.GAME_OVER + " " + winner);
            if (turnTimer != null) turnTimer.shutdown();
            return;
        }
        currentTurn = currentTurn == 1 ? 2 : 1;
        broadcastState();
        long seed = gridRandom.nextLong();
        broadcast(client1, client2, Protocol.GRID_SEED + " " + seed);
        broadcastState();
        scheduleTurnTick();
    }

    public void onShuffle(int playerId, String letters) {
        if (playerId != currentTurn) return;
        if (halfHpActive || lastStandActive) return;
        if (letters == null || letters.length() < 16) return;
        broadcast(client1, client2, Protocol.SHUFFLE_GRID + " " + letters.substring(0, 16));
    }

    private void broadcastState() {
        String msg = Protocol.STATE + " " + p1Hp + " " + p2Hp + " " + currentTurn + " " + Protocol.quote(lastSpellDesc);
        broadcast(client1, client2, msg);
    }

    private ClientHandler client(int id) { return id == 1 ? client1 : client2; }

    private static void sendTo(ClientHandler c, String line) {
        if (c != null) c.send(line);
    }

    private static void broadcast(ClientHandler c1, ClientHandler c2, String line) {
        if (c1 != null) c1.send(line);
        if (c2 != null) c2.send(line);
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
        } catch (IOException e) { /* ignore */ }
        if (client1 != null) client1.close();
        if (client2 != null) client2.close();
        if (turnTimer != null) turnTimer.shutdown();
    }

    // --- ClientHandler ---
    static class ClientHandler {
        private final int id;
        private final Socket socket;
        private final GameServer server;
        private final BufferedReader in;
        private final PrintWriter out;
        private volatile boolean ready = false;

        ClientHandler(int id, Socket socket, GameServer server) throws IOException {
            this.id = id;
            this.socket = socket;
            this.server = server;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        void setReady(boolean r) { ready = r; }
        boolean isReady() { return ready; }

        void start() {
            Thread t = new Thread(this::run, "Server-Client-" + id);
            t.setDaemon(true);
            t.start();
        }

        void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    handle(line);
                }
            } catch (IOException e) {
                if (!socket.isClosed()) System.err.println("Client " + id + " read error: " + e.getMessage());
            } finally {
                close();
            }
        }

        private void handle(String line) {
            String[] parts = line.trim().split("\\s+", 2);
            String cmd = parts.length > 0 ? parts[0] : "";
            String arg = parts.length > 1 ? parts[1].trim() : "";
            switch (cmd) {
                case Protocol.JOIN:
                    server.onJoin(id, Protocol.unquote(arg));
                    break;
                case Protocol.READY:
                    server.onReady(id);
                    break;
                case Protocol.WHEEL_MANA:
                    try {
                        int mana = Integer.parseInt(arg.split("\\s+")[0]);
                        server.forwardWheelMana(id, mana);
                    } catch (Exception e) { /* ignore */ }
                    break;
                case Protocol.WHEEL_SCORE:
                    try {
                        int mana = Integer.parseInt(arg.split("\\s+")[0]);
                        server.onWheelScore(id, mana);
                    } catch (Exception e) { /* ignore */ }
                    break;
                case Protocol.WORD:
                    server.onWord(id, Protocol.unquote(arg));
                    break;
                case Protocol.SHUFFLE:
                    server.onShuffle(id, arg != null ? arg.replaceAll("\\s+", "") : "");
                    break;
                case Protocol.HALF_HP_INITIATE:
                    if (arg.isEmpty()) server.onHalfHpInitiate(id);
                    break;
                case Protocol.HALF_HP_SKIP:
                    server.onHalfHpSkip(id);
                    break;
                case Protocol.HALF_HP_WORD:
                    server.onHalfHpWord(id, Protocol.unquote(arg));
                    break;
                case Protocol.LAST_STAND_WORD:
                    server.onLastStandWord(id, Protocol.unquote(arg));
                    break;
                default:
                    break;
            }
        }

        synchronized void send(String line) {
            try {
                if (out != null && !socket.isClosed()) {
                    out.println(line);
                }
            } catch (Exception e) { /* ignore */ }
        }

        void close() {
            try {
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException e) { /* ignore */ }
        }
    }
}
