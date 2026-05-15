package ADG.services;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/chat")
// The tunnel URL is randomly assigned on each Pi restart, so a fixed origin cannot
// be configured. The risk of * is acceptable for a private server on an ephemeral URL.
@CrossOrigin(origins = "*")
public class ChatController {

    private final Map<String, CopyOnWriteArrayList<Map<String, String>>> messages = new ConcurrentHashMap<>();

    @Autowired
    private ChatSseRegistry chatSseRegistry;

    @PostMapping(value = "/{roomId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> sendMessage(
            @PathVariable("roomId") String roomId,
            @RequestBody Map<String, String> body) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("sender", body.getOrDefault("sender", ""));
        entry.put("message", body.getOrDefault("message", ""));
        String timestampUTC = LocalTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm"));
        entry.put("timestampUTC", timestampUTC);
        CopyOnWriteArrayList<Map<String, String>> roomMessages =
                messages.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>());
        roomMessages.add(entry);
        chatSseRegistry.emit(roomId, new ArrayList<>(roomMessages));
        return ResponseEntity.status(201).build();
    }

    @GetMapping(value = "/{roomId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, String>>> getMessages(@PathVariable("roomId") String roomId) {
        return ResponseEntity.ok(new ArrayList<>(messages.getOrDefault(roomId, new CopyOnWriteArrayList<>())));
    }

    @GetMapping(value = "/{roomId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessages(
            @PathVariable("roomId") String roomId,
            HttpServletResponse response) {
        response.setHeader("X-Accel-Buffering", "no");
        List<Map<String, String>> current =
                new ArrayList<>(messages.getOrDefault(roomId, new CopyOnWriteArrayList<>()));
        SseEmitter emitter = chatSseRegistry.subscribe(roomId);
        try {
            emitter.send(SseEmitter.event().data(current, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }
}