package ru.music.queue.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;
import ru.music.queue.dto.AddTrackRequest;
import ru.music.queue.dto.MoveTrackRequest;
import ru.music.queue.model.TrackSource;
import ru.music.queue.repository.QueueItemRepository;
import ru.music.queue.service.RoomValidationService;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QueueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private QueueItemRepository queueItemRepository;

    @MockitoBean
    private RoomValidationService roomValidationService;

    private UUID roomId;

    @BeforeEach
    void setUp() {
        roomId = UUID.randomUUID();
        queueItemRepository.deleteAll();

        when(roomValidationService.validateRoomExists(any(UUID.class)))
                .thenReturn(Mono.just(true));
    }

    @Test
    @Order(1)
    @DisplayName("POST /api/queue/{roomId}/tracks - Добавление трека в конец очереди")
    void testAddTrackToEnd() throws Exception {
        AddTrackRequest request = createTrackRequest("Bohemian Rhapsody", "Queen", 354);

        mockMvc.perform(post("/api/queue/{roomId}/tracks", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Bohemian Rhapsody"))
                .andExpect(jsonPath("$.artist").value("Queen"))
                .andExpect(jsonPath("$.durationSec").value(354))
                .andExpect(jsonPath("$.source").value("YOUTUBE"))
                .andExpect(jsonPath("$.position").value(0))
                .andExpect(jsonPath("$.roomId").value(roomId.toString()));
    }

    @Test
    @Order(2)
    @DisplayName("POST /api/queue/{roomId}/tracks - Добавление трека на конкретную позицию")
    void testAddTrackAtSpecificPosition() throws Exception {
        // Добавляем первый трек в конец
        AddTrackRequest request1 = createTrackRequest("Track 1", "Artist 1", 200);
        mockMvc.perform(post("/api/queue/{roomId}/tracks", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Добавляем второй трек на позицию 0
        AddTrackRequest request2 = createTrackRequest("Track 2", "Artist 2", 180);
        request2.setPosition(0);

        mockMvc.perform(post("/api/queue/{roomId}/tracks", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.position").value(0));

        // Проверяем порядок в очереди
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
        // Добавляем несколько треков
        addTestTrack("Track 1", "Artist 1", 200);
        addTestTrack("Track 2", "Artist 2", 180);
        addTestTrack("Track 3", "Artist 3", 240);

        mockMvc.perform(get("/api/queue/{roomId}/tracks", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value(roomId.toString()))
                .andExpect(jsonPath("$.totalTracks").value(3))
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
                .andExpect(jsonPath("$.totalTracks").value(0))
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(0));

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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value(2));

        // Проверяем новый порядок
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
                .andExpect(jsonPath("$.totalTracks").value(2))
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTracks").value(0));
    }

    @Test
    @Order(10)
    @DisplayName("POST /api/queue/{roomId}/tracks - Валидация обязательных полей")
    void testValidation() throws Exception {
        AddTrackRequest invalidRequest = new AddTrackRequest();

        mockMvc.perform(post("/api/queue/{roomId}/tracks", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasKey("name")))
                .andExpect(jsonPath("$.errors", hasKey("artist")))
                .andExpect(jsonPath("$.errors", hasKey("durationSec")))
                .andExpect(jsonPath("$.errors", hasKey("source")))
                .andExpect(jsonPath("$.errors", hasKey("externalId")));
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
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
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
        request.setExternalId("ext_" + UUID.randomUUID().toString().substring(0, 8));
        request.setStreamUrl("https://example.com/stream/" + UUID.randomUUID());
        request.setAddedBy(UUID.randomUUID());
        return request;
    }
}