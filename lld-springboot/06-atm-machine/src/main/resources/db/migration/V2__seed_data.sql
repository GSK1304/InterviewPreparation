-- PIN 1234 -> hashCode in hex
INSERT INTO bank_account VALUES ('ACC001','1234567890123456','923ec0af',5000000,'0',FALSE,'Alice Kumar');
INSERT INTO bank_account VALUES ('ACC002','9876543210987654','34b0ec6a',1000000,'0',FALSE,'Bob Sharma');
INSERT INTO bank_account VALUES ('ACC003','1111222233334444','923ec0af',  50000,'0',FALSE,'Carol Nair');

-- Cash cassettes: denomination -> note count
INSERT INTO cash_cassette VALUES (2000, 50);
INSERT INTO cash_cassette VALUES (500,  100);
INSERT INTO cash_cassette VALUES (200,  100);
INSERT INTO cash_cassette VALUES (100,  200);
