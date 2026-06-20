package ru.music.media.dto;

import java.util.UUID;

public record PlayRequest(UUID roomId, String youtubeUrl) {}
