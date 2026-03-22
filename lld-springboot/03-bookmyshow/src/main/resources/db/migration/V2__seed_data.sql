-- Show: Kalki 2898 AD
INSERT INTO show_event (movie_name, language, screen_name, show_time, duration_minutes, total_seats)
VALUES ('Kalki 2898 AD', 'Telugu', 'PVR Audi 1', DATEADD('DAY', 1, CURRENT_TIMESTAMP), 180, 50);

-- Show: RRR
INSERT INTO show_event (movie_name, language, screen_name, show_time, duration_minutes, total_seats)
VALUES ('RRR', 'Telugu', 'PVR Audi 2', DATEADD('DAY', 1, DATEADD('HOUR', 3, CURRENT_TIMESTAMP)), 187, 50);

-- Seats for Show 1: 5 rows x 10 cols
-- Row 0: RECLINER, Row 1-2: PREMIUM, Row 3: EXECUTIVE, Row 4: NORMAL
INSERT INTO seat (show_id, row_number, col_number, seat_code, tier, base_price_paise) VALUES
  -- Recliners
  (1,0,0,'R1',  'RECLINER',15000),(1,0,1,'R2', 'RECLINER',15000),(1,0,2,'R3', 'RECLINER',15000),
  (1,0,3,'R4',  'RECLINER',15000),(1,0,4,'R5', 'RECLINER',15000),(1,0,5,'R6', 'RECLINER',15000),
  (1,0,6,'R7',  'RECLINER',15000),(1,0,7,'R8', 'RECLINER',15000),(1,0,8,'R9', 'RECLINER',15000),(1,0,9,'R10','RECLINER',15000),
  -- Premium
  (1,1,0,'P11','PREMIUM',15000),(1,1,1,'P12','PREMIUM',15000),(1,1,2,'P13','PREMIUM',15000),(1,1,3,'P14','PREMIUM',15000),(1,1,4,'P15','PREMIUM',15000),
  (1,1,5,'P16','PREMIUM',15000),(1,1,6,'P17','PREMIUM',15000),(1,1,7,'P18','PREMIUM',15000),(1,1,8,'P19','PREMIUM',15000),(1,1,9,'P20','PREMIUM',15000),
  (1,2,0,'P21','PREMIUM',15000),(1,2,1,'P22','PREMIUM',15000),(1,2,2,'P23','PREMIUM',15000),(1,2,3,'P24','PREMIUM',15000),(1,2,4,'P25','PREMIUM',15000),
  (1,2,5,'P26','PREMIUM',15000),(1,2,6,'P27','PREMIUM',15000),(1,2,7,'P28','PREMIUM',15000),(1,2,8,'P29','PREMIUM',15000),(1,2,9,'P30','PREMIUM',15000),
  -- Executive
  (1,3,0,'E31','EXECUTIVE',15000),(1,3,1,'E32','EXECUTIVE',15000),(1,3,2,'E33','EXECUTIVE',15000),(1,3,3,'E34','EXECUTIVE',15000),(1,3,4,'E35','EXECUTIVE',15000),
  (1,3,5,'E36','EXECUTIVE',15000),(1,3,6,'E37','EXECUTIVE',15000),(1,3,7,'E38','EXECUTIVE',15000),(1,3,8,'E39','EXECUTIVE',15000),(1,3,9,'E40','EXECUTIVE',15000),
  -- Normal
  (1,4,0,'N41','NORMAL',15000),(1,4,1,'N42','NORMAL',15000),(1,4,2,'N43','NORMAL',15000),(1,4,3,'N44','NORMAL',15000),(1,4,4,'N45','NORMAL',15000),
  (1,4,5,'N46','NORMAL',15000),(1,4,6,'N47','NORMAL',15000),(1,4,7,'N48','NORMAL',15000),(1,4,8,'N49','NORMAL',15000),(1,4,9,'N50','NORMAL',15000);
