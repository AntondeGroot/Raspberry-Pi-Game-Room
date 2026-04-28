package ADG.Lobby;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.ArrayList;


@RemoteServiceRelativePath("gameroom")
public interface RoomService extends RemoteService {
    ArrayList<Room> getRooms();
    Room getRoomById(String roomId);
    Room createRoom(Room room) throws RoomServiceException;
    void deleteRoom(String roomId);
    void updateRoom(Room room) throws RoomServiceException;
    void addPlayerIdToRoom(String playerId, String roomId);
    void removePlayerFromRoom(String playerId, String roomId);
    void setUsernameAndProfile(Room room, String userId, String username, String profileId);
    void publishRoom(String roomId);
    Room startGame(String roomId) throws RoomServiceException;
    ArrayList<GameDefinition> getAvailableGames();
    ArrayList<GameOption> getGameOptions(String gameId) throws RoomServiceException;
}
