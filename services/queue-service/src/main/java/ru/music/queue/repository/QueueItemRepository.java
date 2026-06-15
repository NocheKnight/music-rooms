package ru.music.queue.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.music.queue.model.QueueItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QueueItemRepository extends JpaRepository<QueueItem, UUID> {

    List<QueueItem> findByRoomIdOrderByPositionAsc(UUID roomId);

    Optional<QueueItem> findByRoomIdAndId(UUID roomId, UUID trackId);

    @Query("SELECT COALESCE(MAX(q.position), -1) FROM QueueItem q WHERE q.roomId = :roomId")
    Integer findMaxPositionByRoomId(@Param("roomId") UUID roomId);

    @Modifying
    @Query("UPDATE QueueItem q SET q.position = q.position + 1 " +
            "WHERE q.roomId = :roomId AND q.position >= :fromPosition")
    void shiftPositionsForward(@Param("roomId") UUID roomId,
                               @Param("fromPosition") int fromPosition);

    @Modifying
    @Query("UPDATE QueueItem q SET q.position = q.position - 1 " +
            "WHERE q.roomId = :roomId AND q.position > :fromPosition")
    void shiftPositionsBackward(@Param("roomId") UUID roomId,
                                @Param("fromPosition") int fromPosition);

    @Modifying
    @Query("UPDATE QueueItem q SET q.position = q.position - 1 " +
            "WHERE q.roomId = :roomId AND q.position > :fromPosition AND q.position <= :toPosition")
    void shiftPositionsBetweenBackward(@Param("roomId") UUID roomId,
                                       @Param("fromPosition") int fromPosition,
                                       @Param("toPosition") int toPosition);

    @Modifying
    @Query("UPDATE QueueItem q SET q.position = q.position + 1 " +
            "WHERE q.roomId = :roomId AND q.position >= :fromPosition AND q.position < :toPosition")
    void shiftPositionsBetweenForward(@Param("roomId") UUID roomId,
                                      @Param("fromPosition") int fromPosition,
                                      @Param("toPosition") int toPosition);

    long deleteByRoomIdAndId(UUID roomId, UUID trackId);

    Optional<QueueItem> findFirstByRoomIdOrderByPositionAsc(UUID roomId);

    @Query("SELECT q FROM QueueItem q WHERE q.roomId = :roomId AND q.position = :position")
    Optional<QueueItem> findByRoomIdAndPosition(@Param("roomId") UUID roomId,
                                                @Param("position") int position);

    @Query("SELECT q FROM QueueItem q WHERE q.roomId = :roomId AND q.isCurrent = TRUE")
    Optional<QueueItem> findCurrentItemByRoomId(@Param("roomId") UUID roomI);
}