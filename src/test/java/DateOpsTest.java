import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class DateOpsTest {
    private static final ZoneId ZONE = ZoneId.of("America/Mexico_City");
    private static final LocalTime SHIFT_START = LocalTime.of(20, 0);

    @Test
    void operationalDateBeforeShift() {
        LocalDateTime timestamp = LocalDateTime.of(2025, 10, 11, 7, 59);
        LocalDate opDate = DateOps.operationalDate(timestamp, ZONE, SHIFT_START);
        assertEquals(LocalDate.of(2025, 10, 10), opDate);
    }

    @Test
    void operationalDateAfterShift() {
        LocalDateTime timestamp = LocalDateTime.of(2025, 10, 10, 21, 15);
        LocalDate opDate = DateOps.operationalDate(timestamp, ZONE, SHIFT_START);
        assertEquals(LocalDate.of(2025, 10, 10), opDate);
    }

    @Test
    void operationalDateDstGap() {
        // The DST jump in Mexico City during 2025 occurs on April 6th at 02:00
        LocalDateTime timestamp = LocalDateTime.of(2025, 4, 6, 1, 30);
        LocalDate opDate = DateOps.operationalDate(timestamp, ZONE, SHIFT_START);
        assertEquals(LocalDate.of(2025, 4, 5), opDate);
    }
}
