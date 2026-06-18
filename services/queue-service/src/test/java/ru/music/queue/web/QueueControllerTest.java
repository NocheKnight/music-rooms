package ru.music.queue.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.music.queue.dto.AddTrackRequest;
import ru.music.queue.dto.MoveTrackRequest;
import ru.music.queue.dto.RoomResponse;
import ru.music.queue.dto.TrackChangedEvent;
import ru.music.queue.feign.RoomClient;
import ru.music.queue.model.Queue;
import ru.music.queue.model.TrackSource;
import ru.music.queue.repository.QueueRepository;
import ru.music.queue.repository.TrackRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
class QueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TrackRepository trackRepository;
    @Autowired
    private QueueRepository queueRepository;

    @MockitoBean
    private RoomClient roomClient;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    private UUID roomId;

    @Test
    void contextLoads() {
    }

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();

        Queue queue = new Queue();
        queue.setRoomId(roomId);
        queue.setTracks(new ArrayList<>());
        queueRepository.save(queue);

        RoomResponse roomResponse = new RoomResponse(
                roomId,
                "Name",
                "code",
                UUID.randomUUID(),
                Set.of(),
                Instant.now()
        );
        when(roomClient.getRoom(roomId))
                .thenReturn(new ResponseEntity<>(roomResponse, HttpStatus.OK));

        doNothing().when(rabbitTemplate).convertAndSend(
                any(String.class),
                any(String.class),
                any(TrackChangedEvent.class)
        );
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/queue/{roomId}/tracks - Добавление трека в конец очереди")
    void testAddTrackToEnd() throws Exception {
        AddTrackRequest request = createTrackRequest("Bohemian Rhapsody", "Queen", 354);

        mockMvc.perform(post("/api/queue/{roomId}/tracks", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", UUID.randomUUID())
                ).andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Bohemian Rhapsody"))
                .andExpect(jsonPath("$.artist").value("Queen"))
                .andExpect(jsonPath("$.durationSec").value(354))
                .andExpect(jsonPath("$.source").value("YOUTUBE"))
                .andExpect(jsonPath("$.position").value(0));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/queue/{roomId}/tracks - Добавление трека на конкретную позицию")
    void testAddTrackAtSpecificPosition() throws Exception {
        AddTrackRequest request1 = createTrackRequest("Track 1", "Artist 1", 200);
        mockMvc.perform(post("/api/queue/{roomId}/tracks", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1))
                        .header("X-User-Id", UUID.randomUUID())
                ).andExpect(status().isCreated());

        AddTrackRequest request2 = createTrackRequest("Track 2", "Artist 2", 180);
        request2.setPosition(0);
        mockMvc.perform(post("/api/queue/{roomId}/tracks", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2))
                        .header("X-User-Id", UUID.randomUUID())
                ).andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(0));

        mockMvc.perform(get("/api/queue/{roomId}/tracks", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks[0].name").value("Track 2"))
                .andExpect(jsonPath("$.tracks[1].name").value("Track 1"))
                .andExpect(jsonPath("$.tracks[0].position").value(0))
                .andExpect(jsonPath("$.tracks[1].position").value(1));
    }

    @Test
    @Order(3)
    @DisplayName("GET /api/queue/{roomId}/tracks - Получение всей очереди")
    void testGetQueue() throws Exception {
        addTestTrack("Track 1", "Artist 1", 200);
        addTestTrack("Track 2", "Artist 2", 180);
        addTestTrack("Track 3", "Artist 3", 240);

        mockMvc.perform(get("/api/queue/{roomId}/tracks", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(roomId.toString()))
                .andExpect(jsonPath("$.currentTrackPosition").value(0))
                .andExpect(jsonPath("$.tracks.size()").value(3))
                .andExpect(jsonPath("$.tracks[0].position").value(0))
                .andExpect(jsonPath("$.tracks[1].position").value(1))
                .andExpect(jsonPath("$.tracks[2].position").value(2));
    }

    @Test
    @Order(4)
    @DisplayName("GET /api/queue/{roomId}/tracks - Получение пустой очереди")
    void testGetEmptyQueue() throws Exception {
        mockMvc.perform(get("/api/queue/{roomId}/tracks", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks.size()").value(0))
                .andExpect(jsonPath("$.currentTrackPosition").doesNotExist());
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/queue/{roomId}/tracks/current - Получение текущего трека")
    void testGetCurrentTrack() throws Exception {
        UUID trackId = addTestTrack("Current Track", "Artist", 300);

        mockMvc.perform(get("/api/queue/{roomId}/tracks/current", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(trackId.toString()))
                .andExpect(jsonPath("$.position").value(0));
    }

    @Test
    @Order(6)
    @DisplayName("PUT /api/queue/{roomId}/tracks/{trackId}/move - Перемещение трека вверх")
    void testMoveTrackUp() throws Exception {
        UUID track0 = addTestTrack("Track 0", "Artist 0", 200);
        UUID track1 = addTestTrack("Track 1", "Artist 1", 180);
        UUID track2 = addTestTrack("Track 2", "Artist 2", 240);

        MoveTrackRequest moveRequest = new MoveTrackRequest();
        moveRequest.setNewPosition(0);

        mockMvc.perform(put("/api/queue/{roomId}/tracks/{trackId}/move", roomId, track2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isOk());

        // Проверяем новый порядок
        mockMvc.perform(get("/api/queue/{roomId}/tracks", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks[0].id").value(track2.toString()))
                .andExpect(jsonPath("$.tracks[1].id").value(track0.toString()))
                .andExpect(jsonPath("$.tracks[2].id").value(track1.toString()));
    }

    @Test
    @Order(7)
    @DisplayName("PUT /api/queue/{roomId}/tracks/{trackId}/move - Перемещение трека вниз")
    void testMoveTrackDown() throws Exception {
        UUID track0 = addTestTrack("Track 0", "Artist 0", 200);
        UUID track1 = addTestTrack("Track 1", "Artist 1", 180);
        UUID track2 = addTestTrack("Track 2", "Artist 2", 240);

        MoveTrackRequest moveRequest = new MoveTrackRequest();
        moveRequest.setNewPosition(2);

        mockMvc.perform(put("/api/queue/{roomId}/tracks/{trackId}/move", roomId, track0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/queue/{roomId}/tracks", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks[0].id").value(track1.toString()))
                .andExpect(jsonPath("$.tracks[1].id").value(track2.toString()))
                .andExpect(jsonPath("$.tracks[2].id").value(track0.toString()));
    }

    @Test
    @Order(8)
    @DisplayName("DELETE /api/queue/{roomId}/tracks/{trackId} - Удаление трека")
    void testRemoveTrack() throws Exception {
        UUID track0 = addTestTrack("Track 0", "Artist 0", 200);
        UUID track1 = addTestTrack("Track 1", "Artist 1", 180);
        UUID track2 = addTestTrack("Track 2", "Artist 2", 240);

        mockMvc.perform(delete("/api/queue/{roomId}/tracks/{trackId}", roomId, track1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Track removed successfully"));

        mockMvc.perform(get("/api/queue/{roomId}/tracks", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tracks[0].position").value(0))
                .andExpect(jsonPath("$.tracks[1].position").value(1));
    }

    @Test
    @Order(9)
    @DisplayName("DELETE /api/queue/{roomId}/tracks - Очистка всей очереди")
    void testClearQueue() throws Exception {
        addTestTrack("Track 1", "Artist 1", 200);
        addTestTrack("Track 2", "Artist 2", 180);

        mockMvc.perform(delete("/api/queue/{roomId}/tracks", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Queue cleared successfully"));

        mockMvc.perform(get("/api/queue/{roomId}/tracks", roomId))
                .andExpect(status().isOk());
    }

    @Test
    @Order(10)
    @DisplayName("POST /api/queue/{roomId}/tracks - Валидация обязательных полей")
    void testValidation() throws Exception {
        AddTrackRequest invalidRequest = new AddTrackRequest();

        mockMvc.perform(post("/api/queue/{roomId}/tracks", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest))
                        .header("X-User-Id", UUID.randomUUID())
                ).andExpect(status().isBadRequest());
    }

    @Test
    @Order(11)
    @DisplayName("PUT /api/queue/{roomId}/tracks/{trackId}/move - Перемещение на невалидную позицию")
    void testMoveToInvalidPosition() throws Exception {
        UUID track0 = addTestTrack("Track 0", "Artist 0", 200);

        MoveTrackRequest moveRequest = new MoveTrackRequest();
        moveRequest.setNewPosition(-1);

        mockMvc.perform(put("/api/queue/{roomId}/tracks/{trackId}/move", roomId, track0)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(12)
    @DisplayName("GET /api/queue/{roomId}/tracks/current - Получение текущего трека из пустой очереди")
    void testGetCurrentTrackFromEmptyQueue() throws Exception {
        mockMvc.perform(get("/api/queue/{roomId}/tracks/current", roomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Track not found"));
    }

    @Test
    @Order(13)
    @DisplayName("DELETE /api/queue/{roomId}/tracks/{trackId} - Удаление несуществующего трека")
    void testRemoveNonExistentTrack() throws Exception {
        mockMvc.perform(delete("/api/queue/{roomId}/tracks/{trackId}", roomId, UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Track not found"));
    }

    @Test
    @Order(14)
    @DisplayName("PUT /api/queue/{roomId}/tracks/{trackId}/move - Перемещение несуществующего трека")
    void testMoveNonExistentTrack() throws Exception {
        MoveTrackRequest moveRequest = new MoveTrackRequest();
        moveRequest.setNewPosition(0);

        mockMvc.perform(put("/api/queue/{roomId}/tracks/{trackId}/move", roomId, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Track not found"));
    }

    // Вспомогательные методы
    private UUID addTestTrack(String name, String artist, int duration) throws Exception {
        AddTrackRequest request = createTrackRequest(name, artist, duration);

        String response = mockMvc.perform(post("/api/queue/{roomId}/tracks", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", UUID.randomUUID())
                ).andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(
                objectMapper.readTree(response).get("id").asText()
        );
    }

    private AddTrackRequest createTrackRequest(String name, String artist, int duration) {
        AddTrackRequest request = new AddTrackRequest();
        request.setName(name);
        request.setArtist(artist);
        request.setDurationSec(duration);
        request.setSource(TrackSource.YOUTUBE);
        request.setStreamUrl("https://example.com/stream/" + UUID.randomUUID());
        return request;
    }
}