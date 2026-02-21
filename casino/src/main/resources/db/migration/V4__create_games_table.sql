CREATE TABLE games (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    status_id SMALLINT NOT NULL REFERENCES game_statuses(id),
    outcome_id SMALLINT REFERENCES game_outcomes(id),
    server_roll SMALLINT NOT NULL,
    client_roll SMALLINT,
    r_a VARCHAR(64) NOT NULL,
    r_b VARCHAR(64) NOT NULL,
    h_commit VARCHAR(64) NOT NULL,
    initiated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_games_user_id ON games(user_id);
