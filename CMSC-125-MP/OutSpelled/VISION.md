# OutSpelled — Vision & Implementation Status

## Game Vision (Summary)

- **Core:** Turn-based PvP wizard duel; players spell words from random letters to deal damage. Winner when one wizard reaches 0 HP.
- **Skill checks:** (1) **Wheel at match start** — who goes first. (2) **~50% HP** — initiator can challenge; shared grid; win = opponent drops to your HP, lose = you take word damage. (3) **~10% HP “Last Stand”** — jumbled word; first correct guess gets HP (amount + speed bonus).
- **Economy:** In-game currency from battles → buy cosmetics (skins, animations, arenas); cosmetic-only, no gameplay effect.
- **Platform:** LAN and online multiplayer; strategic turn-based with real-time elements (timers, animations).

---

## What’s Implemented

| Feature | Status | Where in code |
|--------|--------|----------------|
| Turn-based PvP, word = spell | ✅ | `GameEngine`, `GameController`, `LetterGrid` |
| Random letter grid per player | ✅ | `LetterGrid`, `LetterTile` |
| Damage = letter value + word length | ✅ | `Spell`, `LetterValues`, `DamageCalculator` |
| Dictionary validation | ✅ | `DictionaryLoader`, `WordValidator` |
| **Dictionary on background thread** | ✅ | `DictionaryLoader.loadAsync()`, `WordValidator.validateAsync()` |
| HP system + health bars | ✅ | `Player`/`Wizard`, `GameController` (ProgressBar) |
| **Turn timer (independent thread)** | ✅ | `TurnManager` — `ScheduledExecutorService` countdown |
| **Skill-check wheel at match start** | ✅ | `WheelController` — needle + zone, winner goes first; Menu → Wheel → Game |
| Half HP challenge (~50%) | ✅ | `SkillCheckManager` — shared grid, initiator win/lose logic |
| Last Stand (~10% HP) | ✅ | `SkillCheckManager` — scrambled word, first correct = HP gain by speed |
| Cosmetics (skins, arena) selection | ✅ | `CosmeticsController`, `Wizard.WizardSkin`, `ArenaBackground`; used in `MenuController` |
| **Shared state sync (single-machine)** | ✅ | `Collections.synchronizedList` (spell history), `synchronizedSet` (dictionary) |

---

## Threading Concepts (Current)

- **Dictionary validation thread** — `WordValidator` uses a single-thread executor; validation never blocks UI.
- **Turn timer thread** — `TurnManager` uses `ScheduledExecutorService`; ticks and “time expired” run off the JavaFX thread; UI updates via `Platform.runLater`.
- **Skill-check timer** — `GameController` runs a separate scheduled executor for Half HP / Last Stand countdown.
- **Wheel animation** — `WheelController` uses JavaFX `AnimationTimer` (FX thread) for smooth needle.
- **Shared state** — `spellHistory` is a synchronized list; dictionary is a synchronized set; no network yet so no cross-process locking.

---

## Not Yet Implemented (Roadmap)

### 1. In-game currency & shop
- **Goal:** Earn currency from battles; spend on skins, animations, arena backgrounds.
- **Needed:** A `Currency` or `PlayerProgress` model (e.g. balance, owned cosmetic IDs), persisted (e.g. file or DB). Shop UI that lists purchasable cosmetics and applies purchases. Reward currency on win/loss (e.g. more for win).

### 2. LAN / online multiplayer (sockets)
- **Goal:** Two players on different machines; shared game state over the network.
- **Needed:** Server (e.g. `ServerSocket`) and client (e.g. `Socket`); protocol for events (join, turn start/end, word submit, validation result, damage, HP, skill-check triggers, etc.). **Server and client handler threads** so I/O doesn’t block UI. **Network thread** for sending/receiving; game logic can stay on one “authority” (e.g. server) with **synchronized** or **ReentrantLock** on shared state (HP, turn order, match state) before sending updates to clients.

### 3. Spell animations
- **Goal:** When a spell hits, an animation plays without pausing the game.
- **Needed:** Visual effect (e.g. particle burst, projectile) triggered on `onSpellCast`; run on a separate thread or `AnimationTimer`/timeline so the turn timer and other logic continue. Optional: different effects per spell power (e.g. WEAK vs DEVASTATING).

### 4. Stronger sync (for future networking)
- When you add server/client, use **ReentrantLock** or **synchronized** around:
  - HP, turn order, “whose turn”, match state (e.g. WAITING, IN_PROGRESS, GAME_OVER).
  - Any shared structure (e.g. spell history, skill-check state) that multiple threads (network handlers, game logic) can touch. Keep critical sections short and push UI updates to the JavaFX thread with `Platform.runLater`.

---

## Suggested Next Steps

1. **Currency + shop (cosmetics unlock)** — Best way to complete the “earn from battles, buy cosmetics” loop without touching networking.
2. **Spell animation** — High impact on feel; can be done with current single-machine or hot-seat setup.
3. **LAN multiplayer** — Define a small wire protocol (e.g. JSON or simple binary), then implement server, client, and handler threads; sync HP, turn, and match state with locks as above.

If you tell me which of these you want to tackle first (currency/shop, spell animation, or LAN skeleton), I can outline or implement it step by step in your repo.
