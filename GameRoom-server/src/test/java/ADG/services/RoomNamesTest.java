package ADG.services;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoomNamesTest {

    private static final int MAX_LENGTH = 25;

    @Test
    void noRoomNameExceedsMaxLength() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream("room-names.txt");
        assertThat(is).as("room-names.txt must be on the classpath").isNotNull();

        List<String> violations = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                if (trimmed.length() > MAX_LENGTH) {
                    violations.add(String.format("  \"%s\" (%d chars)", trimmed, trimmed.length()));
                }
            }
        }

        assertThat(violations)
                .as("Room names exceeding %d characters:\n%s", MAX_LENGTH, String.join("\n", violations))
                .isEmpty();
    }
}