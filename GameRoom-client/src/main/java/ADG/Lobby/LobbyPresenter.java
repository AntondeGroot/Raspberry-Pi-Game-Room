package ADG.Lobby;

import ADG.*;
import ADG.Utils.Cookie;
import ADG.audio.AudioPlayer;
import ADG.Utils.PollingService;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.ArrayList;

public class LobbyPresenter implements Presenter {

    private static final int POLLING_INTERVAL_MS = 200;
    private static final int OPTIONS_POLLING_INTERVAL_MS = 2000;
    private final LobbyView view;
    private final PresenterManager presenterManager;
    private final RoomServiceAsync roomService;
    private final ArrayList<Room> rooms = new ArrayList<>();
    private PollingService pollingService = new PollingService();
    private final PollingService gameOptionsPollingService = new PollingService();
    private ArrayList<GameOption> cachedGameOptions = null;
    private String pollingForGameId = null;

    @Override
    public void start() {
        History.newItem("");
        checkAdminStatus();
        loadAvailableGames();
        pollingService.startPolling(POLLING_INTERVAL_MS, this::pollServerForRooms);
    }

    @Override
    public void stop() {
        pollingService.stopPolling();
        gameOptionsPollingService.stopPolling();
    }

    public LobbyPresenter(LobbyView view, PresenterManager presenterManager, RoomServiceAsync roomService) {
        this.view = view;
        this.presenterManager = presenterManager;
        this.roomService = roomService;
        bind();
    }

    private void bind() {
        view.setCurrentPlayerId(Cookie.getPlayerId());
        view.addGameSelectionChangeHandler(e -> onGameSelectionChanged());
        view.setDeleteHandler(this::deleteRoomAsAdmin);
        view.getCreateRoomButton().addClickHandler(event -> {
            String roomName = view.getRoomNameInput().getText().trim();
            if (roomName.isEmpty()) {
                AudioPlayer.errorAlert("Room name cannot be empty.");
                return;
            }
            if (roomName.length() < 3) {
                AudioPlayer.errorAlert("Room name must be at least 3 characters.");
                return;
            }
            if (roomName.length() > 20) {
                AudioPlayer.errorAlert("Room name cannot exceed 20 characters.");
                return;
            }
            AudioPlayer.play(AudioPlayer.BUTTON_CLICK);
            createRoom(roomName);
        });
        view.setJoinHandler(room -> {
            if (GameStatus.PLAYING.equals(room.getStatus())) {
                presenterManager.switchToGameRoom(room);
            } else {
                navigateToCharacterSelection(room);
            }
        });
    }

    private void updateRoomTable() {
        view.updateRoomTable(rooms);
    }

    /**
     * Poll the server to get the list of available rooms.
     */
    private void pollServerForRooms() {
        roomService.getRooms(new AsyncCallback<ArrayList<Room>>() {
            @Override
            public void onFailure(Throwable throwable) {
                GWT.log("Failed to fetch rooms: " + throwable.getMessage());
            }

            @Override
            public void onSuccess(ArrayList<Room> fetchedRooms) {
                if(!rooms.equals(fetchedRooms)){
                    GWT.log("update room list table");
                    updateRooms(fetchedRooms);
                    updateRoomTable();
                }
            }
        });
    }

    private void onGameSelectionChanged() {
        startPollingForGameOptions();
    }

    private void startPollingForGameOptions() {
        String gameId = view.getSelectedGameId();
        if (gameId == null || gameId.equals(pollingForGameId)) return;
        pollingForGameId = gameId;
        cachedGameOptions = null;
        gameOptionsPollingService.stopPolling();
        gameOptionsPollingService.startPolling(OPTIONS_POLLING_INTERVAL_MS, () -> fetchGameOptions(gameId));
    }

    private void fetchGameOptions(String gameId) {
        roomService.getGameOptions(gameId, new AsyncCallback<ArrayList<GameOption>>() {
            @Override public void onFailure(Throwable t) { /* retry on next poll */ }
            @Override public void onSuccess(ArrayList<GameOption> options) {
                if (gameId.equals(pollingForGameId)) {
                    cachedGameOptions = options;
                    gameOptionsPollingService.stopPolling();
                }
            }
        });
    }

