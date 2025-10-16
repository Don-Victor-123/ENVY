import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Objects;

public final class ShiftConfiguration {
    private final ZoneId zone;
    private final LocalTime shiftStart;
    private final LocalTime shiftEnd;

    public ShiftConfiguration(ZoneId zone, LocalTime shiftStart, LocalTime shiftEnd) {
        this.zone = Objects.requireNonNull(zone, "zone");
        this.shiftStart = Objects.requireNonNull(shiftStart, "shiftStart");
        this.shiftEnd = Objects.requireNonNull(shiftEnd, "shiftEnd");
    }

    public ZoneId zone() {
        return zone;
    }

    public LocalTime shiftStart() {
        return shiftStart;
    }

    public LocalTime shiftEnd() {
        return shiftEnd;
    }
}
