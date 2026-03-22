CREATE TABLE book (
    isbn           VARCHAR(20)  PRIMARY KEY,
    title          VARCHAR(300) NOT NULL,
    author         VARCHAR(200) NOT NULL,
    genre          VARCHAR(50)  NOT NULL,
    published_year INTEGER
);

CREATE TABLE book_copy (
    barcode  VARCHAR(20) PRIMARY KEY,
    isbn     VARCHAR(20) NOT NULL,
    status   VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT fk_copy_book FOREIGN KEY (isbn) REFERENCES book(isbn)
);
CREATE INDEX idx_copy_isbn_status ON book_copy(isbn, status);

CREATE TABLE member (
    member_id              VARCHAR(20)  PRIMARY KEY,
    name                   VARCHAR(100) NOT NULL,
    email                  VARCHAR(100) NOT NULL UNIQUE,
    type                   VARCHAR(20)  NOT NULL,
    outstanding_fine_paise BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE loan (
    id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    member_id   VARCHAR(20) NOT NULL,
    barcode     VARCHAR(20) NOT NULL,
    borrow_date DATE        NOT NULL,
    due_date    DATE        NOT NULL,
    return_date DATE,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    fine_paise  BIGINT,
    CONSTRAINT fk_loan_member FOREIGN KEY (member_id) REFERENCES member(member_id),
    CONSTRAINT fk_loan_copy   FOREIGN KEY (barcode)   REFERENCES book_copy(barcode)
);

CREATE TABLE reservation (
    id            BIGINT      AUTO_INCREMENT PRIMARY KEY,
    member_id     VARCHAR(20) NOT NULL,
    isbn          VARCHAR(20) NOT NULL,
    reserved_date DATE        NOT NULL,
    active        BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_res_member FOREIGN KEY (member_id) REFERENCES member(member_id)
);
