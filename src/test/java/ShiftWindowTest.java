import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class ShiftWindowTest {
    private static final ZoneId ZONE = ZoneId.of("America/Mexico_City");

    @Test
    void computesWindowAcrossMidnight() {
        ShiftWindow window = ShiftWindow.of(LocalDate.of(2025, 10, 10), ZONE, LocalTime.of(20, 0), LocalTime.of(8, 0));
        assertEquals("2025-10-10T20:00", window.start().toLocalDateTime().toString());
        assertEquals("2025-10-11T08:00", window.endExclusive().toLocalDateTime().toString());
        assertTrue(window.contains(window.start()));
        assertTrue(window.contains(window.endExclusive().minusSeconds(1)));
    }
}
