# MATH TOWER DEFENSE 2D

A complete, playable 2D tower-defense game written in pure Java (Swing/Java2D)
where mathematical questions drive the combat: correct answers charge your
towers with **Math Power** (+50% damage), wrong answers or timeouts weaken them
(−50% damage) plus health/coin penalties.

**Course:** Java Software Engineering / Game Development (academic assessment)

---

## 1. Features

- **Full game flow:** START → LOGIN / REGISTER (or guest) → MAIN MENU → GAME
  (10 waves, bosses on waves 5 & 10) → VICTORY / GAME OVER → SCORE saved →
  LEADERBOARD
- **Fish Game API integration:** questions are fetched live from the official
  service; images are displayed and parsed formats are exactly the documented
  ones (`?out=json`, `?out=json&base64=yes`, `?out=csv`)
- **Failure-proof API layer:** on any network problem the game shows
  "Unable to connect to Math Question Service." and continues in clearly
  labelled **OFFLINE FALLBACK MODE** (local arithmetic questions)
- **4 tower types** (Basic, Rapid, Power, Math Boost) with 4 upgrade levels,
  visible projectiles, first-in-range targeting, range rings, level pips
- **4 enemy types** (Normal, Fast, Tank, Boss) with health bars + crown for
  bosses, particles and floating score text on kills
- **Scoring & coins:** kills +100 (boss +1000), correct +50, wave +500,
  wrong −25, combo bonuses at 3/5/10, coins for everything, tower upgrades
- **Authentication:** register/login with PBKDF2-SHA256 hashed passwords,
  sessions, logout, guest play
- **MySQL persistence:** scores + lifetime statistics + leaderboard
- **Sound:** 12 synthesized SFX + an 8-second background-music loop, all
  procedurally generated (13 real `.wav` files ship in `resources/sounds`;
  missing files are re-rendered live from the same recipes - sound never
  crashes the game)
- **Pause, settings (sound/music/difficulty), help, profile, game-over screens**
- **~150 automated tests** run with zero dependencies

---

## 2. Technologies

| Component | Technology |
|---|---|
| Language | Java 17 (LTS) |
| GUI / rendering | Swing + Java2D (zero frameworks) |
| HTTP | `java.net.HttpURLConnection` (stdlib) |
| JSON | minimal field extraction (stdlib regex, documented 3-field response) |
| Database | MySQL / MariaDB via JDBC (`java.sql` only — compiles without the JAR) |
| Passwords | PBKDF2WithHmacSHA256 + random salt (stdlib) |
| Build | `javac` directly, or Eclipse (`.project` / `.classpath` included) |

---

## 3. Project structure

```
MathTowerDefense/
├── .project / .classpath / .settings   Eclipse metadata (import-ready)
├── .env.example                        safe config template (NO real secrets)
├── mathtowerdefense.sql                XAMPP database setup script
├── README.md
├── libs/                               optional: mysql-connector-j.jar (runtime)
├── resources/                          optional images / sounds / fonts
│   (game runs fine when these are empty - all visuals have Java2D fallbacks)
└── src/com/mathtowerdefense/
    ├── engine/        Game, GameEngine, GameLoop, GameState, GameMap,
    │                  GameConfig, CollisionManager, SoundManager
    ├── entities/      Player, Base, Enemy(+EnemyType), Tower(+TowerType,
    │                  Basic/Rapid/Power/MathBoost), Projectile, Wave
    ├── gui/           MainGUI (main()), Login-, Register-, MainMenu-, Game-,
    │                  GameBoard(panel), MathChallengeOverlay, Profile-,
    │                  Leaderboard-, Help-, Settings-, GameOver-GUI, Theme
    ├── authentication/ LoginService, RegisterService, SessionManager, PasswordHasher
    ├── api/           FishGameAPI, Question, APIResponse, ApiConfig, LocalQuestionGenerator
    ├── managers/      WaveManager, TowerManager, EnemyManager, ScoreManager,
    │                  LevelManager, QuestionManager, ScoreService
    ├── database/      DatabaseConnection, UserDAO, ScoreDAO, LeaderboardDAO
    ├── models/        User, Score, PlayerStats
    └── tests/         TestRunner (plain-Java assertions)
```

Architecture (assessment-friendly):

```
              ┌──────────────────────┐
              │       GUI Layer      │  MainGUI + screens
              └──────────┬───────────┘
                         │
              ┌──────────▼───────────┐
              │    Service Layer     │  Login/Register/ScoreService
              └──────────┬───────────┘
                         │
      ┌──────────────────┼──────────────────┐
      │                  │                  │
      ▼                  ▼                  ▼
 Game Managers       Fish Game API       DAO Layer
  (waves/towers/    (QuestionManager      (User/Score/
   enemies/score)    -> FishGameAPI)       Leaderboard)
      │                  │                  │
      ▼                  ▼                  ▼
 Game Entities    External Service       MySQL
 (Enemy/Tower)    (marcconrad.com)
      │
      ▼
  Game Engine (state machine + 60 FPS loop)
```
---

