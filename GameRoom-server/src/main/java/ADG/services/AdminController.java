package ADG.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private RoomStore roomStore;

    /** Returns 200 when the caller is authenticated as ADMIN (Spring Security guards this). */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkAdmin() {
        return ResponseEntity.ok(Map.of("admin", true));
    }

    /** Forcibly deletes any room by ID. Requires ADMIN role. */
    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable("roomId") String roomId) {
        roomStore.deleteRoom(roomId);
        return ResponseEntity.noContent().build();
    }
}