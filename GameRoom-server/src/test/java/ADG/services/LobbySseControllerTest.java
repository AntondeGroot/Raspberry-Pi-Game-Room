package ADG.services;

import ADG.Lobby.GameStatus;
import ADG.Lobby.Room;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LobbySseControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private RoomStore roomStore;

    @Autowired
    private LobbySseRegistry lobbySseRegistry;

    @BeforeEach
    void clearRooms() {
        roomStore.rooms.clear();
    }

    // ── endpoint contract ────────────────────────────────────────────────────

    @Test
    void setsXAccelBufferingNoHeader() throws Exception {
        HttpURLConnection conn = openSse("/lobby/stream");
        assertThat(conn.getResponseCode()).isEqualTo(200);
        assertThat(conn.getHeaderField("X-Accel-Buffering")).isEqualTo("no");
        conn.disconnect();
    }

    @Test
    void contentTypeIsTextEventStream() throws Exception {
        HttpURLConnection conn = openSse("/lobby/stream");
        assertThat(conn.getResponseCode()).isEqualTo(200);
        assertThat(conn.getHeaderField("Content-Type")).contains("text/event-stream");
        conn.disconnect();
    }

    // ── initial event ────────────────────────────────────────────────────────

    @Test
    void sendsEmptyArrayWhenNoRooms() throws Exception {
        HttpURLConnection conn = openSse("/lobby/stream");
        String dataLine = firstDataLine(conn);
        assertThat(dataLine).startsWith("data:");
        assertThat(dataLine).contains("[]");
        conn.disconnect();
    }

    @Test
    void sendsWaitingRoomsInInitialEvent() throws Exception {
        roomStore.rooms.add(buildRoom("Visible Room", GameStatus.WAITING));
        HttpURLConnection conn = openSse("/lobby/stream");
        String dataLine = firstDataLine(conn);
        assertThat(dataLine).contains("Visible Room");
        conn.disconnect();
    }

    @Test
    void filtersPendingRoomsFromInitialEvent() throws Exception {
        roomStore.rooms.add(buildRoom("Hidden Room", GameStatus.PENDING));
        HttpURLConnection conn = openSse("/lobby/stream");
        String dataLine = firstDataLine(conn);
        assertThat(dataLine).doesNotContain("Hidden Room");
        conn.disconnect();
    }

    // ── push update ──────────────────────────────────────────────────────────

    @Test
    void emitSendsUpdatesToSubscribers() throws Exception {
        HttpURLConnection conn = openSse("/lobby/stream");
        // consume initial event (data line + empty separator)
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            reader.readLine(); // data:[]
            reader.readLine(); // empty line

            Room newRoom = buildRoom("New Room", GameStatus.WAITING);
            lobbySseRegistry.emit(List.of(newRoom));

            String dataLine = reader.readLine();
            assertThat(dataLine).contains("New Room");
        }
        conn.disconnect();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private HttpURLConnection openSse(String path) throws Exception {
        URL url = new URL("http://localhost:" + port + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Accept", "text/event-stream");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.connect();
        return conn;
    }

    private String firstDataLine(HttpURLConnection conn) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return reader.readLine();
        }
    }

    private Room buildRoom(String name, GameStatus status) {
        Room r = new Room();
        r.setId(UUID.randomUUID().toString());
        r.setName(name);
        r.setCreatedByUserId("creator-1");
        r.setStatus(status);
        r.setGameId("keezen");
        return r;
    }
}