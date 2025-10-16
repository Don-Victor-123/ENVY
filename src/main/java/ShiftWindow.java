import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Represents the start/end instants of a night shift for a specific operational date.
 */
public final class ShiftWindow {
    private final ZonedDateTime start;
    private final ZonedDateTime endExclusive;

    private ShiftWindow(ZonedDateTime start, ZonedDateTime endExclusive) {
        this.start = start;
        this.endExclusive = endExclusive;
    }

    public ZonedDateTime start() {
        return start;
    }

    public ZonedDateTime endExclusive() {
        return endExclusive;
    }

    public static ShiftWindow of(LocalDate operationalDate, ZoneId zone, LocalTime shiftStart, LocalTime shiftEnd) {
        Objects.requireNonNull(operationalDate, "operationalDate");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(shiftStart, "shiftStart");
        Objects.requireNonNull(shiftEnd, "shiftEnd");

        ZonedDateTime start = DateOps.resolveShiftBoundary(operationalDate, shiftStart, zone);
        LocalDate endDate = !shiftEnd.isBefore(shiftStart) ? operationalDate : operationalDate.plusDays(1);
        ZonedDateTime end = DateOps.resolveShiftBoundary(endDate, shiftEnd, zone);

        if (!end.isAfter(start)) {
            end = end.plusDays(1);
        }

        return new ShiftWindow(start, end);
    }

    public boolean contains(ZonedDateTime timestamp) {
        return !timestamp.isBefore(start) && timestamp.isBefore(endExclusive);
    }
}
