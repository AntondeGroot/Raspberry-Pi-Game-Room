package ADG.services;

import ADG.Lobby.GameDefinition;
import ADG.Lobby.GameStatus;
import ADG.Lobby.Room;
import ADG.Lobby.RoomServiceException;
import ADG.config.GamesConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    private RoomServiceImpl service;
    private RoomStore roomStore;

    @Mock
    private GamesConfig gamesConfig;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() throws Exception {
        roomStore = new RoomStore();
        // Use a spy so individual tests can stub getPlayerIdFromRequest() without
        // needing a live servlet container.
        service = spy(new RoomServiceImpl());
        // Inject mock GamesConfig and RoomStore — @PostConstruct is NOT invoked when newing
        // directly, so we start with a clean, empty room list for every test.
        Field gamesConfigField = RoomServiceImpl.class.getDeclaredField("gamesConfig");
        gamesConfigField.setAccessible(true);
        gamesConfigField.set(service, gamesConfig);

        Field roomStoreField = RoomServiceImpl.class.getDeclaredField("roomStore");
        roomStoreField.setAccessible(true);
        roomStoreField.set(service, roomStore);

        Field restTemplateField = RoomServiceImpl.class.getDeclaredField("restTemplate");
        restTemplateField.setAccessible(true);
        restTemplateField.set(service, restTemplate);

        lenient().when(gamesConfig.findById(anyString())).thenReturn(Optional.empty());
        // Default: caller is the room creator used by buildRoom().
        lenient().doReturn("creator-1").when(service).getPlayerIdFromRequest();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Room buildRoom(String name) {
        Room r = new Room();
        r.setId(java.util.UUID.randomUUID().toString());
        r.setName(name);
        r.setCreatedByUserId("creator-1");
        r.setStatus(GameStatus.WAITING);
        r.setGameId("keezen");
        return r;
    }

    // ── getRooms ─────────────────────────────────────────────────────────────

    @Test
    void getRoomsStartsEmpty() throws RoomServiceException {
        assertTrue(service.getRooms().isEmpty());
    }

    @Test
    void getRoomsHidesPendingRooms() throws RoomServiceException {
        service.createRoom(buildRoom("Alpha"));
        assertTrue(service.getRooms().isEmpty(), "PENDING room must not appear in lobby list");
    }

    @Test
    void getRoomsShowsPublishedRoom() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.publishRoom(room.getId());
        assertEquals(1, service.getRooms().size());
        assertEquals(GameStatus.WAITING, service.getRooms().get(0).getStatus());
    }

    // ── createRoom ───────────────────────────────────────────────────────────

    @Test
    void createRoomAddsAndReturnsRoom() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        Room result = service.createRoom(room);

        assertSame(room, result);
        assertEquals(GameStatus.PENDING, result.getStatus(), "newly created room must be PENDING");
        assertTrue(service.getRooms().isEmpty(), "PENDING room must not be visible in lobby");
    }

    // ── publishRoom ──────────────────────────────────────────────────────────

    @Test
    void publishRoomMakesRoomVisible() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.publishRoom(room.getId());
        assertEquals(1, service.getRooms().size());
    }

    @Test
    void publishRoomSetsStatusToWaiting() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.publishRoom(room.getId());
        assertEquals(GameStatus.WAITING, service.getRooms().get(0).getStatus());
    }

    @Test
    void publishRoomOnNonPendingRoomIsNoOp() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.publishRoom(room.getId());
        // calling again should not break anything
        service.publishRoom(room.getId());
        assertEquals(GameStatus.WAITING, service.getRooms().get(0).getStatus());
    }

    @Test
    void publishRoomOnUnknownIdIsNoOp() throws RoomServiceException {
        service.publishRoom("non-existent-id"); // must not throw
        assertTrue(service.getRooms().isEmpty());
    }

    @Test
    void createRoomWithBlankNameThrows() throws RoomServiceException {
        Room room = buildRoom("   ");
        assertThrows(RoomServiceException.class, () -> service.createRoom(room));
        assertTrue(service.getRooms().isEmpty());
    }

    @Test
    void createRoomDuplicateThrows() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        assertThrows(RoomServiceException.class, () -> service.createRoom(room));
        assertNotNull(service.getRoomById(room.getId()), "original room must still exist");
    }

    @Test
    void createRoomDuplicateNameDifferentObjectThrows() throws RoomServiceException {
        // Same name but different object (different players/status) — the real duplicate-name bug
        Room original = buildRoom("Alpha");
        service.createRoom(original);
        Room other = buildRoom("Alpha");
        other.addPlayer("some-player");
        assertThrows(RoomServiceException.class, () -> service.createRoom(other));
        assertNotNull(service.getRoomById(original.getId()), "original room must still exist");
    }

    @Test
    void createRoomDuplicateNameCaseInsensitiveThrows() throws RoomServiceException {
        Room original = buildRoom("Alpha");
        service.createRoom(original);
        assertThrows(RoomServiceException.class, () -> service.createRoom(buildRoom("alpha")));
        assertThrows(RoomServiceException.class, () -> service.createRoom(buildRoom("ALPHA")));
        assertNotNull(service.getRoomById(original.getId()), "original room must still exist");
    }

    @Test
    void createRoomSetsMinPlayersFromConfig() throws RoomServiceException {
        GameDefinition game = mock(GameDefinition.class);
        when(game.getMinPlayers()).thenReturn(4);
        when(gamesConfig.findById("keezen")).thenReturn(Optional.of(game));

        Room room = buildRoom("Alpha");
        service.createRoom(room);

        assertEquals(4, room.getMinPlayers());
    }

    @Test
    void createRoomSetsMaxPlayersFromConfig() throws RoomServiceException {
        GameDefinition game = mock(GameDefinition.class);
        when(game.getMaxPlayers()).thenReturn(6);
        when(gamesConfig.findById("keezen")).thenReturn(Optional.of(game));

        Room room = buildRoom("Alpha");
        service.createRoom(room);

        assertEquals(6, room.getMaxPlayers());
    }

    @Test
    void createRoomLeavesMinPlayersAtDefaultWhenGameNotFound() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        assertEquals(1, room.getMinPlayers()); // Room default
    }

    @Test
    void createRoomLeavesMaxPlayersAtDefaultWhenGameNotFound() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        assertEquals(8, room.getMaxPlayers()); // Room default
    }

    @Test
    void createRoomRemovesCreatorFromPreviousRoom() throws RoomServiceException {
        Room roomA = buildRoom("Alpha");
        service.createRoom(roomA);
        service.publishRoom(roomA.getId());
        service.addPlayerIdToRoom("creator-1", roomA.getId());

        Room roomB = buildRoom("Beta"); // same createdByUserId ("creator-1")
        service.createRoom(roomB);
        service.publishRoom(roomB.getId());

        assertEquals(0, service.getRoomById(roomA.getId()).getNrOfPlayers());
        assertEquals(2, service.getRooms().size());
    }

    // ── getRoomById ──────────────────────────────────────────────────────────

    @Test
    void getRoomByIdFindsExistingRoom() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);

        Room found = service.getRoomById(room.getId());
        assertEquals(room, found);
    }

    @Test
    void getRoomByIdReturnsNullForUnknownId() throws RoomServiceException {
        assertNull(service.getRoomById("does-not-exist"));
    }

    // ── deleteEmptyRooms ─────────────────────────────────────────────────────

    @Test
    void emptyRoomIsDeletedAfterTtlExpires() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.publishRoom(room.getId());
        service.addPlayerIdToRoom("player-1", room.getId());
        service.removePlayerFromRoom("player-1", room.getId());

        // Manually backdate the empty timestamp to simulate 15+ minutes passing
        roomStore.emptyRoomTimestamps.put(room.getId(), System.currentTimeMillis() - (16 * 60 * 1000L));

        service.deleteEmptyRooms();

        assertNull(service.getRoomById(room.getId()));
    }

    @Test
    void emptyRoomIsKeptIfTtlHasNotExpired() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.publishRoom(room.getId());
        service.addPlayerIdToRoom("player-1", room.getId());
        service.removePlayerFromRoom("player-1", room.getId());

        service.deleteEmptyRooms(); // called immediately — TTL not yet exceeded

        assertNotNull(service.getRoomById(room.getId()));
    }

    @Test
    void roomWithPlayersIsNotScheduledForDeletion() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.publishRoom(room.getId());
        service.addPlayerIdToRoom("player-1", room.getId());

        roomStore.emptyRoomTimestamps.put(room.getId(), System.currentTimeMillis() - (16 * 60 * 1000L));
        service.deleteEmptyRooms();

        assertNotNull(service.getRoomById(room.getId()));
    }

    @Test
    void joiningRoomCancelsEmptyTimer() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.publishRoom(room.getId());
        service.addPlayerIdToRoom("player-1", room.getId());
        service.removePlayerFromRoom("player-1", room.getId()); // room becomes empty → timer starts
        service.addPlayerIdToRoom("player-2", room.getId());    // player joins → timer cancelled

        // Timer must have been removed — no entry should exist for this room
        assertFalse(roomStore.emptyRoomTimestamps.containsKey(room.getId()));
    }

    @Test
    void pendingRoomIsNotScheduledForDeletion() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room); // status is PENDING, no players

        service.deleteEmptyRooms();

        assertNotNull(service.getRoomById(room.getId()));
        assertTrue(roomStore.emptyRoomTimestamps.isEmpty());
    }

    // ── deleteRoom ───────────────────────────────────────────────────────────
    // deleteRoom() is the GWT-RPC path (not Spring Security protected).
    // Authorization is enforced by checking the playerid cookie against the room creator.

    @Test
    void deleteRoomByCreatorRemovesIt() throws RoomServiceException {
        // Default spy returns "creator-1" which matches buildRoom()'s createdByUserId.
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.publishRoom(room.getId());

        service.deleteRoom(room.getId());

        assertNull(service.getRoomById(room.getId()));
    }

    @Test
    void deleteRoomByNonCreatorIsRejected() throws RoomServiceException {
        doReturn("stranger").when(service).getPlayerIdFromRequest();
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.publishRoom(room.getId());

        service.deleteRoom(room.getId());

        assertNotNull(service.getRoomById(room.getId()), "non-creator must not be able to delete the room");
    }

    @Test
    void deleteRoomWithNoCallerCookieIsRejected() throws RoomServiceException {
        doReturn("").when(service).getPlayerIdFromRequest();
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.publishRoom(room.getId());

        service.deleteRoom(room.getId());

        assertNotNull(service.getRoomById(room.getId()), "unauthenticated caller must not be able to delete the room");
    }

    @Test
    void deleteNonExistentRoomIsNoOp() throws RoomServiceException {
        service.deleteRoom("non-existent-id"); // must not throw
        assertTrue(service.getRooms().isEmpty());
    }

    // ── updateRoom ───────────────────────────────────────────────────────────

    @Test
    void updateRoomReplacesExisting() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.publishRoom(room.getId());

        room.setUniqueProfilePics(true);
        service.updateRoom(room);

        assertEquals(1, service.getRooms().size());
        assertTrue(service.getRooms().get(0).isUniqueProfilePics());
    }

    // ── addPlayerIdToRoom ────────────────────────────────────────────────────

    @Test
    void addPlayerIdToRoomAppendsId() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.addPlayerIdToRoom("player-1", room.getId());

        assertEquals(1, service.getRoomById(room.getId()).getNrOfPlayers());
    }

    @Test
    void addPlayerIdToRoomSetsStatusToFullWhenAtCapacity() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        room.setMaxPlayers(2);
        service.createRoom(room);
        service.publishRoom(room.getId());
        service.addPlayerIdToRoom("player-1", room.getId());

        assertEquals(GameStatus.WAITING, service.getRoomById(room.getId()).getStatus());

        service.addPlayerIdToRoom("player-2", room.getId());

        assertEquals(GameStatus.FULL, service.getRoomById(room.getId()).getStatus());
    }

    @Test
    void addPlayerIdToRoomStatusRemainsWaitingBelowCapacity() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        room.setMaxPlayers(4);
        service.createRoom(room);
        service.publishRoom(room.getId());
        service.addPlayerIdToRoom("player-1", room.getId());
        service.addPlayerIdToRoom("player-2", room.getId());
        service.addPlayerIdToRoom("player-3", room.getId());

        assertEquals(GameStatus.WAITING, service.getRoomById(room.getId()).getStatus());
    }

    @Test
    void addPlayerIdToRoomMovesPlayerFromOldRoomToNewRoom() throws RoomServiceException {
        Room roomA = buildRoom("Alpha");
        Room roomB = buildRoom("Beta");
        service.createRoom(roomA);
        service.createRoom(roomB);
        service.addPlayerIdToRoom("player-1", roomA.getId());

        service.addPlayerIdToRoom("player-1", roomB.getId());

        assertEquals(0, service.getRoomById(roomA.getId()).getNrOfPlayers(), "player must leave old room");
        assertEquals(1, service.getRoomById(roomB.getId()).getNrOfPlayers(), "player must be in new room");
    }

    @Test
    void playerStuckInOldRoomIsMovedToNewRoomOnJoin() throws RoomServiceException {
        // Reproduces bug: browser crash left player listed in roomA; a new room (roomB) was
        // created and a game started there. On re-login the player should see rejoin for roomB,
        // not roomA. addPlayerIdToRoom must move them automatically.
        Room roomA = buildRoom("Alpha");
        Room roomB = buildRoom("Beta");
        service.createRoom(roomA);
        service.createRoom(roomB);
        service.publishRoom(roomB.getId());
        service.addPlayerIdToRoom("player-1", roomA.getId()); // stuck in old room due to crash

        service.addPlayerIdToRoom("player-1", roomB.getId()); // joining the new room

        assertFalse(service.getRoomById(roomA.getId()).getPlayerIds().contains("player-1"),
                "player must no longer appear in old room");
        assertTrue(service.getRoomById(roomB.getId()).getPlayerIds().contains("player-1"),
                "player must appear in new room so the rejoin button shows correctly");
    }

    @Test
    void addingPlayerToOldRoomDoesNotAffectOtherPlayersInIt() throws RoomServiceException {
        Room roomA = buildRoom("Alpha");
        Room roomB = buildRoom("Beta");
        service.createRoom(roomA);
        service.createRoom(roomB);
        service.addPlayerIdToRoom("player-1", roomA.getId());
        service.addPlayerIdToRoom("player-2", roomA.getId()); // other player stays in roomA

        service.addPlayerIdToRoom("player-1", roomB.getId()); // player-1 moves to roomB

        assertTrue(service.getRoomById(roomA.getId()).getPlayerIds().contains("player-2"),
                "other players in the old room must be unaffected");
        assertEquals(1, service.getRoomById(roomA.getId()).getNrOfPlayers());
    }

    @Test
    void addPlayerIdToRoomAllowsRejoiningSameRoom() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.addPlayerIdToRoom("player-1", room.getId());
        service.addPlayerIdToRoom("player-1", room.getId()); // rejoin = idempotent

        assertEquals(1, service.getRoomById(room.getId()).getNrOfPlayers());
    }

    @Test
    void addPlayerIdToRoomIgnoresDuplicates() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.addPlayerIdToRoom("player-1", room.getId());
        service.addPlayerIdToRoom("player-1", room.getId());

        assertEquals(1, service.getRoomById(room.getId()).getNrOfPlayers());
    }

    // ── removePlayerFromRoom ─────────────────────────────────────────────────

    @Test
    void removePlayerFromRoomRemovesPlayer() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.addPlayerIdToRoom("player-1", room.getId());
        service.setUsernameAndProfile(room, "player-1", "Alice", "3");

        service.removePlayerFromRoom("player-1", room.getId());

        Room found = service.getRoomById(room.getId());
        assertEquals(0, found.getNrOfPlayers());
        assertFalse(found.getPlayerNames().containsKey("player-1"));
    }

    @Test
    void removeCreatorTransfersOwnershipToNextPlayer() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        room.setCreatedByUserId("player-1");
        service.createRoom(room);
        service.addPlayerIdToRoom("player-1", room.getId());
        service.addPlayerIdToRoom("player-2", room.getId());

        service.removePlayerFromRoom("player-1", room.getId());

        assertEquals("player-2", service.getRoomById(room.getId()).getCreatedByUserId());
    }

    @Test
    void removePlayerFromFullRoomRevertsStatusToWaiting() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        room.setMaxPlayers(2);
        service.createRoom(room);
        service.addPlayerIdToRoom("player-1", room.getId());
        service.addPlayerIdToRoom("player-2", room.getId());
        assertEquals(GameStatus.FULL, service.getRoomById(room.getId()).getStatus());

        service.removePlayerFromRoom("player-1", room.getId());

        assertEquals(GameStatus.WAITING, service.getRoomById(room.getId()).getStatus());
    }

    @Test
    void removePlayerFromWaitingRoomKeepsStatusWaiting() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        room.setMaxPlayers(4);
        service.createRoom(room);
        service.publishRoom(room.getId());
        service.addPlayerIdToRoom("player-1", room.getId());
        service.addPlayerIdToRoom("player-2", room.getId());

        service.removePlayerFromRoom("player-1", room.getId());

        assertEquals(GameStatus.WAITING, service.getRoomById(room.getId()).getStatus());
    }

    @Test
    void removePlayerFromPlayingRoomKeepsStatusPlaying() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        room.setMaxPlayers(2);
        service.createRoom(room);
        service.addPlayerIdToRoom("player-1", room.getId());
        service.addPlayerIdToRoom("player-2", room.getId());
        room.setStatus(GameStatus.PLAYING); // game started

        service.removePlayerFromRoom("player-1", room.getId());

        assertEquals(GameStatus.PLAYING, service.getRoomById(room.getId()).getStatus());
    }

    @Test
    void playerCanJoinNewRoomAfterLeavingPreviousOne() throws RoomServiceException {
        Room roomA = buildRoom("Alpha");
        Room roomB = buildRoom("Beta");
        service.createRoom(roomA);
        service.createRoom(roomB);
        service.addPlayerIdToRoom("player-1", roomA.getId());

        service.removePlayerFromRoom("player-1", roomA.getId());
        service.addPlayerIdToRoom("player-1", roomB.getId());

        assertEquals(0, service.getRoomById(roomA.getId()).getNrOfPlayers());
        assertEquals(1, service.getRoomById(roomB.getId()).getNrOfPlayers());
    }

    @Test
    void removeLastPlayerLeavesNoCreator() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        room.setCreatedByUserId("player-1");
        service.createRoom(room);
        service.addPlayerIdToRoom("player-1", room.getId());

        service.removePlayerFromRoom("player-1", room.getId());

        // playerIds is empty — no ownership transfer attempted
        assertEquals(0, service.getRoomById(room.getId()).getNrOfPlayers());
    }

    // ── setUsernameAndProfile ────────────────────────────────────────────────

    @Test
    void setUsernameAndProfileStoresBoth() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.addPlayerIdToRoom("player-1", room.getId());
        service.setUsernameAndProfile(room, "player-1", "Alice", "5");

        Room found = service.getRoomById(room.getId());
        assertEquals("Alice", found.getPlayerNames().get("player-1"));
        assertEquals("5", found.getPlayerProfiles().get("player-1"));
    }

    @Test
    void setUsernameAndProfileOverwritesPrevious() throws RoomServiceException {
        Room room = buildRoom("Alpha");
        service.createRoom(room);
        service.addPlayerIdToRoom("player-1", room.getId());
        service.setUsernameAndProfile(room, "player-1", "Alice", "5");
        service.setUsernameAndProfile(room, "player-1", "Alice2", "7");

        assertEquals("Alice2", service.getRoomById(room.getId()).getPlayerNames().get("player-1"));
        assertEquals("7",      service.getRoomById(room.getId()).getPlayerProfiles().get("player-1"));
    }

    // ── getAvailableGames ────────────────────────────────────────────────────

    @Test
    void getAvailableGamesReturnsOnlyReachableGames() throws RoomServiceException {
        // No games configured in mock → should return empty list (not throw)
        when(gamesConfig.getAvailable()).thenReturn(new ArrayList<>());
        assertTrue(service.getAvailableGames().isEmpty());
    }

    // ── verifyGameSessionsExist ──────────────────────────────────────────────

    private Room buildPlayingRoom(String name, String sessionId) {
        Room r = buildRoom(name);
        r.setStatus(GameStatus.PLAYING);
        r.setGameSessionId(sessionId);
        roomStore.rooms.add(r);
        return r;
    }

    private GameDefinition gameDefWithBaseUrl(String baseUrl) {
        GameDefinition def = new GameDefinition();
        def.setId("keezen");
        def.setBaseUrl(baseUrl);
        return def;
    }

    @Test
    void verifyGameSessionsExistDeletesRoomWhenSessionGone() {
        Room room = buildPlayingRoom("Alpha", "session-1");
        when(gamesConfig.findById("keezen")).thenReturn(Optional.of(gameDefWithBaseUrl("http://game-server")));
        when(restTemplate.getForObject("http://game-server/games/session-1", Map.class))
                .thenThrow(new ResourceAccessException("connection refused"));

        service.verifyGameSessionsExist();

        assertNull(service.getRoomById(room.getId()));
    }

    @Test
    void verifyGameSessionsExistKeepsRoomWhenVersionChanges() {
        Room room = buildPlayingRoom("Alpha", "session-1");
        when(gamesConfig.findById("keezen")).thenReturn(Optional.of(gameDefWithBaseUrl("http://game-server")));
        when(restTemplate.getForObject("http://game-server/games/session-1", Map.class))
                .thenReturn(Map.of());
        when(restTemplate.getForObject("http://game-server/gamestates/session-1", Map.class))
                .thenReturn(Map.of("version", 1))
                .thenReturn(Map.of("version", 2));

        service.verifyGameSessionsExist(); // stores version "1"
        roomStore.gameStateVersionTimestamps.put(room.getId(), System.currentTimeMillis() - (2 * 60 * 60 * 1000L));
        service.verifyGameSessionsExist(); // version changed to "2" → timer resets, no deletion

        assertNotNull(service.getRoomById(room.getId()));
    }

    @Test
    void verifyGameSessionsExistDeletesRoomWhenVersionStaleForOneHour() {
        Room room = buildPlayingRoom("Alpha", "session-1");
        when(gamesConfig.findById("keezen")).thenReturn(Optional.of(gameDefWithBaseUrl("http://game-server")));
        when(restTemplate.getForObject("http://game-server/games/session-1", Map.class))
                .thenReturn(Map.of());
        when(restTemplate.getForObject("http://game-server/gamestates/session-1", Map.class))
                .thenReturn(Map.of("version", 42));

        service.verifyGameSessionsExist(); // stores version "42" with current timestamp
        // Backdate the timestamp to simulate 1+ hour of no change
        roomStore.gameStateVersionTimestamps.put(room.getId(), System.currentTimeMillis() - (61 * 60 * 1000L));
        service.verifyGameSessionsExist(); // same version, stale → should delete

        verify(restTemplate).delete("http://game-server/games/session-1");
        assertNull(service.getRoomById(room.getId()));
    }

    @Test
    void verifyGameSessionsExistKeepsRoomWhenVersionUnchangedLessThanOneHour() {
        Room room = buildPlayingRoom("Alpha", "session-1");
        when(gamesConfig.findById("keezen")).thenReturn(Optional.of(gameDefWithBaseUrl("http://game-server")));
        when(restTemplate.getForObject("http://game-server/games/session-1", Map.class))
                .thenReturn(Map.of());
        when(restTemplate.getForObject("http://game-server/gamestates/session-1", Map.class))
                .thenReturn(Map.of("version", 42));

        service.verifyGameSessionsExist(); // stores version "42"
        service.verifyGameSessionsExist(); // same version, but < 1 hour → keep

        verify(restTemplate, never()).delete(anyString());
        assertNotNull(service.getRoomById(room.getId()));
    }

    @Test
    void verifyGameSessionsExistSkipsTestRoom() {
        buildPlayingRoom("Test Room", "session-test");

        service.verifyGameSessionsExist();

        verify(restTemplate, never()).getForObject(anyString(), any());
    }

    @Test
    void verifyGameSessionsExistSkipsRoomWithNoGameDefinition() {
        Room room = buildPlayingRoom("Alpha", "session-1");
        when(gamesConfig.findById("keezen")).thenReturn(Optional.empty());

        service.verifyGameSessionsExist();

        verify(restTemplate, never()).getForObject(anyString(), any());
        assertNotNull(service.getRoomById(room.getId()));
    }

    @Test
    void verifyGameSessionsExistSkipsVersionCheckWhenVersionMissing() {
        Room room = buildPlayingRoom("Alpha", "session-1");
        when(gamesConfig.findById("keezen")).thenReturn(Optional.of(gameDefWithBaseUrl("http://game-server")));
        when(restTemplate.getForObject("http://game-server/games/session-1", Map.class))
                .thenReturn(Map.of());
        when(restTemplate.getForObject("http://game-server/gamestates/session-1", Map.class))
                .thenReturn(Map.of()); // no version field

        service.verifyGameSessionsExist();
        roomStore.gameStateVersionTimestamps.put(room.getId(), System.currentTimeMillis() - (2 * 60 * 60 * 1000L));
        service.verifyGameSessionsExist();

        verify(restTemplate, never()).delete(anyString());
        assertNotNull(service.getRoomById(room.getId()));
    }
}
