CREATE TABLE game_statuses (
    id SMALLINT PRIMARY KEY,
    name VARCHAR(20) UNIQUE NOT NULL
);

INSERT INTO game_statuses (id, name)
VALUES (1, 'IN_PROGRESS'),
       (2, 'COMPLETED');
