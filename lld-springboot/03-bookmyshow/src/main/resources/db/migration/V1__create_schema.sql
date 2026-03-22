CREATE TABLE show_event (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    movie_name       VARCHAR(200) NOT NULL,
    language         VARCHAR(50)  NOT NULL,
    screen_name      VARCHAR(50)  NOT NULL,
    show_time        TIMESTAMP    NOT NULL,
    duration_minutes INTEGER      NOT NULL,
    total_seats      INTEGER      NOT NULL
);

CREATE TABLE seat (
    id                    BIGINT      AUTO_INCREMENT PRIMARY KEY,
    show_id               BIGINT      NOT NULL,
    row_number            INTEGER     NOT NULL,
    col_number            INTEGER     NOT NULL,
    seat_code             VARCHAR(10) NOT NULL,
    tier                  VARCHAR(20) NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    locked_by_booking     VARCHAR(20),
    lock_expiry           TIMESTAMP,
    base_price_paise      BIGINT      NOT NULL,
    CONSTRAINT fk_seat_show FOREIGN KEY (show_id) REFERENCES show_event(id),
    CONSTRAINT uq_seat_code UNIQUE(show_id, seat_code)
);
CREATE INDEX idx_seat_show_status ON seat(show_id, status);

CREATE TABLE booking (
    booking_id  VARCHAR(20)  PRIMARY KEY,
    user_id     VARCHAR(50)  NOT NULL,
    show_id     BIGINT       NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'LOCKED',
    total_paise BIGINT       NOT NULL,
    lock_expiry TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    CONSTRAINT fk_booking_show FOREIGN KEY (show_id) REFERENCES show_event(id)
);

CREATE TABLE booking_seats (
    booking_id VARCHAR(20) NOT NULL,
    seat_id    BIGINT      NOT NULL,
    PRIMARY KEY (booking_id, seat_id)
);
