package ru.music.queue.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "queue_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueueItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID roomId;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(nullable = false, length = 500)
    private String artist;

    @Column(nullable = false)
    private Integer durationSec;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrackSource source;

    @Column(nullable = false, length = 500)
    private String externalId;

    @Column(length = 2048)
    private String streamUrl;

    private Instant streamUrlExpiresAt;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false)
    private UUID addedBy;
}