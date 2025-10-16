import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Utilidades relacionadas con el "día operativo" que cruza medianoche.
 */
public final class DateOps {

    /** Zona predeterminada del proyecto. */
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Mexico_City");
    /** Hora de inicio de la jornada (configurable). */
    public static final LocalTime DEFAULT_SHIFT_START = LocalTime.of(20, 0);
    /** Hora de fin de la jornada (configurable). */
    public static final LocalTime DEFAULT_SHIFT_END = LocalTime.of(8, 0);

    private DateOps() {
    }

    /**
     * Determina el "día operativo" (DO) para un instante concreto.
     *
     * @param timestamp instante evaluado
     * @param zone zona horaria fija
     * @param shiftStart hora de inicio de la jornada
     * @return fecha (LocalDate) correspondiente al DO
     */
    public static LocalDate operationalDate(LocalDateTime timestamp, ZoneId zone, LocalTime shiftStart) {
        return operationalDate(timestamp, zone, shiftStart, DEFAULT_SHIFT_END);
    }

    public static LocalDate operationalDate(LocalDateTime timestamp, ZoneId zone, LocalTime shiftStart,
            LocalTime shiftEnd) {
        Objects.requireNonNull(timestamp, "timestamp");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(shiftStart, "shiftStart");
        Objects.requireNonNull(shiftEnd, "shiftEnd");

        ZonedDateTime zoned = timestamp.atZone(zone);
        LocalDate date = zoned.toLocalDate();
        LocalTime time = zoned.toLocalTime();

        if (shiftStart.equals(shiftEnd)) {
            return date;
        }

        if (shiftStart.isAfter(shiftEnd)) { // cruza medianoche
            if (time.isBefore(shiftEnd)) {
                return date.minusDays(1);
            }
            return date;
        }

        if (!time.isBefore(shiftStart)) {
            return date;
        }
        return date.minusDays(1);
    }

    /**
     * Inicio (incluyente) de la jornada correspondiente a {@code opDate}.
     */
    public static ZonedDateTime windowStart(LocalDate opDate, ZoneId zone, LocalTime shiftStart) {
        Objects.requireNonNull(opDate, "opDate");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(shiftStart, "shiftStart");
        return ZonedDateTime.of(opDate, shiftStart, zone);
    }

    /**
     * Fin (exclusivo) de la jornada correspondiente a {@code opDate}.
     */
    public static ZonedDateTime windowEnd(LocalDate opDate, ZoneId zone, LocalTime shiftStart, LocalTime shiftEnd) {
        Objects.requireNonNull(opDate, "opDate");
        Objects.requireNonNull(zone, "zone");
        Objects.requireNonNull(shiftStart, "shiftStart");
        Objects.requireNonNull(shiftEnd, "shiftEnd");

        LocalDate endDate = shiftEnd.isAfter(shiftStart) || shiftEnd.equals(shiftStart)
                ? opDate
                : opDate.plusDays(1);
        return ZonedDateTime.of(endDate, shiftEnd, zone);
    }

    /**
     * Verifica si un instante cae dentro del rango operativo [{@code start}, {@code end}).
     */
    public static boolean isWithinOperationalWindow(ZonedDateTime timestamp, LocalDate opDate, ZoneId zone,
            LocalTime shiftStart, LocalTime shiftEnd) {
        Objects.requireNonNull(timestamp, "timestamp");
        ZonedDateTime start = windowStart(opDate, zone, shiftStart);
        ZonedDateTime end = windowEnd(opDate, zone, shiftStart, shiftEnd);
        return !timestamp.isBefore(start) && timestamp.isBefore(end);
    }
}
