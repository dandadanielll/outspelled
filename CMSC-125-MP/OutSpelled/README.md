# OutSpelled

OutSpelled is a 1-on-1, competitive word-finding game with a fantasy wizard theme. Two players battle each other by casting spells, which are formed by finding words on a 4x4 grid of letters. The goal is to reduce your opponent's HP to zero.

## Features

- **Fantasy Wizard Theme:** Play as a powerful mage battling an opponent.
- **Word-Based Combat:** The power of your spells depends on the words you form. Longer and more complex words result in more damage.
- **1v1 Network Play:** Play against a friend over a local network (LAN).
- **Turn-Based Gameplay:** Each player has a limited time to find a word and cast their spell.
- **Skill Checks:** Special events like "Half HP Challenge" and "Last Stand" keep matches dynamic and exciting.

## Requirements

- Java Development Kit (JDK) 17 or newer.
- Apache Maven.

## Building and Running

1.  **Navigate to the project directory:**
    ```
    cd CMSC-125-MP/OutSpelled
    ```

2.  **Build with Maven:**
    This command will compile the code and prepare it for running.
    ```
    mvn clean compile
    ```

3.  **Run the game with Maven:**
    ```
    mvn javafx:run
    ```

## How to Play Online

1.  **Launch the game** on both players' computers.
2.  **The Host:**
    - On the main menu, click `Play`.
    - The screen will display **"Your IP for hosting: ..."**. Share this IP address with the other player.
    - Click `Host Game`.
3.  **The Player Joining:**
    - On the main menu, click `Play`.
    - In the "Server IP" box, type the IP address the host gave you.
    - Click `Join Game`.
4.  Once connected, both players will see a "Ready" screen. Click `Ready` to begin the match.
