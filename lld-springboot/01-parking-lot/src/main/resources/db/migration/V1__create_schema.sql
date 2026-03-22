CREATE TABLE parking_floor (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    floor_number INTEGER      NOT NULL UNIQUE,
    name         VARCHAR(50)  NOT NULL
);

CREATE TABLE parking_spot (
    id        BIGINT      AUTO_INCREMENT PRIMARY KEY,
    spot_code VARCHAR(20) NOT NULL UNIQUE,
    size      VARCHAR(20) NOT NULL,
    status    VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    floor_id  BIGINT      NOT NULL,
    CONSTRAINT fk_spot_floor FOREIGN KEY (floor_id) REFERENCES parking_floor(id),
    CONSTRAINT chk_size   CHECK (size   IN ('MOTORCYCLE','COMPACT','LARGE')),
    CONSTRAINT chk_status CHECK (status IN ('AVAILABLE','OCCUPIED','MAINTENANCE'))
);

CREATE INDEX idx_spot_status_size ON parking_spot(status, size);

CREATE TABLE parking_ticket (
    ticket_id     VARCHAR(20)  PRIMARY KEY,
    license_plate VARCHAR(20)  NOT NULL,
    vehicle_type  VARCHAR(20)  NOT NULL,
    spot_id       BIGINT       NOT NULL,
    entry_time    TIMESTAMP    NOT NULL,
    exit_time     TIMESTAMP,
    fee_paise     BIGINT,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_ticket_spot FOREIGN KEY (spot_id) REFERENCES parking_spot(id)
);

CREATE INDEX idx_ticket_plate_active ON parking_ticket(license_plate, is_active);
