import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Resuelve rutas físicas basadas en el "día operativo".
 */
public final class PathResolver {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private PathResolver() {
    }

    public static Path cortesDir(LocalDate opDate) {
        Objects.requireNonNull(opDate, "opDate");
        return baseDir().resolve(ISO.format(opDate));
    }

    public static Path ticketsDir(LocalDate opDate) {
        return cortesDir(opDate);
    }

    public static Path ticketsFile(LocalDate opDate) {
        return ticketsDir(opDate).resolve("tickets.jsonl");
    }

    public static Path ultimoFolioFile(LocalDate opDate) {
        return ticketsDir(opDate).resolve("ultimo_folio.txt");
    }

    public static Path corteFlag(LocalDate opDate) {
        return cortesDir(opDate).resolve("corte_hecho.flag");
    }

    public static Path corteTxt(LocalDate opDate) {
        return cortesDir(opDate).resolve("corte.txt");
    }

    /**
     * Ruta al archivo de tickets guardado por fecha calendario (histórico).
     */
    public static Path legacyTicketsFile(LocalDate calendarDate) {
        Objects.requireNonNull(calendarDate, "calendarDate");
        return cortesDir(calendarDate).resolve("tickets.jsonl");
    }

    /**
     * Rutas candidatas a contener tickets de un mismo día operativo (nuevo esquema + legado).
     */
    public static List<Path> candidateTicketFiles(LocalDate opDate) {
        Objects.requireNonNull(opDate, "opDate");
        List<Path> paths = new ArrayList<>();
        paths.add(ticketsFile(opDate));
        paths.add(legacyTicketsFile(opDate.plusDays(1))); // madrugada almacenada con fecha calendario siguiente
        return paths;
    }

    private static Path baseDir() {
        String override = System.getProperty("envy.cortes.dir");
        if (override != null && !override.isBlank()) {
            return Paths.get(override);
        }
        return Paths.get("cortes");
    }
}
