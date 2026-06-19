package ru.music.queue.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.music.queue.dto.AddTrackRequest;
import ru.music.queue.dto.TrackDto;
import ru.music.queue.model.Track;
import ru.music.queue.model.TrackSource;

import java.util.UUID;

@Mapper(
        componentModel = "spring",
        imports = {
                TrackSource.class
        }
)
public interface TrackMapper {
    @Mapping(target = "addedBy", source = "userId")
    Track addTrackRequestToEntity(AddTrackRequest addTrackRequest, UUID userId);

    TrackDto entityToDto(Track track, int position);
}