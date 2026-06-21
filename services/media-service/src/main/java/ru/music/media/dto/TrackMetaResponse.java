package ru.music.media.dto;

public record TrackMetaResponse(
        String title,
        String artist,
        long durationSeconds,
        long positionSeconds
) {}
