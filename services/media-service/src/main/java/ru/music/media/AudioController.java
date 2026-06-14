package ru.music.media;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/audio")
public class AudioController {
    private final AudioStreamService audioStreamService;

    @GetMapping("/stream")
    public ResponseEntity<StreamingResponseBody> stream(
            @RequestParam("url") String url) {

        StreamingResponseBody body = outputStream -> {
            try {
                InputStream mp3Stream = audioStreamService.getAudioStream(url);

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = mp3Stream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }

            } catch (Exception e) {
                throw new RuntimeException("Ошибка стриминга: " + e.getMessage());
            }
        };

        return ResponseEntity.ok()
                .header("Content-Type", "audio/mpeg")
                .header("Transfer-Encoding", "chunked")
                .body(body);
    }
}
