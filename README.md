# Math Tower Defense 2D

A 2D tower defense game built with Java (Swing & Java2D) where mathematical challenges power up your defense. Correct answers boost towers with **Math Power** (+50% damage), while incorrect answers or timeouts weaken them.

---

## Features

- **Tower Defense Gameplay:** 10 waves, 4 tower types (Basic, Rapid, Power, Math Boost) with upgrades, and 4 enemy types (Normal, Fast, Tank, Boss).
- **Fish Game API Integration:** Live mathematical challenges fetched directly from the external Fish Game API.
- **User Authentication & Leaderboard:** Player accounts with secure hashed passwords (PBKDF2) and persistent leaderboard tracking (MySQL), plus guest play support.
- **Audio & Visuals:** Built-in retro sound effects, background music, custom artwork, and styled UI screens.

---

## Architecture

The project follows a modular, layered architecture that decouples rendering, simulation, and external services:

```
┌────────────────────────────────────────────────────────┐
│                   Presentation Layer                   │
│   Swing Views (MainGUI, GameGUI, MathChallengeOverlay) │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                   Engine & Managers                    │
│      GameLoop (60 FPS) ── GameEngine (State Machine)   │
│   WaveManager • TowerManager • EnemyManager • Sound    │
└──────────────┬──────────────────────────┬──────────────┘
               │                          │
┌──────────────▼─────────────┐ ┌──────────▼──────────────┐
│       Game Entities        │ │   Services & External   │
│  Towers, Enemies, Player,  │ │ • FishGameAPI (HTTP)    │
│  Projectiles, GameMap      │ │ • Auth & Score Services │
└────────────────────────────┘ │ • MySQL DAOs (JDBC)     │
                               └─────────────────────────┘
```

- **Presentation Layer (`gui`):** Swing-based user interface and rendering. Does not contain gameplay rules.
- **Engine Layer (`engine`, `managers`):** Drives the 60 FPS update loop, entity movement, collisions, and wave progressions.
- **Domain Layer (`entities`, `models`):** Encapsulates towers, enemies, projectiles, and player statistics.
- **Service & Data Layer (`api`, `authentication`, `database`):** Handles asynchronous HTTP requests to the Fish Game API, PBKDF2 password hashing, and MySQL persistence via JDBC.

---

## Project Structure

```
MathTowerDefense/
├── libs/                    # External libraries (MySQL Connector/J)
├── resources/               # Game assets
│   ├── images/              # UI backgrounds and artwork
│   └── sounds/              # Sound effects and music loop (.wav)
├── src/com/mathtowerdefense/
│   ├── api/                 # Fish Game API client & question handling
│   ├── authentication/      # Registration, login & PBKDF2 password hashing
│   ├── database/            # MySQL database connection & DAO layer
│   ├── engine/              # Game loop, state machine & audio manager
│   ├── entities/            # Towers, enemies, projectiles & player
│   ├── gui/                 # Swing views, overlays & dark theme
│   ├── managers/            # Wave, tower, score & question managers
│   ├── models/              # User, score & player statistics models
│   ├── tests/               # Automated unit & integration test runner
│   └── util/                # Resource loading utility
└── mathtowerdefense.sql     # Database schema and setup script
```

---

## Requirements

- **Java 17 (JDK) or higher**
- **MySQL / XAMPP** (optional – required only for accounts and leaderboard; guests can play without a database)
- **MySQL Connector/J** (`libs/mysql-connector-j-8.0.33.jar` included)

---

## How to Run

### In an IDE (Eclipse / IntelliJ)
1. Import the project as an **Existing Java Project**.
2. Ensure `src/` and `resources/` are set as source folders.
3. Ensure `libs/mysql-connector-j-8.0.33.jar` is on the build path / classpath.
4. Run `com.mathtowerdefense.gui.MainGUI` as a Java Application.

### From the Command Line
From the `MathTowerDefense` directory:

```bash
# Compile
javac -encoding UTF-8 -d bin -cp "libs/*;resources" (Get-ChildItem -Recurse -Filter *.java src | % { $_.FullName })

# Run Game
java -cp "bin;libs/*;resources" com.mathtowerdefense.gui.MainGUI

# Run Tests
java -cp "bin;libs/*" com.mathtowerdefense.tests.TestRunner
```

---

## Database Setup (Optional)

1. Start Apache & MySQL in **XAMPP Control Panel**.
2. Open **phpMyAdmin** (`http://localhost/phpmyadmin`).
3. Import `mathtowerdefense.sql` to create the `mathtowerdefense` database.
4. The application connects by default to `localhost:3306` with user `root` and no password (configurable via environment variables in `.env.example`).

---

## How to Play

### 1. Objective
Defend your base against 10 waves of enemies traveling along the path. If enemies reach the base, base health decreases. Defeat all 10 waves to win (Bosses arrive on waves 5 & 10).

### 2. Building & Upgrading Towers
- Select a tower from the **Tower Shop** on the right and click on grass to place it (towers cannot be placed on the road).
- Click any existing tower to inspect stats and upgrade it (Levels 1–4):
  - **Basic Tower:** Balanced cost, range, and damage.
  - **Rapid Tower:** High fire rate, ideal against fast enemies.
  - **Power Tower:** High single-shot damage, strong against tanks and bosses.
  - **Math Boost Tower:** Grants bonus coins and score on correct challenge answers.

### 3. Math Challenges
Periodically, the battle pauses for a math challenge fetched from the Fish Game API:
- **Solve:** Count the fish shown in the picture and submit the number.
- **Correct Answer:** Activates **Math Power** (+50% tower damage for 12s), +50 score, +15 coins, and builds combo multipliers.
- **Wrong / Timeout:** **Weakens** towers (-50% damage for 8s), deducts score/coins, and inflicts -5 base HP damage.

---

## Controls

| Control | Action |
|---|---|
| **Left Click** | Select shop item / place tower on map / inspect & upgrade tower |
| **Number Input + Enter / Submit** | Submit answer for math challenge |
| **P / Esc** | Pause / resume game |
