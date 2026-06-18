package ru.music.queue.mapper;

import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.music.queue.dto.AddTrackRequest;
import ru.music.queue.dto.QueueDto;
import ru.music.queue.dto.TrackDto;
import ru.music.queue.model.Queue;
import ru.music.queue.model.Track;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

@Mapper(
        componentModel = "spring"
)
public abstract class QueueMapper {
    @Autowired
    protected TrackMapper trackMapper;

    @Mapping(target = "tracks", source = "tracks", qualifiedByName = "mapTracks")
    public abstract QueueDto entityToDto(Queue queue);

    @Named("mapTracks")
    protected List<TrackDto> mapTracks(List<Track> tracks) {
        List<TrackDto> result = new ArrayList<>(tracks.size());
        for (int i = 0; i < tracks.size(); i++) {
            result.add(trackMapper.entityToDto(tracks.get(i), i));
        }
        return result;
    }
}