CREATE TABLE driver (
    driver_id      VARCHAR(20)  PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    phone          VARCHAR(15)  NOT NULL UNIQUE,
    vehicle_number VARCHAR(20)  NOT NULL,
    vehicle_model  VARCHAR(50)  NOT NULL,
    vehicle_type   VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    current_lat    DOUBLE,
    current_lng    DOUBLE,
    total_ratings  INTEGER      NOT NULL DEFAULT 0,
    rating_sum     DOUBLE       NOT NULL DEFAULT 0.0
);

CREATE TABLE rider (
    rider_id VARCHAR(20)  PRIMARY KEY,
    name     VARCHAR(100) NOT NULL,
    phone    VARCHAR(15)  NOT NULL UNIQUE,
    email    VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE ride (
    ride_id               VARCHAR(20) PRIMARY KEY,
    rider_id              VARCHAR(20) NOT NULL,
    driver_id             VARCHAR(20),
    vehicle_type          VARCHAR(20) NOT NULL,
    pickup_lat            DOUBLE      NOT NULL,
    pickup_lng            DOUBLE      NOT NULL,
    dropoff_lat           DOUBLE      NOT NULL,
    dropoff_lng           DOUBLE      NOT NULL,
    status                VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    estimated_fare_paise  BIGINT,
    actual_fare_paise     BIGINT,
    requested_at          TIMESTAMP   NOT NULL,
    started_at            TIMESTAMP,
    completed_at          TIMESTAMP
);
