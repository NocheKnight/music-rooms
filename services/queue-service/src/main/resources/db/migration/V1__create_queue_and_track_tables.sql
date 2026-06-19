-- Создание таблицы queues (очереди)
CREATE TABLE IF NOT EXISTS queues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL UNIQUE,
    current_track_index INTEGER
);

-- Создание таблицы tracks (треки)
CREATE TABLE IF NOT EXISTS tracks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    artist VARCHAR(100) NOT NULL,
    duration_sec INTEGER NOT NULL,
    source VARCHAR(20) NOT NULL,
    stream_url VARCHAR(1024),
    stream_url_expires_at TIMESTAMP WITH TIME ZONE,
    added_by UUID NOT NULL,
    queue_id UUID NOT NULL,
    CONSTRAINT fk_tracks_queue FOREIGN KEY (queue_id) REFERENCES queues(id) ON DELETE CASCADE
);

-- Индексы для ускорения запросов
CREATE INDEX IF NOT EXISTS idx_queues_room_id ON queues(room_id);
CREATE INDEX IF NOT EXISTS idx_tracks_queue_id ON tracks(queue_id);

-- Удаление устаревшей таблицы
DROP TABLE IF EXISTS queue_items;