package ru.music.queue.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.music.queue.model.Queue;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueRepository extends JpaRepository<Queue, UUID> {
    Optional<Queue> findByRoomId(UUID roomId);
}