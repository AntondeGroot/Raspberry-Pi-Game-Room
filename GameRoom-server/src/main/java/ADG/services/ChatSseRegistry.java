package ADG.services;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ChatSseRegistry {

    private final Map<String, CopyOnWriteArrayList<SseEmitter>> rooms = new ConcurrentHashMap<>();

    SseEmitter subscribe(String roomId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        rooms.computeIfAbsent(roomId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(roomId, emitter));
        emitter.onTimeout(() -> remove(roomId, emitter));
        emitter.onError(e -> remove(roomId, emitter));
        return emitter;
    }

    public void emit(String roomId, List<Map<String, String>> messages) {
        List<SseEmitter> emitters = rooms.get(roomId);
        if (emitters == null) return;
        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().data(messages, MediaType.APPLICATION_JSON));
            } catch (Exception e) {
                dead.add(emitter);
            }
        }
        emitters.removeAll(dead);
    }

    @Scheduled(fixedDelay = 30_000)
    public void heartbeat() {
        for (List<SseEmitter> emitters : rooms.values()) {
            List<SseEmitter> dead = new ArrayList<>();
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    dead.add(emitter);
                }
            }
            emitters.removeAll(dead);
        }
    }

    private void remove(String roomId, SseEmitter emitter) {
        List<SseEmitter> emitters = rooms.get(roomId);
        if (emitters != null) emitters.remove(emitter);
    }
}