## 4. Fish Game API integration (external service)

Official documentation: <https://marcconrad.com/uob/fish/doc.php>
Examples: <https://marcconrad.com/uob/fish/>

| Item | Value (exactly as documented) |
|---|---|
| Base URL | `http://marcconrad.com/uob/fish/api.php` |
| JSON | `?out=json` → `{ "question" : "<image url>", "fish" : 9, "chest" : 1 }` |
| Base64 | `?out=json&base64=yes` → `question` holds the PNG as base64 |
| CSV | `?out=csv` → `<image url>,9,1` |
| **Auth** | **none** — the docs define no API key |

Code: `com.mathtowerdefense.api` (`FishGameAPI`, `Question`, `APIResponse`,
`ApiConfig`) is isolated from game logic; the engine talks only to
`managers/QuestionManager`.

Semantics: the official game asks *"Enter the number of fish in the picture"*,
so the answer used is the documented `fish` value. The request strategy is:

1. `?out=json` and download the image URL (manual 301 http→https redirect
   following — Java does *not* auto-follow cross-scheme redirects);
2. if the image cannot be fetched, retry with the documented
   `?out=json&base64=yes` variant;
3. if everything fails, `QuestionManager` generates a **local** question with
   `Question.Source.LOCAL_FALLBACK`. The HUD/badge always shows
   **API MODE** vs **OFFLINE FALLBACK MODE**, so a local question never
   pretends to be from the API.

`FISH_API_KEY` (see `.env.example`, `ApiConfig`) is provided only as a central
configuration hook — per the official documentation it is **not required and
never sent**. Do not invent request parameters beyond the documented `out` and
`base64`.

---

## 5. Database setup (XAMPP)

1. Start **Apache + MySQL** in the XAMPP Control Panel.
2. Open phpMyAdmin → **Import** → choose `mathtowerdefense.sql` → Go
   (or `mysql -u root < mathtowerdefense.sql`).
3. This creates the database `mathtowerdefense` with tables `users`,
   `scores`, `player_stats` plus a few sample leaderboard rows. The sample
   users' hashes are placeholders and cannot be used to log in.

Connection (env vars, see `.env.example`; XAMPP defaults otherwise):

```
MTD_DB_URL=jdbc:mysql://localhost:3306/mathtowerdefense?...
MTD_DB_USER=root
MTD_DB_PASSWORD=
```

Quick start for a working login/leaderboard:

1. Download **MySQL Connector/J** (mysql-connector-j-8.x.jar) into `libs/`
   and, in Eclipse, *Build Path → Add JARs…* (the code imports only
   `java.sql`, so it compiles even without the JAR).
2. Run the game, **Create Account**, log in, play.

Database unavailability is handled everywhere with friendly messages; the game
remains fully playable via **Play as Guest** (scores simply are not saved).

---

## 6. Import into Eclipse & run

1. **File → Import… → General → Existing Projects into Workspace** →
   select the `MathTowerDefense` folder → Finish.
   (The folder contains `.project` / `.classpath` / `.settings` with Java 17
   compliance, and `src` + `resources` as source folders.)
2. (Optional for database) add `libs/mysql-connector-j-*.jar` to the build path
   and start MySQL as described above.
3. **Main class:** `com.mathtowerdefense.gui.MainGUI` →
   *Run As → Java Application*.

Command line (from the project folder):

```
Get-ChildItem src -Recurse -Filter *.java | % { $_.FullName } |
    Out-File -Encoding ascii sources.txt
javac -encoding UTF-8 -d bin @sources.txt
xcopy /E /I /Y resources bin
java -cp bin com.mathtowerdefense.gui.MainGUI
java -cp bin com.mathtowerdefense.tests.TestRunner        # 154 tests
java -cp bin com.mathtowerdefense.tests.TestRunner --live # + 1 real API call
java -cp bin com.mathtowerdefense.tests.SoundAssetExporter # regenerate the 13 .wav files
```

## 7. Game controls

| Input | Action |
|---|---|
| Mouse click (shop, then map) | build tower |
| Mouse click on existing tower | select / inspect / upgrade |
| Esc or P | pause / resume |
| Text field + ENTER / SUBMIT | answer a math challenge |
| Buttons | coins guard everything (no negative balances) |

## 8. Known limitations

- Leaderboard/profile require MySQL running (guests play without saving).
- Challenge images are one per API call; rapid question spam is limited by
  the 15 s challenge cadence.
- The official service may be discontinued without notice (stated on the doc
  page) — the game still works fully in offline fallback mode.
- Sound files are tiny 8-bit retro-style tones; audiophiles can replace any
  `.wav` in `resources/sounds/` (same name, same format) — or regenerate them
  with the exporter.

## 9. Credits

- Fish Game API & documentation © Marc Conrad
  (<https://marcconrad.com/uob/fish/>) — used strictly as documented.
- Everything else (engine, entities, GUI, database): this project.
