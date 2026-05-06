package ADG.services;

import ADG.Lobby.Room;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds the in-memory room state so both {@link RoomServiceImpl} (GWT-RPC servlet)
 * and {@link AdminController} (REST) share the same data.
 */
@Component
public class RoomStore {

    final CopyOnWriteArrayList<Room> rooms = new CopyOnWriteArrayList<>();
    final ConcurrentHashMap<String, Long> emptyRoomTimestamps = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, String> gameStateVersions = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, Long> gameStateVersionTimestamps = new ConcurrentHashMap<>();

    public void deleteRoom(String roomId) {
        rooms.removeIf(r -> roomId.equals(r.getId()));
        emptyRoomTimestamps.remove(roomId);
        gameStateVersions.remove(roomId);
        gameStateVersionTimestamps.remove(roomId);
    }
}