-- 3 floors with realistic spot distribution
INSERT INTO parking_floor (floor_number, name) VALUES
    (0, 'Ground Floor'),
    (1, 'First Floor'),
    (2, 'Second Floor');

-- Ground Floor: 5 MOTORCYCLE, 10 COMPACT, 3 LARGE (floor_id = 1)
INSERT INTO parking_spot (spot_code, size, floor_id) VALUES
    ('G-M-01','MOTORCYCLE',1), ('G-M-02','MOTORCYCLE',1), ('G-M-03','MOTORCYCLE',1),
    ('G-M-04','MOTORCYCLE',1), ('G-M-05','MOTORCYCLE',1),
    ('G-C-01','COMPACT',1),    ('G-C-02','COMPACT',1),    ('G-C-03','COMPACT',1),
    ('G-C-04','COMPACT',1),    ('G-C-05','COMPACT',1),    ('G-C-06','COMPACT',1),
    ('G-C-07','COMPACT',1),    ('G-C-08','COMPACT',1),    ('G-C-09','COMPACT',1),
    ('G-C-10','COMPACT',1),
    ('G-L-01','LARGE',1),      ('G-L-02','LARGE',1),      ('G-L-03','LARGE',1);

-- First Floor: 5 MOTORCYCLE, 10 COMPACT, 3 LARGE (floor_id = 2)
INSERT INTO parking_spot (spot_code, size, floor_id) VALUES
    ('F1-M-01','MOTORCYCLE',2), ('F1-M-02','MOTORCYCLE',2), ('F1-M-03','MOTORCYCLE',2),
    ('F1-M-04','MOTORCYCLE',2), ('F1-M-05','MOTORCYCLE',2),
    ('F1-C-01','COMPACT',2),    ('F1-C-02','COMPACT',2),    ('F1-C-03','COMPACT',2),
    ('F1-C-04','COMPACT',2),    ('F1-C-05','COMPACT',2),    ('F1-C-06','COMPACT',2),
    ('F1-C-07','COMPACT',2),    ('F1-C-08','COMPACT',2),    ('F1-C-09','COMPACT',2),
    ('F1-C-10','COMPACT',2),
    ('F1-L-01','LARGE',2),      ('F1-L-02','LARGE',2),      ('F1-L-03','LARGE',2);

-- Second Floor: 2 MOTORCYCLE, 8 COMPACT, 5 LARGE (floor_id = 3)
INSERT INTO parking_spot (spot_code, size, floor_id) VALUES
    ('F2-M-01','MOTORCYCLE',3), ('F2-M-02','MOTORCYCLE',3),
    ('F2-C-01','COMPACT',3),    ('F2-C-02','COMPACT',3),    ('F2-C-03','COMPACT',3),
    ('F2-C-04','COMPACT',3),    ('F2-C-05','COMPACT',3),    ('F2-C-06','COMPACT',3),
    ('F2-C-07','COMPACT',3),    ('F2-C-08','COMPACT',3),
    ('F2-L-01','LARGE',3),      ('F2-L-02','LARGE',3),      ('F2-L-03','LARGE',3),
    ('F2-L-04','LARGE',3),      ('F2-L-05','LARGE',3);
