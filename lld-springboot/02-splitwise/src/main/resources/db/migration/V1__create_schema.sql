CREATE TABLE app_user (
    user_id VARCHAR(20)  PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    email   VARCHAR(100) NOT NULL UNIQUE,
    phone   VARCHAR(15)  NOT NULL
);

CREATE TABLE expense_group (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_by VARCHAR(20)  NOT NULL
);

CREATE TABLE group_members (
    group_id BIGINT     NOT NULL,
    user_id  VARCHAR(20) NOT NULL,
    PRIMARY KEY(group_id, user_id),
    CONSTRAINT fk_gm_group FOREIGN KEY (group_id) REFERENCES expense_group(id)
);

CREATE TABLE expense (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    description     VARCHAR(200) NOT NULL,
    total_paise     BIGINT       NOT NULL,
    paid_by_user_id VARCHAR(20)  NOT NULL,
    split_type      VARCHAR(20)  NOT NULL,
    category        VARCHAR(30)  NOT NULL DEFAULT 'OTHER',
    group_id        BIGINT,
    created_at      TIMESTAMP    NOT NULL
);

CREATE TABLE expense_split (
    id           BIGINT     AUTO_INCREMENT PRIMARY KEY,
    expense_id   BIGINT     NOT NULL,
    user_id      VARCHAR(20) NOT NULL,
    amount_paise BIGINT     NOT NULL,
    CONSTRAINT fk_split_expense FOREIGN KEY (expense_id) REFERENCES expense(id)
);

CREATE TABLE balance (
    id           BIGINT     AUTO_INCREMENT PRIMARY KEY,
    debtor_id    VARCHAR(20) NOT NULL,
    creditor_id  VARCHAR(20) NOT NULL,
    amount_paise BIGINT     NOT NULL DEFAULT 0,
    UNIQUE(debtor_id, creditor_id)
);
