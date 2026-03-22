CREATE TABLE bank_account (
    account_number VARCHAR(20)  PRIMARY KEY,
    card_number    VARCHAR(16)  NOT NULL UNIQUE,
    hashed_pin     VARCHAR(50)  NOT NULL,
    balance_paise  BIGINT       NOT NULL,
    failed_attempts INTEGER     NOT NULL DEFAULT 0,
    blocked        BOOLEAN      NOT NULL DEFAULT FALSE,
    account_holder VARCHAR(100) NOT NULL
);

CREATE TABLE cash_cassette (
    denomination INTEGER PRIMARY KEY,
    note_count   INTEGER NOT NULL
);

CREATE TABLE atm_transaction (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20)  NOT NULL,
    masked_card    VARCHAR(20)  NOT NULL,
    type           VARCHAR(30)  NOT NULL,
    amount_paise   BIGINT,
    status         VARCHAR(20)  NOT NULL,
    timestamp      TIMESTAMP    NOT NULL,
    dispensed_notes VARCHAR(100),
    remarks        VARCHAR(200)
);
CREATE INDEX idx_txn_account ON atm_transaction(account_number);
