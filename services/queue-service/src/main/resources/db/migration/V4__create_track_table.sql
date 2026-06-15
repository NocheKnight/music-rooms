CREATE TABLE IF NOT EXISTS queue_items (
    id UUID PRIMARY KEY,
    room_id UUID NOT NULL,
    name VARCHAR(500) NOT NULL,
    artist VARCHAR(500) NOT NULL,
    duration_sec INTEGER NOT NULL,
    source VARCHAR(20) NOT NULL,
    external_id VARCHAR(500) NOT NULL,
    stream_url VARCHAR(2048),
    stream_url_expires_at TIMESTAMP WITH TIME ZONE,
    position INTEGER NOT NULL,
    added_by UUID NOT NULL
);