package com.rst.outspelled.network;

/**
 * Line-based text protocol. One message per line; args space-separated.
 * Use quoted strings for names with spaces (e.g. "Player One").
 */
public final class Protocol {

    public static final int DEFAULT_PORT = 25566;

    // Client -> Server
    public static final String JOIN = "JOIN";
    public static final String READY = "READY";
    public static final String WHEEL_SCORE = "WHEEL_SCORE";
    public static final String WHEEL_MANA = "WHEEL_MANA";
    public static final String WORD = "WORD";
    public static final String SHUFFLE = "SHUFFLE";

    // Server -> Client(s)
    public static final String JOIN_OK = "JOIN_OK";
    public static final String PLAYER_JOINED = "PLAYER_JOINED";
    public static final String READY_ACK = "READY_ACK";
    public static final String BOTH_READY = "BOTH_READY";
    public static final String WHEEL_START = "WHEEL_START";
    public static final String WHEEL_OVERTIME = "WHEEL_OVERTIME";
    public static final String WHEEL_RESULT = "WHEEL_RESULT";
    public static final String GAME_START = "GAME_START";
    public static final String GRID_SEED = "GRID_SEED";
    public static final String SHUFFLE_GRID = "SHUFFLE_GRID";
    public static final String STATE = "STATE";
    public static final String INVALID_WORD = "INVALID_WORD";
    public static final String TURN_START = "TURN_START";
    public static final String TURN_TICK = "TURN_TICK";
    public static final String TURN_EXPIRED = "TURN_EXPIRED";
    public static final String GAME_OVER = "GAME_OVER";
    public static final String ERROR = "ERROR";

    // Skill checks (client -> server)
    public static final String HALF_HP_INITIATE = "HALF_HP_INITIATE";
    public static final String HALF_HP_SKIP = "HALF_HP_SKIP";
    public static final String HALF_HP_WORD = "HALF_HP_WORD";
    public static final String LAST_STAND_WORD = "LAST_STAND_WORD";

    // Skill checks (server -> client)
    public static final String HALF_HP_PROMPT = "HALF_HP_PROMPT";
    public static final String HALF_HP_START = "HALF_HP_START";
    public static final String HALF_HP_RESULT = "HALF_HP_RESULT";
    public static final String LAST_STAND_START = "LAST_STAND_START";
    public static final String LAST_STAND_RESULT = "LAST_STAND_RESULT";

    public static String quote(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public static String unquote(String s) {
        if (s == null || s.isEmpty()) return "";
        if (s.startsWith("\"") && s.endsWith("\""))
            return s.substring(1, s.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        return s;
    }

    private Protocol() {}
}
