import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Centralizes folder naming using the operational date as part of the path.
 */
public final class PathResolver {
    private static final Path DATA_ROOT = Path.of("data");
    private static final Path CORTES_ROOT = DATA_ROOT.resolve("cortes");
    private static final Path TICKETS_ROOT = DATA_ROOT.resolve("tickets");

    private PathResolver() {
    }

    public static Path cortesDir(LocalDate operationalDate) {
        Objects.requireNonNull(operationalDate, "operationalDate");
        return CORTES_ROOT.resolve(operationalDate.toString());
    }

    public static Path ticketsDir(LocalDate operationalDate) {
        Objects.requireNonNull(operationalDate, "operationalDate");
        return TICKETS_ROOT.resolve(operationalDate.toString());
    }

    public static void ensureExists(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot create directory " + directory, ex);
        }
    }
}
