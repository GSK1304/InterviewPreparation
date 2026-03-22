CREATE TABLE chess_game (
    id           BIGINT      AUTO_INCREMENT PRIMARY KEY,
    white_player VARCHAR(100) NOT NULL,
    black_player VARCHAR(100) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    current_turn VARCHAR(10) NOT NULL DEFAULT 'WHITE',
    total_moves  INTEGER     NOT NULL DEFAULT 0,
    created_at   TIMESTAMP   NOT NULL,
    board_state  VARCHAR(512)
);

CREATE TABLE chess_move (
    id                  BIGINT      AUTO_INCREMENT PRIMARY KEY,
    game_id             BIGINT      NOT NULL,
    move_number         INTEGER     NOT NULL,
    color               VARCHAR(10) NOT NULL,
    piece_type          VARCHAR(10) NOT NULL,
    from_col            INTEGER     NOT NULL,
    from_row            INTEGER     NOT NULL,
    to_col              INTEGER     NOT NULL,
    to_row              INTEGER     NOT NULL,
    captured_piece      VARCHAR(20),
    is_check            BOOLEAN     NOT NULL DEFAULT FALSE,
    is_checkmate        BOOLEAN     NOT NULL DEFAULT FALSE,
    algebraic_notation  VARCHAR(10),
    played_at           TIMESTAMP   NOT NULL,
    CONSTRAINT fk_move_game FOREIGN KEY (game_id) REFERENCES chess_game(id)
);
CREATE INDEX idx_move_game ON chess_move(game_id);
