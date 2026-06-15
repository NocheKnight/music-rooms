package ru.music.media.model;

import java.io.InputStream;

public record AudioStream(InputStream inputStream, Process ffmpegProcess) {}
