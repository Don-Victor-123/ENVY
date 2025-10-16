import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class TicketRepository {
    private final Clock clock;
    private final ShiftConfiguration shiftConfiguration;

    public TicketRepository(Clock clock, ShiftConfiguration shiftConfiguration) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.shiftConfiguration = Objects.requireNonNull(shiftConfiguration, "shiftConfiguration");
    }

    public TicketRecord save(String id, BigDecimal total, String body) {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(shiftConfiguration.zone());
        LocalDate operationalDate = DateOps.operationalDate(now.toLocalDateTime(), shiftConfiguration.zone(), shiftConfiguration.shiftStart());
        Path ticketDir = PathResolver.ticketsDir(operationalDate);
        PathResolver.ensureExists(ticketDir);

        TicketRecord record = new TicketRecord(id, now, total, body);
        Path file = ticketDir.resolve(TicketFormatter.fileName(record) + ".txt");
        try {
            Files.writeString(file, TicketFormatter.serialize(record), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store ticket " + id, ex);
        }
        return record;
    }

    public List<TicketRecord> loadFor(LocalDate operationalDate) {
        Objects.requireNonNull(operationalDate, "operationalDate");
        List<TicketRecord> tickets = new ArrayList<>();
        loadDirectory(PathResolver.ticketsDir(operationalDate), tickets);

        // Compatibility for historical data that might have been split across calendar days.
        Path fallback = PathResolver.ticketsDir(operationalDate.plusDays(1));
        if (tickets.isEmpty()) {
            loadDirectory(fallback, tickets);
        } else {
            loadDirectory(fallback, tickets);
        }

        tickets.sort(Comparator.comparing(TicketRecord::timestamp));
        return tickets;
    }

    private void loadDirectory(Path dir, List<TicketRecord> into) {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (var stream = Files.list(dir)) {
            into.addAll(stream
                    .filter(Files::isRegularFile)
                    .sorted()
                    .map(this::readTicket)
                    .collect(Collectors.toList()));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read tickets from " + dir, ex);
        }
    }

    private TicketRecord readTicket(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String fileName = path.getFileName().toString();
            if (fileName.endsWith(".txt")) {
                fileName = fileName.substring(0, fileName.length() - 4);
            }
            return TicketFormatter.parse(fileName, content, shiftConfiguration.zone());
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read ticket file " + path, ex);
        }
    }
}
