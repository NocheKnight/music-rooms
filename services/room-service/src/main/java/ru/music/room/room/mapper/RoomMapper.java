package ru.music.room.room.mapper;

import ru.music.room.auth.model.User;
import ru.music.room.room.dto.RoomResponse;
import ru.music.room.room.dto.UserResponse;
import ru.music.room.room.model.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    RoomMapper INSTANCE = Mappers.getMapper(RoomMapper.class);

    @Mapping(target = "participants", expression = "java(mapParticipants(room.getParticipants()))")
    RoomResponse toResponse(Room room);

    default Set<UserResponse> mapParticipants(Set<User> participants) {
        if (participants == null) {
            return Set.of();
        }
        return participants.stream()
                .map(user -> new UserResponse(user.getKeycloakId(), user.getUsername()))
                .collect(Collectors.toSet());
    }
}