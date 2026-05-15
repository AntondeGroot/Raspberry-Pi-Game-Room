package ADG.services;

import ADG.Lobby.GameStatus;
import ADG.Lobby.Room;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
class RoomSeeder {

    @Autowired
    private RoomStore roomStore;

    @PostConstruct
    void seedTestRoom() {
        String[][] players = {
            {"Alice",   "0"},
            {"Bob",     "15"},
            {"Charlie", "21"},
            {"Diana",   "23"},
            {"Eve",     "47"},
            {"Frank",   "52"},
            {"Grace",   "59"},
        };

        Room room = new Room();
        room.setId(UUID.randomUUID().toString());
        room.setName("Test Room");
        room.setStatus(GameStatus.WAITING);
        room.setGameId("keezen");
        room.setMinPlayers(2);

        for (String[] p : players) {
            String pid = UUID.randomUUID().toString();
            room.addPlayer(pid);
            room.addPlayerName(pid, p[0]);
            room.addPlayerProfile(pid, p[1]);
        }
        room.setCreatedByUserId(room.getPlayerIds().get(0));
        roomStore.rooms.add(room);
    }
}