ALTER TABLE family_account ADD COLUMN stream_epoch uuid NOT NULL DEFAULT gen_random_uuid();
CREATE TABLE learning_event (
    event_id uuid PRIMARY KEY,
    child_id bigint NOT NULL REFERENCES child_profile(id),
    device_id uuid NOT NULL REFERENCES client_device(device_id),
    learning_day date NOT NULL,
    occurred_at timestamptz NOT NULL,
    step_id varchar(32) NOT NULL,
    target_id varchar(80),
    request_hash char(64) NOT NULL,
    commit_cursor bigint NOT NULL DEFAULT 0
);
CREATE INDEX ix_event_day_step_target ON learning_event(child_id, learning_day, step_id, target_id);
CREATE TABLE daily_step_progress (
    child_id bigint NOT NULL REFERENCES child_profile(id),
    learning_day date NOT NULL,
    step_id varchar(32) NOT NULL,
    PRIMARY KEY (child_id, learning_day, step_id)
);
CREATE TABLE star_ledger (
    child_id bigint NOT NULL REFERENCES child_profile(id),
    grant_key text NOT NULL,
    learning_day date NOT NULL,
    event_id uuid NOT NULL REFERENCES learning_event(event_id),
    delta integer NOT NULL CHECK (delta = 1),
    PRIMARY KEY (child_id, grant_key)
);
CREATE TABLE daily_checkin (
    child_id bigint NOT NULL REFERENCES child_profile(id),
    learning_day date NOT NULL,
    PRIMARY KEY (child_id, learning_day)
);
CREATE TABLE badge_award (
    child_id bigint NOT NULL,
    learning_day date NOT NULL,
    badge_code text NOT NULL,
    PRIMARY KEY (child_id, learning_day, badge_code),
    FOREIGN KEY (child_id, learning_day) REFERENCES daily_checkin(child_id, learning_day)
);
CREATE TABLE sync_change_log (
    seq bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    child_id bigint NOT NULL REFERENCES child_profile(id),
    resource text NOT NULL,
    value jsonb NOT NULL
);
CREATE INDEX ix_sync_change_child_seq ON sync_change_log(child_id, seq);
