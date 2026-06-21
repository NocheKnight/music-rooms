package ru.music.media.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.music.media.model.TrackSource;

@Data
public class AddTrackRequest {

    @NotBlank(message = "Track name is required")
    @Size(max = 500)
    private String name;

    @NotBlank(message = "Artist is required")
    @Size(max = 500)
    private String artist;

    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 second")
    private Integer durationSec;

    @NotNull(message = "Source is required")
    private TrackSource source;

    private String streamUrl;

    // Позиция, на которую добавить трек (null - в конец очереди)
    @Min(value = 0, message = "Position must be non-negative")
    private Integer position;
}
