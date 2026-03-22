CREATE TABLE room (
    room_number     VARCHAR(10)  PRIMARY KEY,
    type            VARCHAR(20)  NOT NULL,
    bed_type        VARCHAR(20)  NOT NULL,
    capacity        INTEGER      NOT NULL,
    floor_number    INTEGER      NOT NULL,
    base_rate_paise BIGINT       NOT NULL
);

CREATE TABLE guest (
    id       BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    email    VARCHAR(100) NOT NULL UNIQUE,
    phone    VARCHAR(15)  NOT NULL,
    id_proof VARCHAR(50)  NOT NULL
);

CREATE TABLE hotel_reservation (
    reservation_id VARCHAR(20) PRIMARY KEY,
    guest_id       BIGINT      NOT NULL,
    room_number    VARCHAR(10) NOT NULL,
    check_in       DATE        NOT NULL,
    check_out      DATE        NOT NULL,
    guest_count    INTEGER     NOT NULL,
    total_paise    BIGINT      NOT NULL,
    status         VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    created_at     TIMESTAMP   NOT NULL,
    CONSTRAINT fk_res_guest FOREIGN KEY (guest_id) REFERENCES guest(id),
    CONSTRAINT fk_res_room  FOREIGN KEY (room_number) REFERENCES room(room_number)
);

CREATE TABLE reservation_amenities (
    reservation_id VARCHAR(20) NOT NULL,
    amenity        VARCHAR(20) NOT NULL,
    PRIMARY KEY (reservation_id, amenity)
);
