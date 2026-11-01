-- V4 (Phase 5): a student's accommodation preferences, used by the suitability score.
-- Lists (ranked room types, amenity codes) are stored as short comma-separated text: simple to read and map,
-- and they are never searched inside by SQL.

CREATE TABLE student_preferences (
    user_id                 BIGINT       PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    budget_max              INTEGER      NOT NULL,
    max_commute_minutes     INTEGER      NOT NULL,
    commute_mode            VARCHAR(20)  NOT NULL DEFAULT 'ANY',
    room_choices            VARCHAR(100) NOT NULL DEFAULT '',
    must_have_amenities     VARCHAR(500) NOT NULL DEFAULT '',
    nice_to_have_amenities  VARCHAR(500) NOT NULL DEFAULT '',
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_prefs_budget  CHECK (budget_max BETWEEN 1000 AND 200000),
    CONSTRAINT ck_prefs_commute CHECK (max_commute_minutes BETWEEN 5 AND 180),
    CONSTRAINT ck_prefs_mode    CHECK (commute_mode IN ('ANY', 'WALK', 'AUTO_RICKSHAW', 'BUS', 'LOCAL_TRAIN'))
);
