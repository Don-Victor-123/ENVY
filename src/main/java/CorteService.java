import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class CorteService {
    private static final DateTimeFormatter HUMAN = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);

    private final ShiftConfiguration shiftConfiguration;
    private final TicketRepository ticketRepository;
    private final Printer printer;

    public CorteService(ShiftConfiguration shiftConfiguration, TicketRepository ticketRepository, Printer printer) {
        this.shiftConfiguration = Objects.requireNonNull(shiftConfiguration, "shiftConfiguration");
        this.ticketRepository = Objects.requireNonNull(ticketRepository, "ticketRepository");
        this.printer = Objects.requireNonNull(printer, "printer");
    }

    public void generarCorte(LocalDate operationalDate) {
        String corte = buildCorte(operationalDate);
        Path dir = PathResolver.cortesDir(operationalDate);
        PathResolver.ensureExists(dir);
        Path file = dir.resolve("corte.txt");
        try {
            Files.writeString(file, corte, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to store corte for " + operationalDate, ex);
        }
        printer.print(corte);
    }

    public void imprimirExistente(LocalDate operationalDate) {
        Path file = locateExistingCorte(operationalDate);
        if (file == null || !Files.exists(file)) {
            generarCorte(operationalDate);
            return;
        }
        try {
            printer.print(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read corte file " + file, ex);
        }
    }

    private Path locateExistingCorte(LocalDate operationalDate) {
        Path primary = PathResolver.cortesDir(operationalDate).resolve("corte.txt");
        if (Files.exists(primary)) {
            return primary;
        }
        Path legacy = PathResolver.cortesDir(operationalDate.plusDays(1)).resolve("corte.txt");
        if (Files.exists(legacy)) {
            return legacy;
        }
        return null;
    }

    public String buildCorte(LocalDate operationalDate) {
        ShiftWindow window = ShiftWindow.of(operationalDate, shiftConfiguration.zone(), shiftConfiguration.shiftStart(), shiftConfiguration.shiftEnd());
        List<TicketRecord> tickets = ticketRepository.loadFor(operationalDate).stream()
                .filter(ticket -> window.contains(ticket.timestamp().withZoneSameInstant(shiftConfiguration.zone())))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("CORTE DEL DÍA OPERATIVO ").append(operationalDate).append('\n');
        sb.append("RANGO: ")
                .append(HUMAN.format(window.start()))
                .append(" → ")
                .append(HUMAN.format(window.endExclusive()))
                .append(' ')
                .append(shiftConfiguration.zone().getId())
                .append('\n');
        sb.append("TICKETS: ").append(tickets.size()).append('\n');

        BigDecimal total = BigDecimal.ZERO;
        for (TicketRecord ticket : tickets) {
            sb.append('\n').append(TicketFormatter.toPrintableText(ticket));
            total = total.add(ticket.total());
        }
        sb.append('\n').append("TOTAL: ").append(total);
        sb.append('\n');
        return sb.toString();
    }
}
