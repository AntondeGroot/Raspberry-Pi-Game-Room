package ADG.services;

import ADG.Lobby.GameStatus;
import ADG.Lobby.Room;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.stream.Collectors;

@RestController
class LobbySseController {

    @Autowired
    private LobbySseRegistry registry;

    @Autowired
    private RoomStore roomStore;

    @GetMapping(value = "/lobby/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");

        List<Room> visible = roomStore.rooms.stream()
                .filter(r -> r.getStatus() != GameStatus.PENDING)
                .collect(Collectors.toList());

        SseEmitter emitter = registry.subscribe();
        try {
            emitter.send(SseEmitter.event().data(visible, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }
}