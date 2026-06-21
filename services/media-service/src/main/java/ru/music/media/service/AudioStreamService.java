package ru.music.media.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.music.media.entity.TrackMeta;
import ru.music.media.model.AudioStream;

@Service
@RequiredArgsConstructor
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
        ProcessBuilder ytDlp = new ProcessBuilder(
                "yt-dlp",
                "--print", "%(title)s\n%(uploader)s\n%(duration)s",
                youtubeUrl
        );

        Process process = ytDlp.start();
        String output = new String(process.getInputStream().readAllBytes()).trim();
        String[] lines = output.split("\n");

        String title = lines.length > 0 ? lines[0] : "Unknown";
        String artist = lines.length > 1 ? lines[1] : "Unknown";
        long duration = lines.length > 2 ? Long.parseLong(lines[2].trim()) : 0;

        return new TrackMeta(title, artist, duration);
    }
}
