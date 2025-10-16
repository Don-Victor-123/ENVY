import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Objects;

public final class TicketRecord {
    private final String id;
    private final ZonedDateTime timestamp;
    private final BigDecimal total;
    private final String body;

    public TicketRecord(String id, ZonedDateTime timestamp, BigDecimal total, String body) {
        this.id = Objects.requireNonNull(id, "id");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
        this.total = Objects.requireNonNull(total, "total");
        this.body = Objects.requireNonNull(body, "body");
    }

    public String id() {
        return id;
    }

    public ZonedDateTime timestamp() {
        return timestamp;
    }

    public BigDecimal total() {
        return total;
    }

    public String body() {
        return body;
    }
}
