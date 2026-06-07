package ru.music.media;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class AudioStreamService {
    public InputStream getAudioStream(String youtubeUrl) throws Exception {

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

        return ffmpegProcess.getInputStream();
    }
}