    private void navigateToGameOptions(Room room) {
        presenterManager.switchToGameOptions(room, cachedGameOptions);
    }

    /**
     * Navigate to the selected room.
     */
    private void navigateToCharacterSelection(Room room) {
        addPlayerIdToRoom(room);
        presenterManager.switchToCharacterSelection(room);
    }

    private void addPlayerIdToRoom(Room room){
        roomService.addPlayerIdToRoom(Cookie.getPlayerId(), room.getId(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable throwable) {
                GWT.log("Failed to add user to room");
            }

            @Override
            public void onSuccess(Void v) {
                GWT.log("Succesfully added user to room");
            }
        });
    }

    private boolean isRoomNameValid(String roomName) {
        return roomName != null && !roomName.trim().isEmpty() && roomName.trim().length() <= 20;
    }

    private void loadAvailableGames() {
        roomService.getAvailableGames(new AsyncCallback<ArrayList<GameDefinition>>() {
            @Override
            public void onFailure(Throwable throwable) {
                GWT.log("Failed to load available games: " + throwable.getMessage());
            }

            @Override
            public void onSuccess(ArrayList<GameDefinition> games) {
                view.populateGameList(games);
                startPollingForGameOptions();
            }
        });
    }

    private void createRoom(String roomName) {
        String gameId = view.getSelectedGameId();
        if (gameId == null) {
            AudioPlayer.errorAlert("Please select a game.");
            return;
        }
        // Fast client-side check against cached list before hitting the server
        boolean nameExists = rooms.stream()
                .anyMatch(r -> r.getName().equalsIgnoreCase(roomName));
        if (nameExists) {
            AudioPlayer.errorAlert("A room with this name already exists.");
            return;
        }
        String playerId = Cookie.getPlayerId();
        Room currentRoom = rooms.stream()
                .filter(r -> r.getPlayerIds().contains(playerId))
                .findFirst().orElse(null);
        if (currentRoom != null) {
            boolean confirmed = Window.confirm(
                    "You are currently in room '" + currentRoom.getName() + "'. " +
                    "Creating a new room will remove you from it. Continue?");
            if (!confirmed) return;
        }
        Room room = new Room(roomName, playerId);
        room.setGameId(gameId);
        view.getRoomNameInput().setText("");
        roomService.createRoom(room, new AsyncCallback<Room>() {
            @Override
            public void onFailure(Throwable t) {
                view.showAlert("Could not create room: " + t.getMessage());
            }
            @Override
            public void onSuccess(Room created) {
                navigateToGameOptions(created);
            }
        });
    }

    private synchronized void updateRooms(ArrayList<Room> fetchedRooms) {
        rooms.clear();
        rooms.addAll(fetchedRooms);
    }

    private void checkAdminStatus() {
        RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, "/admin/check");
        rb.setHeader("Accept", "application/json");
        try {
            rb.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (response.getStatusCode() == Response.SC_OK) {
                        view.setAdminMode(true);
                    }
                }
                @Override
                public void onError(Request request, Throwable exception) {
                    GWT.log("Admin check error: " + exception.getMessage());
                }
            });
        } catch (RequestException e) {
            GWT.log("Admin check failed: " + e.getMessage());
        }
    }

    private void deleteRoomAsAdmin(Room room) {
        if (!Window.confirm("Delete room '" + room.getName() + "'?")) return;
        RequestBuilder rb = new RequestBuilder(RequestBuilder.DELETE, "/admin/rooms/" + room.getId());
        rb.setHeader("Accept", "application/json");
        try {
            rb.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    if (response.getStatusCode() == Response.SC_NO_CONTENT) {
                        // Table updates on next poll automatically
                    } else if (response.getStatusCode() == Response.SC_UNAUTHORIZED
                               || response.getStatusCode() == Response.SC_FORBIDDEN) {
                        view.showAlert("Not authorised — please log in as admin first.");
                    } else {
                        view.showAlert("Delete failed (HTTP " + response.getStatusCode() + ")");
                    }
                }
                @Override
                public void onError(Request request, Throwable exception) {
                    view.showAlert("Delete failed: " + exception.getMessage());
                }
            });
        } catch (RequestException e) {
            GWT.log("Delete room failed: " + e.getMessage());
        }
    }
}
