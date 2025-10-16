import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public final class ReprintService {
    private final CorteService corteService;

    public ReprintService(CorteService corteService) {
        this.corteService = Objects.requireNonNull(corteService, "corteService");
    }

    public void reprint(String input) {
        Objects.requireNonNull(input, "input");
        LocalDate date;
        try {
            date = LocalDate.parse(input);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Fecha inválida (usar YYYY-MM-DD)", ex);
        }
        corteService.imprimirExistente(date);
    }
}
