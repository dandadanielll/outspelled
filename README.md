# OutSpelled 

> A wizard dueling word game for two players over LAN. Form words from a 4×4 letter grid to cast spells and deal damage — last wizard standing wins.

---

## Table of Contents

- [Requirements](#requirements)
- [Project Setup in IntelliJ IDEA](#project-setup-in-intellij-idea)
- [Building and Running](#building-and-running)
- [LAN Multiplayer Setup](#lan-multiplayer-setup)
- [How to Play](#how-to-play)
- [Skill Checks](#skill-checks)
- [Project Structure](#project-structure)

---

## Requirements

| Tool | Version |
|------|---------|
| JDK | 21 or higher |
| Maven | 3.9+ (or use the Maven wrapper) |
| IntelliJ IDEA | 2023.1+ (Community or Ultimate) |

> **Note:** JavaFX is bundled via Maven dependencies — you do **not** need to install it separately.

---

## Project Setup in IntelliJ IDEA

### 1. Clone the repository

```bash
git clone https://github.com/dandadanielll/outspelled.git
```

### 2. Open the project

1. Open IntelliJ IDEA.
2. Click **File → Open**.
3. Navigate to the cloned folder and select the inner `OutSpelled` directory (the one that contains `pom.xml`):
   ```
   outspelled/CMSC-125-MP/OutSpelled/
   ```
4. Click **OK** and wait for IntelliJ to finish indexing.

### 3. Set the Project SDK

1. Go to **File → Project Structure → Project**.
2. Set the SDK to **JDK 21** (or higher).
3. Set the language level to **21**.
4. Click **Apply → OK**.

### 4. Load Maven dependencies

IntelliJ should automatically detect `pom.xml` and prompt you to load Maven. If it doesn't:

1. Open the **Maven** panel (right sidebar or **View → Tool Windows → Maven**).
2. Click the **Reload All Maven Projects** button (↻).
3. Wait for all dependencies to download.

### 5. Set the run configuration

1. Go to **Run → Edit Configurations**.
2. Click **+** and choose **Application**.
3. Set:
   - **Name:** `OutSpelled`
   - **Main class:** `com.rst.outspelled.Main`
   - **JVM options:**
     ```
     --enable-native-access=javafx.graphics
     ```
4. Click **Apply → OK**.

> The JVM option suppresses JavaFX native access warnings on Java 21+. The game runs fine without it but the console will be noisy.

---

## Building and Running

### Run via IntelliJ

With the run configuration set up, simply press **Shift + F10** or click the green **Run** button.

### Run via Maven (terminal)

From inside the `OutSpelled/` directory:

```bash
# Compile and run
mvn javafx:run

# Or: package into a JAR first
mvn clean package
```

---

## LAN Multiplayer Setup

OutSpelled is a **two-player LAN game**. Both players must be on the **same Wi-Fi or local network**.

### Step-by-step

**On the Host machine:**

1. Launch the game and create or select a wizard profile.
2. From the main menu, click **Start Game**.
3. On the Connect screen, your **local IP address** is displayed at the top (e.g. `192.168.1.5`). Share this with the other player.
4. Click **Host Game**. The server will start and wait for the other player.

**On the Joining machine:**

1. Launch the game and create or select a wizard profile.
2. From the main menu, click **Start Game**.
3. On the Connect screen, type the **host's IP address** into the field.
4. Click **Join Game**.

**Both players:**

5. Once connected, both players will land on the **Ready** screen showing each other's names.
6. Each player clicks their own **Ready** button.
7. The **Mana Wheel** starts — the player who scores higher goes first.
8. The game begins.

### Troubleshooting

| Problem | Fix |
|---------|-----|
| "Connection failed" on join | Make sure the host clicked **Host Game** first and that both machines are on the same network |
| Firewall blocking connection | Allow Java through your firewall, or temporarily disable it for testing |
| Host IP not showing | Check your network adapter settings manually (`ipconfig` on Windows, `ifconfig` on Mac/Linux) |

---

## How to Play

### Profiles

When you first launch OutSpelled, you are taken to the **Choose Your Wizard** screen. You can create up to **3 local profiles**, each with a name and win/loss record that persists between sessions. Select a profile and click **Play** to continue.

### Objective

Reduce your opponent's HP to zero before they reduce yours. Each player starts with **200 HP (10 hearts)**. You deal damage by forming valid English words from the letter grid.

### Taking a Turn

1. **Select letters** from the 4×4 grid by clicking tiles or typing on your keyboard. Letters must be selected one at a time — any tile on the grid can be picked in any order.
2. The **selected word** appears as floating letters above the grid.
3. Click **✨ Cast Spell!** or press **Enter** to submit your word.
   - Words must be **at least 3 letters** long and must exist in the dictionary.
   - If valid, damage is dealt to your opponent based on the word's letter values and length.
   - If invalid, your turn resets and you can try again.
4. Click **↺ Clear** or press **Escape** to deselect all letters.
5. Click **🔀 Shuffle** or press **Space** to rearrange idle (unselected) tiles.

### Damage Formula

Damage is based on **Scrabble-style letter point values** plus a bonus for word length. Rare letters like Q, Z, X deal more damage. Longer words deal significantly more damage.

### Timer

Each turn has a **30-second timer**. If time runs out, your turn is skipped automatically.

### Battle Log

The panel at the bottom of the screen logs every spell cast by both players during the match, including skill check outcomes.

---

## Skill Checks

Skill checks are special mid-game events that can dramatically shift the momentum of the match.

### ⚔ Half HP Challenge

**Triggers:** At the end of a round (after both players have taken a turn), if either player is at **50% HP or below** and the challenge has not been used yet in the match.

**How it works:**
1. The eligible player is prompted to either **Initiate** or **Skip** the challenge.
2. If initiated, **both players** are given a shared letter grid and a 15-second timer.
3. Each player forms the best word they can as fast as possible — speed gives a bonus to the score.
4. **Initiator wins:** The opponent's HP is reduced to match the initiator's HP.
5. **Initiator loses:** The initiator takes word damage based on their own submitted word.

> This challenge can only happen **once per match**.

### 🛡 Last Stand

**Triggers:** When a player drops to **10% HP or below** for the first time.

**How it works:**
1. A scrambled word appears on screen for **both players** to see.
2. Both players race to type the correct unscrambled word and press **Enter**.
3. The **first player to answer correctly** gains HP — up to **+50 HP**, scaled by how quickly they answered.
4. If no one answers in time, nothing happens and the game resumes.

> Each player can only trigger Last Stand **once per match**.

---

## Project Structure

```
OutSpelled/
├── src/main/java/com/rst/outspelled/
│   ├── Main.java                  # App entry point, scene navigation
│   ├── damage/
│   │   └── DamageCalculator.java  # Applies spell damage to targets
│   ├── dictionary/
│   │   ├── DictionaryLoader.java  # Async word list loader
│   │   └── WordValidator.java     # Validates submitted words
│   ├── engine/
│   │   ├── GameEngine.java        # Local game loop, turn and state management
│   │   ├── SkillCheckManager.java # Triggers and resolves skill checks (local)
│   │   ├── SkillCheckResult.java  # Result data class
│   │   └── TurnManager.java       # Turn timer and player rotation
│   ├── model/
│   │   ├── LetterGrid.java        # 4×4 tile grid with seed support
│   │   ├── LetterTile.java        # Individual tile (letter, value, state)
│   │   ├── Player.java            # Base player (name, HP)
│   │   ├── Spell.java             # Word → damage conversion
│   │   └── Wizard.java            # Player subclass (skin, arena, W/L record)
│   ├── network/
│   │   ├── GameClient.java        # TCP client, read loop, dispatches events
│   │   ├── GameServer.java        # Authoritative server, validates words, manages state
│   │   ├── Protocol.java          # Message constants and quote/unquote helpers
│   │   └── SessionManager.java    # Static session storage (client, names, profile)
│   ├── ui/
│   │   ├── ProfileController.java # Wizard profile selection screen
│   │   ├── MenuController.java    # Main menu
│   │   ├── ConnectController.java # Host / join LAN game
│   │   ├── ReadyController.java   # Ready check before wheel
│   │   ├── WheelController.java   # Mana wheel mini-game
│   │   ├── NetworkGameController.java # LAN game screen
│   │   ├── GameController.java    # Local game screen
│   │   ├── CosmeticsController.java   # Skin and arena selection
│   │   └── SkillCheckController.java  # (Stub, future use)
│   └── util/
│       └── ProfileManager.java    # Local profile save/load via Java Preferences
│
├── src/main/resources/com/rst/outspelled/
│   ├── profile-view.fxml
│   ├── menu-view.fxml
│   ├── connect-view.fxml
│   ├── ready-view.fxml
│   ├── wheel-view.fxml
│   ├── network-game-view.fxml
│   ├── game-view.fxml
│   ├── cosmetics-view.fxml
│   └── words.txt                  # Dictionary word list
│
└── pom.xml                        # Maven build config (JavaFX, ControlsFX, FXGL)
```

---

## Authors

Built as a course project for **CMSC 125** at the University of the Philippines Manila.
