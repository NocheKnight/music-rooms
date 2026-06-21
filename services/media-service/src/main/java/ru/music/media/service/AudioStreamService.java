package ru.music.media.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.music.media.entity.TrackMeta;
import ru.music.media.model.AudioStream;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AudioStreamService {
    public AudioStream getAudioStream(String youtubeUrl) throws Exception {

        ProcessBuilder ytDlp = new ProcessBuilder(
                "yt-dlp",
                "-f", "bestaudio",
                "--get-url",
                youtubeUrl
        );

        Process ytDlpProcess = ytDlp.start();

        String audioUrl = new String(ytDlpProcess.getInputStream().readAllBytes()).trim();

        if (audioUrl.isEmpty()) {
            throw new RuntimeException("yt-dlp не смог получить URL для: " + youtubeUrl);
        }

        ProcessBuilder ffmpeg = new ProcessBuilder(
                "ffmpeg",
                "-i", audioUrl,      // входной URL
                "-vn",               // без видео
                "-acodec", "libmp3lame", // кодек MP3
                "-ab", "128k",       // битрейт
                "-f", "mp3",         // формат вывода
                "pipe:1"             // писать в stdout
        );

        ffmpeg.redirectErrorStream(false);

        Process ffmpegProcess = ffmpeg.start();

        return new AudioStream(ffmpegProcess.getInputStream(), ffmpegProcess);
    }

    public TrackMeta getTrackMeta(String youtubeUrl) throws Exception {
        log.info("getTrackMeta start: {}", youtubeUrl);

        ProcessBuilder ytDlp = new ProcessBuilder(
                "yt-dlp",
                "--no-playlist",
                "--print", "%(title)s\n%(uploader)s\n%(duration)s",
                youtubeUrl
        );

        log.info("Starting yt-dlp process");
        Process process = ytDlp.start();

        log.info("Waiting for yt-dlp process");
        boolean finished = process.waitFor(15, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            log.error("yt-dlp timed out for url: {}", youtubeUrl);
            throw new RuntimeException("yt-dlp timeout");
        }

        log.info("yt-dlp finished with exit code: {}", process.exitValue());

        String stderr = new String(process.getErrorStream().readAllBytes()).trim();
        if (!stderr.isEmpty()) {
            log.warn("yt-dlp stderr: {}", stderr);
        }

        String output = new String(process.getInputStream().readAllBytes()).trim();
        log.info("yt-dlp output: {}", output);

        String[] lines = output.split("\n");
        String title = lines.length > 0 ? lines[0] : "Unknown";
        String artist = lines.length > 1 ? lines[1] : "Unknown";
        long duration = lines.length > 2 ? Long.parseLong(lines[2].trim()) : 0;

        log.info("getTrackMeta done: title={}, duration={}", title, duration);
        return new TrackMeta(title, artist, duration);
    }
}
