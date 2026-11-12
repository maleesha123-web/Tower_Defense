-- ===========================================================================
-- Math Tower Defense 2D - database setup script
-- Target: MySQL / MariaDB (XAMPP)
--
--   phpMyAdmin -> Import -> choose this file -> Go
--   or:  mysql -u root < mathtowerdefense.sql
-- ===========================================================================

CREATE DATABASE IF NOT EXISTS mathtowerdefense
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE mathtowerdefense;

-- ---------------------------------------------------------------------------
-- Registered users. Passwords are stored ONLY as PBKDF2-SHA256 hashes
-- (see PasswordHasher.java). No plain-text passwords, ever.
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
  id             INT UNSIGNED NOT NULL AUTO_INCREMENT,
  username       VARCHAR(20)  NOT NULL,
  email          VARCHAR(120) NOT NULL,
  password_hash  VARCHAR(255) NOT NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uq_users_username (username),
  UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- One row per finished game (used by the leaderboard).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS scores (
  id                INT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id           INT UNSIGNED NOT NULL,
  score             INT          NOT NULL DEFAULT 0,
  level             INT          NOT NULL DEFAULT 1,
  wave              INT          NOT NULL DEFAULT 0,
  enemies_defeated  INT          NOT NULL DEFAULT 0,
  created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_scores_score (score),
  KEY idx_scores_user (user_id),
  CONSTRAINT fk_scores_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Aggregated lifetime statistics per user (used by the profile screen).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS player_stats (
  id                     INT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id                INT UNSIGNED NOT NULL,
  games_played           INT          NOT NULL DEFAULT 0,
  highest_score          INT          NOT NULL DEFAULT 0,
  highest_wave           INT          NOT NULL DEFAULT 0,
  correct_answers        INT          NOT NULL DEFAULT 0,
  incorrect_answers      INT          NOT NULL DEFAULT 0,
  total_enemies_defeated INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uq_stats_user (user_id),
  CONSTRAINT fk_stats_user FOREIGN KEY (user_id)
    REFERENCES users (id) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------------
-- Sample leaderboard records for demonstration/testing.
-- The password hashes below are PLACEHOLDERS and cannot be used to log in.
-- ---------------------------------------------------------------------------
INSERT INTO users (username, email, password_hash) VALUES
  ('MathMaster', 'mathmaster@example.com', 'pbkdf2$120000$sampleSeedOnlyNotARealHash$0000000000000000000000000000000000000000000000000000000000000000'),
  ('TowerKing',  'towerking@example.com',  'pbkdf2$120000$sampleSeedOnlyNotARealHash$0000000000000000000000000000000000000000000000000000000000000000'),
  ('PlayerFour', 'playerfour@example.com', 'pbkdf2$120000$sampleSeedOnlyNotARealHash$0000000000000000000000000000000000000000000000000000000000000000');

INSERT INTO scores (user_id, score, level, wave, enemies_defeated) VALUES
  ((SELECT id FROM users WHERE username='MathMaster'), 12450, 8, 9, 96),
  ((SELECT id FROM users WHERE username='TowerKing'),  10800, 7, 8, 81),
  ((SELECT id FROM users WHERE username='PlayerFour'),  9750, 6, 7, 74);

INSERT INTO player_stats (user_id, games_played, highest_score, highest_wave, correct_answers, incorrect_answers, total_enemies_defeated) VALUES
  ((SELECT id FROM users WHERE username='MathMaster'), 12, 12450, 9, 140, 22, 310),
  ((SELECT id FROM users WHERE username='TowerKing'),  10, 10800, 8, 118, 30, 264),
  ((SELECT id FROM users WHERE username='PlayerFour'),  8,  9750, 7,  95, 25, 219);
