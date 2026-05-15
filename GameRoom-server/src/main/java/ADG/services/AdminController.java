package ADG.services;

import ADG.Lobby.GameStatus;
import ADG.Lobby.Room;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private RoomStore roomStore;

    @Autowired
    private RoomSseRegistry sseRegistry;

    @Autowired
    private LobbySseRegistry lobbySseRegistry;

    /** Returns 200 when the caller is authenticated as ADMIN (Spring Security guards this). */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkAdmin() {
        return ResponseEntity.ok(Map.of("admin", true));
    }

    /** Forcibly deletes any room by ID. Requires ADMIN role. */
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable("roomId") String roomId) {
        roomStore.deleteRoom(roomId);
        sseRegistry.emitClosed(roomId);
        List<Room> visible = roomStore.rooms.stream()
                .filter(r -> r.getStatus() != GameStatus.PENDING)
                .collect(Collectors.toList());
        lobbySseRegistry.emit(visible);
        return ResponseEntity.noContent().build();
    }
}