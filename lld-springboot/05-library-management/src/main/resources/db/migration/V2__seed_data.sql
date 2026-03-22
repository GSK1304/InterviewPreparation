INSERT INTO book VALUES ('9780134685991','Effective Java',        'Joshua Bloch',  'Programming',2018);
INSERT INTO book VALUES ('9780132350884','Clean Code',            'Robert Martin', 'Programming',2008);
INSERT INTO book VALUES ('9780201633610','Design Patterns',       'Gang of Four',  'Programming',1994);
INSERT INTO book VALUES ('9780135957059','The Pragmatic Programmer','Dave Thomas',  'Programming',2019);
INSERT INTO book VALUES ('9780062316097','The Alchemist',         'Paulo Coelho',  'Fiction',    1988);

-- 2 copies each
INSERT INTO book_copy VALUES ('BC-001','9780134685991','AVAILABLE');
INSERT INTO book_copy VALUES ('BC-002','9780134685991','AVAILABLE');
INSERT INTO book_copy VALUES ('BC-003','9780132350884','AVAILABLE');
INSERT INTO book_copy VALUES ('BC-004','9780132350884','AVAILABLE');
INSERT INTO book_copy VALUES ('BC-005','9780201633610','AVAILABLE');
INSERT INTO book_copy VALUES ('BC-006','9780201633610','AVAILABLE');
INSERT INTO book_copy VALUES ('BC-007','9780135957059','AVAILABLE');
INSERT INTO book_copy VALUES ('BC-008','9780062316097','AVAILABLE');
INSERT INTO book_copy VALUES ('BC-009','9780062316097','AVAILABLE');

INSERT INTO member VALUES ('M001','Alice Kumar', 'alice@example.com','STUDENT', 0);
INSERT INTO member VALUES ('M002','Bob Sharma',  'bob@example.com',  'FACULTY', 0);
INSERT INTO member VALUES ('M003','Carol Nair',  'carol@example.com','PUBLIC',  0);
