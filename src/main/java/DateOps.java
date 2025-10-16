import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Utility methods related to the calculation of the "día operativo" (operational day).
 */
public final class DateOps {
    private DateOps() {
    }

    /**
     * Calculates the operational date for a timestamp according to the shift start time.
     * <p>
     * The operational date matches the calendar date of the shift start if the timestamp is on or after the
     * shift start. Otherwise, the operational date is the previous calendar day.
     * </p>
     *
     * @param timestamp the timestamp to classify
     * @param zone the zone identifier to use
     * @param shiftStart the start time of the overnight shift (e.g. 20:00)
     * @return the operational date
     */
    public static LocalDate operationalDate(LocalDateTime timestamp, ZoneId zone, LocalTime shiftStart) {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(shiftStart, "shiftStart");

        ZonedDateTime zonedTimestamp = timestamp.atZone(zone);
        ZonedDateTime shiftStartToday = resolveShiftBoundary(zonedTimestamp.toLocalDate(), shiftStart, zone);

        if (!zonedTimestamp.isBefore(shiftStartToday)) {
            return shiftStartToday.toLocalDate();
        }

        return shiftStartToday.toLocalDate().minusDays(1);
    }

    /**
     * Resolves the exact ZonedDateTime of a shift boundary, dealing with daylight saving gaps or overlaps.
     */
    public static ZonedDateTime resolveShiftBoundary(LocalDate date, LocalTime boundary, ZoneId zone) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(boundary, "boundary");
        Objects.requireNonNull(zone, "zone");

        LocalDateTime candidate = LocalDateTime.of(date, boundary);
        var rules = zone.getRules();
        var offsets = rules.getValidOffsets(candidate);
        if (offsets.size() == 1) {
            return ZonedDateTime.of(candidate, zone);
        }
        if (offsets.isEmpty()) {
            var transition = rules.getTransition(candidate);
            if (transition == null) {
                throw new IllegalStateException("Unable to resolve shift boundary for " + candidate + " in zone " + zone);
            }
            // Gap: use the transition after the gap so that we never go backwards in time.
            return transition.getDateTimeAfter().atZone(zone);
        }
        // Ambiguous (overlap): take the earlier offset to preserve chronological ordering.
        ZoneOffset chosenOffset = offsets.stream().min(ZoneOffset::compareTo).orElseThrow();
        return ZonedDateTime.ofInstant(candidate.toInstant(chosenOffset), zone);
    }
}
