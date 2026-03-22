INSERT INTO app_user VALUES ('U1','Alice Kumar','alice@example.com','9876543210');
INSERT INTO app_user VALUES ('U2','Bob Sharma',  'bob@example.com',  '9876543211');
INSERT INTO app_user VALUES ('U3','Carol Nair',  'carol@example.com','9876543212');
INSERT INTO app_user VALUES ('U4','Dave Reddy',  'dave@example.com', '9876543213');

INSERT INTO expense_group (name, created_by) VALUES ('Goa Trip 2025','U1');
INSERT INTO group_members VALUES (1,'U1'),(1,'U2'),(1,'U3'),(1,'U4');
