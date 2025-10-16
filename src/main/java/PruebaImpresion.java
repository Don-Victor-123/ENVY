import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

public final class PruebaImpresion {
    private static final ZoneId ZONE = ZoneId.of("America/Mexico_City");
    private static final LocalTime SHIFT_START = LocalTime.of(20, 0);
    private static final LocalTime SHIFT_END = LocalTime.of(8, 0);

    public static void main(String[] args) {
        ShiftConfiguration configuration = new ShiftConfiguration(ZONE, SHIFT_START, SHIFT_END);
        TicketRepository repository = new TicketRepository(Clock.system(ZONE), configuration);
        Printer printer = new ConsolePrinter();
        CorteService corteService = new CorteService(configuration, repository, printer);

        LocalDate today = LocalDate.now(ZONE);
        corteService.generarCorte(today);
    }
}
