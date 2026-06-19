package ru.music.queue.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.music.queue.model.Queue;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueRepository extends JpaRepository<Queue, UUID> {
    Optional<Queue> findByRoomId(UUID roomId);
}