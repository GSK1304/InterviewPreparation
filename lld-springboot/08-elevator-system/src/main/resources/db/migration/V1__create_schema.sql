CREATE TABLE elevator (
    elevator_id   VARCHAR(10)  PRIMARY KEY,
    name          VARCHAR(50)  NOT NULL,
    current_floor INTEGER      NOT NULL DEFAULT 0,
    min_floor     INTEGER      NOT NULL DEFAULT 0,
    max_floor     INTEGER      NOT NULL,
    max_capacity  INTEGER      NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'IDLE',
    direction     VARCHAR(10)  NOT NULL DEFAULT 'IDLE',
    pending_stops VARCHAR(200) DEFAULT ''
);

CREATE TABLE elevator_request (
    id                   BIGINT     AUTO_INCREMENT PRIMARY KEY,
    floor_number         INTEGER    NOT NULL,
    direction            VARCHAR(10) NOT NULL,
    assigned_elevator_id VARCHAR(10),
    requested_at         TIMESTAMP  NOT NULL,
    fulfilled            BOOLEAN    NOT NULL DEFAULT FALSE
);
