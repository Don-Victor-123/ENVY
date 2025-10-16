import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

class DateOpsTest {

    private final ZoneId zone = DateOps.DEFAULT_ZONE;
    private final LocalTime shiftStart = DateOps.DEFAULT_SHIFT_START;
    private final LocalTime shiftEnd = DateOps.DEFAULT_SHIFT_END;

    @Test
    void determinesOperationalDateAroundMidnight() {
        LocalDateTime friday1959 = LocalDateTime.of(2025, 10, 10, 19, 59);
        LocalDateTime friday2000 = LocalDateTime.of(2025, 10, 10, 20, 0);
        LocalDateTime friday2359 = LocalDateTime.of(2025, 10, 10, 23, 59);
        LocalDateTime saturday0000 = LocalDateTime.of(2025, 10, 11, 0, 0);
        LocalDateTime saturday0759 = LocalDateTime.of(2025, 10, 11, 7, 59);
        LocalDateTime saturday0800 = LocalDateTime.of(2025, 10, 11, 8, 0);

        assertEquals(LocalDate.of(2025, 10, 10), DateOps.operationalDate(friday1959, zone, shiftStart));
        assertEquals(LocalDate.of(2025, 10, 10), DateOps.operationalDate(friday2000, zone, shiftStart));
        assertEquals(LocalDate.of(2025, 10, 10), DateOps.operationalDate(friday2359, zone, shiftStart));
        assertEquals(LocalDate.of(2025, 10, 10), DateOps.operationalDate(saturday0000, zone, shiftStart));
        assertEquals(LocalDate.of(2025, 10, 10), DateOps.operationalDate(saturday0759, zone, shiftStart));
        assertEquals(LocalDate.of(2025, 10, 11), DateOps.operationalDate(saturday0800, zone, shiftStart));
    }

    @Test
    void computesOperationalWindow() {
        LocalDate opDate = LocalDate.of(2025, 10, 10);
        ZonedDateTime start = DateOps.windowStart(opDate, zone, shiftStart);
        ZonedDateTime end = DateOps.windowEnd(opDate, zone, shiftStart, shiftEnd);

        assertEquals(LocalDateTime.of(2025, 10, 10, 20, 0), start.toLocalDateTime());
        assertEquals(LocalDateTime.of(2025, 10, 11, 8, 0), end.toLocalDateTime());
        assertTrue(DateOps.isWithinOperationalWindow(start, opDate, zone, shiftStart, shiftEnd));
        assertTrue(DateOps.isWithinOperationalWindow(end.minusSeconds(1), opDate, zone, shiftStart, shiftEnd));
    }
}
