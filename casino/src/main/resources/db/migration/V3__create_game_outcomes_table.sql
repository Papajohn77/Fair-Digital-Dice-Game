CREATE TABLE game_outcomes (
    id SMALLINT PRIMARY KEY,
    name VARCHAR(20) UNIQUE NOT NULL
);

INSERT INTO game_outcomes (id, name)
VALUES (1, 'SERVER_WIN'),
       (2, 'CLIENT_WIN'),
       (3, 'TIE'),
       (4, 'EXPIRED');
