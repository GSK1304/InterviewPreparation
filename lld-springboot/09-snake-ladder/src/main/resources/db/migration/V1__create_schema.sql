CREATE TABLE game (
    id                   BIGINT     AUTO_INCREMENT PRIMARY KEY,
    board_size           INTEGER    NOT NULL DEFAULT 100,
    status               VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    current_player_index INTEGER    NOT NULL DEFAULT 0,
    total_turns          INTEGER    NOT NULL DEFAULT 0,
    created_at           TIMESTAMP  NOT NULL,
    winner_name          VARCHAR(100)
);

CREATE TABLE game_player (
    id              BIGINT      AUTO_INCREMENT PRIMARY KEY,
    game_id         BIGINT      NOT NULL,
    player_name     VARCHAR(100) NOT NULL,
    player_token    VARCHAR(5)  NOT NULL,
    player_index    INTEGER     NOT NULL,
    position        INTEGER     NOT NULL DEFAULT 0,
    snake_bites     INTEGER     NOT NULL DEFAULT 0,
    ladders_climbed INTEGER     NOT NULL DEFAULT 0,
    turns_played    INTEGER     NOT NULL DEFAULT 0,
    CONSTRAINT fk_player_game FOREIGN KEY (game_id) REFERENCES game(id)
);

CREATE TABLE board_cell (
    id              BIGINT     AUTO_INCREMENT PRIMARY KEY,
    game_id         BIGINT     NOT NULL,
    position        INTEGER    NOT NULL,
    type            VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    target_position INTEGER,
    CONSTRAINT fk_cell_game FOREIGN KEY (game_id) REFERENCES game(id)
);
