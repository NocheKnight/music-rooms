DROP TABLE IF EXISTS room_participants;

CREATE TABLE room_participants (
                                   room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
                                   user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                   PRIMARY KEY (room_id, user_id)
);