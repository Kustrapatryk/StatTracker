CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE season (
    id          VARCHAR(7) PRIMARY KEY,               -- '2026-27'
    start_year  SMALLINT   NOT NULL UNIQUE,           -- 2026
    is_current  BOOLEAN    NOT NULL DEFAULT FALSE
);

CREATE TABLE team (
    id            BIGINT      PRIMARY KEY,            -- NBA TEAM_ID, e.g. 1610612760
    abbreviation  VARCHAR(3)  NOT NULL,               -- current abbreviation, e.g. 'OKC'
    city          VARCHAR(40) NOT NULL,
    name          VARCHAR(40) NOT NULL,               -- 'Thunder'
    conference    VARCHAR(4),                         -- 'East' / 'West'
    division      VARCHAR(10)
);

CREATE TABLE player (
    id               BIGINT       PRIMARY KEY,        -- NBA PERSON_ID, e.g. 1628983
    first_name       VARCHAR(60)  NOT NULL,
    last_name        VARCHAR(60)  NOT NULL,
    full_name        VARCHAR(120) NOT NULL,           -- 'Nikola Jokić'
    search_name      VARCHAR(120) NOT NULL,           -- 'nikola jokic': lower case, no diacritics (computed in Java)
    position         VARCHAR(10),                     -- 'G', 'F-C'
    jersey_number    VARCHAR(10),
    height           VARCHAR(10),                     -- '6-6'
    weight_lbs       SMALLINT,
    country          VARCHAR(60),
    draft_year       SMALLINT,
    current_team_id  BIGINT REFERENCES team (id),
    is_active        BOOLEAN      NOT NULL,
    from_year        SMALLINT,
    to_year          SMALLINT,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_player_search_name ON player USING gin (search_name gin_trgm_ops);

CREATE TABLE game (
    id            VARCHAR(10) PRIMARY KEY,            -- NBA GAME_ID, e.g. '0022600186'
    season_id     VARCHAR(7)  NOT NULL REFERENCES season (id),
    season_type   VARCHAR(10) NOT NULL CHECK (season_type IN ('REGULAR', 'PLAY_IN', 'PLAYOFFS')),
    game_date     DATE        NOT NULL,               -- US date (US/Eastern), as on NBA.com
    home_team_id  BIGINT      NOT NULL REFERENCES team (id),
    away_team_id  BIGINT      NOT NULL REFERENCES team (id),
    home_score    SMALLINT,
    away_score    SMALLINT,
    -- NBA Cup semifinals, games abroad: NBA lists both teams as visitors, home/away above is nominal
    neutral_site  BOOLEAN     NOT NULL DEFAULT FALSE,
    status        VARCHAR(10) NOT NULL DEFAULT 'FINAL',   -- SCHEDULED | FINAL (SCHEDULED arrives with the schedule import)
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_game_date ON game (game_date);

CREATE TABLE team_game_stats (
    game_id            VARCHAR(10) NOT NULL REFERENCES game (id),
    team_id            BIGINT      NOT NULL REFERENCES team (id),
    team_abbreviation  VARCHAR(3)  NOT NULL,          -- abbreviation on the game day, e.g. 'SEA', 'NJN'
    team_name          VARCHAR(60) NOT NULL,          -- name on the game day, e.g. 'Seattle SuperSonics'
    is_home            BOOLEAN     NOT NULL,
    win                BOOLEAN     NOT NULL,
    minutes            SMALLINT    NOT NULL,          -- 240 = no overtime
    pts  SMALLINT NOT NULL, reb  SMALLINT NOT NULL, ast  SMALLINT NOT NULL,
    fgm  SMALLINT NOT NULL, fga  SMALLINT NOT NULL,
    fg3m SMALLINT NOT NULL, fg3a SMALLINT NOT NULL,
    ftm  SMALLINT NOT NULL, fta  SMALLINT NOT NULL,
    oreb SMALLINT NOT NULL, dreb SMALLINT NOT NULL,
    stl  SMALLINT NOT NULL, blk  SMALLINT NOT NULL,
    tov  SMALLINT NOT NULL, pf   SMALLINT NOT NULL,
    plus_minus SMALLINT,
    PRIMARY KEY (game_id, team_id)
);

CREATE TABLE player_game_stats (
    game_id         VARCHAR(10) NOT NULL REFERENCES game (id),
    player_id       BIGINT      NOT NULL REFERENCES player (id),
    team_id         BIGINT      NOT NULL REFERENCES team (id),
    is_starter      BOOLEAN,                          -- NULL until enriched from the CDN box score
    seconds_played  INTEGER     NOT NULL,             -- game log MIN x 60; exact once enriched
    pts  SMALLINT NOT NULL, reb  SMALLINT NOT NULL, ast  SMALLINT NOT NULL,
    fgm  SMALLINT NOT NULL, fga  SMALLINT NOT NULL,
    fg3m SMALLINT NOT NULL, fg3a SMALLINT NOT NULL,
    ftm  SMALLINT NOT NULL, fta  SMALLINT NOT NULL,
    oreb SMALLINT NOT NULL, dreb SMALLINT NOT NULL,
    stl  SMALLINT NOT NULL, blk  SMALLINT NOT NULL,
    tov  SMALLINT NOT NULL, pf   SMALLINT NOT NULL,
    plus_minus SMALLINT,
    source          VARCHAR(20) NOT NULL,             -- 'NBA_GAMELOG' | 'NBA_BOXSCORE'
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (game_id, player_id)
);
CREATE INDEX ix_player_game_stats_player ON player_game_stats (player_id);

CREATE TABLE ingestion_run (
    id             BIGSERIAL   PRIMARY KEY,
    job            VARCHAR(30) NOT NULL,              -- BACKFILL | DAILY_SYNC
    params         JSONB       NOT NULL DEFAULT '{}', -- e.g. {"from": "2003-04", "to": "2026-27"}
    status         VARCHAR(10) NOT NULL,              -- RUNNING | SUCCESS | FAILED
    progress       VARCHAR(60),                       -- step in progress, e.g. '2019-20 PLAYOFFS'
    started_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    finished_at    TIMESTAMPTZ,
    rows_upserted  INTEGER     NOT NULL DEFAULT 0,
    error          TEXT
);

-- Sums per player, season, phase and team. Averages are computed from the sums in Java,
-- so a season split between two teams can be shown both per team and in total.
CREATE VIEW player_season_team_totals AS
SELECT pgs.player_id,
       g.season_id,
       g.season_type,
       pgs.team_id,
       MAX(tgs.team_abbreviation) AS team_abbreviation,
       MIN(g.game_date)           AS first_game_date,
       COUNT(*)                   AS games_played,
       SUM(pgs.seconds_played)    AS seconds_played,
       SUM(pgs.pts)  AS pts,  SUM(pgs.reb)  AS reb,  SUM(pgs.ast)  AS ast,
       SUM(pgs.fgm)  AS fgm,  SUM(pgs.fga)  AS fga,
       SUM(pgs.fg3m) AS fg3m, SUM(pgs.fg3a) AS fg3a,
       SUM(pgs.ftm)  AS ftm,  SUM(pgs.fta)  AS fta,
       SUM(pgs.oreb) AS oreb, SUM(pgs.dreb) AS dreb,
       SUM(pgs.stl)  AS stl,  SUM(pgs.blk)  AS blk,
       SUM(pgs.tov)  AS tov,  SUM(pgs.pf)   AS pf,
       SUM(pgs.plus_minus)        AS plus_minus,
       COUNT(pgs.plus_minus)      AS plus_minus_games
FROM player_game_stats pgs
JOIN game g ON g.id = pgs.game_id
JOIN team_game_stats tgs ON tgs.game_id = pgs.game_id AND tgs.team_id = pgs.team_id
GROUP BY pgs.player_id, g.season_id, g.season_type, pgs.team_id;
