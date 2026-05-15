package ADG.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChatSseControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    // ── endpoint contract ────────────────────────────────────────────────────

    @Test
    void setsXAccelBufferingNoHeader() throws Exception {
        HttpURLConnection conn = openSse("/chat/sse-room-1/stream");
        assertThat(conn.getResponseCode()).isEqualTo(200);
        assertThat(conn.getHeaderField("X-Accel-Buffering")).isEqualTo("no");
        conn.disconnect();
    }

    @Test
    void contentTypeIsTextEventStream() throws Exception {
        HttpURLConnection conn = openSse("/chat/sse-room-2/stream");
        assertThat(conn.getResponseCode()).isEqualTo(200);
        assertThat(conn.getHeaderField("Content-Type")).contains("text/event-stream");
        conn.disconnect();
    }

    // ── initial event ────────────────────────────────────────────────────────

    @Test
    void sendsInitialEmptyArrayForNewRoom() throws Exception {
        HttpURLConnection conn = openSse("/chat/sse-room-3/stream");
        assertThat(conn.getResponseCode()).isEqualTo(200);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            assertThat(reader.readLine()).startsWith("data:").contains("[]");
        }
        conn.disconnect();
    }

    // ── push update ──────────────────────────────────────────────────────────

    @Test
    void postMessagePushesUpdateToSubscribers() throws Exception {
        String roomId = "sse-room-4";
        HttpURLConnection conn = openSse("/chat/" + roomId + "/stream");
        assertThat(conn.getResponseCode()).isEqualTo(200);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            reader.readLine(); // data:[]
            reader.readLine(); // empty separator

            postMessage(roomId, "Alice", "Hello SSE");

            String dataLine = reader.readLine();
            assertThat(dataLine).contains("Hello SSE").contains("Alice");
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

    private void postMessage(String roomId, String sender, String message) throws Exception {
        URL url = new URL("http://localhost:" + port + "/chat/" + roomId);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        conn.setDoOutput(true);
        String body = objectMapper.writeValueAsString(Map.of("sender", sender, "message", message));
        conn.getOutputStream().write(body.getBytes());
        conn.getResponseCode(); // trigger the request
        conn.disconnect();
    }
}