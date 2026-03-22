CREATE TABLE space_game (
    id            BIGINT      AUTO_INCREMENT PRIMARY KEY,
    player_name   VARCHAR(50) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    score         INTEGER     NOT NULL DEFAULT 0,
    lives         INTEGER     NOT NULL DEFAULT 3,
    player_x      INTEGER     NOT NULL DEFAULT 400,
    player_y      INTEGER     NOT NULL DEFAULT 550,
    shield_active BOOLEAN     NOT NULL DEFAULT FALSE,
    rapid_fire    BOOLEAN     NOT NULL DEFAULT FALSE,
    wave_number   INTEGER     NOT NULL DEFAULT 1,
    enemies_killed INTEGER    NOT NULL DEFAULT 0,
    created_at    TIMESTAMP   NOT NULL,
    last_updated  TIMESTAMP   NOT NULL
);

CREATE TABLE game_event (
    id           BIGINT      AUTO_INCREMENT PRIMARY KEY,
    game_id      BIGINT      NOT NULL,
    event_type   VARCHAR(30) NOT NULL,
    details      VARCHAR(200) NOT NULL,
    occurred_at  TIMESTAMP   NOT NULL
);
CREATE INDEX idx_event_game ON game_event(game_id);
