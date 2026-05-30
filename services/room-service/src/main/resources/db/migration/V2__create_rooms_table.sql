CREATE TABLE rooms
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    name        VARCHAR(50) NOT NULL,
    invite_code VARCHAR(8)  NOT NULL UNIQUE,
    created_by  UUID        NOT NULL,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_rooms_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);